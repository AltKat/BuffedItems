package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.manager.crafting.RecipeIngredient;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CraftingListener implements Listener {

    private final BuffedItems plugin;
    private final Set<UUID> isCrafting = new HashSet<>();

    public CraftingListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (e.getView().getPlayer() != null && isCrafting.contains(e.getView().getPlayer().getUniqueId())) {
            return;
        }

        CraftingInventory inv = e.getInventory();
        ItemStack[] matrix = inv.getMatrix();

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
        } else {
            Recipe bukkitRecipe = e.getRecipe();
            if (bukkitRecipe != null) {
                ItemStack result = inv.getResult();
                if (plugin.getItemManager().isBuffedItem(result)) {
                    inv.setResult(null);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent e) {
        CraftingInventory inv = e.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        if (matrix.length < 9) return;

        CustomRecipe match = plugin.getCraftingManager().findRecipe(matrix);
        if (match == null) return;

        if (!e.isShiftClick()) {
            ItemStack cursor = e.getWhoClicked().getItemOnCursor();
            ItemStack result = inv.getResult();

            if (cursor.getType() != Material.AIR) {
                if (result == null || !cursor.isSimilar(result) || (cursor.getAmount() + result.getAmount() > cursor.getMaxStackSize())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        isCrafting.add(e.getWhoClicked().getUniqueId());

        try {
            if (e.isShiftClick()) {
                e.setCancelled(true);
                handleShiftClick(e, match, matrix, inv);
                return;
            }

            boolean matrixChanged = false;
            for (int i = 0; i < matrix.length; i++) {
                if (i >= 9) break;

                RecipeIngredient ingredient = match.getIngredient(i);
                ItemStack itemInSlot = matrix[i];

                if (ingredient != null && itemInSlot != null && itemInSlot.getType() != Material.AIR) {
                    int required = ingredient.getAmount();
                    if (required > 1) {
                        int currentAmount = itemInSlot.getAmount();
                        int amountToReduceExtra = required - 1;

                        if (currentAmount > amountToReduceExtra) {
                            itemInSlot.setAmount(currentAmount - amountToReduceExtra);
                        } else {
                            itemInSlot.setAmount(0);
                        }
                        matrixChanged = true;
                    }
                }
            }

            if (matrixChanged) {
                inv.setMatrix(matrix);
            }

        } finally {
            isCrafting.remove(e.getWhoClicked().getUniqueId());
        }
    }

    private void handleShiftClick(CraftItemEvent e, CustomRecipe match, ItemStack[] matrix, CraftingInventory inv) {
        Player player = (Player) e.getWhoClicked();

        BuffedItem resultBuffedItem = plugin.getItemManager().getBuffedItem(match.getResultItemId());
        if (resultBuffedItem == null) return;

        ItemStack resultTemplate = new ItemBuilder(resultBuffedItem, plugin).build();
        resultTemplate.setAmount(match.getAmount());

        int maxCraftsByMaterials = 64 * 9;

        for (int i = 0; i < matrix.length; i++) {
            if (i >= 9) break;
            RecipeIngredient ingredient = match.getIngredient(i);
            ItemStack itemInSlot = matrix[i];

            if (ingredient != null) {
                if (itemInSlot == null || itemInSlot.getType() == Material.AIR) {
                    maxCraftsByMaterials = 0;
                    break;
                }
                int required = ingredient.getAmount();
                int available = itemInSlot.getAmount();
                int canCraft = available / required;

                if (canCraft < maxCraftsByMaterials) {
                    maxCraftsByMaterials = canCraft;
                }
            }
        }

        if (maxCraftsByMaterials <= 0) return;

        int maxCraftsByInventory = getSpaceFor(player.getInventory(), resultTemplate) / match.getAmount();

        int actualCrafts = Math.min(maxCraftsByMaterials, maxCraftsByInventory);

        if (actualCrafts <= 0) {
            return;
        }


        for (int i = 0; i < matrix.length; i++) {
            if (i >= 9) break;
            RecipeIngredient ingredient = match.getIngredient(i);
            ItemStack itemInSlot = matrix[i];

            if (ingredient != null && itemInSlot != null) {
                int requiredTotal = ingredient.getAmount() * actualCrafts;
                int current = itemInSlot.getAmount();

                if (current > requiredTotal) {
                    itemInSlot.setAmount(current - requiredTotal);
                } else {
                    Material type = itemInSlot.getType();
                    if (isBucket(type)) {
                        itemInSlot.setType(Material.BUCKET);
                        itemInSlot.setAmount(1);
                    } else {
                        itemInSlot.setAmount(0);
                    }
                }
            }
        }
        inv.setMatrix(matrix);

        ItemStack toGive = resultTemplate.clone();
        toGive.setAmount(actualCrafts * match.getAmount());

        var leftOvers = player.getInventory().addItem(toGive);
        for (ItemStack drop : leftOvers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
    }

    private int getSpaceFor(Inventory inventory, ItemStack item) {
        int space = 0;
        int maxStack = item.getMaxStackSize();

        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType() == Material.AIR) {
                space += maxStack;
            } else if (content.isSimilar(item)) {
                space += Math.max(0, maxStack - content.getAmount());
            }
        }
        return space;
    }

    private boolean isBucket(Material material) {
        return material == Material.WATER_BUCKET ||
                material == Material.LAVA_BUCKET ||
                material == Material.MILK_BUCKET ||
                material == Material.POWDER_SNOW_BUCKET ||
                material == Material.COD_BUCKET ||
                material == Material.SALMON_BUCKET ||
                material == Material.AXOLOTL_BUCKET ||
                material == Material.TADPOLE_BUCKET ||
                material == Material.TROPICAL_FISH_BUCKET ||
                material == Material.PUFFERFISH_BUCKET;
    }
}