package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.active.ActiveItemVisualsMenu;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.passive.PassiveItemVisualsMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
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
        if (e.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        boolean isPassive = "passive_visuals".equals(playerMenuUtility.getChatInputPath());
        String configPath = isPassive ? "passive_effects.visuals.boss-bar.style" : "active_ability.visuals.cooldown.boss-bar.style";

        if (e.getCurrentItem().getType() == Material.BARRIER) {
            openPrevious(isPassive);
            return;
        }

        if (e.getSlot() < 9) {
            String styleName = e.getCurrentItem().getItemMeta().getDisplayName().substring(2);
            try {
                BarStyle style = BarStyle.valueOf(styleName);
                ConfigManager.setItemValue(itemId, configPath, style.name());
                openPrevious(isPassive);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void openPrevious(boolean passive) {
        if (passive) {
            new PassiveItemVisualsMenu(playerMenuUtility, plugin).open();
        } else {
            new ActiveItemVisualsMenu(playerMenuUtility, plugin).open();
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