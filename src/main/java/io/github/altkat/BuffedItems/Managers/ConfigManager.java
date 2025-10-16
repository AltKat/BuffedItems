package io.github.altkat.BuffedItems.Managers;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
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

        saveConfiguration();
    }

    public static boolean createNewItem(String itemId) {
        FileConfiguration config = plugin.getConfig();
        if (config.contains("items." + itemId)) {
            return false;
        }
        config.set("items." + itemId + ".display_name", "&f" + itemId);
        config.set("items." + itemId + ".material", "STONE");
        config.set("items." + itemId + ".lore", Collections.singletonList("&7A new BuffedItem."));
        saveConfiguration();
        return true;
    }

    private static void saveConfiguration() {
        try {
            plugin.saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("Could not save config.yml!");
            e.printStackTrace();
        }
        plugin.reloadConfig();
        plugin.getItemManager().loadItems();
    }

    public static void reloadConfig() {
        plugin.reloadConfig();
        plugin.getItemManager().loadItems();
    }
}