package io.github.altkat.BuffedItems.Managers;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.function.Supplier;

public class ConfigManager {

    private static BuffedItems plugin;
    public static final String NO_PERMISSION = "NONE";
    private static boolean debugMode = false;

    public static void setup(BuffedItems pluginInstance) {
        plugin = pluginInstance;
    }

    public static void reloadConfig() {
        long startTime = System.currentTimeMillis();

        sendDebugMessage(() -> "[Config] Reloading configuration...");

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.getLogger().warning("config.yml not found! Creating default config...");
            try {
                plugin.saveDefaultConfig();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create default config: " + e.getMessage());
                return;
            }
        }

        plugin.reloadConfig();
        plugin.getItemManager().loadItems(false);
        updateDebugMode();
        invalidateAllPlayerCaches();

        long elapsedTime = System.currentTimeMillis() - startTime;
        sendDebugMessage(() -> "[Config] Reload complete in " + elapsedTime + "ms");
    }

    public static void updateDebugMode() {
        debugMode = plugin.getConfig().getBoolean("debug-mode", false);

        if (debugMode) {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&9[&6BuffedItems&9] §e[Debug Mode] Enabled - Detailed logs will be shown."));
        }
    }

    /**
     * Sends a debug message (String)
     * @deprecated Use sendDebugMessage(Supplier<String>) for performance
     */
    @Deprecated
    public static void sendDebugMessage(String message) {
        if (debugMode) {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&9[&6BuffedItems&9] &e[Debug] &r" + message));
        }
    }

    /**
     * Sends a debug message (with Lazy evaluation)
     * This method only constructs the string if debug mode is enabled, providing a performance gain.
     *
     * Usage: ConfigManager.sendDebugMessage(() -> "[Task] Player: " + player.getName());
     */
    public static void sendDebugMessage(Supplier<String> messageSupplier) {
        if (debugMode) {
            String message = messageSupplier.get();
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&9[&6BuffedItems&9] &e[Debug] &r" + message));
        }
    }

    public static void setItemValue(String itemId, String path, Object value) {
        sendDebugMessage(() -> "[Config] Setting value: items." + itemId + "." + path + " = " + value);

        String fullPath = (path == null) ? "items." + itemId : "items." + itemId + "." + path;

        if ("permission".equals(path) && value == null) {
            plugin.getConfig().set(fullPath, NO_PERMISSION);
        } else {
            plugin.getConfig().set(fullPath, value);
        }

        plugin.saveConfig();
        plugin.getItemManager().loadItems(true);
        invalidateAllPlayerCaches();
    }

    public static boolean createNewItem(String itemId) {
        sendDebugMessage(() -> "[Config] Creating new item: " + itemId);

        FileConfiguration config = plugin.getConfig();
        if (config.contains("items." + itemId)) {
            sendDebugMessage(() -> "[Config] Item already exists: " + itemId);
            return false;
        }

        config.set("items." + itemId + ".display_name", "&f" + itemId);
        config.set("items." + itemId + ".material", "STONE");
        config.set("items." + itemId + ".lore", Collections.singletonList("&7A new BuffedItem."));

        plugin.saveConfig();
        plugin.getItemManager().loadItems(true);

        plugin.getLogger().info("Created new item: " + itemId);
        return true;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Clears the caches of all online players.
     * Should be called after config changes.
     */
    private static void invalidateAllPlayerCaches() {
        if (plugin.getEffectApplicatorTask() == null) {
            return;
        }

        int playerCount = org.bukkit.Bukkit.getOnlinePlayers().size();
        sendDebugMessage(() -> "[Config] Invalidating cache for all (" + playerCount + ") online players after config change...");

        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            plugin.getEffectApplicatorTask().invalidateCache(player.getUniqueId());
        }

        sendDebugMessage(() -> "[Config] Cache invalidation complete. Changes will apply on next tick.");
    }
}