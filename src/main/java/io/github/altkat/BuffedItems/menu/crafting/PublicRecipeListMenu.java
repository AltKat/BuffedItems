package io.github.altkat.BuffedItems.menu.crafting;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PublicRecipeListMenu extends PaginatedMenu {

    private final BuffedItems plugin;

    public PublicRecipeListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Server Recipes";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        List<CustomRecipe> recipes = getValidRecipes();

        if (e.getCurrentItem() == null) return;
        if (handlePageChange(e, recipes.size())) return;

        if (e.getSlot() == 49) {
            e.getWhoClicked().closeInventory();
            return;
        }

        if (e.getSlot() < 45) {
            int index = maxItemsPerPage * page + e.getSlot();
            if (index >= recipes.size()) return;

            CustomRecipe recipe = recipes.get(index);
            new RecipePreviewMenu(playerMenuUtility, plugin, recipe).open();
        }
    }

    @Override
    public void setMenuItems() {
        addMenuControls();
        setFillerGlass();

        inventory.setItem(49, makeItem(Material.BARRIER, "§cClose Menu"));

        List<CustomRecipe> recipes = getValidRecipes();

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= recipes.size()) break;

            CustomRecipe recipe = recipes.get(index);

            ItemStack icon;
            BuffedItem item = plugin.getItemManager().getBuffedItem(recipe.getResultItemId());
            if (item != null) {
                icon = new ItemBuilder(item, plugin).build();
            } else {
                icon = makeItem(Material.CHEST, "§f" + recipe.getId());
            }

            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {

                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                lore.add(ConfigManager.fromSection("§eClick to view recipe"));

                meta.lore(lore);
                icon.setItemMeta(meta);
            }

            icon.setAmount(1);
            inventory.setItem(i, icon);
        }
    }

    private List<CustomRecipe> getValidRecipes() {
        return plugin.getCraftingManager().getRecipes().values().stream()
                .filter(CustomRecipe::isValid)
                .sorted(Comparator.comparing(CustomRecipe::getId))
                .collect(Collectors.toList());
    }
}