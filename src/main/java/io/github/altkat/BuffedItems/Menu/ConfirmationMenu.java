package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
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

        switch (e.getCurrentItem().getType()) {
            case GREEN_WOOL:
                ConfigManager.setItemValue(itemToDeleteId, null, null);
                p.sendMessage("§aItem '" + itemToDeleteId + "' has been successfully deleted.");
                new MainMenu(playerMenuUtility, plugin).open();
                break;
            case RED_WOOL:
                p.sendMessage("§cDeletion cancelled.");
                new MainMenu(playerMenuUtility, plugin).open();
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