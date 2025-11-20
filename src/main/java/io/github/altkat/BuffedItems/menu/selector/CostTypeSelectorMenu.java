package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.active.CostListMenu;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CostTypeSelectorMenu extends Menu {

    private final BuffedItems plugin;

    public CostTypeSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Select Cost Type";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new CostListMenu(playerMenuUtility, plugin).open();
            return;
        }

        String type = e.getCurrentItem().getItemMeta().getDisplayName().substring(2);
        Player p = (Player) e.getWhoClicked();

        if (type.equals("BUFFED_ITEM")) {
            new BuffedItemSelectorMenu(playerMenuUtility, plugin,
                    BuffedItemSelectorMenu.SelectionContext.COST).open();
            return;
        }

        playerMenuUtility.setWaitingForChatInput(true);
        playerMenuUtility.setChatInputPath("active.costs.add." + type);

        p.closeInventory();
        p.sendMessage(ConfigManager.fromSection("§aSelected Type: " + type));

        if (type.equals("ITEM")) {
            p.sendMessage(ConfigManager.fromSection("§eFormat: AMOUNT;MATERIAL"));
            p.sendMessage(ConfigManager.fromSection("§7Example: 1;DIAMOND"));
        }
        else if (type.equals("BUFFED_ITEM")) {
            p.sendMessage(ConfigManager.fromSection("§eFormat: AMOUNT;BUFFED_ITEM_ID"));
            p.sendMessage(ConfigManager.fromSection("§7Example: 1;warriors_talisman"));
        }
        else if (type.equals("COINSENGINE")) {
            p.sendMessage(ConfigManager.fromSection("§eFormat: AMOUNT;CURRENCY_ID"));
            p.sendMessage(ConfigManager.fromSection("§7Example: 100;coins"));
            p.sendMessage(ConfigManager.fromSection("§7(If you use default currency 'coins', you can just type the amount)"));
        }
        else {
            p.sendMessage(ConfigManager.fromSection("§aPlease enter the Amount in chat."));
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        inventory.setItem(10, makeItem(Material.GOLD_INGOT, "§aMONEY", "§7Vault Currency"));
        inventory.setItem(11, makeItem(Material.EXPERIENCE_BOTTLE, "§aEXPERIENCE", "§7XP Points"));
        inventory.setItem(12, makeItem(Material.ENCHANTING_TABLE, "§aLEVEL", "§7XP Levels"));
        inventory.setItem(13, makeItem(Material.COOKED_BEEF, "§aHUNGER", "§7Food Level"));
        inventory.setItem(14, makeItem(Material.RED_DYE, "§aHEALTH", "§7Health Points (Hearts)"));
        inventory.setItem(15, makeItem(Material.CHEST, "§aITEM", "§7Physical Items"));
        inventory.setItem(16, makeItem(Material.NETHER_STAR, "§aBUFFED_ITEM", "§7Custom Buffed Items"));
        if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") != null) {
            inventory.setItem(17, makeItem(Material.SUNFLOWER, "§aCOINSENGINE", "§7CoinsEngine Currency"));
        }

        inventory.setItem(22, makeItem(Material.BARRIER, "§cCancel"));
    }
}