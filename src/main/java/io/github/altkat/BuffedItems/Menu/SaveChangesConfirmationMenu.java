package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SaveChangesConfirmationMenu extends Menu {

    private final BuffedItems plugin;

    public SaveChangesConfirmationMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "You have unsaved changes!";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null) return;

        playerMenuUtility.setNavigating(true);

        switch (e.getCurrentItem().getType()) {
            case RED_WOOL:
                ConfigManager.discardChanges();
                p.sendMessage("§cUnsaved changes have been discarded.");
                new MainMenu(playerMenuUtility, plugin).open();
                break;
            case GREEN_WOOL:
                new ItemEditorMenu(playerMenuUtility, plugin).open();
                break;
            default:
                playerMenuUtility.setNavigating(false);
                break;
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(11, makeItem(Material.RED_WOOL, "§cDiscard Changes & Exit", "§7All changes made will be lost."));
        inventory.setItem(15, makeItem(Material.GREEN_WOOL, "§aCancel", "§7Return to the item editor."));
        setFillerGlass();
    }
}