package io.github.altkat.BuffedItems.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryListener implements Listener {

    private final BuffedItems plugin;
    private final NamespacedKey nbtKey;

    private final Map<UUID, Long> lastCheck = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_MS = 100;

    public InventoryListener(BuffedItems plugin) {
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player) {

            ItemStack clicked = e.getCurrentItem();
            if (isBuffedItem(clicked)) {
                ItemStack updated = plugin.getItemUpdater().updateItem(clicked, (Player) e.getWhoClicked());
                if (updated != null && !updated.isSimilar(clicked)) {
                    e.setCurrentItem(updated);
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[LiveUpdate] Item updated in inventory click.");
                }
            }

            if (isBuffedItem(e.getCurrentItem()) || isBuffedItem(e.getCursor())) {
                scheduleInventoryCheckWithDebounce((Player) e.getWhoClicked());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            if (isBuffedItem(e.getOldCursor())) {
                scheduleInventoryCheckWithDebounce((Player) e.getWhoClicked());
                return;
            }

            for (ItemStack item : e.getNewItems().values()) {
                if (isBuffedItem(item)) {
                    scheduleInventoryCheckWithDebounce((Player) e.getWhoClicked());
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {
            ItemStack item = e.getItem().getItemStack();

            if (isBuffedItem(item)) {
                ItemStack updated = plugin.getItemUpdater().updateItem(item, player);
                if (updated != null && !updated.isSimilar(item)) {
                    e.getItem().setItemStack(updated);
                }
                scheduleInventoryCheckWithDebounce(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent e) {
        if (isBuffedItem(e.getItemDrop().getItemStack())) {
            scheduleInventoryCheckWithDebounce(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItem(e.getNewSlot());

        if (isBuffedItem(item)) {
            ItemStack updated = plugin.getItemUpdater().updateItem(item, player);
            if (updated != null && !updated.isSimilar(item)) {
                player.getInventory().setItem(e.getNewSlot(), updated);
            }
            scheduleInventoryCheckWithDebounce(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();

        ItemStack mainItem = e.getMainHandItem();
        if (isBuffedItem(mainItem)) {
            ItemStack updated = plugin.getItemUpdater().updateItem(mainItem, player);
            if (updated != null && !updated.isSimilar(mainItem)) {
                e.setMainHandItem(updated);
            }
        }

        ItemStack offItem = e.getOffHandItem();
        if (isBuffedItem(offItem)) {
            ItemStack updated = plugin.getItemUpdater().updateItem(offItem, player);
            if (updated != null && !updated.isSimilar(offItem)) {
                e.setOffHandItem(updated);
            }
        }

        scheduleInventoryCheckWithDebounce(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemBreak(PlayerItemBreakEvent e) {
        if (isBuffedItem(e.getBrokenItem())) {
            scheduleInventoryCheckWithDebounce(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent e) {
        Player player = e.getPlayer();
        ItemStack oldItem = e.getOldItem();
        ItemStack newItem = e.getNewItem();

        if ((oldItem == null || oldItem.getType().isAir()) &&
                (newItem == null || newItem.getType().isAir())) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () ->
                "[ArmorChange] Armor change detected for " + player.getName() +
                        " in slot " + e.getSlotType().name() + ". Marking IMMEDIATE update.");

        plugin.getEffectApplicatorTask().markPlayerForUpdate(playerUUID);
    }

    public void clearPlayerData(UUID uuid) {
        this.lastCheck.remove(uuid);
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                () -> "[InventoryChange] Cleared cached data for player: " + uuid);
    }

    @EventHandler
    public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();

        if (e.getInventory().getHolder() instanceof Menu) {
            PlayerMenuUtility pmu = BuffedItems.getPlayerMenuUtility(p);

            if (pmu.isWaitingForChatInput()) {
                return;
            }

            if (pmu.hasUnsavedChanges()) {

                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!p.isOnline()) return;
                    if (p.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                        return;
                    }

                    RecipesConfig.save();
                    plugin.getCraftingManager().loadRecipes(true);

                    pmu.setUnsavedChanges(false);
                    p.sendMessage(ConfigManager.fromSectionWithPrefix("§eUnsaved crafting changes have been auto-saved."));

                }, 1L);
            }
        }
    }
}