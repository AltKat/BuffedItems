package io.github.altkat.BuffedItems.manager.upgrade;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.manager.cost.types.BuffedItemCost;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpgradeManager {

    private final BuffedItems plugin;
    private final Map<String, UpgradeRecipe> recipes = new HashMap<>();

    public UpgradeManager(BuffedItems plugin) {
        this.plugin = plugin;
    }

    public void loadRecipes(boolean silent) {
        long startTime = System.currentTimeMillis();
        recipes.clear();
        ConfigurationSection section = UpgradesConfig.get().getConfigurationSection("upgrades");

        if (section == null) {
            if (!silent) ConfigManager.logInfo("&eNo upgrade recipes found in upgrades.yml.");
            return;
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[UpgradeManager] Loading recipes from config...");

        int validCount = 0;
        int invalidCount = 0;
        List<String> recipesWithErrors = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            ConfigurationSection recipeSection = section.getConfigurationSection(key);
            if (recipeSection == null) continue;

            List<String> errors = new ArrayList<>();
            boolean isValid = true;

            String displayName = recipeSection.getString("display_name", key);
            String resultItem = recipeSection.getString("result.item");
            int resultAmount = recipeSection.getInt("result.amount", 1);
            double chance = recipeSection.getDouble("success_rate", 100.0);
            String actionStr = recipeSection.getString("failure_action", "LOSE_EVERYTHING");

            FailureAction failureAction;
            try {
                failureAction = FailureAction.valueOf(actionStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                failureAction = FailureAction.LOSE_EVERYTHING;
                errors.add("Invalid failure_action '" + actionStr + "'. Defaulted to LOSE_EVERYTHING.");
            }

            if (resultItem == null || plugin.getItemManager().getBuffedItem(resultItem) == null) {
                isValid = false;
                errors.add("Invalid Result Item: " + (resultItem == null ? "NULL" : resultItem));
            }

            if (resultAmount <= 0) {
                isValid = false;
                errors.add("Result amount must be positive.");
            }

            ICost baseCost = null;
            if (recipeSection.contains("base")) {
                if (recipeSection.isString("base")) {
                    String itemId = recipeSection.getString("base");

                    Map<String, Object> syntheticMap = new HashMap<>();
                    syntheticMap.put("type", "BUFFED_ITEM");
                    syntheticMap.put("amount", 1);
                    syntheticMap.put("item_id", itemId);

                    try {
                        baseCost = plugin.getCostManager().parseCost(syntheticMap);
                    } catch (Exception e) {
                        errors.add("Base Item Error: " + e.getMessage());
                    }
                }
            }

            if (baseCost == null) {
                isValid = false;
                errors.add("Missing or Invalid Base Item ID.");
            }
            else {
                if (!(baseCost instanceof BuffedItemCost)) {
                    isValid = false;
                    errors.add("Base item MUST be a BUFFED_ITEM (Vanilla items not allowed as base).");
                }
                else {
                    BuffedItemCost bCost = (BuffedItemCost) baseCost;

                    if (bCost.getAmount() != 1) {
                        isValid = false;
                        errors.add("Base item amount MUST be exactly 1.");
                    }
                    else if (plugin.getItemManager().getBuffedItem(bCost.getRequiredItemId()) == null) {
                        isValid = false;
                        errors.add("Invalid Base Item ID: " + bCost.getRequiredItemId() + " (Not loaded)");
                    }
                }
            }

            List<ICost> ingredients = new ArrayList<>();
            if (recipeSection.contains("ingredients")) {
                List<Map<?, ?>> ingredientsMap = recipeSection.getMapList("ingredients");

                for (int i = 0; i < ingredientsMap.size(); i++) {
                    Map<?, ?> rawMap = ingredientsMap.get(i);
                    try {
                        ICost cost = plugin.getCostManager().parseCost(rawMap);
                        if (cost != null) {
                            ingredients.add(cost);
                        } else {
                            isValid = false;
                            String type = String.valueOf(rawMap.get("type"));
                            errors.add("Invalid Ingredient at index " + (i + 1) + " (Unknown Type: " + type + ")");
                        }
                    } catch (Exception e) {
                        isValid = false;
                        String type = String.valueOf(rawMap.get("type"));
                        errors.add("Invalid Ingredient at index " + (i + 1) + " (" + type + "): " + e.getMessage());
                    }
                }
            }

            if (isValid) {
                validCount++;
            } else {
                invalidCount++;
                recipesWithErrors.add(key);
            }

            UpgradeRecipe recipe = new UpgradeRecipe(key, displayName, baseCost, ingredients, resultItem, resultAmount, chance, failureAction, isValid, errors);
            recipes.put(key, recipe);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        if (!silent) {
            ConfigManager.logInfo("&aLoaded &e" + recipes.size() + "&a upgrade recipes (&e" + validCount + "&a valid, &e" + invalidCount + "&c with errors&a) in &e" + elapsedTime + "&ams");

            if (invalidCount > 0) {
                String separator = "============================================================";
                plugin.getLogger().warning(separator);
                plugin.getLogger().warning("⚠ " + invalidCount + " upgrade recipe(s) have configuration errors:");
                for (String recipeId : recipesWithErrors) {
                    UpgradeRecipe recipe = recipes.get(recipeId);
                    plugin.getLogger().warning("  • " + recipeId + " (" + recipe.getErrorMessages().size() + " error(s))");

                    if (ConfigManager.isDebugLevelEnabled(ConfigManager.DEBUG_INFO)) {
                        for (String error : recipe.getErrorMessages()) {
                            plugin.getLogger().warning("    - " + ConfigManager.stripLegacy(error));
                        }
                    }
                }
                plugin.getLogger().warning("Use /bi menu -> Configure Upgrades to fix these errors.");
                plugin.getLogger().warning(separator);
            }
        }
    }

    public UpgradeRecipe getRecipe(String id) {
        return recipes.get(id);
    }

    public Map<String, UpgradeRecipe> getRecipes() {
        return recipes;
    }
}