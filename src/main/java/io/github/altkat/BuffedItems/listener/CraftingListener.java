package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.manager.crafting.RecipeIngredient;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CraftingListener implements Listener {

    private final BuffedItems plugin;
    private final Set<UUID> isCrafting = new HashSet<>();
    private final Set<UUID> isCalculating = new HashSet<>();

    public CraftingListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        if (!(e.getView().getPlayer() instanceof Player p)) return;

        if (isCrafting.contains(p.getUniqueId())) return;

        if (isCalculating.contains(p.getUniqueId())) return;

        CraftingInventory inv = e.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        boolean isEmpty = true;
        for (ItemStack item : matrix) {
            if (item != null && item.getType() != Material.AIR) {
                isEmpty = false;
                break;
            }
        }
        if (isEmpty) return;

        isCalculating.add(p.getUniqueId());
        try {
            CustomRecipe match = plugin.getCraftingManager().findRecipe(matrix);

            if (match != null) {
                if (!match.isEnabled()) {
                    inv.setResult(null);
                    return;
                }

                if (match.getPermission() != null && !match.getPermission().isEmpty() && !p.hasPermission(match.getPermission())) {
                    inv.setResult(null);
                    return;
                }

                BuffedItem resultItem = plugin.getItemManager().getBuffedItem(match.getResultItemId());
                if (resultItem != null) {
                    ItemStack resultStack = new ItemBuilder(resultItem, plugin).build();
                    resultStack.setAmount(match.getAmount());
                    inv.setResult(resultStack);
                } else {
                    inv.setResult(null);
                }
            } else {
                for (ItemStack item : matrix) {
                    if (plugin.getItemManager().isBuffedItem(item)) {
                        inv.setResult(null);
                        return;
                    }
                }
                ItemStack currentResult = inv.getResult();
                if (currentResult != null && plugin.getItemManager().isBuffedItem(currentResult)) {
                    inv.setResult(null);
                }
            }
        } finally {
            isCalculating.remove(p.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getSlotType() != InventoryType.SlotType.RESULT) return;
        if (!(e.getClickedInventory() instanceof CraftingInventory)) return;

        CraftingInventory inv = (CraftingInventory) e.getClickedInventory();
        ItemStack result = inv.getResult();

        if (result == null || result.getType() == Material.AIR) return;

        if (!plugin.getItemManager().isBuffedItem(result)) {
            ItemStack[] matrix = inv.getMatrix();
            for (ItemStack item : matrix) {
                if (plugin.getItemManager().isBuffedItem(item)) {
                    e.setCancelled(true);
                    return;
                }
            }
            return;
        }

        ItemStack[] matrix = inv.getMatrix();
        CustomRecipe match = plugin.getCraftingManager().findRecipe(matrix);

        if (match == null) {
            e.setCancelled(true);
            e.getWhoClicked().closeInventory();
            return;
        }


        Player player = (Player) e.getWhoClicked();

        if (match.getPermission() != null && !match.getPermission().isEmpty() && !player.hasPermission(match.getPermission())) {
            e.setCancelled(true);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cYou do not have permission to craft this."));
            return;
        }

        e.setCancelled(true);

        isCrafting.add(player.getUniqueId());

        try {
            if (e.isShiftClick()) {
                handleShiftClick(player, match, matrix, inv);
            } else {
                handleNormalClick(player, e, match, matrix, inv);
            }
        } finally {
            isCrafting.remove(player.getUniqueId());
            player.updateInventory();
        }
    }

    private void handleNormalClick(Player player, InventoryClickEvent e, CustomRecipe match, ItemStack[] matrix, CraftingInventory inv) {
        ItemStack cursor = e.getCursor();

        BuffedItem resultBuffedItem = plugin.getItemManager().getBuffedItem(match.getResultItemId());
        if (resultBuffedItem == null) return;

        ItemStack resultStack = new ItemBuilder(resultBuffedItem, plugin).build();
        resultStack.setAmount(match.getAmount());

        if (cursor != null && cursor.getType() != Material.AIR) {
            if (!cursor.isSimilar(resultStack)) return;
            if (cursor.getAmount() + resultStack.getAmount() > cursor.getMaxStackSize()) return;
        }

        updateMatrix(inv, matrix, match, 1);

        if (cursor == null || cursor.getType() == Material.AIR) {
            player.setItemOnCursor(resultStack);
        } else {
            cursor.setAmount(cursor.getAmount() + resultStack.getAmount());
            player.setItemOnCursor(cursor);
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
    }

    private void handleShiftClick(Player player, CustomRecipe match, ItemStack[] matrix, CraftingInventory inv) {
        BuffedItem resultBuffedItem = plugin.getItemManager().getBuffedItem(match.getResultItemId());
        if (resultBuffedItem == null) return;

        ItemStack resultTemplate = new ItemBuilder(resultBuffedItem, plugin).build();
        resultTemplate.setAmount(match.getAmount());

        int maxCraftsByMaterials = 64 * 9;

        for (ItemStack itemInSlot : matrix) {
            if (itemInSlot == null || itemInSlot.getType() == Material.AIR) continue;

            RecipeIngredient matchingIngredient = findMatchingIngredient(itemInSlot, match);

            if (matchingIngredient != null) {
                int required = matchingIngredient.getAmount();
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

        if (actualCrafts <= 0) return;

        updateMatrix(inv, matrix, match, actualCrafts);

        ItemStack toGive = resultTemplate.clone();
        toGive.setAmount(actualCrafts * match.getAmount());

        var leftOvers = player.getInventory().addItem(toGive);
        for (ItemStack drop : leftOvers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
    }

    private void updateMatrix(CraftingInventory inv, ItemStack[] matrix, CustomRecipe match, int multiplier) {
        for (int i = 0; i < matrix.length; i++) {
            ItemStack itemInSlot = matrix[i];

            if (itemInSlot == null || itemInSlot.getType() == Material.AIR) continue;

            RecipeIngredient matchingIngredient = findMatchingIngredient(itemInSlot, match);

            if (matchingIngredient != null) {
                int requiredTotal = matchingIngredient.getAmount() * multiplier;

                if (itemInSlot.getAmount() > requiredTotal) {
                    itemInSlot.setAmount(itemInSlot.getAmount() - requiredTotal);
                } else {
                    if (isBucket(itemInSlot.getType())) {
                        itemInSlot.setType(Material.BUCKET);
                        itemInSlot.setAmount(1);
                    } else {
                        itemInSlot.setAmount(0);
                    }
                }
                inv.setItem(i + 1, itemInSlot);
            }
        }
    }

    private RecipeIngredient findMatchingIngredient(ItemStack item, CustomRecipe recipe) {
        for (RecipeIngredient ing : recipe.getIngredients().values()) {
            if (plugin.getCraftingManager().getItemMatcher().matches(item, ing)) {
                return ing;
            }
        }
        return null;
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent e) {
        if (!(e.getBlock().getState() instanceof Crafter crafter)) return;

        ItemStack[] matrix = crafter.getInventory().getContents();

        for (ItemStack item : matrix) {
            if (item != null && item.getType() != Material.AIR) {
                if (plugin.getItemManager().isBuffedItem(item)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        CustomRecipe match = plugin.getCraftingManager().findRecipe(matrix);
        if (match != null) {
            e.setCancelled(true);
        }
    }
}