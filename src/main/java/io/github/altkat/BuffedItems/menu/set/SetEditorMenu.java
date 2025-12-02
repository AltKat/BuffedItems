package io.github.altkat.BuffedItems.menu.set;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.set.BuffedSet;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SetEditorMenu extends Menu {

    private final BuffedItems plugin;
    private final String setId;

    public SetEditorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.setId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Edit Set: " + setId;
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
            case 11: // Display Name
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("set_display_name");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter new Display Name for the set."));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                break;

            case 13: // Manage Items
                new SetItemsMenu(playerMenuUtility, plugin).open();
                break;

            case 15: // Manage Bonuses
                new SetBonusesMenu(playerMenuUtility, plugin).open();
                break;

            case 22: // Back
                new SetListMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        BuffedSet set = plugin.getSetManager().getSet(setId);
        if (set == null) {
            inventory.setItem(13, makeItem(Material.BARRIER, "§cError", "§7Set not found."));
            return;
        }

        inventory.setItem(11, makeItem(Material.NAME_TAG, "§eDisplay Name",
                "§7Current: " + ConfigManager.toSection(ConfigManager.fromLegacy(set.getDisplayName())),
                "", "§aClick to Edit"));

        inventory.setItem(13, makeItem(Material.DIAMOND_CHESTPLATE, "§bManage Items",
                "§7Add or remove items",
                "§7from this set.",
                "",
                "§7Count: §f" + set.getItemIds().size(),
                "", "§eClick to Manage"));

        inventory.setItem(15, makeItem(Material.ENCHANTING_TABLE, "§dManage Bonuses",
                "§7Configure effects gained",
                "§7by wearing set pieces.",
                "",
                "§7Tiers: §f" + set.getBonuses().size(),
                "", "§eClick to Manage"));

        inventory.setItem(22, makeItem(Material.BARRIER, "§cBack"));
    }
}