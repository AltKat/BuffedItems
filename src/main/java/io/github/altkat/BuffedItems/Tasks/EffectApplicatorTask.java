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

public class EffectApplicatorTask extends BukkitRunnable {

    private final BuffedItems plugin;
    private final NamespacedKey nbtKey;
    private final ActiveAttributeManager attributeManager;
    private final EffectManager effectManager;

    private final Map<UUID, Set<PotionEffectType>> managedEffects = new HashMap<>();
    private final Map<UUID, CachedPlayerData> playerCache = new HashMap<>();
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
            ConfigManager.sendDebugMessage("[Task] Running effect applicator check (tick: " + tickCount + ", players: " + Bukkit.getOnlinePlayers().size() + ")");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            processPlayer(player, debugTick);
        }
    }

    private void processPlayer(Player player, boolean debugTick) {
        UUID playerUUID = player.getUniqueId();

        CachedPlayerData cached = playerCache.get(playerUUID);
        int currentHash = calculateInventoryHash(player);

        if (cached != null && cached.inventoryHash == currentHash) {
            if (!cached.desiredPotionEffects.isEmpty()) {
                effectManager.applyOrRefreshPotionEffects(player, cached.desiredPotionEffects, debugTick);
            }
            return;
        }

        if (debugTick || cached == null) {
            ConfigManager.sendDebugMessage("[Task] Cache miss for " + player.getName() + " - performing full scan");
        }

        List<Map.Entry<BuffedItem, String>> activeItems = findActiveItems(player, debugTick);
        Map<PotionEffectType, Integer> desiredPotionEffects = new HashMap<>();
        Map<UUID, ParsedAttribute> desiredModifiersByUUID = new HashMap<>();
        Map<UUID, String[]> desiredModifierContext = new HashMap<>();

        if (debugTick && !activeItems.isEmpty()) {
            ConfigManager.sendDebugMessage("[Task] Found " + activeItems.size() + " active item(s) for " + player.getName());
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

        Map<Attribute, List<AttributeModifier>> trackedModifiersMap = attributeManager.getActiveModifiers(playerUUID);
        List<ModifierToRemove> modifiersToRemove = new ArrayList<>();

        for (Map.Entry<Attribute, List<AttributeModifier>> trackedEntry : trackedModifiersMap.entrySet()) {
            Attribute trackedAttribute = trackedEntry.getKey();
            for (AttributeModifier trackedModifier : trackedEntry.getValue()) {
                if (!desiredModifiersByUUID.containsKey(trackedModifier.getUniqueId())) {
                    modifiersToRemove.add(new ModifierToRemove(trackedAttribute, trackedModifier.getUniqueId()));
                    if (debugTick) {
                        ConfigManager.sendDebugMessage("[Task] Marking modifier for removal: " + trackedModifier.getUniqueId() + " on " + trackedAttribute.name());
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
                    ConfigManager.sendDebugMessage("[Task] Marking modifier for addition: " + desiredUUID + " (" + attrString + ") via " + key);
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

        Set<PotionEffectType> lastApplied = managedEffects.getOrDefault(playerUUID, Collections.emptySet());
        effectManager.removeObsoletePotionEffects(player, lastApplied, desiredPotionEffects.keySet(), debugTick);
        effectManager.applyOrRefreshPotionEffects(player, desiredPotionEffects, debugTick);
        managedEffects.put(playerUUID, desiredPotionEffects.keySet());
        playerCache.put(playerUUID, new CachedPlayerData(currentHash, desiredPotionEffects, activeItems));
    }

    /**
     * Calculates the hash value of the player's inventory.
     * If the inventory hasn't changed, it returns the same hash, thus preventing unnecessary scanning.
     */
    private int calculateInventoryHash(Player player) {
        PlayerInventory inv = player.getInventory();
        int hash = 0;

        hash = 31 * hash + hashItem(inv.getItemInMainHand());
        hash = 31 * hash + hashItem(inv.getItemInOffHand());
        hash = 31 * hash + hashItem(inv.getHelmet());
        hash = 31 * hash + hashItem(inv.getChestplate());
        hash = 31 * hash + hashItem(inv.getLeggings());
        hash = 31 * hash + hashItem(inv.getBoots());

        ItemStack[] contents = inv.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null && contents[i].hasItemMeta()) {
                ItemStack item = contents[i];
                if (item.getItemMeta().getPersistentDataContainer().has(nbtKey, PersistentDataType.STRING)) {
                    hash = 31 * hash + hashItem(item);
                }
            }
        }

        return hash;
    }

    /**
     * Calculates the hash value of an item
     */
    private int hashItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }

        int hash = item.getType().hashCode();
        hash = 31 * hash + item.getAmount();

        if (item.hasItemMeta()) {
            String itemId = item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
            if (itemId != null) {
                hash = 31 * hash + itemId.hashCode();
            }
        }

        return hash;
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
                    ConfigManager.sendDebugMessage("[Task] Detected BuffedItem: " + itemId + " in " + slot + " for " + player.getName());
                }
            } else if (debugTick) {
                ConfigManager.sendDebugMessage("[Task] Unknown BuffedItem ID found on item: " + itemId + " (player: " + player.getName() + ", slot: " + slot + ")");
            }
        }
    }

    public void playerQuit(Player player) {
        ConfigManager.sendDebugMessage("[Task] Removing player data: " + player.getName());
        UUID uuid = player.getUniqueId();
        managedEffects.remove(uuid);
        playerCache.remove(uuid);
    }

    /**
     * Clear the player's cache - Should be called on inventory change
     */
    public void invalidateCache(UUID playerUUID) {
        CachedPlayerData cached = playerCache.get(playerUUID);
        if (cached != null) {
            ConfigManager.sendDebugMessage("[Task] Cache invalidated for player: " + playerUUID);
            playerCache.remove(playerUUID);
        }
    }

    public Set<PotionEffectType> getManagedEffects(UUID playerUUID) {
        return managedEffects.getOrDefault(playerUUID, Collections.emptySet());
    }

    /**
     * Cache class - Holds the player's current state
     */
    private static class CachedPlayerData {
        final int inventoryHash;
        final Map<PotionEffectType, Integer> desiredPotionEffects;
        final List<Map.Entry<BuffedItem, String>> activeItems;

        CachedPlayerData(int inventoryHash, Map<PotionEffectType, Integer> desiredPotionEffects,
                         List<Map.Entry<BuffedItem, String>> activeItems) {
            this.inventoryHash = inventoryHash;
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