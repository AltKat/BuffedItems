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
    private static int debugLevel = 0;
    private static boolean showPotionIcons = true;

    public static final int DEBUG_OFF = 0;
    public static final int DEBUG_INFO = 1;      // Basic plugin status
    public static final int DEBUG_TASK = 2;      // Core task loops, major events
    public static final int DEBUG_DETAILED = 3;  // Per-player effect details
    public static final int DEBUG_VERBOSE = 4;   // GUI, Chat, Inventory events (spammy)

    public static void setup(BuffedItems pluginInstance) {
        plugin = pluginInstance;
    }

    public static void reloadConfig() {
        long startTime = System.currentTimeMillis();

        sendDebugMessage(DEBUG_INFO, () -> "[Config] Reloading configuration...");

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
        loadGlobalSettings();
        plugin.reloadConfigSettings();
        invalidateAllPlayerCaches();

        long elapsedTime = System.currentTimeMillis() - startTime;
        sendDebugMessage(DEBUG_INFO, () -> "[Config] Reload complete in " + elapsedTime + "ms");
    }

    public static void loadGlobalSettings() {
        debugLevel = plugin.getConfig().getInt("debug-level", 0);
        if (debugLevel < 0) {
            debugLevel = 0;
        }

        showPotionIcons = plugin.getConfig().getBoolean("show-potion-icons", true);

        if (isDebugLevelEnabled(DEBUG_INFO)) {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&9[&6BuffedItems&9] &e[Debug Level " + debugLevel + "] Enabled - Detailed logs will be shown according to level."));
        }
    }

    /**
     * Sends a debug message (with Lazy evaluation) if the specified level is enabled.
     * Prepends the message with "[L<level>]".
     *
     * @param level The debug level required to show this message.
     * @param messageSupplier The message supplier (only called if level is met).
     */
    public static void sendDebugMessage(int level, Supplier<String> messageSupplier) {
        if (debugLevel >= level) {
            String message = messageSupplier.get();
            String prefix = "&9[&6BuffedItems&9] &e[L" + level + "] &r";
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    prefix + message));
        }
    }

    public static void setItemValue(String itemId, String path, Object value) {
        sendDebugMessage(DEBUG_INFO, () -> "[Config] Setting value: items." + itemId + "." + path + " = " + value);

        String fullPath = (path == null) ? "items." + itemId : "items." + itemId + "." + path;

        if ("permission".equals(path) && value == null) {
            plugin.getConfig().set(fullPath, NO_PERMISSION);
        } else {
            plugin.getConfig().set(fullPath, value);
        }
        plugin.getItemManager().reloadSingleItem(itemId);
        plugin.getEffectApplicatorTask().invalidateCacheForHolding(itemId);
    }

    public static boolean createNewItem(String itemId) {
        sendDebugMessage(DEBUG_INFO, () -> "[Config] Creating new item: " + itemId);

        FileConfiguration config = plugin.getConfig();
        if (config.contains("items." + itemId)) {
            sendDebugMessage(DEBUG_INFO, () -> "[Config] Item already exists: " + itemId);
            return false;
        }

        config.set("items." + itemId + ".display_name", "&f" + itemId);
        config.set("items." + itemId + ".material", "STONE");
        config.set("items." + itemId + ".lore", Collections.singletonList("&7A new BuffedItem."));

        plugin.getItemManager().reloadSingleItem(itemId);

        plugin.getLogger().info("Created new item: " + itemId);
        return true;
    }

    private static void invalidateAllPlayerCaches() {
        if (plugin.getEffectApplicatorTask() == null) {
            return;
        }

        int playerCount = org.bukkit.Bukkit.getOnlinePlayers().size();
        sendDebugMessage(DEBUG_TASK, () -> "[Config] Invalidating cache for all (" + playerCount + ") online players after config change...");

        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            plugin.getEffectApplicatorTask().markPlayerForUpdate(player.getUniqueId());
        }

        sendDebugMessage(DEBUG_TASK, () -> "[Config] Cache invalidation complete. Changes will apply on next tick.");
    }

    /**
     * Checks if the specified debug level (or higher) is currently enabled.
     * @param level The level to check (e.g., ConfigManager.DEBUG_DETAILED).
     * @return true if logging is enabled for this level.
     */
    public static boolean isDebugLevelEnabled(int level) {
        return debugLevel >= level;
    }

    public static boolean shouldShowPotionIcons() {
        return showPotionIcons;
    }
}