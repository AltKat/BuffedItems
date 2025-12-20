package io.github.altkat.BuffedItems.menu.upgrade;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.manager.cost.types.BuffedItemCost;
import io.github.altkat.BuffedItems.manager.upgrade.UpgradeRecipe;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.MainMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class UpgradeRecipeListMenu extends PaginatedMenu {

    private final BuffedItems plugin;

    public UpgradeRecipeListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.maxItemsPerPage = 36;
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

        if (e.getSlot() == 51) {
            boolean current = UpgradesConfig.get().getBoolean("settings.browser-button", true);
            UpgradesConfig.get().set("settings.browser-button", !current);
            UpgradesConfig.saveAsync();
            UpgradesConfig.reload();

            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            this.open();
            return;
        }

        if (e.getSlot() == 53) {
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() >= 9 && e.getSlot() < 45) {
            int index = maxItemsPerPage * page + (e.getSlot() - 9);
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
        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);

        addMenuControls();

        inventory.setItem(49, makeItem(Material.ANVIL, "§aCreate New Recipe", "§7Click to create a new upgrade recipe."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack to Main Menu"));

        boolean browserEnabled = UpgradesConfig.get().getBoolean("settings.browser-button", true);
        String status = browserEnabled ? "§aEnabled" : "§cDisabled";

        inventory.setItem(51, makeItem(Material.BOOKSHELF,
                "§6Station Browser Button",
                "§7Toggle the visibility of the",
                "§7'Browse All Recipes' button",
                "§7in the Upgrade Station (/bi upgrade).",
                "",
                "§7Current: " + status,
                "",
                "§eClick to Toggle"));

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

            if (recipe.getResultAmount() > 1) {
                resultName += " §e(x" + recipe.getResultAmount() + ")";
            }

            List<String> lore = new ArrayList<>();
            lore.add("§8ID: " + recipe.getId());
            lore.add("");

            String baseName = "§cUnknown";
            if (recipe.getBaseCost() != null) {
                baseName = "§f" + recipe.getBaseCost().getDisplayString();
                if (recipe.getBaseCost() instanceof BuffedItemCost) {
                    String bId = ((BuffedItemCost) recipe.getBaseCost()).getRequiredItemId();
                    BuffedItem bItem = plugin.getItemManager().getBuffedItem(bId);
                    if (bItem != null) {
                        baseName = ConfigManager.toSection(ConfigManager.fromLegacy(bItem.getDisplayName()));
                    }
                }
            }

            lore.add("§7From: " + baseName);
            lore.add("§7To: " + resultName);
            lore.add("");

            String chanceColor = (recipe.getSuccessRate() >= 100) ? "§a" : (recipe.getSuccessRate() >= 50 ? "§e" : "§c");
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

            ItemStack iconStack;
            ICost baseCost = recipe.getBaseCost();

            if (!recipe.isValid()) {
                iconStack = new ItemStack(Material.BARRIER);
            }
            else {
                BuffedItemCost bCost = (BuffedItemCost) baseCost;
                String bId = bCost.getRequiredItemId();
                BuffedItem bItem = plugin.getItemManager().getBuffedItem(bId);

                if (bItem != null) {
                    iconStack = new ItemBuilder(bItem, plugin).build();
                } else {
                    iconStack = new ItemStack(Material.BEDROCK);
                }
            }

            ItemMeta meta = iconStack.getItemMeta();
            if (meta != null) {
                meta.displayName(ConfigManager.fromSection(title));

                List<net.kyori.adventure.text.Component> loreComps = new ArrayList<>();
                for (String line : lore) {
                    loreComps.add(ConfigManager.fromSection(line));
                }
                meta.lore(loreComps);

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_UNBREAKABLE);
                iconStack.setItemMeta(meta);
            }

            inventory.setItem(9 + i, iconStack);
        }
    }
}