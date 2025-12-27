package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.entity.Player;

public interface ChatInputHandler {
    /**
     * Checks if this handler can process the given path.
     * @param path The path to check
     * @return true if this handler should process the input
     */
    boolean shouldHandle(String path);

    /**
     * Processes incoming chat input.
     * @param player The player who made the input
     * @param pmu The player's menu helper object
     * @param input The text the player typed
     * @param path The path being processed (e.g., display_name)
     * @param itemId The ID of the item being edited
     */
    void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId);

    /**
     * Handles the cancellation of the chat input operation (e.g. typing 'cancel').
     * @param player The player
     * @param pmu The player's menu helper object
     * @param path The path that was being edited
     */
    void onCancel(Player player, PlayerMenuUtility pmu, String path);
}