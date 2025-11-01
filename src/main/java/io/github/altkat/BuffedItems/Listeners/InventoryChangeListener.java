package io.github.altkat.BuffedItems.Listeners;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
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
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryChangeListener implements Listener {

    private final BuffedItems plugin;
    private final NamespacedKey nbtKey;

    private final Map<UUID, Long> lastCheck = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_MS = 200;

    public InventoryChangeListener(BuffedItems plugin) {
        this.plugin = plugin;
        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
    }

    private boolean isBuffedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(nbtKey, PersistentDataType.STRING);
    }

    private void scheduleInventoryCheckWithDebounce(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastCheck.get(uuid);

        if (last == null || (now - last) >= DEBOUNCE_MS) {
            lastCheck.put(uuid, now);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getEffectApplicatorTask().markPlayerForUpdate(player.getUniqueId());
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[InventoryChange] (Debounced) Marked " + player.getName() + " for update.");
            }, 1L);
        } else {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[InventoryChange] (Debounced) Skipped marking " + player.getName() + " (rate limited).");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            scheduleInventoryCheckWithDebounce((Player) e.getWhoClicked());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            scheduleInventoryCheckWithDebounce((Player) e.getWhoClicked());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player) {
            scheduleInventoryCheckWithDebounce((Player) e.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent e) {
        scheduleInventoryCheckWithDebounce(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent e) {
        PlayerInventory inv = e.getPlayer().getInventory();
        ItemStack oldItem = inv.getItem(e.getPreviousSlot());
        ItemStack newItem = inv.getItem(e.getNewSlot());

        if (isBuffedItem(oldItem) || isBuffedItem(newItem)) {
            scheduleInventoryCheckWithDebounce(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        if (isBuffedItem(e.getMainHandItem()) || isBuffedItem(e.getOffHandItem())) {
            scheduleInventoryCheckWithDebounce(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent e) {
        if (isBuffedItem(e.getBrokenItem())) {
            scheduleInventoryCheckWithDebounce(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent e) {
        Player player = e.getPlayer();
        ItemStack oldItem = e.getOldItem();
        ItemStack newItem = e.getNewItem();

        if ((oldItem == null || oldItem.getType().isAir()) && (newItem == null || newItem.getType().isAir())) {
            return;
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () ->
                "[ArmorChangeListener] Armor change detected for " + player.getName() +
                        " in slot " + e.getSlotType().name() + ". Scheduling IMMEDIATE update.");

        plugin.getEffectApplicatorTask().markPlayerForUpdate(player.getUniqueId());
    }
}