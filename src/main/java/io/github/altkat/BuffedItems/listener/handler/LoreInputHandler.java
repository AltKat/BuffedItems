package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.editor.LoreEditorMenu;
import io.github.altkat.BuffedItems.menu.utility.MainMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LoreInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public LoreInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Item '" + itemId + "' not found in memory."));
            closeChatInput(pmu);
            new MainMenu(pmu, plugin).open();
            return;
        }

        List<String> currentLore = new ArrayList<>(item.getLore());

        if (path.equals("lore.add")) {
            currentLore.add(input);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                    () -> "[Chat] Added new lore line to " + itemId);
        } else {
            try {
                int index = Integer.parseInt(path.substring(5));
                if (index >= 0 && index < currentLore.size()) {
                    currentLore.set(index, input);
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                            () -> "[Chat] Updated lore line " + index + " for " + itemId);
                } else {
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Invalid lore index."));
                    closeChatInput(pmu);
                    new LoreEditorMenu(pmu, plugin).open();
                    return;
                }
            } catch (NumberFormatException ex) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Could not parse lore index from path: " + path));
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                        () -> "[Chat] Could not parse lore index from path: " + path);
                closeChatInput(pmu);
                new LoreEditorMenu(pmu, plugin).open();
                return;
            }
        }

        ConfigManager.setItemValue(itemId, "lore", currentLore);
        player.sendMessage(ConfigManager.fromSectionWithPrefix("§aLore has been updated!"));

        closeChatInput(pmu);
        new LoreEditorMenu(pmu, plugin).open();
    }

    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }
}