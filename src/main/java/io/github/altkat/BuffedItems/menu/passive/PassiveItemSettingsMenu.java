package io.github.altkat.BuffedItems.menu.passive;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.editor.ItemEditorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class PassiveItemSettingsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public PassiveItemSettingsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Passive Effects: " + itemId;
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        Material type = e.getCurrentItem().getType();

        if (type == Material.BARRIER) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        switch (type) {
            case POTION:
                new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.POTION_EFFECT).open();
                break;
            case IRON_SWORD:
                new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.ATTRIBUTE).open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        inventory.setItem(11, makeItem(Material.POTION, "§aPassive Potion Effects",
                "§7Add constant potion effects like",
                "§7Speed, Jump Boost, etc.",
                "",
                "§7(Requires selecting a slot)",
                "§eClick to Edit"));

        inventory.setItem(15, makeItem(Material.IRON_SWORD, "§bPassive Attributes",
                "§7Add permanent stats like",
                "§7Max Health, Damage, Speed, etc.",
                "",
                "§7(Requires selecting a slot)",
                "§eClick to Edit"));

        inventory.setItem(26, makeItem(Material.BARRIER, "§cBack to Main Editor"));
    }
}