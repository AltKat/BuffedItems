package io.github.altkat.BuffedItems.menu.utility;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ConfirmationMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemToDeleteId;

    public ConfirmationMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin, String itemToDeleteId) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemToDeleteId = itemToDeleteId;
    }

    @Override
    public String getMenuName() {
        return "Delete: " + itemToDeleteId + "?";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            case GREEN_WOOL:
                ConfigManager.setItemValue(itemToDeleteId, null, null);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aItem '" + itemToDeleteId + "' has been successfully deleted."));
                new ItemListMenu(playerMenuUtility, plugin).open();
                break;
            case RED_WOOL:
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cDeletion cancelled."));
                new ItemListMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(11, makeItem(Material.GREEN_WOOL, "§aConfirm Deletion", "§7This action cannot be undone."));
        inventory.setItem(15, makeItem(Material.RED_WOOL, "§cCancel", "§7Return to the main menu."));
        setFillerGlass();
    }
}