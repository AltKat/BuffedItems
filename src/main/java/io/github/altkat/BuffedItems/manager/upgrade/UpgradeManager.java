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
        recipes.clear();
        ConfigurationSection section = UpgradesConfig.get().getConfigurationSection("upgrades");

        if (section == null) {
            if (!silent) ConfigManager.logInfo("&eNo upgrade recipes found in upgrades.yml.");
            return;
        }

        int count = 0;
        for (String key : section.getKeys(false)) {
            ConfigurationSection recipeSection = section.getConfigurationSection(key);
            if (recipeSection == null) continue;

            List<String> errors = new ArrayList<>();
            boolean isValid = true;

            String displayName = recipeSection.getString("display_name", key);
            String resultItem = recipeSection.getString("result.item");
            int resultAmount = recipeSection.getInt("result.amount", 1);
            double chance = recipeSection.getDouble("success_rate", 100.0);
            boolean keepOnFail = recipeSection.getBoolean("prevent_failure_loss", false);

            if (resultItem == null || plugin.getItemManager().getBuffedItem(resultItem) == null) {
                isValid = false;
                errors.add("Invalid Result Item: " + (resultItem == null ? "NULL" : resultItem));
            }

            ICost baseCost = null;
            if (recipeSection.contains("base")) {
                if (recipeSection.isString("base")) {
                    String itemId = recipeSection.getString("base");

                    Map<String, Object> syntheticMap = new HashMap<>();
                    syntheticMap.put("type", "BUFFED_ITEM");
                    syntheticMap.put("amount", 1);
                    syntheticMap.put("item_id", itemId);

                    baseCost = plugin.getCostManager().parseCost(syntheticMap);
                }
            }

            if (baseCost == null) {
                isValid = false;
                errors.add("Missing or Invalid Base Item");
            }
            else {
                if (!(baseCost instanceof BuffedItemCost)) {
                    isValid = false;
                    errors.add("Base item MUST be a BUFFED_ITEM (Vanilla items not allowed).");
                }
                else {
                    BuffedItemCost bCost = (BuffedItemCost) baseCost;

                    if (bCost.getAmount() != 1) {
                        isValid = false;
                        errors.add("Base item amount MUST be exactly 1.");
                    }
                    else if (plugin.getItemManager().getBuffedItem(bCost.getRequiredItemId()) == null) {
                        isValid = false;
                        errors.add("Invalid Base Item: " + bCost.getRequiredItemId());
                    }
                }
            }

            List<Map<?, ?>> ingredientsMap = recipeSection.getMapList("ingredients");
            List<ICost> ingredients = plugin.getCostManager().parseCosts(ingredientsMap);

            UpgradeRecipe recipe = new UpgradeRecipe(key, displayName, baseCost, ingredients, resultItem, resultAmount, chance, keepOnFail, isValid, errors);
            recipes.put(key, recipe);
            count++;
        }

        if (!silent) {
            ConfigManager.logInfo("&aLoaded &e" + count + "&a upgrade recipes.");
        }
    }

    public UpgradeRecipe getRecipe(String id) {
        return recipes.get(id);
    }

    public Map<String, UpgradeRecipe> getRecipes() {
        return recipes;
    }
}