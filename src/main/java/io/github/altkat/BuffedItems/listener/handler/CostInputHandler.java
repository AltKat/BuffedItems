package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.active.CostListMenu;
import io.github.altkat.BuffedItems.menu.selector.CostTypeSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CostInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public CostInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path.equals("active.costs.add.ITEM_QUANTITY")) {
            handleItemQuantityInput(player, pmu, input, itemId);
            return;
        }

        if (path.equals("active.costs.add.BUFFED_ITEM_QUANTITY")) {
            handleBuffedItemQuantityInput(player, pmu, input, itemId);
            return;
        }

        if (path.startsWith("active.costs.add.")) {
            String type = path.substring(17);
            if (type.equals("BUFFED_ITEM_QUANTITY")) type = "BUFFED_ITEM";
            handleAddCost(player, pmu, input, type, itemId);
        } else if (path.equals("active.costs.edit.amount")) {
            handleEditCostAmount(player, pmu, input, itemId);
        } else if (path.equals("active.costs.edit.message")) {
            handleEditCostMessage(player, pmu, input, itemId);
        }
    }

    private void handleAddCost(Player player, PlayerMenuUtility pmu, String input, String type, String itemId) {
        Map<String, Object> newCost = new LinkedHashMap<>();
        newCost.put("type", type);

        try {
            if (type.equals("COINSENGINE")) {
                double amount;
                String currencyId = "coins";

                if (input.contains(";")) {
                    String[] parts = input.split(";");
                    if (parts.length != 2) throw new IllegalArgumentException("Invalid format");
                    amount = Double.parseDouble(parts[0]);
                    currencyId = parts[1];
                } else {
                    amount = Double.parseDouble(input);
                }

                if (amount <= 0) throw new NumberFormatException();

                if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") != null) {
                    if (su.nightexpress.coinsengine.api.CoinsEngineAPI.getCurrency(currencyId) == null) {
                        player.sendMessage(ConfigManager.fromSectionWithPrefix("§eWarning: Currency ID '" + currencyId + "' not found in CoinsEngine."));
                    }
                }

                newCost.put("currency_id", currencyId);
                newCost.put("amount", amount);
            }
            else if (type.equals("BUFFED_ITEM")) {
                String[] parts = input.split(";");
                if (parts.length != 2) throw new IllegalArgumentException("Invalid format");

                int amount = Integer.parseInt(parts[0]);
                String buffedItemId = parts[1];

                if (plugin.getItemManager().getBuffedItem(buffedItemId) == null) {
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§eWarning: Item ID '" + buffedItemId + "' is not loaded yet."));
                }

                newCost.put("item_id", buffedItemId);
                newCost.put("amount", amount);
            }
            else if (type.equals("ITEM")) {
                String[] parts = input.split(";");
                if (parts.length != 2) throw new IllegalArgumentException("Invalid format");

                int amount = Integer.parseInt(parts[0]);
                String material = parts[1].toUpperCase();

                if (Material.matchMaterial(material) == null) {
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid material: " + material));
                    closeChatInput(pmu);
                    new CostTypeSelectorMenu(pmu, plugin).open();
                    return;
                }

                newCost.put("material", material);
                newCost.put("amount", amount);
            } else {
                double amount = Double.parseDouble(input);
                if (amount <= 0) throw new NumberFormatException();

                if (amount == (int) amount) {
                    newCost.put("amount", (int) amount);
                } else {
                    newCost.put("amount", amount);
                }
            }

            List<Map<?, ?>> costs = ItemsConfig.get().getMapList("items." + itemId + ".active-mode.costs");
            costs.add(newCost);
            ConfigManager.setItemValue(itemId, "costs", costs);

            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCost added successfully!"));
            closeChatInput(pmu);
            new CostListMenu(pmu, plugin).open();

        } catch (Exception e) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid input."));
            if (type.equals("ITEM")) player.sendMessage(ConfigManager.fromSection("§7Use format: AMOUNT;MATERIAL"));
            else if (type.equals("BUFFED_ITEM")) player.sendMessage(ConfigManager.fromSection("§7Use format: AMOUNT;ITEM_ID"));
            else if (type.equals("COINSENGINE")) player.sendMessage(ConfigManager.fromSection("§7Use format: AMOUNT;CURRENCY_ID"));
            else player.sendMessage(ConfigManager.fromSection("§7Please enter a valid positive number."));

            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("active.costs.add." + type);
        }
    }


    @SuppressWarnings("unchecked")
    private void handleEditCostAmount(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        int index = pmu.getEditIndex();
        List<Map<?, ?>> costList = ItemsConfig.get().getMapList("items." + itemId + ".active-mode.costs");

        if (index < 0 || index >= costList.size()) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Cost index out of bounds."));
            closeChatInput(pmu);
            new CostListMenu(pmu, plugin).open();
            return;
        }

        List<Map<String, Object>> editableList = new ArrayList<>();
        for (Map<?, ?> map : costList) {
            editableList.add((java.util.Map<String, Object>) map);
        }

        Map<String, Object> targetCost = editableList.get(index);
        String type = (String) targetCost.get("type");

        try {
            if ("ITEM".equals(type) || "LEVEL".equals(type) || "HUNGER".equals(type) || "BUFFED_ITEM".equals(type)) {
                int val = Integer.parseInt(input);
                if (val <= 0) throw new NumberFormatException();
                targetCost.put("amount", val);
            } else {
                double val = Double.parseDouble(input);
                if (val <= 0) throw new NumberFormatException();

                if (val == (int) val) targetCost.put("amount", (int) val);
                else targetCost.put("amount", val);
            }

            ConfigManager.setItemValue(itemId, "costs", editableList);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCost amount updated!"));

            closeChatInput(pmu);
            new CostListMenu(pmu, plugin).open();

        } catch (NumberFormatException e) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid amount. Please enter a positive number."));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("active.costs.edit.amount");
        }
    }

    @SuppressWarnings("unchecked")
    private void handleEditCostMessage(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        int index = pmu.getEditIndex();
        List<Map<?, ?>> costList = ItemsConfig.get().getMapList("items." + itemId + ".active-mode.costs");

        if (index < 0 || index >= costList.size()) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Cost index out of bounds."));
            closeChatInput(pmu);
            new CostListMenu(pmu, plugin).open();
            return;
        }

        List<Map<String, Object>> editableList = new ArrayList<>();
        for (Map<?, ?> map : costList) {
            editableList.add((java.util.Map<String, Object>) map);
        }

        Map<String, Object> targetCost = editableList.get(index);

        if ("default".equalsIgnoreCase(input) || "reset".equalsIgnoreCase(input)) {
            targetCost.remove("message");
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aMessage reset to default config value."));
        } else {
            targetCost.put("message", input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aFailure message updated!"));
        }

        ConfigManager.setItemValue(itemId, "costs", editableList);
        closeChatInput(pmu);
        new CostListMenu(pmu, plugin).open();
    }

    private void handleBuffedItemQuantityInput(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        try {
            int amount = Integer.parseInt(input);
            if (amount <= 0) throw new NumberFormatException();

            String selectedId = pmu.getTempId();
            String simulatedInput = amount + ";" + selectedId;

            handleAddCost(player, pmu, simulatedInput, "BUFFED_ITEM", itemId);

        } catch (NumberFormatException e) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid amount. Please enter a valid number."));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("active.costs.add.BUFFED_ITEM_QUANTITY");
        }
    }

    private void handleItemQuantityInput(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        try {
            int amount = Integer.parseInt(input);
            if (amount <= 0) throw new NumberFormatException();

            Material selectedMat = pmu.getTempMaterial();
            if (selectedMat == null) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Session lost. Please select material again."));
                closeChatInput(pmu);
                return;
            }

            String simulatedInput = amount + ";" + selectedMat.name();
            handleAddCost(player, pmu, simulatedInput, "ITEM", itemId);

        } catch (NumberFormatException e) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid amount. Please enter a valid number."));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("active.costs.add.ITEM_QUANTITY");
        }
    }

    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }
}