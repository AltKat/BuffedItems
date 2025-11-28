package io.github.altkat.BuffedItems.menu;

import io.github.altkat.BuffedItems.menu.base.Menu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener implements Listener {

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        InventoryHolder holder = e.getInventory().getHolder();

        if (holder instanceof Menu) {
            e.setCancelled(true);
            if (e.getClickedInventory() == null) return;
            Menu menu = (Menu) holder;

            if (e.getClickedInventory() == e.getView().getBottomInventory()) {
                if (!menu.allowBottomInventoryClick()) {
                    return;
                }
            }

            menu.handleMenu(e);
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Menu menu) {
            menu.handleClose(e);
        }
    }
}