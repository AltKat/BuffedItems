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

    public static void setItemValue(String itemId, String path, Object value) {
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
        FileConfiguration config = plugin.getConfig();
        if (config.contains("items." + itemId)) {
            return false;
        }
        config.set("items." + itemId + ".display_name", "&f" + itemId);
        config.set("items." + itemId + ".material", "STONE");
        config.set("items." + itemId + ".lore", Collections.singletonList("&7A new BuffedItem."));
        plugin.saveConfig();
        plugin.getItemManager().loadItems(true);
        return true;
    }

    public static void reloadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.getLogger().warning("config.yml not found! Attempting to recover from memory...");
            plugin.saveConfig();
        }
        plugin.reloadConfig();
        plugin.getItemManager().loadItems(false);
    }
}