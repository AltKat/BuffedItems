package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.Material;
import org.bukkit.boss.BarStyle;
import org.bukkit.event.inventory.InventoryClickEvent;

public class BossBarStyleMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public BossBarStyleMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Select BossBar Style";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new ActiveItemVisualsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() < 9) {
            String styleName = e.getCurrentItem().getItemMeta().getDisplayName().substring(2);
            try {
                BarStyle style = BarStyle.valueOf(styleName);
                ConfigManager.setItemValue(itemId, "visuals.boss-bar-style", style.name());
                new ActiveItemVisualsMenu(playerMenuUtility, plugin).open();
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        inventory.setItem(0, makeItem(Material.STONE_BRICKS, "§bSOLID", "§7Full continuous bar."));
        inventory.setItem(1, makeItem(Material.CRACKED_STONE_BRICKS, "§bSEGMENTED_6", "§7Divided into 6 parts."));
        inventory.setItem(2, makeItem(Material.MOSSY_STONE_BRICKS, "§bSEGMENTED_10", "§7Divided into 10 parts."));
        inventory.setItem(3, makeItem(Material.BRICKS, "§bSEGMENTED_12", "§7Divided into 12 parts."));
        inventory.setItem(4, makeItem(Material.NETHER_BRICKS, "§bSEGMENTED_20", "§7Divided into 20 parts."));

        inventory.setItem(22, makeItem(Material.BARRIER, "§cCancel"));
    }
}