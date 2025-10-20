package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;




public class InventoryChangeListener implements Listener {

    private final BuffedItems plugin;
    private final NamespacedKey nbtKey;

    public InventoryChangeListener(BuffedItems plugin) {
        this.plugin = plugin;
        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
    }

    private boolean isBuffedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(nbtKey, PersistentDataType.STRING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            if (isBuffedItem(e.getCurrentItem()) || isBuffedItem(e.getCursor())) {
                scheduleInventoryCheck((Player) e.getWhoClicked());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            if (isBuffedItem(e.getOldCursor())) {
                scheduleInventoryCheck((Player) e.getWhoClicked());
                return;
            }
            for (ItemStack item : e.getNewItems().values()) {
                if (isBuffedItem(item)) {
                    scheduleInventoryCheck((Player) e.getWhoClicked());
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player) {
            if (isBuffedItem(e.getItem().getItemStack())) {
                scheduleInventoryCheck((Player) e.getEntity());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent e) {
        if (isBuffedItem(e.getItemDrop().getItemStack())) {
            scheduleInventoryCheck(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent e) {
        PlayerInventory inv = e.getPlayer().getInventory();
        ItemStack oldItem = inv.getItem(e.getPreviousSlot());
        ItemStack newItem = inv.getItem(e.getNewSlot());

        if (isBuffedItem(oldItem) || isBuffedItem(newItem)) {
            scheduleInventoryCheck(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        if (isBuffedItem(e.getMainHandItem()) || isBuffedItem(e.getOffHandItem())) {
            scheduleInventoryCheck(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent e) {
        if (isBuffedItem(e.getBrokenItem())) {
            scheduleInventoryCheck(e.getPlayer());
        }
    }

    private void scheduleInventoryCheck(Player player) {
        plugin.getEffectApplicatorTask().markPlayerForUpdate(player.getUniqueId());
        ConfigManager.sendDebugMessage(() -> "[InventoryChange] Marked " + player.getName() + " for update due to inventory change");
    }
}