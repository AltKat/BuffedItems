package io.github.altkat.BuffedItems.Tasks;

import io.github.altkat.BuffedItems.BuffedItems;
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

    public EffectApplicatorTask(BuffedItems plugin) {
        this.plugin = plugin;
        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            List<Map.Entry<BuffedItem, String>> activeItems = findActiveItems(player);
            Map<PotionEffectType, Integer> desiredPotionEffects = new HashMap<>();

            plugin.getEffectManager().clearAllAttributes(player);

            for (Map.Entry<BuffedItem, String> entry : activeItems) {
                BuffedItem item = entry.getKey();
                String slot = entry.getValue();

                if (item.getPermission().isPresent() && !player.hasPermission(item.getPermission().get())) {
                    continue;
                }

                if (item.getEffects().containsKey(slot)) {
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
        checkItem(inventory.getItemInMainHand(), "MAIN_HAND", activeItems);
        checkItem(inventory.getItemInOffHand(), "OFF_HAND", activeItems);
        checkItem(inventory.getHelmet(), "HELMET", activeItems);
        checkItem(inventory.getChestplate(), "CHESTPLATE", activeItems);
        checkItem(inventory.getLeggings(), "LEGGINGS", activeItems);
        checkItem(inventory.getBoots(), "BOOTS", activeItems);
        for (ItemStack item : inventory.getStorageContents()) {
            checkItem(item, "INVENTORY", activeItems);
        }
        return activeItems;
    }

    private void checkItem(ItemStack item, String slot, List<Map.Entry<BuffedItem, String>> activeItems) {
        if (item == null || !item.hasItemMeta()) return;
        if (item.getItemMeta().getPersistentDataContainer().has(nbtKey, PersistentDataType.STRING)) {
            String itemId = item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
            BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
            if (buffedItem != null) {
                activeItems.add(new AbstractMap.SimpleEntry<>(buffedItem, slot));
            }
        }
    }

    public void playerQuit(Player player) {
        managedEffects.remove(player.getUniqueId());
    }
}