package io.github.altkat.BuffedItems.manager.visual;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.data.visual.*;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PassiveVisualsManager {

    private final BuffedItems plugin;
    
    // Tracking active items per player: UUID -> Map<Slot, ItemId>
    private final Map<UUID, Map<String, String>> playerEquippedItems = new ConcurrentHashMap<>();

    // Tracking when items were equipped: UUID -> Map<Slot, EquipTimestamp (Millis)>
    private final Map<UUID, Map<String, Long>> slotEquipTimes = new ConcurrentHashMap<>();
    
    // Persistent BossBars: UUID -> Map<Identifier, BossBar>
    private final Map<UUID, Map<String, BossBar>> activeBossBars = new ConcurrentHashMap<>();
    
    // Tracking active ON_EQUIP BossBars to prevent spam: UUID -> Map<ItemId, OneTimeBossBarInfo>
    private final Map<UUID, Map<String, OneTimeBossBarInfo>> activeOneTimeBossBars = new ConcurrentHashMap<>();
    
    private long currentTick = 0;
    private final List<String> slotPriority = Arrays.asList("MAIN_HAND", "OFF_HAND", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS");

    private BukkitTask particleTask;
    private BukkitTask actionBarTask;


    public PassiveVisualsManager(BuffedItems plugin) {
        this.plugin = plugin;
        startActionBarTask();
        startParticleTask();
    }

    private void startParticleTask() {
        this.particleTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            currentTick++;
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                Map<String, String> items = playerEquippedItems.get(uuid);
                Map<String, Long> equipTimes = slotEquipTimes.get(uuid);
                
                if (items == null || items.isEmpty()) continue;

                // Priority logic
                String highestPrioritySlot = null;
                BuffedItem priorityItem = null;

                for (String slot : slotPriority) {
                    if (items.containsKey(slot)) {
                        String itemId = items.get(slot);
                        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
                        if (item != null && item.getPassiveVisuals() != null) {
                            // Check if this item has ANY continuous particles
                            boolean hasContinuous = item.getPassiveVisuals().getParticles().stream()
                                    .anyMatch(p -> p.getTriggerMode() == VisualTriggerMode.CONTINUOUS);
                            
                            if (hasContinuous) {
                                highestPrioritySlot = slot;
                                priorityItem = item;
                                break; // Found the highest priority item with particles, stop searching
                            }
                        }
                    }
                }

                if (highestPrioritySlot != null && priorityItem != null) {
                    long equipTime = (equipTimes != null && equipTimes.containsKey(highestPrioritySlot)) ? equipTimes.get(highestPrioritySlot) : 0;
                    long currentTime = System.currentTimeMillis();

                    for (io.github.altkat.BuffedItems.utility.item.data.particle.ParticleDisplay display : priorityItem.getPassiveVisuals().getParticles()) {
                        // Only process CONTINUOUS particles
                        if (display.getTriggerMode() != VisualTriggerMode.CONTINUOUS) continue;

                        // Check delay
                        if (display.getDelay() > 0) {
                            long delayInMillis = display.getDelay() * 50L;
                            if (currentTime - equipTime < delayInMillis) {
                                continue;
                            }
                        }
                        ParticleEngine.spawn(player.getLocation().add(0, 1, 0), display, currentTick);
                    }
                }
            }
        }, 0L, 2L); // Every 2 ticks
    }

    /**
     * Updates visuals for a player based on their currently active items.
     * Called from EffectApplicatorTask.
     */
    public void updatePlayerVisuals(Player player, List<ActiveItemInfo> currentActiveItems) {
        UUID uuid = player.getUniqueId();
        boolean isFirstLoad = !playerEquippedItems.containsKey(uuid);
        Map<String, String> lastItems = playerEquippedItems.getOrDefault(uuid, new HashMap<>());
        Map<String, String> currentItemsMap = new HashMap<>();

        Map<String, Long> equipTimes = slotEquipTimes.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        Set<String> newlyEquippedSlots = new HashSet<>();
        long now = System.currentTimeMillis();
        
        for (ActiveItemInfo info : currentActiveItems) {
            currentItemsMap.put(info.slot, info.item.getId());
            
            if (!lastItems.containsKey(info.slot) || !lastItems.get(info.slot).equals(info.item.getId())) {
                newlyEquippedSlots.add(info.slot);
                equipTimes.put(info.slot, now); // Update equip time
            }
        }
        
        // Cleanup old equip times for empty slots
        equipTimes.keySet().removeIf(slot -> !currentItemsMap.containsKey(slot));

        // Handle ON_EQUIP visuals
        if (!isFirstLoad) {
            for (ActiveItemInfo info : currentActiveItems) {
                if (newlyEquippedSlots.contains(info.slot)) {
                    handleOnEquip(player, info.item);
                }
            }
        }

        // Handle Continuous BossBars (Add/Remove)
        updateContinuousBossBars(player, currentActiveItems);

        // Update tracking
        playerEquippedItems.put(uuid, currentItemsMap);
    }

    private net.kyori.adventure.bossbar.BossBar.Overlay convertStyle(org.bukkit.boss.BarStyle style) {
        return switch (style) {
            case SOLID -> net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS;
            case SEGMENTED_6 -> net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_6;
            case SEGMENTED_10 -> net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_10;
            case SEGMENTED_12 -> net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_12;
            case SEGMENTED_20 -> net.kyori.adventure.bossbar.BossBar.Overlay.NOTCHED_20;
        };
    }

    private void handleOnEquip(Player player, BuffedItem item) {
        PassiveVisuals visuals = item.getPassiveVisuals();
        
        // 1. Title (Always On Equip)
        TitleSettings titleSettings = visuals.getTitle();
        if (titleSettings.isEnabled() && (titleSettings.getHeader() != null || titleSettings.getSubtitle() != null)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String header = titleSettings.getHeader() != null ? plugin.getHookManager().processPlaceholders(player, titleSettings.getHeader()) : "";
                String sub = titleSettings.getSubtitle() != null ? plugin.getHookManager().processPlaceholders(player, titleSettings.getSubtitle()) : "";
                
                Title.Times times = Title.Times.times(
                        java.time.Duration.ofMillis(titleSettings.getFadeIn() * 50L),
                        java.time.Duration.ofMillis(titleSettings.getStay() * 50L),
                        java.time.Duration.ofMillis(titleSettings.getFadeOut() * 50L)
                );
                Title title = Title.title(ConfigManager.fromLegacy(header), ConfigManager.fromLegacy(sub), times);
                player.showTitle(title);
            }, titleSettings.getDelay());
        }

        // 2. Sound (Always On Equip)
        SoundSettings soundSettings = visuals.getSound();
        if (soundSettings.isEnabled() && soundSettings.getSound() != null && !soundSettings.getSound().equalsIgnoreCase("NONE")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String soundName = soundSettings.getSound();
                if (soundName == null) return;

                try {
                    String[] parts = soundName.split(";");
                    String soundId = parts[0];
                    float vol = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                    float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;

                    try {
                        Sound soundEnum = Sound.valueOf(soundId.toUpperCase());
                        player.playSound(player.getLocation(), soundEnum, vol, pitch);
                         ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "Played sound via ENUM: " + soundId);
                    } catch (IllegalArgumentException e) {
                        player.playSound(player.getLocation(), soundId.toLowerCase(), vol, pitch);
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "Played sound via STRING: " + soundId);
                    }
                } catch (Exception e) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "Failed to parse or play passive sound: '" + soundSettings.getSound() + "' - Error: " + e.getMessage());
                }
            }, soundSettings.getDelay());
        }

        // 3. One-time BossBar (Only for ON_EQUIP mode)
        BossBarSettings bossBarSettings = visuals.getBossBar();
        if (bossBarSettings.getTriggerMode() == VisualTriggerMode.ON_EQUIP && bossBarSettings.isEnabled()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                UUID uuid = player.getUniqueId();
                String itemId = item.getId();
                Map<String, OneTimeBossBarInfo> playerOneTimeBars = activeOneTimeBossBars.computeIfAbsent(uuid, k -> new HashMap<>());

                String titleStr = bossBarSettings.getTitle() != null ? bossBarSettings.getTitle() : item.getItemDisplay().getDisplayName();
                titleStr = plugin.getHookManager().processPlaceholders(player, titleStr);
                Component titleComp = ConfigManager.fromLegacy(titleStr);

                BossBar bar;
                if (playerOneTimeBars.containsKey(itemId)) {
                    OneTimeBossBarInfo info = playerOneTimeBars.get(itemId);
                    bar = info.bar;
                    bar.name(titleComp); 
                    
                    if (info.removalTask != null && !info.removalTask.isCancelled()) {
                        info.removalTask.cancel();
                    }
                } else {
                    bar = BossBar.bossBar(
                            titleComp,
                            1.0f,
                            BossBar.Color.valueOf(bossBarSettings.getColor().name()),
                            convertStyle(bossBarSettings.getStyle())
                    );
                    player.showBossBar(bar);
                }

                org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.hideBossBar(bar);
                    if (activeOneTimeBossBars.containsKey(uuid)) {
                        activeOneTimeBossBars.get(uuid).remove(itemId);
                    }
                }, bossBarSettings.getDuration() * 20L);

                playerOneTimeBars.put(itemId, new OneTimeBossBarInfo(bar, task));
            }, bossBarSettings.getDelay());
        }
        
        // 4. One-time ActionBar (Only for ON_EQUIP mode)
        ActionBarSettings actionBarSettings = visuals.getActionBar();
        if (actionBarSettings.getTriggerMode() == VisualTriggerMode.ON_EQUIP && actionBarSettings.isEnabled() && actionBarSettings.getMessage() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String msg = plugin.getHookManager().processPlaceholders(player, actionBarSettings.getMessage());
                player.sendActionBar(ConfigManager.fromLegacy(msg));
            }, actionBarSettings.getDelay());
        }

        // 5. Particles (ON_EQUIP)
        if (!visuals.getParticles().isEmpty()) {
            for (io.github.altkat.BuffedItems.utility.item.data.particle.ParticleDisplay display : visuals.getParticles()) {
                if (display.getTriggerMode() == VisualTriggerMode.ON_EQUIP) {
                    ParticleEngine.spawnScheduled(plugin, player, display);
                }
            }
        }
    }

    private void updateContinuousBossBars(Player player, List<ActiveItemInfo> currentActiveItems) {
        UUID uuid = player.getUniqueId();
        Map<String, BossBar> playerBars = activeBossBars.computeIfAbsent(uuid, k -> new HashMap<>());
        
        Set<String> currentIdentifiers = new HashSet<>();
        
        for (ActiveItemInfo info : currentActiveItems) {
            PassiveVisuals visuals = info.item.getPassiveVisuals();
            
            BossBarSettings bbSettings = visuals.getBossBar();
            if (bbSettings.isEnabled() && bbSettings.getTriggerMode() == VisualTriggerMode.CONTINUOUS) {
                String identifier = info.slot + ":" + info.item.getId();
                currentIdentifiers.add(identifier);
                
                String titleStr = bbSettings.getTitle() != null ? bbSettings.getTitle() : info.item.getItemDisplay().getDisplayName();
                titleStr = plugin.getHookManager().processPlaceholders(player, titleStr);
                Component titleComp = ConfigManager.fromLegacy(titleStr);

                if (!playerBars.containsKey(identifier)) {
                    BossBar bar = BossBar.bossBar(
                            titleComp,
                            1.0f,
                            BossBar.Color.valueOf(bbSettings.getColor().name()),
                            convertStyle(bbSettings.getStyle())
                    );
                    player.showBossBar(bar);
                    playerBars.put(identifier, bar);
                } else {
                    BossBar bar = playerBars.get(identifier);
                    bar.name(titleComp);
                }
            }
        }
        
        Set<String> toRemove = new HashSet<>();
        for (String id : playerBars.keySet()) {
            if (!currentIdentifiers.contains(id)) {
                toRemove.add(id);
            }
        }

        for (String id : toRemove) {
            BossBar bar = playerBars.remove(id);
            if (bar != null) {
                player.hideBossBar(bar);
            }
        }
    }

    private void startActionBarTask() {
        this.actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                sendPriorityActionBar(player);
            }
        }, 0L, 20L); // Every second
    }

    private void sendPriorityActionBar(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, String> items = playerEquippedItems.get(uuid);
        if (items == null || items.isEmpty()) return;

        for (String slot : slotPriority) {
            String itemId = items.get(slot);
            if (itemId == null) continue;
            
            BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
            if (item == null) continue;
            
            PassiveVisuals visuals = item.getPassiveVisuals();

            ActionBarSettings abSettings = visuals.getActionBar();
            if (abSettings.getTriggerMode() == VisualTriggerMode.CONTINUOUS && abSettings.isEnabled() && abSettings.getMessage() != null) {
                String msg = plugin.getHookManager().processPlaceholders(player, abSettings.getMessage());
                player.sendActionBar(ConfigManager.fromLegacy(msg));
                return; // Found highest priority, stop.
            }
        }
    }

    public void clearPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        playerEquippedItems.remove(uuid);
        slotEquipTimes.remove(uuid);
        Map<String, BossBar> bars = activeBossBars.remove(uuid);
        if (bars != null) {
            bars.values().forEach(player::hideBossBar);
        }

        Map<String, OneTimeBossBarInfo> oneTimeBars = activeOneTimeBossBars.remove(uuid);
        if (oneTimeBars != null) {
            oneTimeBars.values().forEach(info -> {
                player.hideBossBar(info.bar);
                if (info.removalTask != null && !info.removalTask.isCancelled()) {
                    info.removalTask.cancel();
                }
            });
        }
    }

    public void clearCache() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearPlayer(player);
        }
        activeBossBars.clear();
        activeOneTimeBossBars.clear();
        playerEquippedItems.clear();
        slotEquipTimes.clear();
    }

    public void cleanup() {
        if (particleTask != null && !particleTask.isCancelled()) {
            particleTask.cancel();
        }
        if (actionBarTask != null && !actionBarTask.isCancelled()) {
            actionBarTask.cancel();
        }

        clearCache();
    }

    /**
     * Helper DTO to pass info from Task to Manager
     */
    public static class ActiveItemInfo {
        public final BuffedItem item;
        public final String slot;

        public ActiveItemInfo(BuffedItem item, String slot) {
            this.item = item;
            this.slot = slot;
        }
    }

    private static class OneTimeBossBarInfo {
        final BossBar bar;
        org.bukkit.scheduler.BukkitTask removalTask;

        public OneTimeBossBarInfo(BossBar bar, org.bukkit.scheduler.BukkitTask removalTask) {
            this.bar = bar;
            this.removalTask = removalTask;
        }
    }
}