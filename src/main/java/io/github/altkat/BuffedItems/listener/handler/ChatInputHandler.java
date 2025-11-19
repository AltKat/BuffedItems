package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.entity.Player;

public interface ChatInputHandler {
    /**
     * Processes incoming chat input.
     * @param player The player who made the input
     * @param pmu The player's menu helper object
     * @param input The text the player typed
     * @param path The path being processed (e.g., display_name)
     * @param itemId The ID of the item being edited
     */
    void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId);
}