package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.manager.crafting.RecipeIngredient;
import io.github.altkat.BuffedItems.manager.crafting.RecipeType;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.ItemStack;

public class FurnaceListener implements Listener {

    private final BuffedItems plugin;

    public FurnaceListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        ItemStack source = event.getSource();
        if (source == null || source.getType() == Material.AIR) return;

        Block block = event.getBlock();
        RecipeType currentStationType = getRecipeTypeFromBlock(block.getType());
        if (currentStationType == null) return;

        for (CustomRecipe recipe : plugin.getCraftingManager().getRecipes().values()) {
            if (!recipe.isValid() || !recipe.isEnabled()) continue;
            
            if (recipe.getType() != currentStationType) {
                continue; 
            }

            if (recipe.getIngredients().isEmpty()) continue;
            RecipeIngredient input = recipe.getIngredients().values().iterator().next();

            if (matches(source, input)) {
                BuffedItem resultItem = plugin.getItemManager().getBuffedItem(recipe.getResultItemId());
                if (resultItem != null) {
                    ItemStack resultStack = new ItemBuilder(resultItem, plugin).build();
                    resultStack.setAmount(recipe.getAmount());
                    
                    event.setResult(resultStack);
                    return;
                }
            }
        }
    }

    private boolean matches(ItemStack source, RecipeIngredient ingredient) {
        return plugin.getCraftingManager().getItemMatcher().matches(source, ingredient);
    }

    private RecipeType getRecipeTypeFromBlock(Material blockType) {
        if (blockType == Material.FURNACE) return RecipeType.FURNACE;
        if (blockType == Material.BLAST_FURNACE) return RecipeType.BLAST_FURNACE;
        if (blockType == Material.SMOKER) return RecipeType.SMOKER;
        if (blockType == Material.CAMPFIRE || blockType == Material.SOUL_CAMPFIRE) return RecipeType.CAMPFIRE;
        return null;
    }
}