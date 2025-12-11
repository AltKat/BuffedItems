package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.menu.active.CostListMenu;
import io.github.altkat.BuffedItems.menu.active.UsageLimitSettingsMenu;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.crafting.IngredientSettingsMenu;
import io.github.altkat.BuffedItems.menu.crafting.RecipeEditorMenu;
import io.github.altkat.BuffedItems.menu.set.SetItemsMenu;
import io.github.altkat.BuffedItems.menu.upgrade.IngredientListMenu;
import io.github.altkat.BuffedItems.menu.upgrade.UpgradeRecipeEditorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BuffedItemSelectorMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final SelectionContext context;
    private final List<BuffedItem> items;

    public enum SelectionContext {
        COST,
        INGREDIENT,
        BASE,
        RESULT,
        USAGE_LIMIT,
        SET_MEMBER,
        CRAFTING_RESULT,
        CRAFTING_INGREDIENT
    }

    public BuffedItemSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin, SelectionContext context) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.context = context;
        this.items = new ArrayList<>(plugin.getItemManager().getLoadedItems().values());
        this.items.sort(Comparator.comparing(BuffedItem::getId));
        this.maxItemsPerPage = 36;
    }

    @Override
    public String getMenuName() {
        return "Select Item (" + context.name() + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null) return;
        if (handlePageChange(e, items.size())) return;

        if (e.getSlot() == 49 && e.getCurrentItem().getType() == Material.PAPER) {
            handleManualInput(p);
            return;
        }

        if (e.getSlot() == 53 && e.getCurrentItem().getType() == Material.BARRIER) {
            handleBack();
            return;
        }

        if (e.getSlot() >= 9 && e.getSlot() < 45) {
            int index = maxItemsPerPage * page + (e.getSlot() - 9);
            if (index >= items.size()) return;

            BuffedItem selectedItem = items.get(index);
            handleSelection(p, selectedItem.getId());
        }
    }

    private void handleSelection(Player p, String itemId) {
        switch (context) {
            case BASE:
                ConfigManager.setUpgradeValue(playerMenuUtility.getItemToEditId(), "base", itemId);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aBase item updated to: §e" + itemId));
                new UpgradeRecipeEditorMenu(playerMenuUtility, plugin).open();
                break;

            case RESULT:
                ConfigManager.setUpgradeValue(playerMenuUtility.getItemToEditId(), "result.item", itemId);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aResult item updated to: §e" + itemId));
                new UpgradeRecipeEditorMenu(playerMenuUtility, plugin).open();
                break;

            case INGREDIENT:
                playerMenuUtility.setTempId(itemId);
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("upgrade.ingredients.add.BUFFED_ITEM_QUANTITY");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aSelected Item: §e" + itemId));
                p.sendMessage(ConfigManager.fromSection("§aPlease enter the Amount (Quantity) in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                break;

            case COST:
                playerMenuUtility.setTempId(itemId);
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("active.costs.add.BUFFED_ITEM_QUANTITY");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aSelected Item: §e" + itemId));
                p.sendMessage(ConfigManager.fromSection("§aPlease enter the Amount (Quantity) in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                break;

            case USAGE_LIMIT:
                ConfigManager.setItemValue(playerMenuUtility.getItemToEditId(), "usage-limit.transform-item", itemId);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aTransform Target updated to: §e" + itemId));
                new UsageLimitSettingsMenu(playerMenuUtility, plugin).open();
                break;

            case SET_MEMBER:
                String setId = playerMenuUtility.getItemToEditId();
                List<String> items = SetsConfig.get().getStringList("sets." + setId + ".items");

                if (items.contains(itemId)) {
                    p.sendMessage(ConfigManager.fromSectionWithPrefix("§cItem already in set."));
                } else {
                    items.add(itemId);
                    SetsConfig.get().set("sets." + setId + ".items", items);
                    SetsConfig.save();
                    plugin.getSetManager().loadSets(true);
                    p.sendMessage(ConfigManager.fromSectionWithPrefix("§aAdded " + itemId + " to set."));
                }
                new SetItemsMenu(playerMenuUtility, plugin).open();
                break;

            case CRAFTING_RESULT:
                String recipeId = playerMenuUtility.getRecipeToEditId();
                RecipesConfig.get().set("recipes." + recipeId + ".result.item", itemId);
                RecipesConfig.save();
                plugin.getCraftingManager().loadRecipes(true);

                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aRecipe result updated to: §e" + itemId));
                new RecipeEditorMenu(playerMenuUtility, plugin).open();
                break;

            case CRAFTING_INGREDIENT:
                playerMenuUtility.setTempId(itemId);
                new IngredientSettingsMenu(playerMenuUtility, plugin, true).open();
                break;
        }
    }

    private void handleManualInput(Player p) {
        playerMenuUtility.setWaitingForChatInput(true);
        p.closeInventory();

        switch (context) {
            case BASE:
                playerMenuUtility.setChatInputPath("upgrade.base.set_id");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Base Item ID manually."));
                break;

            case RESULT:
                playerMenuUtility.setChatInputPath("upgrade.result.item");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Result Item ID manually."));
                break;

            case INGREDIENT:
                playerMenuUtility.setChatInputPath("upgrade.ingredients.add.BUFFED_ITEM");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Buffed Item ID manually."));
                p.sendMessage(ConfigManager.fromSection("§eFormat: AMOUNT;ITEM_ID"));
                break;

            case COST:
                playerMenuUtility.setChatInputPath("active.costs.add.BUFFED_ITEM");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Buffed Item ID manually."));
                p.sendMessage(ConfigManager.fromSection("§eFormat: AMOUNT;ITEM_ID"));
                break;

            case USAGE_LIMIT:
                playerMenuUtility.setChatInputPath("usage-limit.transform-item");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Target Buffed Item ID manually."));
                break;

            case CRAFTING_RESULT:
                playerMenuUtility.setChatInputPath("recipe_result_manual");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Result Buffed Item ID manually."));
                break;

            case CRAFTING_INGREDIENT:
                playerMenuUtility.setChatInputPath("recipe_ingredient_buffed_manual");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Buffed Item ID manually."));
                break;
        }
        p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
    }

    private void handleBack() {
        switch (context) {
            case BASE:
            case RESULT:
                new UpgradeRecipeEditorMenu(playerMenuUtility, plugin).open();
                break;

            case INGREDIENT:
                new IngredientListMenu(playerMenuUtility, plugin).open();
                break;

            case COST:
                new CostListMenu(playerMenuUtility, plugin).open();
                break;

            case USAGE_LIMIT:
                new UsageLimitSettingsMenu(playerMenuUtility, plugin).open();
                break;

            case SET_MEMBER:
                new SetItemsMenu(playerMenuUtility, plugin).open();
                break;

            case CRAFTING_RESULT:
                new RecipeEditorMenu(playerMenuUtility, plugin).open();
                break;

            case CRAFTING_INGREDIENT:
                new IngredientSettingsMenu(playerMenuUtility, plugin, false).open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);

        addMenuControls();

        inventory.setItem(49, makeItem(Material.PAPER, "§eManual Input", "§7Click to type ID in chat manually."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack"));

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= items.size()) break;

            BuffedItem item = items.get(index);
            ItemStack stack = new ItemBuilder(item, plugin).build();

            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore();
                if (lore == null) {
                    lore = new ArrayList<>();
                }
                lore.add(Component.empty());
                lore.add(ConfigManager.fromSection("§7ID: §f" + item.getId()));
                lore.add(Component.empty());
                lore.add(ConfigManager.fromSection("§eClick to select"));
                meta.lore(lore);
                stack.setItemMeta(meta);
            }

            inventory.setItem(9 + i, stack);
        }
    }
}