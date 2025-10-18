package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ItemConsumeListener implements Listener {

    private final BuffedItems plugin;
    private final NamespacedKey nbtKey;

    public ItemConsumeListener(BuffedItems plugin) {
        this.plugin = plugin;
        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
    }

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent e) {
        ItemStack consumedItem = e.getItem();

        if (consumedItem == null || !consumedItem.hasItemMeta()) {
            return;
        }

        String itemId = consumedItem.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
        if (itemId == null) {
            return;
        }

        BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
        if (buffedItem == null) {
            return;
        }

        if (buffedItem.getFlag("PREVENT_CONSUME")) {
            e.setCancelled(true);
        }
    }
}