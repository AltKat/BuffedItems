package io.github.altkat.BuffedItems.menu.crafting;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.manager.crafting.MatchType;
import io.github.altkat.BuffedItems.manager.crafting.RecipeIngredient;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RecipePreviewMenu extends Menu {

    private final BuffedItems plugin;
    private final CustomRecipe recipe;

    public RecipePreviewMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin, CustomRecipe recipe) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.recipe = recipe;
    }

    @Override
    public String getMenuName() {
        return "Recipe View: " + recipe.getId();
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getSlot() == 40) {
            new PublicRecipeListMenu(playerMenuUtility, plugin).open();
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        ItemStack resultIcon;
        BuffedItem resultBi = plugin.getItemManager().getBuffedItem(recipe.getResultItemId());
        if (resultBi != null) {
            resultIcon = new ItemBuilder(resultBi, plugin).build();
        } else {
            resultIcon = makeItem(Material.BARRIER, "§cUnknown Item");
        }

        resultIcon.setAmount(recipe.getAmount());

        if (recipe.getAmount() > 1) {
            ItemMeta meta = resultIcon.getItemMeta();
            if (meta != null) {
                Component currentName = meta.hasDisplayName() ? meta.displayName() : ConfigManager.fromSection("§f" + formatMaterialName(resultIcon.getType()));
                Component amountPrefix = ConfigManager.fromSection("§e" + recipe.getAmount() + "x ");
                meta.displayName(amountPrefix.append(currentName));

                resultIcon.setItemMeta(meta);
            }
        }

        inventory.setItem(25, resultIcon);

        inventory.setItem(23, makeItem(Material.ARROW, "§e->"));

        int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};

        List<String> shape = recipe.getShape();
        if (shape != null) {
            for (int row = 0; row < shape.size(); row++) {
                String line = shape.get(row);
                for (int col = 0; col < line.length(); col++) {
                    char key = line.charAt(col);
                    RecipeIngredient ing = recipe.getIngredient(key);

                    if (ing != null) {
                        int slotIndex = (row * 3) + col;
                        if (slotIndex < gridSlots.length) {
                            ItemStack ingredientIcon = getIngredientIcon(ing);
                            inventory.setItem(gridSlots[slotIndex], ingredientIcon);
                        }
                    }
                }
            }
        }

        inventory.setItem(40, makeItem(Material.BARRIER, "§cBack to List"));
    }

    private ItemStack getIngredientIcon(RecipeIngredient ing) {
        ItemStack icon = new ItemStack(Material.BARRIER);

        if (ing.getMatchType() == MatchType.BUFFED_ITEM) {
            BuffedItem bi = plugin.getItemManager().getBuffedItem(ing.getData());
            if (bi != null) icon = new ItemBuilder(bi, plugin).build();
        }
        else if (ing.getMatchType() == MatchType.EXACT) {
            if (ing.getExactReferenceItem() != null) icon = ing.getExactReferenceItem().clone();
        }
        else {
            Material mat = ing.getMaterial() != null ? ing.getMaterial() : Material.BEDROCK;
            icon = new ItemStack(mat);
        }

        icon.setAmount(ing.getAmount());

        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            Component displayName;

            if (ing.getMatchType() == MatchType.MATERIAL || !meta.hasDisplayName()) {
                displayName = ConfigManager.fromSection("§f" + formatMaterialName(icon.getType()));
            } else {
                displayName = meta.displayName();
            }

            if (ing.getAmount() > 1) {
                Component amountPrefix = ConfigManager.fromSection("§e" + ing.getAmount() + "x ");
                displayName = amountPrefix.append(displayName);
            }
            meta.displayName(displayName);

            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(Component.empty());
            lore.add(ConfigManager.fromSection("§7Required Amount: §e" + ing.getAmount()));
            meta.lore(lore);

            icon.setItemMeta(meta);
        }

        return icon;
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
}