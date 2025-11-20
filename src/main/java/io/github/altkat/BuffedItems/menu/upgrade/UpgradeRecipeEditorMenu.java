package io.github.altkat.BuffedItems.menu.upgrade;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.manager.upgrade.UpgradeRecipe;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.BuffedItemSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class UpgradeRecipeEditorMenu extends Menu {

    private final BuffedItems plugin;
    private final String recipeId;

    public UpgradeRecipeEditorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.recipeId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Editing: " + recipeId;
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
            case 10:
                askInput(p, "upgrade.display_name", "§aEnter new Display Name (Color codes supported).");
                break;
            case 11:
                askInput(p, "upgrade.success_rate", "§aEnter Success Rate (0-100).");
                break;
            case 12:
                boolean currentVal = UpgradesConfig.get().getBoolean("upgrades." + recipeId + ".prevent_failure_loss", false);
                ConfigManager.setUpgradeValue(recipeId, "prevent_failure_loss", !currentVal);
                this.open();
                break;

            case 13:
                new BuffedItemSelectorMenu(playerMenuUtility, plugin,
                        BuffedItemSelectorMenu.SelectionContext.BASE).open();
                break;
            case 14:
                new IngredientListMenu(playerMenuUtility, plugin).open();
                break;

            case 15:
                new BuffedItemSelectorMenu(playerMenuUtility, plugin,
                        BuffedItemSelectorMenu.SelectionContext.RESULT).open();
                break;
            case 16:
                askInput(p, "upgrade.result.amount", "§aEnter Result Amount.");
                break;

            case 22:
                new UpgradeRecipeListMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    private void askInput(Player p, String path, String msg) {
        playerMenuUtility.setWaitingForChatInput(true);
        playerMenuUtility.setChatInputPath(path);
        p.closeInventory();
        p.sendMessage(ConfigManager.fromSection(msg));
        p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        String path = "upgrades." + recipeId;
        String displayName = UpgradesConfig.get().getString(path + ".display_name", "Unknown");
        double successRate = UpgradesConfig.get().getDouble(path + ".success_rate", 100);
        boolean preventLoss = UpgradesConfig.get().getBoolean(path + ".prevent_failure_loss", false);

        String resultItemId = UpgradesConfig.get().getString(path + ".result.item", "None");
        int resultAmount = UpgradesConfig.get().getInt(path + ".result.amount", 1);

        String resultDisplayName = "§f" + resultItemId;
        BuffedItem resultItem = plugin.getItemManager().getBuffedItem(resultItemId);
        if (resultItem != null) {
            resultDisplayName = ConfigManager.toSection(ConfigManager.fromLegacy(resultItem.getDisplayName()));
        }

        String baseDisplayName = "§cNone";
        String baseIdDisplay = "";

        if (UpgradesConfig.get().contains(path + ".base")) {
            ICost baseCost = plugin.getCostManager().parseCost(UpgradesConfig.get().getConfigurationSection(path + ".base").getValues(false));

            if (baseCost != null) {
                baseDisplayName = "§f" + baseCost.getDisplayString();

                if (baseCost instanceof io.github.altkat.BuffedItems.manager.cost.types.BuffedItemCost) {
                    String bId = ((io.github.altkat.BuffedItems.manager.cost.types.BuffedItemCost) baseCost).getRequiredItemId();
                    BuffedItem bItem = plugin.getItemManager().getBuffedItem(bId);

                    if (bItem != null) {
                        baseDisplayName = ConfigManager.toSection(ConfigManager.fromLegacy(bItem.getDisplayName()));
                    } else {
                        baseDisplayName = "§f" + bId;
                    }
                    baseIdDisplay = "§8(ID: " + bId + ")";
                }
            }
        }

        int ingCount = UpgradesConfig.get().getMapList(path + ".ingredients").size();

        inventory.setItem(10, makeItem(Material.NAME_TAG, "§eDisplay Name", "§7Current: §r" + ConfigManager.toSection(ConfigManager.fromLegacy(displayName)), "", "§aClick to Edit"));
        inventory.setItem(11, makeItem(Material.DIAMOND, "§bSuccess Rate", "§7Current: §e" + successRate + "%", "", "§aClick to Edit"));
        inventory.setItem(12, makeItem(Material.SHIELD, "§6Prevent Failure Loss", "§7Current: " + (preventLoss ? "§aTRUE" : "§cFALSE"), "", "§aClick to Toggle"));

        List<String> baseLore = new ArrayList<>();
        baseLore.add("§7Current: " + baseDisplayName);
        if (!baseIdDisplay.isEmpty()) baseLore.add(baseIdDisplay);
        baseLore.add("");
        baseLore.add("§eClick to Select (Input Item)");
        inventory.setItem(13, makeItem(Material.ANVIL, "§aSet Base Item", baseLore.toArray(new String[0])));

        inventory.setItem(14, makeItem(Material.HOPPER, "§bManage Ingredients", "§7Current: §e" + ingCount + " costs", "", "§eClick to Edit List"));

        inventory.setItem(15, makeItem(Material.CHEST, "§dResult Item",
                "§7Current: " + resultDisplayName,
                "§8(ID: " + resultItemId + ")",
                "",
                "§aClick to Select"));

        inventory.setItem(16, makeItem(Material.GOLD_NUGGET, "§6Result Amount", "§7Current: §e" + resultAmount, "", "§aClick to Edit"));

        inventory.setItem(22, makeItem(Material.BARRIER, "§cBack"));
    }
}