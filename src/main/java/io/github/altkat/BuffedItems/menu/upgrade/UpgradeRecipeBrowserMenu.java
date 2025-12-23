package io.github.altkat.BuffedItems.menu.upgrade;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.manager.cost.types.BuffedItemCost;
import io.github.altkat.BuffedItems.manager.upgrade.UpgradeRecipe;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class UpgradeRecipeBrowserMenu extends PaginatedMenu {

    private final BuffedItems plugin;

    public UpgradeRecipeBrowserMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.maxItemsPerPage = 36;
    }

    @Override
    public String getMenuName() {
        return "Available Upgrades (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        List<UpgradeRecipe> recipes = getSortedRecipes();
        if (handlePageChange(e, recipes.size())) return;

        if (e.getCurrentItem().getType() == Material.BARRIER && e.getSlot() == 49) {
            new UpgradeMenu(playerMenuUtility, plugin).open();
            return;
        }
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);

        addMenuControls();

        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack to Station"));

        List<UpgradeRecipe> recipes = getSortedRecipes();

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= recipes.size()) break;

            UpgradeRecipe recipe = recipes.get(index);
            inventory.setItem(9 + i, createRecipeIcon(recipe));
        }
    }

    private ItemStack createRecipeIcon(UpgradeRecipe recipe) {
        BuffedItem resultItem = plugin.getItemManager().getBuffedItem(recipe.getResultItemId());
        ItemStack icon;
        String resultName;

        if (resultItem != null) {
            icon = new ItemBuilder(resultItem, plugin).build();
            resultName = ConfigManager.toSection(ConfigManager.fromLegacy(resultItem.getItemDisplay().getDisplayName()));
        } else {
            icon = new ItemStack(Material.BARRIER);
            resultName = "§cUnknown Item";
        }

        icon.setAmount(Math.max(1, recipe.getResultAmount()));

        List<Component> lore = new ArrayList<>();
        lore.add(ConfigManager.fromSection(""));
        lore.add(ConfigManager.fromSection("§7Recipe: §f" + ConfigManager.toSection(ConfigManager.fromLegacy(recipe.getDisplayName()))));
        lore.add(ConfigManager.fromSection(""));

        String baseName = "§cUnknown";
        if (recipe.getBaseCost() instanceof BuffedItemCost) {
            String bId = ((BuffedItemCost) recipe.getBaseCost()).getRequiredItemId();
            BuffedItem bItem = plugin.getItemManager().getBuffedItem(bId);
            if (bItem != null) {
                baseName = ConfigManager.toSection(ConfigManager.fromLegacy(bItem.getItemDisplay().getDisplayName()));
            } else {
                baseName = bId;
            }
        } else if (recipe.getBaseCost() != null) {
            baseName = recipe.getBaseCost().getDisplayString();
        }
        lore.add(ConfigManager.fromSection("§7From: §f" + baseName));
        lore.add(ConfigManager.fromSection("   §7⬇"));
        lore.add(ConfigManager.fromSection("§7To: §f" + resultName + (recipe.getResultAmount() > 1 ? " §e(x" + recipe.getResultAmount() + ")" : "")));
        lore.add(ConfigManager.fromSection(""));

        lore.add(ConfigManager.fromSection("§7Requires:"));
        for (ICost cost : recipe.getIngredients()) {
            lore.add(ConfigManager.fromSection(" §8- §7" + cost.getDisplayString()));
        }

        lore.add(ConfigManager.fromSection(""));
        String chanceColor = (recipe.getSuccessRate() >= 100) ? "§a" : (recipe.getSuccessRate() >= 50 ? "§e" : "§c");
        lore.add(ConfigManager.fromSection("§7Chance: " + chanceColor + recipe.getSuccessRate() + "%"));

        String risk = switch (recipe.getFailureAction()) {
            case KEEP_EVERYTHING -> "§aSafe (No Loss)";
            case KEEP_BASE_ONLY -> "§eModerate (Item Kept)";
            case LOSE_EVERYTHING -> "§cRisky (Lose All)";
        };
        lore.add(ConfigManager.fromSection("§7Risk: " + risk));

        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ENCHANTS);
            icon.setItemMeta(meta);
        }

        return icon;
    }

    private List<UpgradeRecipe> getSortedRecipes() {
        List<UpgradeRecipe> recipes = new ArrayList<>(plugin.getUpgradeManager().getRecipes().values());
        recipes.removeIf(r -> !r.isValid());
        recipes.sort(Comparator.comparing(UpgradeRecipe::getId));
        return recipes;
    }
}