package io.github.altkat.BuffedItems.menu.upgrade;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.manager.cost.types.BuffedItemCost;
import io.github.altkat.BuffedItems.manager.upgrade.FailureAction;
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
                askInput(p, "upgrade.display_name", "§aEnter new Display Name (Color codes and hex colors supported).");
                break;
            case 11:
                askInput(p, "upgrade.success_rate", "§aEnter Success Rate (0-100).");
                break;
            case 12:
                String path = "upgrades." + recipeId + ".failure_action";
                String currentStr = UpgradesConfig.get().getString(path, "LOSE_EVERYTHING");

                FailureAction currentAction;
                try {
                    currentAction = FailureAction.valueOf(currentStr);
                } catch(Exception ex) { currentAction = FailureAction.LOSE_EVERYTHING; }

                FailureAction nextAction;
                switch (currentAction) {
                    case LOSE_EVERYTHING: nextAction = FailureAction.KEEP_BASE_ONLY; break;
                    case KEEP_BASE_ONLY: nextAction = FailureAction.KEEP_EVERYTHING; break;
                    default: nextAction = FailureAction.LOSE_EVERYTHING; break;
                }

                ConfigManager.setUpgradeValue(recipeId, "failure_action", nextAction.name());
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
        p.sendMessage(ConfigManager.fromSectionWithPrefix(msg));
        p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        String path = "upgrades." + recipeId;
        String displayName = UpgradesConfig.get().getString(path + ".display_name", "Unknown");
        double successRate = UpgradesConfig.get().getDouble(path + ".success_rate", 100);

        String resultItemId = UpgradesConfig.get().getString(path + ".result.item", "None");
        int resultAmount = UpgradesConfig.get().getInt(path + ".result.amount", 1);

        String resultDisplayName = "§f" + resultItemId;
        BuffedItem resultItem = plugin.getItemManager().getBuffedItem(resultItemId);
        if (resultItem != null) {
            resultDisplayName = ConfigManager.toSection(ConfigManager.fromLegacy(resultItem.getItemDisplay().getDisplayName()));
        }

        String baseDisplayName = "§cNone";
        String baseIdDisplay = "";

        if (UpgradesConfig.get().contains(path + ".base")) {
            ICost baseCost = null;

            if (UpgradesConfig.get().isString(path + ".base")) {
                String bId = UpgradesConfig.get().getString(path + ".base");

                java.util.Map<String, Object> syntheticMap = new java.util.HashMap<>();
                syntheticMap.put("type", "BUFFED_ITEM");
                syntheticMap.put("amount", 1);
                syntheticMap.put("item_id", bId);

                baseCost = plugin.getCostManager().parseCost(syntheticMap);
            }

            if (baseCost != null) {
                baseDisplayName = "§f" + baseCost.getDisplayString();

                if (baseCost instanceof BuffedItemCost) {
                    String bId = ((BuffedItemCost) baseCost).getRequiredItemId();
                    BuffedItem bItem = plugin.getItemManager().getBuffedItem(bId);

                    if (bItem != null) {
                        baseDisplayName = ConfigManager.toSection(ConfigManager.fromLegacy(bItem.getItemDisplay().getDisplayName()));
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

        String failPath = "upgrades." + recipeId + ".failure_action";
        String failStr = UpgradesConfig.get().getString(failPath, "LOSE_EVERYTHING");
        FailureAction fAction;
        try {
            fAction = FailureAction.valueOf(failStr);
        } catch(Exception e) {
            fAction = FailureAction.LOSE_EVERYTHING;
        }

        Material iconMat;
        String actionName;
        String actionDesc;

        if (fAction == FailureAction.KEEP_EVERYTHING) {
            iconMat = Material.TOTEM_OF_UNDYING;
            actionName = "§aKeep Everything";
            actionDesc = "§7Nothing is lost on failure.";
        } else if (fAction == FailureAction.KEEP_BASE_ONLY) {
            iconMat = Material.IRON_CHESTPLATE;
            actionName = "§eKeep Base Item";
            actionDesc = "§7Ingredients lost, Item kept.";
        } else {
            iconMat = Material.SKELETON_SKULL;
            actionName = "§cLose Everything";
            actionDesc = "§7Item and ingredients lost.";
        }

        inventory.setItem(12, makeItem(iconMat,
                "§6Failure Action",
                "§7Current: " + actionName,
                actionDesc,
                "",
                "§eClick to Cycle",
                "§8(Lose All -> Keep Base -> Keep All)"));


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