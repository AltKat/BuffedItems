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
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CraftingManager {

    private final BuffedItems plugin;
    private final Map<String, CustomRecipe> recipes;
    private final ItemMatcher itemMatcher;
    private final Set<NamespacedKey> trackedKeys = new HashSet<>();

    private boolean isFirstLoad = true;
    private BukkitRunnable registrationTask;

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

        if (isFirstLoad) {
            unloadRecipes();
        } else {
            if (!silent) ConfigManager.logInfo("&eReload detected. Updating internal cache only. Vanilla Recipe Book will NOT be updated to prevent lag/kicks.");
        }

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
                
                boolean isCooking = recipe.getType() != RecipeType.SHAPED && recipe.getType() != RecipeType.SHAPELESS;
                
                if (recipe.isEnabled()) {
                    if (isCooking) {
                        registrationQueue.add(recipe);
                    } else if (isFirstLoad && shouldRegisterToBook) {
                        registrationQueue.add(recipe);
                    }
                }
            } else {
                invalidCount++;
                recipesWithErrors.add(recipeId);
            }
        }

        if (!registrationQueue.isEmpty()) {
            startBatchRegistration(registrationQueue, silent);
        }

        if (isFirstLoad) {
            isFirstLoad = false;
        }

        printLoadLog(silent, validCount, invalidCount, recipesWithErrors, startTime);
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

    public void unloadRecipes() {
        if (registrationTask != null && !registrationTask.isCancelled()) {
            registrationTask.cancel();
            registrationTask = null;
        }

        if (trackedKeys.isEmpty()) return;

        for (NamespacedKey key : trackedKeys) {
            try {
                Bukkit.removeRecipe(key);
            } catch (Exception ignored) {}
        }
        trackedKeys.clear();
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

    private CustomRecipe parseRecipeFromConfig(String recipeId, ConfigurationSection rSection) {
        String resultItemId = rSection.getString("result.item");
        int resultAmount = rSection.getInt("result.amount", 1);
        List<String> shape = rSection.getStringList("shape");
        String permission = rSection.getString("permission");
        String typeStr = rSection.getString("type", "SHAPED");
        
        RecipeType type;
        try {
            type = RecipeType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = RecipeType.SHAPED;
        }

        boolean enabled = rSection.getBoolean("enabled", true);

        CustomRecipe recipe = new CustomRecipe(recipeId, resultItemId, resultAmount, shape, permission, type);
        recipe.setEnabled(enabled);
        
        if (type == RecipeType.FURNACE || type == RecipeType.BLAST_FURNACE || type == RecipeType.SMOKER || type == RecipeType.CAMPFIRE) {
            recipe.setCookTime((int) (rSection.getDouble("cook_time", 10.0) * 20));
            recipe.setExperience(rSection.getDouble("experience", 0.7));
        }

        if (resultItemId == null || plugin.getItemManager().getBuffedItem(resultItemId) == null)
            recipe.addErrorMessage("Invalid Result Item: " + resultItemId);

        if (resultAmount <= 0) recipe.addErrorMessage("Result amount must be positive.");

        if (type == RecipeType.SHAPED) {
            if (shape.isEmpty() || shape.size() > 3) {
                recipe.addErrorMessage("Invalid Shape Size.");
            } else {
                boolean empty = true;
                for (String s : shape) if (!s.trim().isEmpty()) empty = false;
                if (empty) recipe.addErrorMessage("Shape cannot be empty.");
            }
        }

        ConfigurationSection ingSection = rSection.getConfigurationSection("ingredients");

        if (ingSection != null) {
            for (String key : ingSection.getKeys(false)) {
                if (key.length() != 1) continue;
                char c = key.charAt(0);

                String ingTypeStr = ingSection.getString(key + ".type", "MATERIAL");
                String value = ingSection.getString(key + ".value", "AIR");
                int amount = ingSection.getInt(key + ".amount", 1);

                MatchType ingType;
                try { ingType = MatchType.valueOf(ingTypeStr.toUpperCase()); }
                catch (IllegalArgumentException e) {
                    recipe.addErrorMessage("Invalid type: " + ingTypeStr); continue;
                }

                Material mat = Material.STONE;
                ItemStack exactStack = null;

                if (ingType == MatchType.MATERIAL) {
                    mat = Material.matchMaterial(value);
                    if (mat == null) recipe.addErrorMessage("Invalid Material: " + value);
                } else if (ingType == MatchType.BUFFED_ITEM) {
                    BuffedItem bi = plugin.getItemManager().getBuffedItem(value);
                    if (bi != null) mat = bi.getMaterial();
                    else recipe.addErrorMessage("Unknown BuffedItem: " + value);
                } else if (ingType == MatchType.EXACT) {
                    exactStack = io.github.altkat.BuffedItems.utility.Serializer.fromBase64(value);
                    if (exactStack != null) mat = exactStack.getType();
                    else recipe.addErrorMessage("Invalid Base64");
                }

                RecipeIngredient ingredient = new RecipeIngredient(ingType, mat, value, amount);
                if (exactStack != null) ingredient.setExactReferenceItem(exactStack);
                recipe.addIngredient(c, ingredient);
            }
        }

        if (type == RecipeType.FURNACE || type == RecipeType.BLAST_FURNACE || type == RecipeType.SMOKER || type == RecipeType.CAMPFIRE) {
            for (RecipeIngredient ing : recipe.getIngredients().values()) {
                if (ing.getAmount() > 1) {
                    recipe.addErrorMessage("Cooking recipes cannot have ingredients with amount > 1.");
                    break;
                }
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

            Recipe bukkitRecipe = null;

            switch (recipe.getType()) {
                case SHAPED:
                    ShapedRecipe shaped = new ShapedRecipe(key, resultStack);
                    shaped.shape(recipe.getShape().toArray(new String[0]));
                    for (Map.Entry<Character, RecipeIngredient> entry : recipe.getIngredients().entrySet()) {
                        shaped.setIngredient(entry.getKey(), createRecipeChoice(entry.getValue()));
                    }
                    bukkitRecipe = shaped;
                    break;

                case SHAPELESS:
                    ShapelessRecipe shapeless = new ShapelessRecipe(key, resultStack);
                    for (RecipeIngredient ing : recipe.getIngredients().values()) {
                        shapeless.addIngredient(createRecipeChoice(ing));
                    }
                    bukkitRecipe = shapeless;
                    break;

                case FURNACE:
                case BLAST_FURNACE:
                case SMOKER:
                case CAMPFIRE:
                    RecipeIngredient input = getFirstIngredient(recipe);
                    if (input == null) return;
                    RecipeChoice inputChoice = createRecipeChoice(input);
                    
                    if (recipe.getType() == RecipeType.FURNACE) {
                        bukkitRecipe = new FurnaceRecipe(key, resultStack, inputChoice, (float) recipe.getExperience(), recipe.getCookTime());
                    } else if (recipe.getType() == RecipeType.BLAST_FURNACE) {
                        bukkitRecipe = new BlastingRecipe(key, resultStack, inputChoice, (float) recipe.getExperience(), recipe.getCookTime());
                    } else if (recipe.getType() == RecipeType.SMOKER) {
                        bukkitRecipe = new SmokingRecipe(key, resultStack, inputChoice, (float) recipe.getExperience(), recipe.getCookTime());
                    } else {
                        bukkitRecipe = new CampfireRecipe(key, resultStack, inputChoice, (float) recipe.getExperience(), recipe.getCookTime());
                    }
                    break;
            }

            if (bukkitRecipe != null) {
                Bukkit.addRecipe(bukkitRecipe);
            }

        } catch (Exception e) {
            ConfigManager.logInfo("Failed to register Bukkit recipe: " + recipe.getId());
            e.printStackTrace();
        }
    }

    private RecipeIngredient getFirstIngredient(CustomRecipe recipe) {
        if (recipe.getIngredients().isEmpty()) return null;
        return recipe.getIngredients().values().iterator().next();
    }

    private RecipeChoice createRecipeChoice(RecipeIngredient ingredient) {
        if (ingredient.getMatchType() == MatchType.BUFFED_ITEM) {
            BuffedItem bi = plugin.getItemManager().getBuffedItem(ingredient.getData());
            if (bi != null) {
                ItemStack exactItem = new ItemBuilder(bi, plugin).build();
                exactItem.setAmount(1);
                return new RecipeChoice.ExactChoice(exactItem);
            }
        } else if (ingredient.getMatchType() == MatchType.EXACT && ingredient.getExactReferenceItem() != null) {
            ItemStack exactItem = ingredient.getExactReferenceItem().clone();
            exactItem.setAmount(1);
            return new RecipeChoice.ExactChoice(exactItem);
        }
        
        if (ingredient.getMaterial() != null) {
            return new RecipeChoice.MaterialChoice(ingredient.getMaterial());
        }
        return null;
    }

    public CustomRecipe findRecipe(ItemStack[] matrix) {
        if (isMatrixEmpty(matrix)) return null;

        // Collect all unique materials present in the matrix to find candidates
        Set<Material> presentMaterials = new HashSet<>();
        for (ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                presentMaterials.add(item.getType());
            }
        }

        for (Material mat : presentMaterials) {
            List<CustomRecipe> candidates = recipeCache.get(mat);
            if (candidates == null) continue;

            for (CustomRecipe recipe : candidates) {
                if (!recipe.isValid() || !recipe.isEnabled()) continue;

                if (recipe.getType() == RecipeType.SHAPELESS) {
                    if (matchesShapeless(matrix, recipe)) return recipe;
                } else if (recipe.getType() == RecipeType.SHAPED) {
                    if (matchesShaped(matrix, recipe)) return recipe;
                }
            }
        }
        return null;
    }

    private void cacheRecipe(CustomRecipe recipe) {
        if (recipe.getType() == RecipeType.SHAPED) {
            char[][] grid = parseShape(recipe.getShape());
            if (grid.length == 0) return;

            for (char[] row : grid) {
                for (char keyChar : row) {
                    if (keyChar != ' ') {
                        RecipeIngredient ing = recipe.getIngredient(keyChar);
                        if (ing != null && ing.getMaterial() != null) {
                            List<CustomRecipe> list = recipeCache.computeIfAbsent(ing.getMaterial(), k -> new ArrayList<>());
                            if (!list.contains(recipe)) {
                                if (ing.getMatchType() == MatchType.MATERIAL) list.add(recipe);
                                else list.add(0, recipe);
                            }
                            return; // Only cache under the first non-empty material for SHAPED
                        }
                    }
                }
            }
        } else if (recipe.getType() == RecipeType.SHAPELESS) {
            for (RecipeIngredient ing : recipe.getIngredients().values()) {
                if (ing.getMaterial() != null) {
                    List<CustomRecipe> list = recipeCache.computeIfAbsent(ing.getMaterial(), k -> new ArrayList<>());
                    if (!list.contains(recipe)) {
                        if (ing.getMatchType() == MatchType.MATERIAL) list.add(recipe);
                        else list.add(0, recipe);
                    }
                }
            }
        }
    }

    private boolean matchesShapeless(ItemStack[] matrix, CustomRecipe recipe) {
        List<ItemStack> inputItems = new ArrayList<>();
        for (ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                inputItems.add(item.clone());
            }
        }

        List<RecipeIngredient> requiredIngredients = new ArrayList<>(recipe.getIngredients().values());

        if (inputItems.size() != requiredIngredients.size()) return false;

        for (RecipeIngredient ingredient : requiredIngredients) {
            boolean found = false;
            for (int i = 0; i < inputItems.size(); i++) {
                ItemStack input = inputItems.get(i);
                if (plugin.getCraftingManager().getItemMatcher().matches(input, ingredient)) {
                    inputItems.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }

        return inputItems.isEmpty();
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