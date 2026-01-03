package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.active.CostListMenu;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.upgrade.IngredientListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility.MaterialSelectionContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class TypeSelectorMenu extends Menu {

    private final BuffedItems plugin;
    private final MaterialSelectionContext context;

    public TypeSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin, MaterialSelectionContext context) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.context = context;
    }

    @Override
    public String getMenuName() {
        return "Select " + (context == MaterialSelectionContext.COST ? "Cost" : "Ingredient") + " Type";
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
            if (context == MaterialSelectionContext.COST) {
                new CostListMenu(playerMenuUtility, plugin).open();
            } else {
                new IngredientListMenu(playerMenuUtility, plugin).open();
            }
            return;
        }

        String type = e.getCurrentItem().getItemMeta().getDisplayName().substring(2);
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem().getType() == Material.GRAY_DYE) {
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§cThis feature requires a plugin that is not installed."));
            return;
        }

        if (type.equals("BUFFED_ITEM")) {
            BuffedItemSelectorMenu.SelectionContext buffedContext =
                    (context == MaterialSelectionContext.COST)
                            ? BuffedItemSelectorMenu.SelectionContext.COST
                            : BuffedItemSelectorMenu.SelectionContext.INGREDIENT;

            new BuffedItemSelectorMenu(playerMenuUtility, plugin, buffedContext).open();
            return;
        }

        if (type.equals("ITEM")) {
            playerMenuUtility.setMaterialContext(context);
            new MaterialSelectorMenu(playerMenuUtility, plugin).open();
            return;
        }

        String prefix = (context == MaterialSelectionContext.COST)
                ? "active_ability.costs.add."
                : "upgrade.ingredients.add.";

        playerMenuUtility.setWaitingForChatInput(true);
        playerMenuUtility.setChatInputPath(prefix + type);

        p.closeInventory();
        p.sendMessage(ConfigManager.fromSectionWithPrefix("§aSelected Type: " + type));

        if (type.equals("COINSENGINE")) {
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§eFormat: AMOUNT;CURRENCY_ID"));
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§7Example: 100;coins"));
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§7(If you use default currency 'coins', you can just type the amount)"));
        } else {
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aPlease enter the Amount in chat."));
        }
        p.sendMessage(ConfigManager.fromSectionWithPrefix("§7(Type 'cancel' to exit)"));
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        // MONEY (Vault)
        if (plugin.getHookManager().isVaultLoaded()) {
            inventory.setItem(10, makeItem(Material.GOLD_INGOT, "§aMONEY", "§7Vault Currency"));
        } else {
            inventory.setItem(10, makeItem(Material.GRAY_DYE, "§7MONEY", "§cPlugin not installed", "§7Install Vault & Economy to use this feature."));
        }

        inventory.setItem(11, makeItem(Material.EXPERIENCE_BOTTLE, "§aEXPERIENCE", "§7XP Points"));
        inventory.setItem(12, makeItem(Material.ENCHANTING_TABLE, "§aLEVEL", "§7XP Levels"));
        inventory.setItem(13, makeItem(Material.COOKED_BEEF, "§aHUNGER", "§7Food Level"));
        inventory.setItem(14, makeItem(Material.RED_DYE, "§aHEALTH", "§7Health Points (Hearts)"));
        inventory.setItem(15, makeItem(Material.CHEST, "§aITEM", "§7Physical Items"));
        inventory.setItem(16, makeItem(Material.NETHER_STAR, "§aBUFFED_ITEM", "§7Custom Buffed Items"));

        // COINSENGINE
        if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") != null) {
            inventory.setItem(19, makeItem(Material.SUNFLOWER, "§aCOINSENGINE", "§7CoinsEngine Currency"));
        } else {
            inventory.setItem(19, makeItem(Material.GRAY_DYE, "§7COINSENGINE", "§cPlugin not installed", "§7Install CoinsEngine to use this feature."));
        }

        // AURASKILLS
        if (plugin.getHookManager().isAuraSkillsLoaded()) {
            inventory.setItem(20, makeItem(Material.LAPIS_LAZULI, "§aAURASKILLS_MANA", "§7AuraSkills Mana"));
        } else {
            inventory.setItem(20, makeItem(Material.GRAY_DYE, "§7AURASKILLS_MANA", "§cPlugin not installed", "§7Install AuraSkills to use this feature."));
        }

        inventory.setItem(31, makeItem(Material.BARRIER, "§cCancel"));
    }
}