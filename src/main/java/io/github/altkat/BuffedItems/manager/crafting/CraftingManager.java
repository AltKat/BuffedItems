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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CraftingManager {

    private final BuffedItems plugin;
    private final Map<String, CustomRecipe> recipes;
    private final ItemMatcher itemMatcher;
    private final Set<NamespacedKey> trackedKeys = new HashSet<>();

    private BukkitRunnable registrationTask;
    private BukkitRunnable removalTask;

    private final Map<Material, List<CustomRecipe>> recipeCache = new HashMap<>();

    public CraftingManager(BuffedItems plugin) {
        this.plugin = plugin;
        this.recipes = new LinkedHashMap<>();
        this.itemMatcher = new ItemMatcher(plugin);
    }

    public void loadRecipes(boolean silent) {
        long startTime = System.currentTimeMillis();

        if (registrationTask != null && !registrationTask.isCancelled()) {
            registrationTask.cancel();
            registrationTask = null;
        }

        if (removalTask != null && !removalTask.isCancelled()) {
            removalTask.cancel();
            removalTask = null;
        }

        unloadRecipes();
        recipes.clear();
        recipeCache.clear();

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

        Queue<CustomRecipe> registrationQueue = new ConcurrentLinkedQueue<>();
        boolean shouldRegisterToBook = RecipesConfig.get().getBoolean("settings.register-to-book", true);

        for (String recipeId : section.getKeys(false)) {
            ConfigurationSection rSection = section.getConfigurationSection(recipeId);
            if (rSection == null) continue;

            CustomRecipe recipe = parseRecipeFromConfig(recipeId, rSection);
            recipes.put(recipeId, recipe);

            if (recipe.isValid()) {
                cacheRecipe(recipe);
                validCount++;
                if (shouldRegisterToBook && recipe.isEnabled()) {
                    registrationQueue.add(recipe);
                }
            } else {
                invalidCount++;
                recipesWithErrors.add(recipeId);
            }
        }

        printLoadLog(silent, validCount, invalidCount, recipesWithErrors, startTime);

        if (!registrationQueue.isEmpty()) {
            startBatchRegistration(registrationQueue, silent);
        }
    }

    private void startBatchRegistration(Queue<CustomRecipe> queue, boolean silent) {
        final int BATCH_SIZE = 50;
        final int totalToRegister = queue.size();

        registrationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    this.cancel();
                    if (!silent) {
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () ->
                                "[CraftingManager] Background task finished: Registered " + totalToRegister + " recipes to Bukkit.");
                    }
                    return;
                }

                long batchStart = System.nanoTime();
                int count = 0;

                while (!queue.isEmpty() && count < BATCH_SIZE) {
                    CustomRecipe recipe = queue.poll();
                    if (recipe != null) {
                        registerBukkitRecipe(recipe);
                    }
                    count++;
                    if (System.nanoTime() - batchStart > 2000000) break;
                }
            }
        };

        registrationTask.runTaskTimer(plugin, 1L, 2L);
    }

    private void printLoadLog(boolean silent, int valid, int invalid, List<String> errorIds, long startTime) {
        if (silent) return;
        long elapsedTime = System.currentTimeMillis() - startTime;
        ConfigManager.logInfo("&aLoaded &e" + valid + "&a custom recipes into memory (&e" + valid + "&a valid, &c" + invalid + "&a with errors) in &e" + elapsedTime + "&ams.");

        if (invalid > 0) {
            String separator = "============================================================";
            plugin.getLogger().warning(separator);
            plugin.getLogger().warning("⚠ " + invalid + " custom recipe(s) have configuration errors:");
            for (String rId : errorIds) {
                plugin.getLogger().warning("  • " + rId);

                CustomRecipe r = recipes.get(rId);
                if (r != null) {
                    for (String err : r.getErrorMessages()) {
                        plugin.getLogger().warning("    - " + err);
                    }
                }
            }
            plugin.getLogger().warning(separator);
        }
    }

    public void unloadRecipes() {

        if (registrationTask != null && !registrationTask.isCancelled()) {
            registrationTask.cancel();
            registrationTask = null;
        }

        if (removalTask != null && !removalTask.isCancelled()) {
            removalTask.cancel();
            removalTask = null;
        }

        if (trackedKeys.isEmpty()) return;

        Queue<NamespacedKey> removalQueue = new ConcurrentLinkedQueue<>(trackedKeys);

        trackedKeys.clear();

        startBatchRemoval(removalQueue);
    }

    private void startBatchRemoval(Queue<NamespacedKey> queue) {
        final int BATCH_SIZE = 50;

        removalTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (queue.isEmpty()) {
                    this.cancel();
                    return;
                }

                long batchStart = System.nanoTime();
                int count = 0;

                while (!queue.isEmpty() && count < BATCH_SIZE) {
                    NamespacedKey key = queue.poll();
                    if (key != null) {
                        try {
                            Bukkit.removeRecipe(key);
                        } catch (Exception ignored) {}
                    }
                    count++;
                    if (System.nanoTime() - batchStart > 2000000) break;
                }
            }
        };
        removalTask.runTaskTimer(plugin, 0L, 2L);
    }

    private CustomRecipe parseRecipeFromConfig(String recipeId, ConfigurationSection rSection) {
        String resultItemId = rSection.getString("result.item");
        int resultAmount = rSection.getInt("result.amount", 1);
        List<String> shape = rSection.getStringList("shape");
        String permission = rSection.getString("permission");

        boolean enabled = rSection.getBoolean("enabled", true);

        CustomRecipe recipe = new CustomRecipe(recipeId, resultItemId, resultAmount, shape, permission);

        recipe.setEnabled(enabled);

        if (resultItemId == null || plugin.getItemManager().getBuffedItem(resultItemId) == null)
            recipe.addErrorMessage("Invalid Result Item: " + resultItemId);

        if (resultAmount <= 0) recipe.addErrorMessage("Result amount must be positive.");

        if (shape.isEmpty() || shape.size() > 3) {
            recipe.addErrorMessage("Invalid Shape Size.");
        } else {
            boolean empty = true;
            for(String s : shape) if(!s.trim().isEmpty()) empty = false;
            if(empty) recipe.addErrorMessage("Shape cannot be empty.");
        }

        ConfigurationSection ingSection = rSection.getConfigurationSection("ingredients");

        if (ingSection != null) {
            for (String key : ingSection.getKeys(false)) {
                if (key.length() != 1) continue;
                char c = key.charAt(0);

                String typeStr = ingSection.getString(key + ".type", "MATERIAL");
                String value = ingSection.getString(key + ".value", "AIR");
                int amount = ingSection.getInt(key + ".amount", 1);

                MatchType type;
                try { type = MatchType.valueOf(typeStr.toUpperCase()); }
                catch (IllegalArgumentException e) {
                    recipe.addErrorMessage("Invalid type: " + typeStr); continue;
                }

                Material mat = Material.STONE;
                ItemStack exactStack = null;

                if (type == MatchType.MATERIAL) {
                    mat = Material.matchMaterial(value);
                    if (mat == null) recipe.addErrorMessage("Invalid Material: " + value);
                } else if (type == MatchType.BUFFED_ITEM) {
                    BuffedItem bi = plugin.getItemManager().getBuffedItem(value);
                    if (bi != null) mat = bi.getMaterial();
                    else recipe.addErrorMessage("Unknown BuffedItem: " + value);
                } else if (type == MatchType.EXACT) {
                    exactStack = io.github.altkat.BuffedItems.utility.Serializer.fromBase64(value);
                    if (exactStack != null) mat = exactStack.getType();
                    else recipe.addErrorMessage("Invalid Base64");
                }

                RecipeIngredient ingredient = new RecipeIngredient(type, mat, value, amount);
                if (exactStack != null) ingredient.setExactReferenceItem(exactStack);
                recipe.addIngredient(c, ingredient);
            }
        }

        if (recipe.isValid()) {
            Set<Character> shapeChars = new HashSet<>();
            for (String line : shape) {
                for (char c : line.toCharArray()) {
                    if (c != ' ') {
                        shapeChars.add(c);
                    }
                }
            }

            for (char c : shapeChars) {
                if (!recipe.getIngredients().containsKey(c)) {
                    recipe.addErrorMessage("Shape contains character '" + c + "' but it is not defined in ingredients.");
                }
            }

            for (char c : recipe.getIngredients().keySet()) {
                if (!shapeChars.contains(c)) {
                    recipe.addErrorMessage("Ingredient '" + c + "' is defined but not used in the shape.");
                }
            }
        }

        return recipe;
    }

    private void registerBukkitRecipe(CustomRecipe recipe) {
        try {
            NamespacedKey key = new NamespacedKey(plugin, recipe.getId());

            try {
                Bukkit.removeRecipe(key);
            } catch (Exception ignored) {}

            trackedKeys.add(key);

            ItemStack resultStack;
            BuffedItem item = plugin.getItemManager().getBuffedItem(recipe.getResultItemId());

            if (item != null) {
                resultStack = new ItemBuilder(item, plugin).build();
                resultStack.setAmount(recipe.getAmount());
            } else {
                return;
            }

            ShapedRecipe bukkitRecipe = new ShapedRecipe(key, resultStack);

            List<String> shape = recipe.getShape();
            bukkitRecipe.shape(shape.toArray(new String[0]));

            for (Map.Entry<Character, RecipeIngredient> entry : recipe.getIngredients().entrySet()) {
                char charKey = entry.getKey();
                RecipeIngredient ingredient = entry.getValue();

                if (ingredient.getMatchType() == MatchType.BUFFED_ITEM) {
                    BuffedItem bi = plugin.getItemManager().getBuffedItem(ingredient.getData());
                    if (bi != null) {
                        ItemStack exactItem = new ItemBuilder(bi, plugin).build();
                        exactItem.setAmount(1);
                        bukkitRecipe.setIngredient(charKey, new org.bukkit.inventory.RecipeChoice.ExactChoice(exactItem));
                    }
                } else if (ingredient.getMatchType() == MatchType.EXACT && ingredient.getExactReferenceItem() != null) {
                    ItemStack exactItem = ingredient.getExactReferenceItem().clone();
                    exactItem.setAmount(1);
                    bukkitRecipe.setIngredient(charKey, new org.bukkit.inventory.RecipeChoice.ExactChoice(exactItem));
                } else {
                    if (ingredient.getMaterial() != null) {
                        bukkitRecipe.setIngredient(charKey, ingredient.getMaterial());
                    }
                }
            }

            Bukkit.addRecipe(bukkitRecipe);

        } catch (Exception e) {
            ConfigManager.logInfo("Failed to register Bukkit recipe: " + recipe.getId());
        }
    }

    public CustomRecipe findRecipe(ItemStack[] matrix) {
        if (isMatrixEmpty(matrix)) return null;

        ItemStack firstItem = null;
        for (ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                firstItem = item;
                break;
            }
        }

        if (firstItem == null) return null;

        List<CustomRecipe> candidates = recipeCache.get(firstItem.getType());

        if (candidates == null || candidates.isEmpty()) return null;

        for (CustomRecipe recipe : candidates) {
            if (!recipe.isValid()) continue;
            if (matchesShaped(matrix, recipe)) return recipe;
        }
        return null;
    }

    private void cacheRecipe(CustomRecipe recipe) {
        char[][] grid = parseShape(recipe.getShape());

        if (grid.length == 0) return;

        for (char keyChar : grid[0]) {
            if (keyChar != ' ') {
                RecipeIngredient ing = recipe.getIngredient(keyChar);

                if (ing != null && ing.getMaterial() != null) {
                    List<CustomRecipe> list = recipeCache.computeIfAbsent(ing.getMaterial(), k -> new ArrayList<>());

                    if (ing.getMatchType() == MatchType.MATERIAL) {
                        list.add(recipe);
                    } else {
                        list.add(0, recipe);
                    }
                    return;
                }
            }
        }
    }

    private boolean isMatrixEmpty(ItemStack[] matrix) {
        for (ItemStack item : matrix) if (item != null && !item.getType().isAir()) return false;
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