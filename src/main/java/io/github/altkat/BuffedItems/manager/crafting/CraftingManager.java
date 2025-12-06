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
    private final Map<String, CustomRecipe> recipes = new HashMap<>();
    private final ItemMatcher itemMatcher;

    public CraftingManager(BuffedItems plugin) {
        this.plugin = plugin;
        this.itemMatcher = new ItemMatcher(plugin);
    }

    public void loadRecipes(boolean silent) {
        long startTime = System.currentTimeMillis();
        unloadRecipes();
        recipes.clear();

        ConfigurationSection section = RecipesConfig.get().getConfigurationSection("recipes");
        if (section == null) {
            if (!silent) ConfigManager.logInfo("&eNo custom recipes found in recipes.yml.");
            return;
        }

        if (!silent) ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[CraftingManager] Loading recipes...");

        int validCount = 0;
        int invalidCount = 0;
        List<String> recipesWithErrors = new ArrayList<>();

        for (String recipeId : section.getKeys(false)) {
            ConfigurationSection rSection = section.getConfigurationSection(recipeId);
            if (rSection == null) continue;

            String resultItemId = rSection.getString("result.item");
            int resultAmount = rSection.getInt("result.amount", 1);
            List<String> shape = rSection.getStringList("shape");

            CustomRecipe recipe = new CustomRecipe(recipeId, resultItemId, resultAmount, true);

            if (resultItemId == null || plugin.getItemManager().getBuffedItem(resultItemId) == null) {
                recipe.addErrorMessage("Invalid Result Item: " + (resultItemId == null ? "NULL" : resultItemId));
            }

            if (resultAmount <= 0) {
                recipe.addErrorMessage("Result amount must be positive.");
            }

            if (shape.isEmpty() || shape.size() > 3) {
                recipe.addErrorMessage("Invalid shape definition. Must be 1-3 lines.");
            }

            Map<Character, RecipeIngredient> charMap = new HashMap<>();
            ConfigurationSection ingSection = rSection.getConfigurationSection("ingredients");

            if (ingSection != null) {
                for (String key : ingSection.getKeys(false)) {
                    if (key.length() != 1) continue;

                    char c = key.charAt(0);
                    String typeStr = ingSection.getString(key + ".type", "MATERIAL");
                    String value = ingSection.getString(key + ".value");
                    int amount = ingSection.getInt(key + ".amount", 1);

                    MatchType type;
                    try {
                        type = MatchType.valueOf(typeStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        recipe.addErrorMessage("Invalid MatchType for '" + c + "': " + typeStr);
                        continue;
                    }

                    Material mat = Material.STONE;

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
                    }

                    charMap.put(c, new RecipeIngredient(type, mat, value, amount));
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
                            if (charMap.containsKey(c)) {
                                recipe.addIngredient(row * 3 + col, charMap.get(c));
                            } else {
                                recipe.addErrorMessage("Shape contains undefined character: '" + c + "'");
                            }
                        }
                    }
                }
            }

            if (recipe.isValid()) {
                validCount++;
                registerBukkitRecipe(recipe, shape, charMap);
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
                for (String recipeId : recipesWithErrors) {
                    CustomRecipe recipe = recipes.get(recipeId);
                    plugin.getLogger().warning("  • " + recipeId + " (" + recipe.getErrorMessages().size() + " error(s))");

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
        Iterator<org.bukkit.inventory.Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            org.bukkit.inventory.Recipe recipe = it.next();
            if (recipe instanceof ShapedRecipe) {
                NamespacedKey key = ((ShapedRecipe) recipe).getKey();
                if (key.getNamespace().equals(plugin.getName().toLowerCase())) {
                    Bukkit.removeRecipe(key);
                }
            }
        }
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
            }
            else {
                if (ingredient.getMaterial() != null) {
                    bukkitRecipe.setIngredient(charKey, ingredient.getMaterial());
                }
            }
        }

        try {
            Bukkit.addRecipe(bukkitRecipe);
        } catch (Exception ignored) {}
    }

    public CustomRecipe findRecipe(ItemStack[] matrix) {
        for (CustomRecipe recipe : recipes.values()) {
            if (recipe.isValid() && matches(recipe, matrix)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean matches(CustomRecipe recipe, ItemStack[] matrix) {
        for (int i = 0; i < 9; i++) {
            RecipeIngredient required = recipe.getIngredient(i);
            ItemStack input = (i < matrix.length) ? matrix[i] : null;

            if (required == null) {
                if (input != null && !input.getType().isAir()) return false;
                continue;
            }

            if (!itemMatcher.matches(input, required)) {
                return false;
            }
        }
        return true;
    }

    public Map<String, CustomRecipe> getRecipes() {
        return recipes;
    }
}