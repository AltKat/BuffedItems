package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import io.github.altkat.BuffedItems.menu.upgrade.IngredientListMenu;
import io.github.altkat.BuffedItems.menu.upgrade.UpgradeRecipeEditorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IngredientInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public IngredientInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String recipeId) {
        if (path.equals("upgrade.ingredients.add.BUFFED_ITEM_QUANTITY")) {
            handleQuantityInput(player, pmu, input, recipeId);
            return;
        }

        if (path.startsWith("upgrade.ingredients.add.")) {
            String type = path.substring(24);
            handleAddIngredient(player, pmu, input, type, recipeId);
        }
        else if (path.equals("upgrade.base.set_id")) {
            handleSetBaseId(player, pmu, input, recipeId);
        }
        else if (path.equals("upgrade.ingredients.edit.amount")) {
            handleEditIngredientAmount(player, pmu, input, recipeId);
        }
    }

    private void handleSetBaseId(Player player, PlayerMenuUtility pmu, String input, String recipeId) {
        String itemId = input.trim();

        if (plugin.getItemManager().getBuffedItem(itemId) == null) {
            player.sendMessage(ConfigManager.fromSection("§eWarning: Item ID '" + itemId + "' not found in loaded items."));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("type", "BUFFED_ITEM");
        data.put("amount", 1);
        data.put("item_id", itemId);

        ConfigManager.setUpgradeValue(recipeId, "base", data);

        player.sendMessage(ConfigManager.fromSection("§aBase item updated to: §e" + itemId));
        closeChat(pmu);
        new UpgradeRecipeEditorMenu(pmu, plugin).open();
    }

    private void handleAddIngredient(Player player, PlayerMenuUtility pmu, String input, String type, String recipeId) {

        Map<String, Object> data = parseCostInput(player, input, type);
        if (data == null) {
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("upgrade.ingredients.add." + type);
            return;
        }

        List<Map<?, ?>> currentList = UpgradesConfig.get().getMapList("upgrades." + recipeId + ".ingredients");
        List<Map<String, Object>> newList = new ArrayList<>();
        for (Map<?, ?> map : currentList) newList.add((Map<String, Object>) map);

        newList.add(data);
        ConfigManager.setUpgradeValue(recipeId, "ingredients", newList);

        player.sendMessage(ConfigManager.fromSection("§aIngredient added successfully!"));
        closeChat(pmu);
        new IngredientListMenu(pmu, plugin).open();
    }

    @SuppressWarnings("unchecked")
    private void handleEditIngredientAmount(Player player, PlayerMenuUtility pmu, String input, String recipeId) {
        int index = pmu.getEditIndex();
        List<Map<?, ?>> currentList = UpgradesConfig.get().getMapList("upgrades." + recipeId + ".ingredients");

        if (index < 0 || index >= currentList.size()) {
            closeChat(pmu);
            new IngredientListMenu(pmu, plugin).open();
            return;
        }

        List<Map<String, Object>> editableList = new ArrayList<>();
        for (Map<?, ?> map : currentList) editableList.add((Map<String, Object>) map);

        Map<String, Object> target = editableList.get(index);
        String type = (String) target.get("type");

        try {
            if (isIntegerType(type)) {
                int val = Integer.parseInt(input);
                if (val <= 0) throw new NumberFormatException();
                target.put("amount", val);
            } else {
                double val = Double.parseDouble(input);
                if (val <= 0) throw new NumberFormatException();
                target.put("amount", (val % 1 == 0) ? (int) val : val);
            }

            ConfigManager.setUpgradeValue(recipeId, "ingredients", editableList);
            player.sendMessage(ConfigManager.fromSection("§aAmount updated!"));
            closeChat(pmu);
            new IngredientListMenu(pmu, plugin).open();

        } catch (NumberFormatException e) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid amount. Enter a positive number."));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("upgrade.ingredients.edit.amount");
        }
    }

    private Map<String, Object> parseCostInput(Player player, String input, String type) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);

        try {
            if (type.equals("ITEM")) {
                String[] parts = input.split(";");
                if (parts.length != 2) throw new IllegalArgumentException();
                int amount = Integer.parseInt(parts[0]);
                String material = parts[1].toUpperCase();
                if (Material.matchMaterial(material) == null) {
                    player.sendMessage(ConfigManager.fromSection("§cInvalid material: " + material));
                    return null;
                }
                data.put("amount", amount);
                data.put("material", material);
            }
            else if (type.equals("BUFFED_ITEM")) {
                String[] parts = input.split(";");
                if (parts.length != 2) throw new IllegalArgumentException();
                int amount = Integer.parseInt(parts[0]);
                String itemId = parts[1];
                if (plugin.getItemManager().getBuffedItem(itemId) == null) {
                    player.sendMessage(ConfigManager.fromSection("§eWarning: Item ID '" + itemId + "' not found (yet)."));
                }
                data.put("amount", amount);
                data.put("item_id", itemId);
            }
            else if (type.equals("COINSENGINE")) {
                if (input.contains(";")) {
                    String[] parts = input.split(";");
                    data.put("amount", Double.parseDouble(parts[0]));
                    data.put("currency_id", parts[1]);
                } else {
                    data.put("amount", Double.parseDouble(input));
                    data.put("currency_id", "coins");
                }
            }
            else {
                if (isIntegerType(type)) {
                    data.put("amount", Integer.parseInt(input));
                } else {
                    double val = Double.parseDouble(input);
                    data.put("amount", (val % 1 == 0) ? (int) val : val);
                }
            }
            return data;
        } catch (Exception e) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid format."));
            if (type.equals("ITEM")) player.sendMessage(ConfigManager.fromSection("§7Format: AMOUNT;MATERIAL (e.g., 5;DIAMOND)"));
            else if (type.equals("BUFFED_ITEM")) player.sendMessage(ConfigManager.fromSection("§7Format: AMOUNT;ITEM_ID"));
            else player.sendMessage(ConfigManager.fromSection("§7Enter a valid number."));
            return null;
        }
    }

    private void handleQuantityInput(Player player, PlayerMenuUtility pmu, String input, String recipeId) {
        try {
            int amount = Integer.parseInt(input);
            if (amount <= 0) throw new NumberFormatException();

            String itemId = pmu.getTempId();

            String simulatedInput = amount + ";" + itemId;
            handleAddIngredient(player, pmu, simulatedInput, "BUFFED_ITEM", recipeId);

        } catch (NumberFormatException e) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid amount. Please enter a valid number."));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("upgrade.ingredients.add.BUFFED_ITEM_QUANTITY");
        }
    }

    private boolean isIntegerType(String type) {
        return type.equals("EXPERIENCE") || type.equals("LEVEL") || type.equals("HUNGER") || type.equals("ITEM") || type.equals("BUFFED_ITEM");
    }

    private void closeChat(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }
}