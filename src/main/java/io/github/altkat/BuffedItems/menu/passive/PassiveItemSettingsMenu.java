package io.github.altkat.BuffedItems.menu.passive;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.editor.ItemEditorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
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
        Player p = (Player) e.getWhoClicked();

        if (e.getSlot() == 8) {
            BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
            if (item != null) {
                BuffedItem.AttributeMode current = item.getPassiveEffects().getAttributeMode();
                BuffedItem.AttributeMode next = (current == BuffedItem.AttributeMode.STATIC)
                        ? BuffedItem.AttributeMode.DYNAMIC
                        : BuffedItem.AttributeMode.STATIC;

                ConfigManager.setItemValue(item.getId(), "passive_effects.attribute_mode", next.name());
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                this.open();
            }
            return;
        }

        if (type == Material.BARRIER) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() == 13) {
            new PassiveItemVisualsMenu(playerMenuUtility, plugin).open();
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

        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item != null) {
            BuffedItem.AttributeMode attrMode = item.getPassiveEffects().getAttributeMode();
            String modeColor = (attrMode == BuffedItem.AttributeMode.DYNAMIC) ? "§b" : "§7";
            Material modeIcon = (attrMode == BuffedItem.AttributeMode.DYNAMIC) ? Material.GOLD_BLOCK : Material.IRON_BLOCK;

            inventory.setItem(8, makeItem(modeIcon, "§6Attribute Mode",
                    "§7Current: " + modeColor + attrMode.name(),
                    "",
                    "§fSTATIC (Iron): §7Native NBT Attributes.",
                    "§7✔ §aBest Performance (Zero Latency)",
                    "§7✔ §aUniversal (NPCs, Minions)",
                    "§7✖ §cIgnores Passive Permissions",
                    "",
                    "§bDYNAMIC (Gold): §7Plugin Managed.",
                    "§7✔ §bEnforces Permissions",
                    "§7✖ §cReal Players Only",
                    "§7✖ §cSlight Update Delay (Task)",
                    "",
                    "§eClick to Toggle"));
        }

        inventory.setItem(11, makeItem(Material.POTION, "§aPassive Potion Effects",
                "§7Add constant potion effects like",
                "§7Speed, Jump Boost, etc.",
                "",
                "§7(Requires selecting a slot)",
                "§eClick to Edit"));

        inventory.setItem(13, makeItem(Material.GLOW_INK_SAC, "§dVisual Effects",
                "§7Add visual feedback when this",
                "§7item is held or worn.",
                "",
                "§7- BossBar, Action Bar",
                "§7- Title Alerts, Sounds",
                "",
                "§eClick to Configure"));

        inventory.setItem(15, makeItem(Material.IRON_SWORD, "§bPassive Attributes",
                "§7Add permanent stats like",
                "§7Max Health, Damage, Speed, etc.",
                "",
                "§7(Requires selecting a slot)",
                "§eClick to Edit"));

        inventory.setItem(26, makeItem(Material.BARRIER, "§cBack to Main Editor"));
    }
}