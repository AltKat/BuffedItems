package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SlotSelectionMenu extends Menu {

    private final BuffedItems plugin;
    private final MenuType menuType;

    public enum MenuType {
        POTION_EFFECT,
        ATTRIBUTE
    }

    public SlotSelectionMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin, MenuType menuType) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.menuType = menuType;
    }

    @Override
    public String getMenuName() {
        return "Select a Slot";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        String slotName = e.getCurrentItem().getItemMeta().getDisplayName().substring(2).replace(" ", "_").toUpperCase();
        playerMenuUtility.setTargetSlot(slotName);

        if (menuType == MenuType.POTION_EFFECT) {
            new PotionEffectListMenu(playerMenuUtility, plugin).open();
        } else if (menuType == MenuType.ATTRIBUTE) {
            new AttributeListMenu(playerMenuUtility, plugin).open();
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(10, makeItem(Material.DIAMOND_SWORD, "§aMain Hand", "§7Effects active when held in the main hand."));
        inventory.setItem(11, makeItem(Material.SHIELD, "§aOff Hand", "§7Effects active when held in the off hand."));
        inventory.setItem(12, makeItem(Material.DIAMOND_HELMET, "§aHelmet", "§7Effects active when worn as a helmet."));
        inventory.setItem(13, makeItem(Material.DIAMOND_CHESTPLATE, "§aChestplate", "§7Effects active when worn as a chestplate."));
        inventory.setItem(14, makeItem(Material.DIAMOND_LEGGINGS, "§aLeggings", "§7Effects active when worn as leggings."));
        inventory.setItem(15, makeItem(Material.DIAMOND_BOOTS, "§aBoots", "§7Effects active when worn as boots."));
        inventory.setItem(16, makeItem(Material.CHEST, "§aInventory", "§7Effects active anywhere in the inventory."));

        addBackButton(new ItemEditorMenu(playerMenuUtility, plugin));
        setFillerGlass();
    }
}