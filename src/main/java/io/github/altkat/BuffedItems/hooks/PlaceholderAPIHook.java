package io.github.altkat.BuffedItems.hooks;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

public class PlaceholderAPIHook {

    public PlaceholderAPIHook() {
        ConfigManager.logInfo("&aPlaceholderAPI hooked successfully!");
    }

    public String setPlaceholders(OfflinePlayer player, String text) {
        if (text == null) return null;
        return PlaceholderAPI.setPlaceholders(player, text);
    }
}