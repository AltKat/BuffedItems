package io.github.altkat.BuffedItems.Tasks;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ActiveAttributeManager;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Managers.EffectManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.github.altkat.BuffedItems.utils.BuffedItemEffect;
import io.github.altkat.BuffedItems.utils.ParsedAttribute;
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

    private final Map<UUID, Set<PotionEffectType>> managedEffects = new ConcurrentHashMap<>();
    private final Map<UUID, CachedPlayerData> playerCache = new ConcurrentHashMap<>();

    private final Set<UUID> playersToUpdate = ConcurrentHashMap.newKeySet();

    private int tickCount = 0;

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

        Set<UUID> playersToScanFully;
        synchronized (playersToUpdate) {
            playersToScanFully = new HashSet<>(playersToUpdate);
            playersToUpdate.clear();
        }

        if (playersToScanFully.isEmpty() && playerCache.isEmpty()) {
            return;
        }

        if (debugTick && !playersToScanFully.isEmpty()) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task] Running FULL scan (Potions + INVENTORY Attributes) for " + playersToScanFully.size() + " modified players...");
        }

        for (UUID playerUUID : playersToScanFully) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                continue;
            }
            processPlayer(player, debugTick);
        }

        if (debugTick && !playerCache.isEmpty()) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task] Running POTION APPLY/REFRESH for " + playerCache.size() + " cached players...");
        }

        List<UUID> offlinePlayers = new ArrayList<>();

        for (Map.Entry<UUID, CachedPlayerData> cacheEntry : playerCache.entrySet()) {
            UUID playerUUID = cacheEntry.getKey();
            CachedPlayerData cachedData = cacheEntry.getValue();

            Player player = Bukkit.getPlayer(playerUUID);
            if (player == null || !player.isOnline()) {
                offlinePlayers.add(playerUUID);
                continue;
            }

            if (cachedData == null) {
                continue;
            }

            Set<PotionEffectType> lastApplied = managedEffects.getOrDefault(playerUUID, Collections.emptySet());
            effectManager.removeObsoletePotionEffects(player, lastApplied, cachedData.desiredPotionEffects.keySet(), debugTick);
            effectManager.applyOrRefreshPotionEffects(player, cachedData.desiredPotionEffects, debugTick);

            managedEffects.put(playerUUID, cachedData.desiredPotionEffects.keySet());
        }

        if (!offlinePlayers.isEmpty()) {
            for (UUID offlineUUID : offlinePlayers) {
                playerCache.remove(offlineUUID);
                managedEffects.remove(offlineUUID);
            }
            if (debugTick) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task] Cleaned up " + offlinePlayers.size() + " offline player(s) from cache");
            }
        }
    }

    private void processPlayer(Player player, boolean debugTick) {
        UUID playerUUID = player.getUniqueId();

        if (debugTick) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task-FullScan] Performing manual scan (Potions + INVENTORY Attr.) for " + player.getName());
        }

        List<Map.Entry<BuffedItem, String>> activeItems = findActiveItems(player, debugTick);
        Map<PotionEffectType, Integer> desiredPotionEffects = new HashMap<>();
        Map<UUID, ParsedAttribute> desiredModifiersByUUID = new HashMap<>();
        Map<UUID, String[]> desiredModifierContext = new HashMap<>();

        if (debugTick && !activeItems.isEmpty()) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Task-FullScan] Found " + activeItems.size() + " active item(s) for " + player.getName());
        }

        for (Map.Entry<BuffedItem, String> entry : activeItems) {
            BuffedItem item = entry.getKey();
            String slot = entry.getValue();

            if (item.getPermission().isPresent() && !player.hasPermission(item.getPermission().get())) {
                continue;
            }

            if (item.getEffects().containsKey(slot)) {
                BuffedItemEffect itemEffects = item.getEffects().get(slot);

                itemEffects.getPotionEffects().forEach((type, level) ->
                        desiredPotionEffects.merge(type, level, Integer::max));

                if (slot.equals("INVENTORY")) {
                    for (ParsedAttribute parsedAttr : itemEffects.getParsedAttributes()) {
                        UUID modifierUUID = parsedAttr.getUuid();

                        if (desiredModifiersByUUID.containsKey(modifierUUID)) {
                            plugin.getLogger().warning("Duplicate modifier UUID detected during task run: " + modifierUUID + " for item " + item.getId() + ". This is unexpected.");
                        }

                        desiredModifiersByUUID.put(modifierUUID, parsedAttr);
                        desiredModifierContext.put(modifierUUID, new String[]{item.getId(), slot});
                    }
                }
            }
        }

        Map<Attribute, List<AttributeModifier>> trackedModifiersMap = attributeManager.getActiveModifiers(playerUUID);
        List<ModifierToRemove> modifiersToRemove = new ArrayList<>();

        for (Map.Entry<Attribute, List<AttributeModifier>> trackedEntry : trackedModifiersMap.entrySet()) {
            Attribute trackedAttribute = trackedEntry.getKey();
            for (AttributeModifier trackedModifier : trackedEntry.getValue()) {
                if (!desiredModifiersByUUID.containsKey(trackedModifier.getUniqueId())) {
                    modifiersToRemove.add(new ModifierToRemove(trackedAttribute, trackedModifier.getUniqueId()));
                    if (debugTick) {
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Task-FullScan] Marking INVENTORY modifier for removal: " + trackedModifier.getUniqueId() + " on " + trackedAttribute.name());
                    }
                }
            }
        }

        Map<String, List<String>> attributesToAddByItemSlot = new HashMap<>();

        for (Map.Entry<UUID, ParsedAttribute> desiredEntry : desiredModifiersByUUID.entrySet()) {
            UUID desiredUUID = desiredEntry.getKey();
            ParsedAttribute desiredAttr = desiredEntry.getValue();

            if (!attributeManager.hasModifier(playerUUID, desiredAttr.getAttribute(), desiredUUID)) {
                String[] context = desiredModifierContext.get(desiredUUID);
                String itemId = context[0];
                String slot = context[1];
                String key = itemId + "." + slot;

                String attrString = desiredAttr.getAttribute().name() + ";" + desiredAttr.getOperation().name() + ";" + desiredAttr.getAmount();

                attributesToAddByItemSlot.computeIfAbsent(key, k -> new ArrayList<>()).add(attrString);
                if (debugTick) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Task-FullScan] Marking INVENTORY modifier for addition: " + desiredUUID + " (" + attrString + ") via " + key);
                }
            }
        }

        for (ModifierToRemove toRemove : modifiersToRemove) {
            effectManager.removeAttributeModifier(player, toRemove.attribute, toRemove.uuid);
        }
        for (Map.Entry<String, List<String>> toAddEntry : attributesToAddByItemSlot.entrySet()) {
            String[] itemSlotKey = toAddEntry.getKey().split("\\.");
            String itemId = itemSlotKey[0];
            String slot = itemSlotKey[1];
            List<String> attrsToAdd = toAddEntry.getValue();
            effectManager.applyAttributeEffects(player, itemId, slot, attrsToAdd);
        }
        playerCache.put(playerUUID, new CachedPlayerData(desiredPotionEffects, activeItems));
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

    public void playerQuit(Player player) {
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Task] Removing player data: " + player.getName());
        UUID uuid = player.getUniqueId();
        managedEffects.remove(uuid);
        playerCache.remove(uuid);
        playersToUpdate.remove(uuid);
    }

    /**
     * Marks the player for a FULL SCAN in the next tick.
     * This method replaces 'invalidateCache'.
     */
    public void markPlayerForUpdate(UUID playerUUID) {
        playersToUpdate.add(playerUUID);
    }

    /**
     * Ensures all players holding or wearing a specific item ID
     * are updated in the next tick.
     */
    public void invalidateCacheForHolding(String itemId) {
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Task] Invalidating cache for players holding: " + itemId);
        int markedCount = 0;
        for (Map.Entry<UUID, CachedPlayerData> entry : playerCache.entrySet()) {
            UUID playerUUID = entry.getKey();
            CachedPlayerData data = entry.getValue();

            if (data == null || data.activeItems == null) continue;

            boolean isHolding = data.activeItems.stream()
                    .anyMatch(itemEntry -> itemEntry.getKey().getId().equals(itemId));

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
        return managedEffects.getOrDefault(playerUUID, Collections.emptySet());
    }

    /**
     * Cache class - Holds the player's current state
     */
    private static class CachedPlayerData {
        final Map<PotionEffectType, Integer> desiredPotionEffects;
        final List<Map.Entry<BuffedItem, String>> activeItems;

        CachedPlayerData(Map<PotionEffectType, Integer> desiredPotionEffects,
                         List<Map.Entry<BuffedItem, String>> activeItems) {
            this.desiredPotionEffects = new HashMap<>(desiredPotionEffects);
            this.activeItems = new ArrayList<>(activeItems);
        }
    }

    /**
     * Class that holds modifier removal information
     */
    private static class ModifierToRemove {
        final Attribute attribute;
        final UUID uuid;

        ModifierToRemove(Attribute attribute, UUID uuid) {
            this.attribute = attribute;
            this.uuid = uuid;
        }
    }
}