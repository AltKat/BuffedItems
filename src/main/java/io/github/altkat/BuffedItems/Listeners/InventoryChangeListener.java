package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.Bukkit;
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

/**
 * Bu listener oyuncunun envanterinde değişiklik olduğunda cache'i temizler.
 * Böylece EffectApplicatorTask bir sonraki tick'te yeni tarama yapar.
 */
public class InventoryChangeListener implements Listener {

    private final BuffedItems plugin;

    public InventoryChangeListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            scheduleInventoryCheck((Player) e.getWhoClicked());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            scheduleInventoryCheck((Player) e.getWhoClicked());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player) {
            scheduleInventoryCheck((Player) e.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent e) {
        scheduleInventoryCheck(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent e) {
        scheduleInventoryCheck(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        scheduleInventoryCheck(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent e) {
        scheduleInventoryCheck(e.getPlayer());
    }

    /**
     * Oyuncunun cache'ini temizler ve bir sonraki tick'te tarama yapılmasını sağlar.
     * Birden fazla event aynı anda tetiklenirse, sadece bir kez temizlenir.
     */
    private void scheduleInventoryCheck(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getEffectApplicatorTask().invalidateCache(player.getUniqueId());
            ConfigManager.sendDebugMessage("[InventoryChange] Cache invalidated for " + player.getName() + " due to inventory change");
        }, 1L);
    }
}