package io.github.altkat.BuffedItems.manager.config;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SetsConfig {

    private static File file;
    private static FileConfiguration config;

    public static void setup(BuffedItems plugin) {
        file = new File(plugin.getDataFolder(), "sets.yml");

        if (!file.exists()) {
            try {
                if (plugin.getResource("sets.yml") != null) {
                    plugin.saveResource("sets.yml", false);
                } else {
                    file.createNewFile();
                }
                ConfigManager.logInfo("&aCreated new sets.yml file.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create sets.yml!");
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration get() {
        return config;
    }

    public static void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save sets.yml file!");
        }
    }

    public static void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }
}