package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.manager.crafting.RecipeIngredient;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class CraftingListener implements Listener {

    private final BuffedItems plugin;

    public CraftingListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        CraftingInventory inv = e.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        if (matrix.length < 9) return;

        CustomRecipe match = plugin.getCraftingManager().findRecipe(matrix);

        if (match != null) {
            BuffedItem resultItem = plugin.getItemManager().getBuffedItem(match.getResultItemId());

            if (resultItem != null) {
                ItemStack resultStack = new ItemBuilder(resultItem, plugin).build();
                resultStack.setAmount(match.getAmount());
                inv.setResult(resultStack);
            } else {
                inv.setResult(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent e) {
        if (!(e.getInventory() instanceof CraftingInventory)) return;

        CraftingInventory inv = (CraftingInventory) e.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        if (matrix.length < 9) return;

        CustomRecipe match = plugin.getCraftingManager().findRecipe(matrix);
        if (match == null) return;

        for (int i = 0; i < matrix.length; i++) {
            if (i >= 9) break;

            RecipeIngredient ingredient = match.getIngredient(i);
            ItemStack itemInSlot = matrix[i];

            if (ingredient != null && itemInSlot != null && itemInSlot.getType() != Material.AIR) {
                int required = ingredient.getAmount();

                if (required > 1) {
                    int currentAmount = itemInSlot.getAmount();

                    if (currentAmount >= required) {
                        int amountToReduceExtra = required - 1;
                        itemInSlot.setAmount(currentAmount - amountToReduceExtra);
                    }
                }
            }
        }
        inv.setMatrix(matrix);
    }
}