package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ChatInputRouter {

    private final List<ChatInputHandler> handlers = new ArrayList<>();

    public void registerHandler(ChatInputHandler handler) {
        handlers.add(handler);
    }

    public void handleInput(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        for (ChatInputHandler handler : handlers) {
            if (handler.shouldHandle(path)) {
                handler.handle(player, pmu, input, path, itemId);
                return;
            }
        }
        
        // Fallback if no handler found
        player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Unknown input path: " + path));
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[ChatRouter] No handler found for path: " + path);
        closeChatInput(pmu);
    }

    public void handleCancel(Player player, PlayerMenuUtility pmu, String path) {
        for (ChatInputHandler handler : handlers) {
            if (handler.shouldHandle(path)) {
                handler.onCancel(player, pmu, path);
                return;
            }
        }
        
        // Fallback cancellation (should rarely happen if paths are managed correctly)
        closeChatInput(pmu);
    }
    
    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }
}
