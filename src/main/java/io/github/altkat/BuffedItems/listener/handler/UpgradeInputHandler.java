package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import io.github.altkat.BuffedItems.menu.upgrade.UpgradeRecipeEditorMenu;
import io.github.altkat.BuffedItems.menu.upgrade.UpgradeRecipeListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.entity.Player;

public class UpgradeInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public UpgradeInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path.equals("create_upgrade")) {
            handleCreateUpgrade(player, pmu, input);
        } else if (path.equals("upgrade.display_name")) {
            ConfigManager.setUpgradeValue(itemId, "display_name", input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aDisplay Name updated!"));
            returnToEditor(pmu);
        } else if (path.equals("upgrade.success_rate")) {
            try {
                double rate = Double.parseDouble(input);
                if (rate < 0 || rate > 100) throw new NumberFormatException();
                ConfigManager.setUpgradeValue(itemId, "success_rate", rate);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSuccess Rate updated to " + rate + "%"));
            } catch (NumberFormatException e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number! Enter a value between 0-100."));
            }
            returnToEditor(pmu);
        } else if (path.equals("upgrade.result.item")) {
            ConfigManager.setUpgradeValue(itemId, "result.item", input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aResult Item ID updated to: " + input));
            returnToEditor(pmu);
        } else if (path.equals("upgrade.result.amount")) {
            try {
                int amount = Integer.parseInt(input);
                if (amount < 1) throw new NumberFormatException();
                ConfigManager.setUpgradeValue(itemId, "result.amount", amount);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aResult Amount updated to: " + amount));
            } catch (NumberFormatException e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number!"));
            }
            returnToEditor(pmu);
        }
    }

    private void handleCreateUpgrade(Player player, PlayerMenuUtility pmu, String input) {
        String newId = input.toLowerCase().replace(" ", "_");
        if (UpgradesConfig.get().contains("upgrades." + newId)) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: An upgrade with this ID already exists."));
            new UpgradeRecipeListMenu(pmu, plugin).open();
            closeChat(pmu);
            return;
        }

        String defaultItemId = "select_item_id";

        if (!plugin.getItemManager().getLoadedItems().isEmpty()) {
            defaultItemId = plugin.getItemManager().getLoadedItems().keySet().iterator().next();
        }

        ConfigManager.setUpgradeValue(newId, "display_name", "&eNew Upgrade");
        ConfigManager.setUpgradeValue(newId, "success_rate", 100.0);
        ConfigManager.setUpgradeValue(newId, "failure_action", "LOSE_EVERYTHING");
        ConfigManager.setUpgradeValue(newId, "base", defaultItemId);
        ConfigManager.setUpgradeValue(newId, "result.item", defaultItemId);
        ConfigManager.setUpgradeValue(newId, "result.amount", 1);

        player.sendMessage(ConfigManager.fromSectionWithPrefix("§aUpgrade recipe '" + newId + "' created!"));

        if (defaultItemId.equals("select_item_id")) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§eWarning: No BuffedItems found! Recipe is invalid until you create an item."));
        }

        pmu.setItemToEditId(newId);
        closeChat(pmu);
        new UpgradeRecipeEditorMenu(pmu, plugin).open();
    }

    private void returnToEditor(PlayerMenuUtility pmu) {
        closeChat(pmu);
        new UpgradeRecipeEditorMenu(pmu, plugin).open();
    }

    private void closeChat(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }
}