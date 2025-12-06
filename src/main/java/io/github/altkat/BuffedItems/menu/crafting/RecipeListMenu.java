package io.github.altkat.BuffedItems.menu.crafting;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.MainMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RecipeListMenu extends PaginatedMenu {

    private final BuffedItems plugin;

    public RecipeListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Crafting Recipes";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        List<CustomRecipe> recipes = getSortedRecipes();

        if (e.getCurrentItem() == null) return;
        if (handlePageChange(e, recipes.size())) return;

        if (e.getSlot() == 49) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("create_recipe");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter a unique ID for the new recipe (e.g. 'magic_wand_craft')."));
            return;
        }

        if (e.getSlot() == 53) {
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() < 45) {
            int index = maxItemsPerPage * page + e.getSlot();
            if (index >= recipes.size()) return;

            CustomRecipe recipe = recipes.get(index);

            if (e.getClick() == ClickType.LEFT) {
                playerMenuUtility.setRecipeToEditId(recipe.getId());
                new RecipeEditorMenu(playerMenuUtility, plugin).open();
            }
            else if (e.getClick() == ClickType.RIGHT) {
                RecipesConfig.get().set("recipes." + recipe.getId(), null);
                RecipesConfig.save();
                plugin.getCraftingManager().loadRecipes(true);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cRecipe '" + recipe.getId() + "' deleted."));
                this.open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuControls();
        setFillerGlass();

        inventory.setItem(49, makeItem(Material.ANVIL, "§aCreate New Recipe", "§7Click to create a new crafting recipe."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack to Main Menu"));

        List<CustomRecipe> recipes = getSortedRecipes();

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= recipes.size()) break;

            CustomRecipe recipe = recipes.get(index);
            ItemStack icon = getResultIcon(recipe);

            ItemMeta meta = icon.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            if (meta.hasLore()) lore.addAll(meta.lore());

            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(ConfigManager.fromSection("§8ID: " + recipe.getId()));

            if (!recipe.isValid()) {
                lore.add(ConfigManager.fromSection("§c⚠ Invalid Config!"));
            }

            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(ConfigManager.fromSection("§eLeft-Click to Edit"));
            lore.add(ConfigManager.fromSection("§cRight-Click to Delete"));

            meta.lore(lore);
            icon.setItemMeta(meta);

            inventory.setItem(i, icon);
        }
    }

    private ItemStack getResultIcon(CustomRecipe recipe) {
        String itemId = recipe.getResultItemId();
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);

        if (item != null) {
            ItemStack stack = new ItemBuilder(item, plugin).build();
            stack.setAmount(Math.max(1, recipe.getAmount()));
            return stack;
        }

        return makeItem(Material.BARRIER, "§cUnknown Result", "§7ID: " + itemId);
    }

    private List<CustomRecipe> getSortedRecipes() {
        List<CustomRecipe> list = new ArrayList<>(plugin.getCraftingManager().getRecipes().values());
        list.sort(Comparator.comparing(CustomRecipe::getId));
        return list;
    }
}