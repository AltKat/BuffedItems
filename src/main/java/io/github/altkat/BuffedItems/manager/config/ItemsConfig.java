package io.github.altkat.BuffedItems.manager.config;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;

public class ItemsConfig extends BaseConfig {

    private static ItemsConfig instance;

    public ItemsConfig(BuffedItems plugin) {
        super(plugin, "items.yml");
        instance = this;
        migrateFromOldConfig();
        ConfigUpdater.migrateItems(config, new File(plugin.getDataFolder(), "items.yml"));
        reload();
    }

    public static void setup(BuffedItems plugin) {
        new ItemsConfig(plugin);
    }

    public static void saveAsync() {
        if (instance != null) instance.saveFileAsync();
    }

    public static FileConfiguration get() {
        return instance.getConfigData();
    }

    public static void save() {
        if (instance != null) instance.saveFile();
    }

    public static void reload() {
        if (instance != null) instance.reloadFile();
    }

    public static boolean isDirty() {
        return instance != null && instance.hasUnsavedChanges();
    }

    private void migrateFromOldConfig() {
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
}