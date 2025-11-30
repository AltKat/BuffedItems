package io.github.altkat.BuffedItems.task;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.attribute.ActiveAttributeManager;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.effect.EffectManager;
import io.github.altkat.BuffedItems.utility.attribute.ParsedAttribute;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EffectApplicatorTask extends BukkitRunnable {

    private final BuffedItems plugin;
    private final NamespacedKey nbtKey;
    private final ActiveAttributeManager attributeManager;
    private final EffectManager effectManager;

    private final Map<UUID, Set<PotionEffectType>> managedPotions = new ConcurrentHashMap<>();

    private final Set<UUID> playersToUpdate = ConcurrentHashMap.newKeySet();

    private final Map<UUID, CachedPlayerData> playerCache = new ConcurrentHashMap<>();

    private int tickCount = 0;

    private final Map<UUID, Map<String, Boolean>> permissionCache = new ConcurrentHashMap<>();
    private long lastCacheClearTime = 0;
    private static final long CACHE_EXPIRE_MS = 1000;

    private Iterator<UUID> staleCheckIterator = null;
    private static final int PLAYERS_TO_CHECK_PER_TICK = 2;

    public EffectApplicatorTask(BuffedItems plugin) {
        this.plugin = plugin;
        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
        this.attributeManager = plugin.getActiveAttributeManager();
        this.effectManager = plugin.getEffectManager();
    }

    @Override
    public void run() {
        tickCount++;
        boolean debugTick = (tickCount % 20 == 0);
        long now = System.currentTimeMillis();

        if (now - lastCacheClearTime > CACHE_EXPIRE_MS) {
            permissionCache.clear();
            lastCacheClearTime = now;
            if (debugTick) ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Task] Permission cache auto-cleared.");
        }

        processDistributedStaleCheck(debugTick);

        Set<UUID> playersToScanFully;
        synchronized (playersToUpdate) {
            playersToScanFully = new HashSet<>(playersToUpdate);
            playersToUpdate.clear();
        }

        for (UUID uuid : playersToScanFully) {
            permissionCache.remove(uuid);
        }

        if (debugTick && !playersToScanFully.isEmpty()) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task] Running 'Slow Path' (Full Scan) for " + playersToScanFully.size() + " modified players...");
        }

        for (UUID playerUUID : playersToScanFully) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                playerCache.remove(playerUUID);
                permissionCache.remove(playerUUID);
                continue;
            }
            processPlayer(player, debugTick);
        }

        List<UUID> offlinePlayers = new ArrayList<>();
        Map<PotionEffectType, Integer> desiredPotionEffects = new HashMap<>();
        Set<UUID> desiredInventoryAttributeUUIDs = new HashSet<>();

        for (Map.Entry<UUID, CachedPlayerData> cacheEntry : playerCache.entrySet()) {
            UUID playerUUID = cacheEntry.getKey();
            CachedPlayerData cachedData = cacheEntry.getValue();
            Player player = Bukkit.getPlayer(playerUUID);

            if (player == null || !player.isOnline()) {
                offlinePlayers.add(playerUUID);
                continue;
            }

            if (cachedData == null || cachedData.activeItems == null) continue;

            desiredPotionEffects.clear();
            desiredInventoryAttributeUUIDs.clear();

            Map<String, Integer> currentSetCounts = new HashMap<>();

            for (CachedItem entry : cachedData.activeItems) {
                BuffedItem item = entry.item;
                String slot = entry.slot;

                if (!checkCachedPermission(player, item)) {
                    if (debugTick) {
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Task-Fast Path] Player " + player.getName() + " lacks permission for " + item.getId() + ". Skipping effects.");
                    }
                    continue;
                }

                if (entry.setId != null && !slot.equals("INVENTORY")) {

                    boolean isHandSlot = slot.equals("MAIN_HAND") || slot.equals("OFF_HAND");
                    boolean isArmorItem = isArmor(item.getMaterial());

                    if (!(isHandSlot && isArmorItem)) {
                        currentSetCounts.merge(entry.setId, 1, Integer::sum);
                    }
                }

                if (item.getEffects().containsKey(slot)) {
                    BuffedItemEffect itemEffects = item.getEffects().get(slot);

                    itemEffects.getPotionEffects().forEach((type, level) ->
                            desiredPotionEffects.merge(type, level, Integer::max));

                    if (slot.equals("INVENTORY")) {
                        for (ParsedAttribute parsedAttr : itemEffects.getParsedAttributes()) {
                            desiredInventoryAttributeUUIDs.add(parsedAttr.getUuid());

                            if (!attributeManager.hasModifier(playerUUID, parsedAttr.getAttribute(), parsedAttr.getUuid())) {
                                effectManager.applySingleAttribute(player, parsedAttr, slot);
                            }
                        }
                    }
                }
            }

            for (Map.Entry<String, Integer> setEntry : currentSetCounts.entrySet()) {
                String setId = setEntry.getKey();
                int count = setEntry.getValue();

                io.github.altkat.BuffedItems.utility.set.BuffedSet set = plugin.getSetManager().getSet(setId);
                if (set == null) continue;

                for (int i = 1; i <= count; i++) {
                    io.github.altkat.BuffedItems.utility.set.SetBonus bonus = set.getBonusFor(i);
                    if (bonus != null) {
                        BuffedItemEffect effects = bonus.getEffects();

                        effects.getPotionEffects().forEach((type, level) ->
                                desiredPotionEffects.merge(type, level, Integer::max));

                        for (ParsedAttribute parsedAttr : effects.getParsedAttributes()) {
                            desiredInventoryAttributeUUIDs.add(parsedAttr.getUuid());
                            if (!attributeManager.hasModifier(playerUUID, parsedAttr.getAttribute(), parsedAttr.getUuid())) {
                                effectManager.applySingleAttribute(player, parsedAttr, "SET_BONUS");
                            }
                        }
                    }
                }
            }

            Set<PotionEffectType> lastAppliedPotions = managedPotions.getOrDefault(playerUUID, Collections.emptySet());
            Set<PotionEffectType> desiredPotionTypes = desiredPotionEffects.keySet();

            if (!lastAppliedPotions.equals(desiredPotionTypes)) {

                effectManager.removeObsoletePotionEffects(player, lastAppliedPotions, desiredPotionTypes, debugTick);

                if (!desiredPotionEffects.isEmpty()) {
                    effectManager.applyOrRefreshPotionEffects(player, desiredPotionEffects, debugTick);
                }

                managedPotions.put(playerUUID, new HashSet<>(desiredPotionTypes));
            } else if (!desiredPotionEffects.isEmpty()) {

                effectManager.applyOrRefreshPotionEffects(player, desiredPotionEffects, debugTick);
            }

            Map<Attribute, List<AttributeModifier>> trackedModifiersMap = attributeManager.getActiveModifiers(playerUUID);
            List<ModifierToRemove> modifiersToRemove = new ArrayList<>();

            for (Map.Entry<Attribute, List<AttributeModifier>> trackedEntry : trackedModifiersMap.entrySet()) {
                Attribute trackedAttribute = trackedEntry.getKey();
                for (AttributeModifier trackedModifier : trackedEntry.getValue()) {
                    if (!desiredInventoryAttributeUUIDs.contains(trackedModifier.getUniqueId())) {
                        modifiersToRemove.add(new ModifierToRemove(trackedAttribute, trackedModifier.getUniqueId()));
                    }
                }
            }

            for (ModifierToRemove toRemove : modifiersToRemove) {
                effectManager.removeAttributeModifier(player, toRemove.attribute, toRemove.uuid);
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () ->
                        "[Task-Fast Path] REMOVED attribute modifier: " + toRemove.uuid + " for " + player.getName());
            }

        }

        if (!offlinePlayers.isEmpty()) {
            for (UUID offlineUUID : offlinePlayers) {
                playerCache.remove(offlineUUID);
                managedPotions.remove(offlineUUID);
            }
            if (debugTick) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task] Cleaned up " + offlinePlayers.size() + " offline player(s) from cache");
            }
        }
    }

    private void processPlayer(Player player, boolean debugTick) {
        UUID playerUUID = player.getUniqueId();

        if (debugTick) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task-Slow Path] Scanning inventory and updating cache for " + player.getName());
        }

        List<CachedItem> activeItems = findActiveItems(player, debugTick);

        if (debugTick && !activeItems.isEmpty()) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Task-Slow Path] Found " + activeItems.size() + " active item(s) for " + player.getName());
        }

        playerCache.put(playerUUID, new CachedPlayerData(activeItems));

    }

    private List<CachedItem> findActiveItems(Player player, boolean debugTick) {
        List<CachedItem> activeItems = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();

        checkItem(inventory.getItemInMainHand(), "MAIN_HAND", activeItems, player, debugTick);
        checkItem(inventory.getItemInOffHand(), "OFF_HAND", activeItems, player, debugTick);
        checkItem(inventory.getHelmet(), "HELMET", activeItems, player, debugTick);
        checkItem(inventory.getChestplate(), "CHESTPLATE", activeItems, player, debugTick);
        checkItem(inventory.getLeggings(), "LEGGINGS", activeItems, player, debugTick);
        checkItem(inventory.getBoots(), "BOOTS", activeItems, player, debugTick);

        for (ItemStack item : inventory.getStorageContents()) {
            if (item != null && item.hasItemMeta()) {
                checkItem(item, "INVENTORY", activeItems, player, debugTick);
            }
        }

        return activeItems;
    }

    private void checkItem(ItemStack item, String slot, List<CachedItem> activeItems, Player player, boolean debugTick) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }

        String itemId = item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);

        if (itemId != null) {
            BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
            if (buffedItem != null) {
                String setId = plugin.getSetManager().getSetIdByItem(itemId);
                activeItems.add(new CachedItem(buffedItem, slot, setId));
                if (debugTick) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Task-FindItem] Detected " + itemId + " (Set: " + setId + ") in " + slot);
                }
            } else {
                if (debugTick) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Task-FindItem] Unknown BuffedItem ID found on item: " + itemId + " (player: " + player.getName() + ", slot: " + slot + ")");
                }
            }
        }
    }

    private void processDistributedStaleCheck(boolean debugTick) {
        if (playerCache.isEmpty()) return;

        if (staleCheckIterator == null || !staleCheckIterator.hasNext()) {
            staleCheckIterator = playerCache.keySet().iterator();
        }

        int checkedCount = 0;
        while (staleCheckIterator.hasNext() && checkedCount < PLAYERS_TO_CHECK_PER_TICK) {
            UUID playerUUID = staleCheckIterator.next();
            Player player = Bukkit.getPlayer(playerUUID);

            if (player != null && player.isOnline()) {
                CachedPlayerData cachedData = playerCache.get(playerUUID);
                List<CachedItem> currentItems = findActiveItems(player, false);

                if (cachedData != null && !isSameActiveItems(cachedData.activeItems, currentItems)) {
                    markPlayerForUpdate(playerUUID);
                    if (debugTick) {
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK,
                                () -> "[Task-StaleCheck] Stale cache detected for " + player.getName() + " (Distributed check)");
                    }
                }
            }
            checkedCount++;
        }
    }

    private boolean checkCachedPermission(Player player, BuffedItem item) {
        String permNode = item.getPassivePermissionRaw();
        if (permNode == null && item.getPermission() != null) {
            permNode = item.getPermission();
        }

        if (permNode == null || permNode.equalsIgnoreCase("NONE")) {
            return true;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Boolean> playerPerms = permissionCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        return playerPerms.computeIfAbsent(permNode, player::hasPermission);
    }

    private boolean isSameActiveItems(List<CachedItem> cacheList, List<CachedItem> realList) {
        if (cacheList.size() != realList.size()) {return false;}

        for (int i = 0; i < cacheList.size(); i++) {
            CachedItem cacheItem = cacheList.get(i);
            CachedItem realItem = realList.get(i);

            if (!cacheItem.item.getId().equals(realItem.item.getId()) ||
                    !cacheItem.slot.equals(realItem.slot) ||
                    !Objects.equals(cacheItem.setId, realItem.setId)) {
                return false;
            }
        }
        return true;
    }

    public void playerQuit(Player player) {
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Task] Removing player data: " + player.getName());
        UUID uuid = player.getUniqueId();
        managedPotions.remove(uuid);
        playerCache.remove(uuid);
        playersToUpdate.remove(uuid);
        permissionCache.remove(uuid);
        attributeManager.clearPlayer(uuid);
    }

    public void markPlayerForUpdate(UUID playerUUID) {
        playersToUpdate.add(playerUUID);
    }

    public void invalidateCacheForHolding(String itemId) {
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task] Invalidating cache for players holding: " + itemId);
        int markedCount = 0;
        for (Map.Entry<UUID, CachedPlayerData> entry : playerCache.entrySet()) {
            UUID playerUUID = entry.getKey();
            CachedPlayerData data = entry.getValue();

            if (data == null || data.activeItems == null) continue;

            boolean isHolding = false;
            for (CachedItem itemEntry : data.activeItems) {
                if (itemEntry.item.getId().equals(itemId)) {
                    isHolding = true;
                    break;
                }
            }

            if (isHolding) {
                markPlayerForUpdate(playerUUID);
                markedCount++;
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Task] --> Marked player " + playerUUID + " due to holding " + itemId);
            }
        }
        final int finalMarkedCount = markedCount;
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task] Marked " + finalMarkedCount + " player(s) for update due to holding " + itemId);
    }

    public Set<PotionEffectType> getManagedEffects(UUID playerUUID) {
        return managedPotions.getOrDefault(playerUUID, Collections.emptySet());
    }

    private boolean isArmor(Material m) {
        String name = m.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || name.equals("ELYTRA") || name.equals("TURTLE_HELMET");
    }

    private static class CachedPlayerData {
        final List<CachedItem> activeItems;

        CachedPlayerData(List<CachedItem> activeItems) {
            this.activeItems = new ArrayList<>(activeItems);
        }
    }

    private static class CachedItem {
        final BuffedItem item;
        final String slot;
        final String setId;

        CachedItem(BuffedItem item, String slot, String setId) {
            this.item = item;
            this.slot = slot;
            this.setId = setId;
        }
    }

    private static class ModifierToRemove {
        final Attribute attribute;
        final UUID uuid;

        ModifierToRemove(Attribute attribute, UUID uuid) {
            this.attribute = attribute;
            this.uuid = uuid;
        }
    }
}