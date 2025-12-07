package io.github.altkat.BuffedItems.manager.crafting;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.*;

public class CraftingManager {

    private final BuffedItems plugin;
    private final Map<String, CustomRecipe> recipes;
    private final ItemMatcher itemMatcher;

    public CraftingManager(BuffedItems plugin) {
        this.plugin = plugin;
        this.recipes = new LinkedHashMap<>();
        this.itemMatcher = new ItemMatcher(plugin);
    }

    public void loadRecipes(boolean reload) {
        if (reload) {
            RecipesConfig.reload();
        }
        recipes.clear();

        ConfigurationSection section = RecipesConfig.get().getConfigurationSection("recipes");
        if (section == null) return;

        int validCount = 0;
        int invalidCount = 0;
        List<String> recipesWithErrors = new ArrayList<>();
        boolean shouldRegisterToBook = RecipesConfig.get().getBoolean("settings.register-to-book", true);

        for (String recipeId : section.getKeys(false)) {
            ConfigurationSection recipeSection = section.getConfigurationSection(recipeId);
            if (recipeSection == null) continue;

            String resultItemId = recipeSection.getString("result.item");
            int resultAmount = recipeSection.getInt("result.amount", 1);
            if (resultItemId == null || plugin.getItemManager().getBuffedItem(resultItemId) == null) {
                invalidCount++;
                recipesWithErrors.add(recipeId + " (Invalid Result)");
                continue;
            }

            List<String> shape = recipeSection.getStringList("shape");
            if (shape.size() < 1 || shape.size() > 3) {
                invalidCount++;
                recipesWithErrors.add(recipeId + " (Invalid Shape)");
                continue;
            }

            ConfigurationSection ingSection = recipeSection.getConfigurationSection("ingredients");
            Map<Character, RecipeIngredient> charMap = new HashMap<>();
            CustomRecipe recipe = new CustomRecipe(recipeId, resultItemId, resultAmount, shape);
            boolean ingredientsValid = true;

            if (ingSection != null) {
                for (String key : ingSection.getKeys(false)) {
                    if (key.length() != 1) continue;
                    char c = key.charAt(0);

                    String typeStr = ingSection.getString(key + ".type", "MATERIAL");
                    String value = ingSection.getString(key + ".value", "AIR");
                    int amount = ingSection.getInt(key + ".amount", 1);

                    MatchType type;
                    try {
                        type = MatchType.valueOf(typeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        recipe.addErrorMessage("Invalid MatchType for '" + c + "': " + typeStr);
                        ingredientsValid = false;
                        break;
                    }

                    Material mat = Material.STONE;
                    ItemStack exactStack = null;

                    if (type == MatchType.MATERIAL) {
                        mat = Material.matchMaterial(value);
                        if (mat == null) {
                            recipe.addErrorMessage("Invalid Material: " + value);
                            ingredientsValid = false;
                        }
                    } else if (type == MatchType.BUFFED_ITEM) {
                        BuffedItem bi = plugin.getItemManager().getBuffedItem(value);
                        if (bi != null) mat = bi.getMaterial();
                        else {
                            recipe.addErrorMessage("Unknown BuffedItem: " + value);
                            ingredientsValid = false;
                        }
                    } else if (type == MatchType.EXACT) {
                        exactStack = io.github.altkat.BuffedItems.utility.Serializer.fromBase64(value);
                        if (exactStack != null) mat = exactStack.getType();
                        else {
                            recipe.addErrorMessage("Invalid Base64 for EXACT");
                            ingredientsValid = false;
                        }
                    }

                    if (!ingredientsValid) break;

                    RecipeIngredient ingredient = new RecipeIngredient(type, mat, value, amount);
                    if (exactStack != null) ingredient.setExactReferenceItem(exactStack);
                    charMap.put(c, ingredient);
                    recipe.addIngredient(c, ingredient);
                }
            }

            if (ingredientsValid) {
                validCount++;
                if (shouldRegisterToBook) registerBukkitRecipe(recipe, shape, charMap);
            } else {
                invalidCount++;
                recipesWithErrors.add(recipeId);
            }
            recipes.put(recipeId, recipe);
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "Loaded " + recipes.size() + " recipes.");
    }

    private void registerBukkitRecipe(CustomRecipe recipe, List<String> shape, Map<Character, RecipeIngredient> charMap) {
        NamespacedKey key = new NamespacedKey(plugin, recipe.getId());
        ItemStack resultStack;
        BuffedItem item = plugin.getItemManager().getBuffedItem(recipe.getResultItemId());

        if (item != null) {
            resultStack = new ItemBuilder(item, plugin).build();
            resultStack.setAmount(recipe.getAmount());
        } else {
            resultStack = new ItemStack(Material.BARRIER);
        }

        ShapedRecipe bukkitRecipe = new ShapedRecipe(key, resultStack);
        bukkitRecipe.shape(shape.toArray(new String[0]));

        for (Map.Entry<Character, RecipeIngredient> entry : charMap.entrySet()) {
            char charKey = entry.getKey();
            RecipeIngredient ingredient = entry.getValue();
            ItemStack choiceStack = null;

            if (ingredient.getMatchType() == MatchType.BUFFED_ITEM) {
                BuffedItem bi = plugin.getItemManager().getBuffedItem(ingredient.getData());
                if (bi != null) choiceStack = new ItemBuilder(bi, plugin).build();
            } else if (ingredient.getMatchType() == MatchType.EXACT) {
                if (ingredient.getExactReferenceItem() != null) choiceStack = ingredient.getExactReferenceItem().clone();
            }

            if (choiceStack != null) {
                choiceStack.setAmount(1);
                bukkitRecipe.setIngredient(charKey, new org.bukkit.inventory.RecipeChoice.ExactChoice(choiceStack));
            } else {
                if (ingredient.getMaterial() != null) bukkitRecipe.setIngredient(charKey, ingredient.getMaterial());
            }
        }
        try { Bukkit.addRecipe(bukkitRecipe); } catch (Exception ignored) {}
    }


    public CustomRecipe findRecipe(ItemStack[] matrix) {
        if (isMatrixEmpty(matrix)) {
            return null;
        }

        for (CustomRecipe recipe : recipes.values()) {
            if (matchesShaped(matrix, recipe)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean isMatrixEmpty(ItemStack[] matrix) {
        for (ItemStack item : matrix) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesShaped(ItemStack[] inputMatrix, CustomRecipe recipe) {
        List<String> shape = recipe.getShape();
        if (shape == null || shape.isEmpty()) return false;

        char[][] recipeGrid = parseShape(shape);
        int recipeHeight = recipeGrid.length;
        int recipeWidth = recipeGrid[0].length;

        for (int i = 0; i <= 3 - recipeHeight; i++) {
            for (int j = 0; j <= 3 - recipeWidth; j++) {
                if (checkMatchAt(inputMatrix, recipe, recipeGrid, i, j)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkMatchAt(ItemStack[] inputMatrix, CustomRecipe recipe, char[][] recipeGrid, int startRow, int startCol) {
        int recipeHeight = recipeGrid.length;
        int recipeWidth = recipeGrid[0].length;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int slotIndex = r * 3 + c;
                ItemStack inputItem = (slotIndex < inputMatrix.length) ? inputMatrix[slotIndex] : null;

                boolean inRecipeArea = (r >= startRow && r < startRow + recipeHeight) &&
                        (c >= startCol && c < startCol + recipeWidth);

                if (inRecipeArea) {
                    char key = recipeGrid[r - startRow][c - startCol];
                    RecipeIngredient ingredient = recipe.getIngredient(key);

                    if (ingredient == null) {
                        if (inputItem != null && !inputItem.getType().isAir()) return false;
                    } else {
                        if (!plugin.getCraftingManager().getItemMatcher().matches(inputItem, ingredient)) return false;
                    }
                } else {
                    if (inputItem != null && !inputItem.getType().isAir()) return false;
                }
            }
        }
        return true;
    }

    private char[][] parseShape(List<String> rawShape) {
        int minRow = 3, maxRow = -1;
        int minCol = 3, maxCol = -1;

        for (int r = 0; r < rawShape.size() && r < 3; r++) {
            String rowStr = rawShape.get(r);
            for (int c = 0; c < rowStr.length() && c < 3; c++) {
                char ch = rowStr.charAt(c);
                if (ch != ' ') {
                    if (r < minRow) minRow = r;
                    if (r > maxRow) maxRow = r;
                    if (c < minCol) minCol = c;
                    if (c > maxCol) maxCol = c;
                }
            }
        }

        if (maxRow == -1) return new char[0][0];

        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;
        char[][] grid = new char[height][width];

        for (int r = 0; r < height; r++) {
            String rowStr = rawShape.get(minRow + r);
            for (int c = 0; c < width; c++) {
                int targetCol = minCol + c;
                if (targetCol < rowStr.length()) {
                    grid[r][c] = rowStr.charAt(targetCol);
                } else {
                    grid[r][c] = ' ';
                }
            }
        }
        return grid;
    }

    public Map<String, CustomRecipe> getRecipes() { return recipes; }
    public ItemMatcher getItemMatcher() { return itemMatcher; }
}