package io.github.altkat.BuffedItems.menu.set;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.passive.EffectListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SetBonusEffectSelectorMenu extends Menu {

    private final BuffedItems plugin;
    private final String setId;
    private final int count;

    public SetBonusEffectSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.setId = playerMenuUtility.getTempSetId();
        this.count = playerMenuUtility.getTempBonusCount();
    }

    @Override
    public String getMenuName() {
        return "Bonus Effects: " + setId + " (" + count + ")";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new SetBonusesMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getCurrentItem().getType() == Material.POTION) {
            new EffectListMenu(playerMenuUtility, plugin,
                    EffectListMenu.EffectType.POTION_EFFECT, "SET_BONUS").open();
        }
        else if (e.getCurrentItem().getType() == Material.IRON_SWORD) {
            new EffectListMenu(playerMenuUtility, plugin,
                    EffectListMenu.EffectType.ATTRIBUTE, "SET_BONUS").open();
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        inventory.setItem(11, makeItem(Material.POTION, "§aPotion Effects",
                "§7Add effects like Speed, Strength, etc.",
                "§7to this set bonus tier.",
                "", "§eClick to Edit"));

        inventory.setItem(15, makeItem(Material.IRON_SWORD, "§bAttributes",
                "§7Add stats like Max Health, Damage, etc.",
                "§7to this set bonus tier.",
                "", "§eClick to Edit"));

        inventory.setItem(22, makeItem(Material.BARRIER, "§cBack"));
    }
}