package io.github.altkat.BuffedItems.manager.config;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ItemsConfig {

    private static File file;
    private static FileConfiguration config;

    public static void setup(BuffedItems plugin) {
        file = new File(plugin.getDataFolder(), "items.yml");

        if (!file.exists()) {
            try {
                if (plugin.getResource("items.yml") != null) {
                    plugin.saveResource("items.yml", false);
                } else {
                    file.createNewFile();
                }
                ConfigManager.logInfo("&aCreated new items.yml file.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create items.yml!");
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        migrateFromOldConfig(plugin);
    }

    private static void migrateFromOldConfig(BuffedItems plugin) {
        FileConfiguration mainConfig = plugin.getConfig();

        if (mainConfig.contains("items")) {
            ConfigManager.logInfo("&eDetected old 'items' section in config.yml. Starting migration...");

            ConfigManager.backupConfig();

            ConfigurationSection itemsSection = mainConfig.getConfigurationSection("items");
            config.set("items", itemsSection);
            save();

            java.util.Map<String, Object> oldValues = new java.util.HashMap<>();
            for (String key : mainConfig.getKeys(false)) {
                if (!key.equals("items")) {
                    oldValues.put(key, mainConfig.get(key));
                }
            }

            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (configFile.delete()) {
                plugin.saveResource("config.yml", false);

                plugin.reloadConfig();
                FileConfiguration freshConfig = plugin.getConfig();

                for (java.util.Map.Entry<String, Object> entry : oldValues.entrySet()) {
                    if (!java.util.Objects.equals(freshConfig.get(entry.getKey()), entry.getValue())) {
                        freshConfig.set(entry.getKey(), entry.getValue());
                    }
                }

                plugin.saveConfig();
            } else {
                plugin.getLogger().warning("Could not delete old config.yml for clean migration. Standard migration applied.");
                mainConfig.set("items", null);
                plugin.saveConfig();
            }

            ConfigManager.logInfo("&aMigration complete! Your config.yml is now clean and items are in items.yml.");
        }
    }

    public static FileConfiguration get() {
        return config;
    }

    public static void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            System.out.println("Could not save items.yml file!");
        }
    }

    public static void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }
}