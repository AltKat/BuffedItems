package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.active.ActiveCastBossBarSettingsMenu;
import io.github.altkat.BuffedItems.menu.active.ActiveItemCooldownVisualsMenu;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.passive.BossBarSettingsMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.event.inventory.InventoryClickEvent;

public class BossBarColorMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public BossBarColorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Select BossBar Color";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        if (e.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        
        String mode = playerMenuUtility.getChatInputPath();
        String configPath;
        if ("passive_visuals".equals(mode)) {
            configPath = "passive_effects.visuals.boss-bar.color";
        } else if ("active_cast_visuals".equals(mode)) {
            configPath = "active_ability.visuals.cast.boss-bar.color";
        } else {
            configPath = "active_ability.visuals.cooldown.boss-bar.color";
        }

        if (e.getCurrentItem().getType() == Material.BARRIER) {
            openPrevious(mode);
            return;
        }

        if (e.getSlot() < 9) {
            String colorName = e.getCurrentItem().getItemMeta().getDisplayName().substring(2);
            try {
                BarColor color = BarColor.valueOf(colorName);
                ConfigManager.setItemValue(itemId, configPath, color.name());
                openPrevious(mode);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void openPrevious(String mode) {
        if ("passive_visuals".equals(mode)) {
            new BossBarSettingsMenu(playerMenuUtility, plugin).open();
        } else if ("active_cast_visuals".equals(mode)) {
            new ActiveCastBossBarSettingsMenu(playerMenuUtility, plugin).open();
        } else {
            new ActiveItemCooldownVisualsMenu(playerMenuUtility, plugin).open();
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        inventory.setItem(0, makeItem(Material.PINK_DYE, "§dPINK"));
        inventory.setItem(1, makeItem(Material.BLUE_DYE, "§9BLUE"));
        inventory.setItem(2, makeItem(Material.RED_DYE, "§cRED"));
        inventory.setItem(3, makeItem(Material.GREEN_DYE, "§aGREEN"));
        inventory.setItem(4, makeItem(Material.YELLOW_DYE, "§eYELLOW"));
        inventory.setItem(5, makeItem(Material.PURPLE_DYE, "§5PURPLE"));
        inventory.setItem(6, makeItem(Material.WHITE_DYE, "§fWHITE"));

        inventory.setItem(22, makeItem(Material.BARRIER, "§cCancel"));
    }
}