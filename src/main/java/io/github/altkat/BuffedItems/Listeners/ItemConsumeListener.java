package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
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

        if (consumedItem.getItemMeta().getPersistentDataContainer().has(nbtKey, PersistentDataType.STRING)) {
            e.setCancelled(true);
        }
    }
}