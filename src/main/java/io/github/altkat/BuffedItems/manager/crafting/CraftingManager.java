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
    private final Set<NamespacedKey> trackedKeys = new HashSet<>();

    public CraftingManager(BuffedItems plugin) {
        this.plugin = plugin;
        this.recipes = new LinkedHashMap<>();
        this.itemMatcher = new ItemMatcher(plugin);
    }

    public void loadRecipes(boolean silent) {
        long startTime = System.currentTimeMillis();
        unloadRecipes();
        recipes.clear();

        if (!RecipesConfig.get().getBoolean("settings.enabled", true)) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "Custom Crafting system is disabled in recipes.yml");
            return;
        }

        ConfigurationSection section = RecipesConfig.get().getConfigurationSection("recipes");
        if (section == null) {
            if (!silent) ConfigManager.logInfo("&eNo custom recipes found in recipes.yml.");
            return;
        }

        if (!silent) ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[CraftingManager] Loading recipes...");

        int validCount = 0;
        int invalidCount = 0;
        List<String> recipesWithErrors = new ArrayList<>();
        boolean shouldRegisterToBook = RecipesConfig.get().getBoolean("settings.register-to-book", true);

        for (String recipeId : section.getKeys(false)) {
            ConfigurationSection rSection = section.getConfigurationSection(recipeId);
            if (rSection == null) continue;

            String resultItemId = rSection.getString("result.item");
            int resultAmount = rSection.getInt("result.amount", 1);
            List<String> shape = rSection.getStringList("shape");
            String permission = rSection.getString("permission");

            CustomRecipe recipe = new CustomRecipe(recipeId, resultItemId, resultAmount, shape, permission);

            if (resultItemId == null || plugin.getItemManager().getBuffedItem(resultItemId) == null) {
                recipe.addErrorMessage("Invalid Result Item: " + (resultItemId == null ? "NULL" : resultItemId));
            }

            if (resultAmount <= 0) {
                recipe.addErrorMessage("Result amount must be positive.");
            }

            if (shape.isEmpty() || shape.size() > 3) {
                recipe.addErrorMessage("Invalid Shape Size (Must be 1-3 lines).");
            } else {
                boolean isEmptyShape = true;
                for (String line : shape) {
                    if (!line.trim().isEmpty()) {
                        isEmptyShape = false;
                        break;
                    }
                }
                if (isEmptyShape) {
                    recipe.addErrorMessage("Recipe shape cannot be completely empty.");
                }
            }

            ConfigurationSection ingSection = rSection.getConfigurationSection("ingredients");
            Map<Character, RecipeIngredient> charMap = new HashMap<>();

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
                        continue;
                    }

                    Material mat = Material.STONE;
                    ItemStack exactStack = null;

                    if (type == MatchType.MATERIAL) {
                        mat = Material.matchMaterial(value);
                        if (mat == null) {
                            recipe.addErrorMessage("Invalid Material for '" + c + "': " + value);
                        }
                    } else if (type == MatchType.BUFFED_ITEM) {
                        BuffedItem bi = plugin.getItemManager().getBuffedItem(value);
                        if (bi != null) {
                            mat = bi.getMaterial();
                        } else {
                            recipe.addErrorMessage("Ingredient '" + c + "' references unknown BuffedItem: " + value);
                        }
                    } else if (type == MatchType.EXACT) {
                        exactStack = io.github.altkat.BuffedItems.utility.Serializer.fromBase64(value);
                        if (exactStack != null) {
                            mat = exactStack.getType();
                        } else {
                            recipe.addErrorMessage("Invalid Base64 for EXACT ingredient '" + c + "'");
                        }
                    }

                    RecipeIngredient ingredient = new RecipeIngredient(type, mat, value, amount);
                    if (exactStack != null) {
                        ingredient.setExactReferenceItem(exactStack);
                    }
                    charMap.put(c, ingredient);
                    recipe.addIngredient(c, ingredient);
                }
            }

            if (recipe.isValid()) {
                for (int row = 0; row < shape.size(); row++) {
                    String line = shape.get(row);
                    if (line.length() > 3) {
                        recipe.addErrorMessage("Shape line " + (row + 1) + " is too long (Max 3 chars).");
                        break;
                    }
                    for (int col = 0; col < line.length(); col++) {
                        char c = line.charAt(col);
                        if (c != ' ') {
                            if (!charMap.containsKey(c)) {
                                recipe.addErrorMessage("Shape contains undefined character: '" + c + "'");
                            }
                        }
                    }
                }
            }

            if (recipe.isValid()) {
                validCount++;
                if (shouldRegisterToBook) {
                    registerBukkitRecipe(recipe, shape, charMap);
                }
            } else {
                invalidCount++;
                recipesWithErrors.add(recipeId);
            }
            recipes.put(recipeId, recipe);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        if (!silent) {
            ConfigManager.logInfo("&aLoaded &e" + recipes.size() + "&a custom recipes (&e" + validCount + "&a valid, &e" + invalidCount + "&c with errors&a) in &e" + elapsedTime + "&ams");

            if (invalidCount > 0) {
                String separator = "============================================================";
                plugin.getLogger().warning(separator);
                plugin.getLogger().warning("⚠ " + invalidCount + " custom recipe(s) have configuration errors:");
                for (String rId : recipesWithErrors) {
                    CustomRecipe recipe = recipes.get(rId);
                    plugin.getLogger().warning("  • " + rId + " (" + recipe.getErrorMessages().size() + " error(s))");
                    for (String error : recipe.getErrorMessages()) {
                        plugin.getLogger().warning("    - " + ConfigManager.stripLegacy(error));
                    }
                }
                plugin.getLogger().warning("Use /bi menu -> Crafting to fix these errors.");
                plugin.getLogger().warning(separator);
            }
        }
    }

    public void unloadRecipes() {
        if (!trackedKeys.isEmpty()) {
            for (NamespacedKey key : trackedKeys) {
                Bukkit.removeRecipe(key);
            }
            trackedKeys.clear();
        }
    }

    private void registerBukkitRecipe(CustomRecipe recipe, List<String> shape, Map<Character, RecipeIngredient> charMap) {
        NamespacedKey key = new NamespacedKey(plugin, recipe.getId());
        trackedKeys.add(key);
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

            if (ingredient.getMatchType() == MatchType.BUFFED_ITEM) {
                BuffedItem bi = plugin.getItemManager().getBuffedItem(ingredient.getData());
                if (bi != null) {
                    ItemStack exactItem = new ItemBuilder(bi, plugin).build();
                    exactItem.setAmount(1);
                    bukkitRecipe.setIngredient(charKey, new org.bukkit.inventory.RecipeChoice.ExactChoice(exactItem));
                } else {
                    if (ingredient.getMaterial() != null) {
                        bukkitRecipe.setIngredient(charKey, ingredient.getMaterial());
                    }
                }
            } else if (ingredient.getMatchType() == MatchType.EXACT) {
                if (ingredient.getExactReferenceItem() != null) {
                    ItemStack exactItem = ingredient.getExactReferenceItem().clone();
                    exactItem.setAmount(1);
                    bukkitRecipe.setIngredient(charKey, new org.bukkit.inventory.RecipeChoice.ExactChoice(exactItem));
                }
            } else {
                if (ingredient.getMaterial() != null) {
                    bukkitRecipe.setIngredient(charKey, ingredient.getMaterial());
                }
            }
        }
        try { Bukkit.addRecipe(bukkitRecipe); } catch (Exception ignored) {}
    }

    public CustomRecipe findRecipe(ItemStack[] matrix) {
        if (isMatrixEmpty(matrix)) {
            return null;
        }

        for (CustomRecipe recipe : recipes.values()) {
            if (!recipe.isValid()) continue;

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
        if (recipeGrid.length == 0) return false;
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