package io.github.altkat.BuffedItems.manager.config;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class RecipesConfig {

    private static File file;
    private static FileConfiguration config;

    public static void setup(BuffedItems plugin) {
        file = new File(plugin.getDataFolder(), "recipes.yml");

        if (!file.exists()) {
            try {
                if (plugin.getResource("recipes.yml") != null) {
                    plugin.saveResource("recipes.yml", false);
                } else {
                    file.createNewFile();
                }
                ConfigManager.logInfo("&aCreated new recipes.yml file.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create recipes.yml!");
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
            System.out.println("Could not save recipes.yml file!");
        }
    }

    public static void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }
}