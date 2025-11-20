package io.github.altkat.BuffedItems.menu.upgrade;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.upgrade.UpgradeRecipe;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.MainMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class UpgradeRecipeListMenu extends PaginatedMenu {

    private final BuffedItems plugin;

    public UpgradeRecipeListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Upgrade Recipes";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    private List<UpgradeRecipe> getSortedRecipes() {
        List<UpgradeRecipe> recipes = new ArrayList<>(plugin.getUpgradeManager().getRecipes().values());
        recipes.sort(Comparator.comparing(UpgradeRecipe::getId));
        return recipes;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        List<UpgradeRecipe> recipes = getSortedRecipes();

        if (e.getCurrentItem() == null) return;
        if (handlePageChange(e, recipes.size())) return;

        if (e.getSlot() == 49) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("create_upgrade");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter a unique ID for the new upgrade recipe (e.g. 'sword_upgrade')."));
            return;
        }

        if (e.getSlot() == 53) {
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() < 45) {
            int index = maxItemsPerPage * page + e.getSlot();
            if (index >= recipes.size()) return;

            UpgradeRecipe recipe = recipes.get(index);

            if (e.getClick() == ClickType.LEFT) {
                playerMenuUtility.setItemToEditId(recipe.getId());
                new UpgradeRecipeEditorMenu(playerMenuUtility, plugin).open();
            } else if (e.getClick() == ClickType.RIGHT) {
                ConfigManager.setUpgradeValue(recipe.getId(), null, null);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cUpgrade recipe '" + recipe.getId() + "' deleted."));
                this.open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuControls();
        setFillerGlass();

        inventory.setItem(49, makeItem(Material.ANVIL, "§aCreate New Recipe", "§7Click to create a new upgrade recipe."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack to Main Menu"));

        List<UpgradeRecipe> recipes = getSortedRecipes();

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= recipes.size()) break;

            UpgradeRecipe recipe = recipes.get(index);

            String title = ConfigManager.toSection(ConfigManager.fromLegacy(recipe.getDisplayName()));

            String resultName = "§f" + recipe.getResultItemId();
            BuffedItem resultItem = plugin.getItemManager().getBuffedItem(recipe.getResultItemId());
            if (resultItem != null) {
                resultName = ConfigManager.toSection(ConfigManager.fromLegacy(resultItem.getDisplayName()));
            }

            String baseName = "§cUnknown";
            if (recipe.getBaseCost() != null) {
                baseName = "§f" + recipe.getBaseCost().getDisplayString();

                if (recipe.getBaseCost() instanceof io.github.altkat.BuffedItems.manager.cost.types.BuffedItemCost) {
                    String bId = ((io.github.altkat.BuffedItems.manager.cost.types.BuffedItemCost) recipe.getBaseCost()).getRequiredItemId();
                    BuffedItem bItem = plugin.getItemManager().getBuffedItem(bId);
                    if (bItem != null) {
                        baseName = ConfigManager.toSection(ConfigManager.fromLegacy(bItem.getDisplayName()));
                    }
                }
            }

            String chanceColor = (recipe.getSuccessRate() >= 100) ? "§a" : (recipe.getSuccessRate() >= 50 ? "§e" : "§c");
            Material icon = recipe.isValid() ? Material.PAPER : Material.BARRIER;

            List<String> lore = new ArrayList<>();
            lore.add("§8ID: " + recipe.getId());
            lore.add("");

            lore.add("§7From: " + baseName);
            lore.add("§7To: " + resultName);
            lore.add("");

            lore.add("§7Chance: " + chanceColor + recipe.getSuccessRate() + "%");
            lore.add("§7Ingredients: §f" + recipe.getIngredients().size());

            if (!recipe.isValid()) {
                lore.add("");
                lore.add("§c§l⚠ CONFIGURATION ERRORS:");
                for (String err : recipe.getErrorMessages()) {
                    lore.add("§c- " + err);
                }
            }

            lore.add("");
            lore.add("§8§m------------------");
            lore.add("§eLeft-Click to Edit");
            lore.add("§cRight-Click to Delete");

            inventory.setItem(i, makeItem(icon, title, lore.toArray(new String[0])));
        }
    }
}