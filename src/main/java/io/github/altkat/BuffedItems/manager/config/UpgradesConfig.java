package io.github.altkat.BuffedItems.manager.config;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class UpgradesConfig {

    private static File file;
    private static FileConfiguration config;

    public static void setup(BuffedItems plugin) {
        file = new File(plugin.getDataFolder(), "upgrades.yml");

        if (!file.exists()) {
            try {
                if (plugin.getResource("upgrades.yml") != null) {
                    plugin.saveResource("upgrades.yml", false);
                } else {
                    if (!file.createNewFile()) {
                        plugin.getLogger().warning("Failed to create upgrades.yml file!");
                        return;
                    }
                }
                ConfigManager.logInfo("&aCreated new upgrades.yml file.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create upgrades.yml!");
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
            System.out.println("Could not save upgrades.yml file!");
        }
    }

    public static void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }
}