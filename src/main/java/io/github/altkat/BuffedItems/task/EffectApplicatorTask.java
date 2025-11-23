package io.github.altkat.BuffedItems.task;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.attribute.ActiveAttributeManager;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.effect.EffectManager;
import io.github.altkat.BuffedItems.utility.attribute.ParsedAttribute;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;
import org.bukkit.Bukkit;
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

    private int staleCacheCheckTicks = 0;
    private static final int STALE_CHECK_INTERVAL = 200;

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

        staleCacheCheckTicks++;
        if (staleCacheCheckTicks >= STALE_CHECK_INTERVAL) {
            staleCacheCheckTicks = 0;
            if (debugTick) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task] Running periodic stale cache validation...");
            }
            validateCacheForStaleness(debugTick);
        }

        Set<UUID> playersToScanFully;
        synchronized (playersToUpdate) {
            playersToScanFully = new HashSet<>(playersToUpdate);
            playersToUpdate.clear();
        }

        if (debugTick && !playersToScanFully.isEmpty()) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task] Running 'Slow Path' (Full Scan) for " + playersToScanFully.size() + " modified players...");
        }

        for (UUID playerUUID : playersToScanFully) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                playerCache.remove(playerUUID);
                continue;
            }
            processPlayer(player, debugTick);
        }


        if (debugTick && !playerCache.isEmpty()) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task] Running 'Fast Path' (Cache Check) for " + playerCache.size() + " cached players...");
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

            if (cachedData == null || cachedData.activeItems == null) {
                continue;
            }

            desiredPotionEffects.clear();
            desiredInventoryAttributeUUIDs.clear();

            for (Map.Entry<BuffedItem, String> entry : cachedData.activeItems) {
                BuffedItem item = entry.getKey();
                String slot = entry.getValue();

                if (!item.hasPassivePermission(player)) {
                    if (debugTick) {
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Task-Fast Path] Player " + player.getName() + " lacks permission for " + item.getId() + ". Skipping effects.");
                    }
                    continue;
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

        List<Map.Entry<BuffedItem, String>> activeItems = findActiveItems(player, debugTick);

        if (debugTick && !activeItems.isEmpty()) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Task-Slow Path] Found " + activeItems.size() + " active item(s) for " + player.getName());
        }

        playerCache.put(playerUUID, new CachedPlayerData(activeItems));

    }

    private List<Map.Entry<BuffedItem, String>> findActiveItems(Player player, boolean debugTick) {
        List<Map.Entry<BuffedItem, String>> activeItems = new ArrayList<>();
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

    private void checkItem(ItemStack item, String slot, List<Map.Entry<BuffedItem, String>> activeItems, Player player, boolean debugTick) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return;
        }

        String itemId = item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);

        if (itemId != null) {
            BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
            if (buffedItem != null) {
                activeItems.add(new AbstractMap.SimpleEntry<>(buffedItem, slot));
                if (debugTick) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Task-FindItem] Detected BuffedItem: " + itemId + " in " + slot + " for " + player.getName());
                }
            } else {
                if (debugTick) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Task-FindItem] Unknown BuffedItem ID found on item: " + itemId + " (player: " + player.getName() + ", slot: " + slot + ")");
                }
            }
        }
    }

    private void validateCacheForStaleness(boolean debugTick) {
        for (Map.Entry<UUID, CachedPlayerData> entry : playerCache.entrySet()) {
            UUID playerUUID = entry.getKey();
            Player player = Bukkit.getPlayer(playerUUID);

            if (player == null || !player.isOnline()) continue;

            List<Map.Entry<BuffedItem, String>> currentItems = findActiveItems(player, false);
            CachedPlayerData cachedData = entry.getValue();

            if (!isSameActiveItems(cachedData.activeItems, currentItems)) {
                markPlayerForUpdate(playerUUID);
                if (debugTick) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK,
                            () -> "[Task-StaleCheck] Stale cache detected for " + player.getName() + ", marking for full update.");
                }
            }
        }
    }

    private boolean isSameActiveItems(List<Map.Entry<BuffedItem, String>> cacheList,
                                      List<Map.Entry<BuffedItem, String>> realList) {
        if (cacheList.size() != realList.size()) {
            return false;
        }

        for (int i = 0; i < cacheList.size(); i++) {
            Map.Entry<BuffedItem, String> cacheItem = cacheList.get(i);
            Map.Entry<BuffedItem, String> realItem = realList.get(i);

            if (!cacheItem.getKey().getId().equals(realItem.getKey().getId()) ||
                    !cacheItem.getValue().equals(realItem.getValue())) {
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
            for (Map.Entry<BuffedItem, String> itemEntry : data.activeItems) {
                if (itemEntry.getKey().getId().equals(itemId)) {
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

    private static class CachedPlayerData {
        final List<Map.Entry<BuffedItem, String>> activeItems;

        CachedPlayerData(List<Map.Entry<BuffedItem, String>> activeItems) {
            this.activeItems = new ArrayList<>(activeItems);
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