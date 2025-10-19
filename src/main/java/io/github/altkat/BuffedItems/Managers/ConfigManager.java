package io.github.altkat.BuffedItems.Managers;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import java.util.Collections;

public class ConfigManager {

    private static BuffedItems plugin;
    public static final String NO_PERMISSION = "NONE";

    public static void setup(BuffedItems pluginInstance) {
        plugin = pluginInstance;
    }

    public static void reloadConfig() {
        long startTime = System.currentTimeMillis();

        plugin.getLogger().fine("[Config] Reloading configuration...");

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

        long elapsedTime = System.currentTimeMillis() - startTime;
        plugin.getLogger().fine("[Config] Reload complete in " + elapsedTime + "ms");
    }

    public static void updateDebugMode() {
        boolean debugMode = plugin.getConfig().getBoolean("debug-mode", false);

        if (debugMode) {
            plugin.getLogger().setLevel(java.util.logging.Level.FINE);
            plugin.getLogger().info("§e[Debug Mode] Enabled - Detailed logs will be shown");
        } else {
            plugin.getLogger().setLevel(java.util.logging.Level.INFO);
            plugin.getLogger().info("§e[Debug Mode] Disabled - Only important logs will be shown");
        }
    }

    public static void setItemValue(String itemId, String path, Object value) {
        plugin.getLogger().fine("[Config] Setting value: items." + itemId + "." + path + " = " + value);

        String fullPath = (path == null) ? "items." + itemId : "items." + itemId + "." + path;

        if ("permission".equals(path) && value == null) {
            plugin.getConfig().set(fullPath, NO_PERMISSION);
        } else {
            plugin.getConfig().set(fullPath, value);
        }

        plugin.saveConfig();
        plugin.getItemManager().loadItems(true);
    }

    public static boolean createNewItem(String itemId) {
        plugin.getLogger().fine("[Config] Creating new item: " + itemId);

        FileConfiguration config = plugin.getConfig();
        if (config.contains("items." + itemId)) {
            plugin.getLogger().fine("[Config] Item already exists: " + itemId);
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
}