package io.github.altkat.BuffedItems.menu.upgrade;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.BuffedItemSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public class IngredientTypeSelectorMenu extends Menu {

    private final BuffedItems plugin;

    public IngredientTypeSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Select Ingredient Type";
    }

    @Override
    public int getSlots() {
        return 36;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        if (e.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) return;

        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new IngredientListMenu(playerMenuUtility, plugin).open();
            return;
        }

        String type = e.getCurrentItem().getItemMeta().getDisplayName().substring(2);

        if (type.equals("BUFFED_ITEM")) {
            new BuffedItemSelectorMenu(playerMenuUtility, plugin,
                    BuffedItemSelectorMenu.SelectionContext.INGREDIENT).open();
            return;
        }

        String chatPath = "upgrade.ingredients.add.";

        playerMenuUtility.setWaitingForChatInput(true);
        playerMenuUtility.setChatInputPath(chatPath + type);

        e.getWhoClicked().closeInventory();
        e.getWhoClicked().sendMessage(ConfigManager.fromSection("§aSelected: " + type));

        if (type.equals("ITEM")) {
            e.getWhoClicked().sendMessage(ConfigManager.fromSection("§eFormat: AMOUNT;MATERIAL (e.g. 1;DIAMOND)"));
        }else if (type.equals("COINSENGINE")) {
            e.getWhoClicked().sendMessage(ConfigManager.fromSection("§eFormat: AMOUNT;CURRENCY_ID (e.g. 100;gold)"));
            e.getWhoClicked().sendMessage(ConfigManager.fromSection("§7(Or just AMOUNT for default 'coins')"));
        }else {
            e.getWhoClicked().sendMessage(ConfigManager.fromSection("§aEnter amount:"));
        }
    }

    @Override
    public void setMenuItems() {

        inventory.setItem(10, makeItem(Material.NETHER_STAR, "§aBUFFED_ITEM", "§7Custom Items"));
        inventory.setItem(11, makeItem(Material.CHEST, "§aITEM", "§7Vanilla Items"));
        inventory.setItem(12, makeItem(Material.GOLD_INGOT, "§aMONEY", "§7Vault Currency"));
        inventory.setItem(13, makeItem(Material.EXPERIENCE_BOTTLE, "§aEXPERIENCE", "§7XP Points"));
        inventory.setItem(14, makeItem(Material.ENCHANTING_TABLE, "§aLEVEL", "§7XP Levels"));
        inventory.setItem(15, makeItem(Material.COOKED_BEEF, "§aHUNGER", "§7Food Level"));
        inventory.setItem(16, makeItem(Material.RED_DYE, "§aHEALTH", "§7Health Points"));

        if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") != null) {
            inventory.setItem(19, makeItem(Material.SUNFLOWER, "§aCOINSENGINE", "§7Custom Currency"));
        }

        inventory.setItem(31, makeItem(Material.BARRIER, "§cCancel"));

        setFillerGlass();
    }
}