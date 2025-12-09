package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.manager.crafting.MatchType;
import io.github.altkat.BuffedItems.menu.crafting.IngredientSettingsMenu;
import io.github.altkat.BuffedItems.menu.crafting.RecipeEditorMenu;
import io.github.altkat.BuffedItems.menu.crafting.RecipeListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RecipeInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public RecipeInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path.equals("create_recipe")) {
            String newRecipeId = input.toLowerCase().replace(" ", "_");

            if (RecipesConfig.get().contains("recipes." + newRecipeId)) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: A recipe with that ID already exists."));
                new RecipeListMenu(pmu, plugin).open();
            } else {
                createDefaultRecipe(newRecipeId);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aRecipe '" + newRecipeId + "' created successfully!"));

                pmu.setRecipeToEditId(newRecipeId);
                new RecipeEditorMenu(pmu, plugin).open();
            }
            closeChat(pmu);
        }

        else if (path.equals("recipe_result_amount")) {
            try {
                int amount = Integer.parseInt(input);
                if (amount < 1) throw new NumberFormatException();

                String recipeId = pmu.getRecipeToEditId();
                RecipesConfig.get().set("recipes." + recipeId + ".result.amount", amount);
                RecipesConfig.save();
                plugin.getCraftingManager().loadRecipes(true);

                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aResult amount updated: " + amount));
                new RecipeEditorMenu(pmu, plugin).open();

            } catch (NumberFormatException e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number."));
                new RecipeEditorMenu(pmu, plugin).open();
            }
            closeChat(pmu);
        }

        else if (path.equals("recipe_permission")) {
            String perm = input.trim();
            if (perm.equalsIgnoreCase("none") || perm.equalsIgnoreCase("null")) perm = null;

            String recipeId = pmu.getRecipeToEditId();
            RecipesConfig.get().set("recipes." + recipeId + ".permission", perm);
            RecipesConfig.save();
            plugin.getCraftingManager().loadRecipes(true);

            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aRecipe permission updated: " + (perm == null ? "None" : perm)));
            new RecipeEditorMenu(pmu, plugin).open();
            closeChat(pmu);
        }

        else if (path.equals("recipe_ingredient_amount")) {
            try {
                int amount = Integer.parseInt(input);
                if (amount < 1) throw new NumberFormatException();

                boolean success = updateIngredientAmountHelper(pmu, amount);

                if (success) {
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aIngredient amount updated: " + amount));
                } else {
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§cYou must select an ingredient first!"));
                }

                new IngredientSettingsMenu(pmu, plugin).open();

            } catch (NumberFormatException e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number. Please enter a positive integer."));
                new IngredientSettingsMenu(pmu, plugin).open();
            }
            closeChat(pmu);
        }

        else if (path.equals("recipe_result_manual")) {
            if (plugin.getItemManager().getBuffedItem(input) == null) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§eWarning: Item ID '" + input + "' not found."));
            }

            String recipeId = pmu.getRecipeToEditId();
            RecipesConfig.get().set("recipes." + recipeId + ".result.item", input);
            RecipesConfig.save();
            plugin.getCraftingManager().loadRecipes(true);

            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aRecipe result updated to: §e" + input));
            new RecipeEditorMenu(pmu, plugin).open();
            closeChat(pmu);
        }

        else if (path.equals("recipe_ingredient_buffed_manual")) {
            if (plugin.getItemManager().getBuffedItem(input) == null) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§eWarning: Item ID '" + input + "' not found."));
            }

            saveIngredientHelper(pmu, MatchType.BUFFED_ITEM, Material.STONE, input, 1);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aIngredient set to Buffed Item: §e" + input));
            new IngredientSettingsMenu(pmu, plugin, false).open();
            closeChat(pmu);
        }

        else if (path.equals("recipe_ingredient_material_manual")) {
            String matName = input.toUpperCase().replace(" ", "_");
            Material mat = Material.matchMaterial(matName);

            if (mat == null) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid Material: " + input));
                player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                return;
            }

            saveIngredientHelper(pmu, MatchType.MATERIAL, mat, mat.name(), 1);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aIngredient set to Material: §e" + mat.name()));
            new IngredientSettingsMenu(pmu, plugin, false).open();
            closeChat(pmu);
        }
    }

    private boolean updateIngredientAmountHelper(PlayerMenuUtility pmu, int newAmount) {
        String recipeId = pmu.getRecipeToEditId();
        int slotIndex = pmu.getSelectedRecipeSlot();
        char key = (char) ('A' + slotIndex);
        String path = "recipes." + recipeId + ".ingredients." + key;

        ConfigurationSection config = RecipesConfig.get();

        if (!config.contains(path + ".type")) {
            return false;
        }

        String typeStr = config.getString(path + ".type");
        String value = config.getString(path + ".value");
        MatchType type = MatchType.valueOf(typeStr);

        saveIngredientHelper(pmu, type, null, value, newAmount);
        return true;
    }

    private void saveIngredientHelper(PlayerMenuUtility pmu, MatchType type, Material mat, String value, int amount) {
        String recipeId = pmu.getRecipeToEditId();
        int slotIndex = pmu.getSelectedRecipeSlot();

        char key = (char) ('A' + slotIndex);
        String path = "recipes." + recipeId + ".ingredients." + key;
        String shapePath = "recipes." + recipeId + ".shape";

        ConfigurationSection config = RecipesConfig.get();

        if (type == null) {
            config.set(path, null);
        } else {
            config.set(path + ".type", type.name());
            config.set(path + ".value", value);
            config.set(path + ".amount", amount);
        }

        List<String> rawShape = config.getStringList(shapePath);
        List<String> shape = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            String line = (i < rawShape.size()) ? rawShape.get(i) : "   ";
            if (line == null) line = "   ";

            StringBuilder sb = new StringBuilder(line);
            while (sb.length() < 3) sb.append(" ");
            if (sb.length() > 3) sb.setLength(3);

            shape.add(sb.toString());
        }

        int row = slotIndex / 3;
        int col = slotIndex % 3;

        StringBuilder targetLine = new StringBuilder(shape.get(row));
        targetLine.setCharAt(col, (type == null) ? ' ' : key);
        shape.set(row, targetLine.toString());

        config.set(shapePath, shape);

        RecipesConfig.save();
        plugin.getCraftingManager().loadRecipes(true);
    }

    private void createDefaultRecipe(String recipeId) {
        String path = "recipes." + recipeId;
        ConfigurationSection config = RecipesConfig.get();

        config.set(path + ".result.item", "select_item_id");
        config.set(path + ".result.amount", 1);

        java.util.List<String> shape = new ArrayList<>();
        shape.add("   ");
        shape.add("   ");
        shape.add("   ");
        config.set(path + ".shape", shape);

        RecipesConfig.save();
        plugin.getCraftingManager().loadRecipes(true);
    }

    private void closeChat(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }
}