package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class BuffedItemCost implements ICost {

    private final BuffedItems plugin;
    private final String requiredItemId;
    private final int amount;
    private final String failureMessage;
    private final NamespacedKey nbtKey;

    public BuffedItemCost(Map<String, Object> data, BuffedItems plugin) {
        this.plugin = plugin;
        this.requiredItemId = (String) data.get("item_id");
        this.amount = ((Number) data.getOrDefault("amount", 1)).intValue();

        String defaultMsg = "&cMissing Item: &e{amount}x {item_name}";
        if (plugin.getConfig().contains("active-items.costs.messages.buffed_item")) {
            defaultMsg = plugin.getConfig().getString("active-items.costs.messages.buffed_item");
        }
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);

        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
    }

    @Override
    public boolean hasEnough(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isMatchingItem(item)) {
                count += item.getAmount();
            }
        }
        return count >= amount;
    }

    @Override
    public void deduct(Player player) {
        int remainingToRemove = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isMatchingItem(item)) {
                if (item.getAmount() <= remainingToRemove) {
                    remainingToRemove -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remainingToRemove);
                    remainingToRemove = 0;
                }

                if (remainingToRemove <= 0) break;
            }
        }
    }

    @Override
    public String getFailureMessage() {
        BuffedItem item = plugin.getItemManager().getBuffedItem(requiredItemId);

        String displayName;
        if (item != null) {
            displayName = item.getDisplayName();
        } else {
            displayName = requiredItemId;
        }

        return ConfigManager.stripLegacy(failureMessage)
                .replace("{amount}", String.valueOf(amount))
                .replace("{item_name}", displayName)
                .replace("{item_id}", requiredItemId);
    }

    private boolean isMatchingItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String itemId = item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
        return requiredItemId.equals(itemId);
    }
}