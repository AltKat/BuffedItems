package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.editor.ItemEditorMenu;
import io.github.altkat.BuffedItems.menu.utility.MainMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.entity.Player;

public class CreationInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public CreationInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path.equals("createnewitem")) {
            handleCreateNewItem(player, pmu, input);
        } else if (path.equals("duplicateitem")) {
            handleDuplicateItem(player, pmu, input);
        }
    }

    private void handleCreateNewItem(Player player, PlayerMenuUtility pmu, String input) {
        String newItemId = input.toLowerCase().replaceAll("\\s+", "_");
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[CreationHandler] Creating new item: " + newItemId);

        if (ConfigManager.createNewItem(newItemId)) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aNew item '" + newItemId + "' created. Now editing..."));
            pmu.setItemToEditId(newItemId);
            closeChatInput(pmu);
            new ItemEditorMenu(pmu, plugin).open();
        } else {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: An item with the ID '" + newItemId + "' already exists."));
            closeChatInput(pmu);
            new MainMenu(pmu, plugin).open();
        }
    }

    private void handleDuplicateItem(Player player, PlayerMenuUtility pmu, String input) {
        String sourceItemId = pmu.getItemToEditId();
        String newItemId = input.toLowerCase().replaceAll("\\s+", "_");

        if (newItemId.equals(sourceItemId)) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: New ID cannot be the same as the source ID."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("duplicateitem");
            return;
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[CreationHandler] Attempting to duplicate '" + sourceItemId + "' as '" + newItemId + "'");

        String createdId = ConfigManager.duplicateItem(sourceItemId, newItemId);

        if (createdId != null) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aItem '§e" + sourceItemId + "§a' successfully duplicated as '§e" + createdId + "§a'."));
            closeChatInput(pmu);
            new MainMenu(pmu, plugin).open();
        } else {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: An item with the ID '§e" + newItemId + "§c' already exists."));
            player.sendMessage(ConfigManager.fromSection("§aPlease try a different ID."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("duplicateitem");
        }
    }

    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }
}