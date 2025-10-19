package io.github.altkat.BuffedItems.Tasks;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
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
    private final Map<UUID, Set<PotionEffectType>> managedEffects = new ConcurrentHashMap<>();
    private int tickCount = 0;

    public EffectApplicatorTask(BuffedItems plugin) {
        this.plugin = plugin;
        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
    }

    @Override
    public void run() {
        tickCount++;
        if (tickCount % 20 == 0) {
            ConfigManager.sendDebugMessage("[Task] Running effect applicator (tick: " + tickCount + ", players: " + Bukkit.getOnlinePlayers().size() + ")");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            List<Map.Entry<BuffedItem, String>> activeItems = findActiveItems(player);
            if (!activeItems.isEmpty()) {
                ConfigManager.sendDebugMessage("[Task] Found " + activeItems.size() + " active item(s) for " + player.getName());
            }
            Map<PotionEffectType, Integer> desiredPotionEffects = new HashMap<>();

            plugin.getEffectManager().clearAllAttributes(player);

            for (Map.Entry<BuffedItem, String> entry : activeItems) {
                BuffedItem item = entry.getKey();
                String slot = entry.getValue();

                if (item.getPermission().isPresent() && !player.hasPermission(item.getPermission().get())) {
                    ConfigManager.sendDebugMessage("[Task] Player " + player.getName() + " lacks permission for item: " + item.getId() + " (requires: " + item.getPermission().get() + ")");
                    continue;
                }

                if (item.getEffects().containsKey(slot)) {

                    ConfigManager.sendDebugMessage("[Task] Applying effects from " + item.getId() + " in slot " + slot + " for " + player.getName());

                    item.getEffects().get(slot).getPotionEffects().forEach((type, level) ->
                            desiredPotionEffects.merge(type, level, Integer::max));
                    plugin.getEffectManager().applyAttributeEffects(player, item.getId(), slot, item.getEffects().get(slot).getAttributes());
                }
            }

            Set<PotionEffectType> lastApplied = managedEffects.getOrDefault(player.getUniqueId(), Collections.emptySet());

            plugin.getEffectManager().removeObsoletePotionEffects(player, lastApplied, desiredPotionEffects.keySet());
            plugin.getEffectManager().applyOrRefreshPotionEffects(player, desiredPotionEffects);

            managedEffects.put(player.getUniqueId(), desiredPotionEffects.keySet());
        }
    }

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

    private void checkItem(ItemStack item, String slot, List<Map.Entry<BuffedItem, String>> activeItems, Player player) {
        if (item == null || !item.hasItemMeta()) return;
        if (item.getItemMeta().getPersistentDataContainer().has(nbtKey, PersistentDataType.STRING)) {
            String itemId = item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
            BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
            if (buffedItem != null) {
                activeItems.add(new AbstractMap.SimpleEntry<>(buffedItem, slot));
                ConfigManager.sendDebugMessage("[Task] Detected BuffedItem: " + itemId + " in " + slot + " for " + player.getName());
            } else {
                plugin.getLogger().warning("[Task] Unknown BuffedItem ID in player inventory: " + itemId + " (player: " + player.getName() + ")");
            }
        }
    }

    public void playerQuit(Player player) {
        ConfigManager.sendDebugMessage("[Task] Removing player from tracking: " + player.getName());
        managedEffects.remove(player.getUniqueId());
    }

    public Set<PotionEffectType> getManagedEffects(UUID playerUUID) {
        return managedEffects.getOrDefault(playerUUID, Collections.emptySet());
    }
}