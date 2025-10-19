package io.github.altkat.BuffedItems.Tasks;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ActiveAttributeManager;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Managers.EffectManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.github.altkat.BuffedItems.utils.BuffedItemEffect;
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

        if (debugTick) {
            ConfigManager.sendDebugMessage("[Task] Running effect applicator (tick: " + tickCount + ", players: " + Bukkit.getOnlinePlayers().size() + ")");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();

            List<Map.Entry<BuffedItem, String>> activeItems = findActiveItems(player);
            Map<PotionEffectType, Integer> desiredPotionEffects = new HashMap<>();
            Map<UUID, ParsedModifierInfo> desiredModifiersByUUID = new HashMap<>();


            if (debugTick && !activeItems.isEmpty()) {
                ConfigManager.sendDebugMessage("[Task] Found " + activeItems.size() + " active item(s) for " + player.getName());
            }

            for (Map.Entry<BuffedItem, String> entry : activeItems) {
                BuffedItem item = entry.getKey();
                String slot = entry.getValue();


                if (item.getPermission().isPresent() && !player.hasPermission(item.getPermission().get())) {
                    ConfigManager.sendDebugMessage("[Task] Player " + player.getName() + " lacks permission for item: " + item.getId() + " (requires: " + item.getPermission().get() + ")");
                    continue;
                }

                if (item.getEffects().containsKey(slot)) {
                    ConfigManager.sendDebugMessage("[Task] Processing effects from " + item.getId() + " in slot " + slot + " for " + player.getName());
                    BuffedItemEffect itemEffects = item.getEffects().get(slot);

                    itemEffects.getPotionEffects().forEach((type, level) ->
                            desiredPotionEffects.merge(type, level, Integer::max));

                    for (String attrString : itemEffects.getAttributes()) {
                        try {
                            String[] parts = attrString.split(";");
                            if (parts.length != 3) continue;

                            Attribute attribute = Attribute.valueOf(parts[0].toUpperCase());
                            AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                            double amount = Double.parseDouble(parts[2]);
                            UUID modifierUUID = UUID.nameUUIDFromBytes(("buffeditems." + item.getId() + "." + slot + "." + attribute.name()).getBytes());

                            if (desiredModifiersByUUID.containsKey(modifierUUID)) {
                                plugin.getLogger().warning("Duplicate modifier UUID detected: " + modifierUUID + " for item " + item.getId() + " in slot " + slot + ". Overwriting previous definition.");
                            }
                            desiredModifiersByUUID.put(modifierUUID, new ParsedModifierInfo(attribute, operation, amount, item.getId(), slot));

                        } catch (Exception e) {
                            plugin.getLogger().warning("[Task] Error parsing attribute string '" + attrString + "' for item " + item.getId() + " in slot " + slot + ": " + e.getMessage());
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
                        ConfigManager.sendDebugMessage("[Task] Marking modifier for removal: " + trackedModifier.getUniqueId() + " on " + trackedAttribute.name());
                    }
                }
            }

            Map<String, List<String>> attributesToAddByItemSlot = new HashMap<>();
            for (Map.Entry<UUID, ParsedModifierInfo> desiredEntry : desiredModifiersByUUID.entrySet()) {
                UUID desiredUUID = desiredEntry.getKey();
                ParsedModifierInfo desiredInfo = desiredEntry.getValue();

                if (!attributeManager.hasModifier(playerUUID, desiredInfo.attribute, desiredUUID)) {
                    String key = desiredInfo.itemId + "." + desiredInfo.slot;
                    String attrString = desiredInfo.attribute.name() + ";" + desiredInfo.operation.name() + ";" + desiredInfo.amount;
                    attributesToAddByItemSlot.computeIfAbsent(key, k -> new ArrayList<>()).add(attrString);
                    ConfigManager.sendDebugMessage("[Task] Marking modifier for addition: " + desiredUUID + " (" + attrString + ") via " + key);
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

            Set<PotionEffectType> lastApplied = managedEffects.getOrDefault(playerUUID, Collections.emptySet());
            effectManager.removeObsoletePotionEffects(player, lastApplied, desiredPotionEffects.keySet());
            effectManager.applyOrRefreshPotionEffects(player, desiredPotionEffects);
            managedEffects.put(playerUUID, desiredPotionEffects.keySet());
        }
    }


    /**
     * Scans player's inventory (main hand, offhand, armor, storage) for BuffedItems.
     * @param player The player whose inventory to scan.
     * @return A list of Map Entries, where the key is the BuffedItem found and the value is the slot name (e.g., "MAIN_HAND").
     */
    private List<Map.Entry<BuffedItem, String>> findActiveItems(Player player) {
        List<Map.Entry<BuffedItem, String>> activeItems = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();

        checkItem(inventory.getItemInMainHand(), "MAIN_HAND", activeItems, player);
        checkItem(inventory.getItemInOffHand(), "OFF_HAND", activeItems, player);
        checkItem(inventory.getHelmet(), "HELMET", activeItems, player);
        checkItem(inventory.getChestplate(), "CHESTPLATE", activeItems, player);
        checkItem(inventory.getLeggings(), "LEGGINGS", activeItems, player);
        checkItem(inventory.getBoots(), "BOOTS", activeItems, player);

        for (ItemStack item : inventory.getStorageContents()) {
            checkItem(item, "INVENTORY", activeItems, player);
        }

        return activeItems;
    }

    /**
     * Helper method to check a single ItemStack for the BuffedItem NBT tag.
     * If found and valid, adds it to the list of active items.
     * @param item The ItemStack to check.
     * @param slot The name of the slot this item is in.
     * @param activeItems The list to add the found BuffedItem entry to.
     * @param player The player owning the item (for debug messages).
     */
    private void checkItem(ItemStack item, String slot, List<Map.Entry<BuffedItem, String>> activeItems, Player player) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getPersistentDataContainer().isEmpty()) {
            return;
        }

        if (item.getItemMeta().getPersistentDataContainer().has(nbtKey, PersistentDataType.STRING)) {
            String itemId = item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
            if (itemId != null) {
                BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
                if (buffedItem != null) {
                    activeItems.add(new AbstractMap.SimpleEntry<>(buffedItem, slot));
                    ConfigManager.sendDebugMessage("[Task] Detected BuffedItem: " + itemId + " in " + slot + " for " + player.getName());
                } else {
                    ConfigManager.sendDebugMessage("[Task] Unknown BuffedItem ID found on item: " + itemId + " (player: " + player.getName() + ", slot: "+slot+")");
                }
            }
        }
    }

    /**
     * Called when a player quits to clean up managed potion effects.
     * Attribute cleanup is handled separately by the PlayerQuitListener calling EffectManager.clearAllAttributes.
     * @param player The player who quit.
     */
    public void playerQuit(Player player) {
        ConfigManager.sendDebugMessage("[Task] Removing player potion effect tracking: " + player.getName());
        managedEffects.remove(player.getUniqueId());
    }

    public Set<PotionEffectType> getManagedEffects(UUID playerUUID) {
        return managedEffects.getOrDefault(playerUUID, Collections.emptySet());
    }

    /** Simple data class to hold parsed modifier info before applying. */
    private static class ParsedModifierInfo {
        final Attribute attribute;
        final AttributeModifier.Operation operation;
        final double amount;
        final String itemId;
        final String slot;

        ParsedModifierInfo(Attribute attribute, AttributeModifier.Operation operation, double amount, String itemId, String slot) {
            this.attribute = attribute;
            this.operation = operation;
            this.amount = amount;
            this.itemId = itemId;
            this.slot = slot;
        }
    }

    /** Simple data class to hold info needed for modifier removal. */
    private static class ModifierToRemove {
        final Attribute attribute;
        final UUID uuid;

        ModifierToRemove(Attribute attribute, UUID uuid) {
            this.attribute = attribute;
            this.uuid = uuid;
        }
    }
}