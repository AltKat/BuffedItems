package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ActiveItemVisualsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public ActiveItemVisualsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Visual Settings: " + itemId;
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;
        
        switch (e.getSlot()) {
            case 11:
                new ActiveItemCooldownVisualsMenu(playerMenuUtility, plugin).open();
                break;
            case 15:
                new ActiveItemCastVisualsMenu(playerMenuUtility, plugin).open();
                break;
            case 22:
                new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        ItemStack cooldown = makeItem(Material.CLOCK, "§eCooldown Visuals",
                "§7Configure visuals that appear",
                "§7when the item is on cooldown.", "",
                "§eClick to Open");

        ItemStack cast = makeItem(Material.BLAZE_ROD, "§eCast Visuals",
                "§7Configure visuals that appear",
                "§7when the ability is cast.", "",
                "§eClick to Open");

        inventory.setItem(11, cooldown);
        inventory.setItem(15, cast);
        inventory.setItem(22, makeItem(Material.BARRIER, "§cBack"));
    }
}
