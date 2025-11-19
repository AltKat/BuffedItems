package io.github.altkat.BuffedItems.manager.config;

import com.google.common.base.Charsets;
import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigUpdater {

    public static void update(BuffedItems plugin, String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        FileConfiguration defaultConfig = null;

        try {
            InputStream resource = plugin.getResource(fileName);
            if (resource == null) {
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(resource, Charsets.UTF_8)) {
                defaultConfig = YamlConfiguration.loadConfiguration(reader);
            }
        } catch (Exception e) {
            ConfigManager.logInfo("&cFailed to load default " + fileName + " for update check: " + e.getMessage());
            return;
        }

        if (!configFile.exists()) {
            return;
        }
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);

        boolean needsSave = false;

        if (fileName.equals("config.yml")) {
            String[] obsoleteKeys = {
                    "auto-save-interval-minutes",
            };

            for (String key : obsoleteKeys) {
                if (userConfig.contains(key)) {
                    userConfig.set(key, null);
                    needsSave = true;
                    ConfigManager.logInfo("&eRemoved obsolete key '" + key + "' from " + fileName);
                }
            }
        }

        int keysAdded = 0;

        for (String key : defaultConfig.getKeys(false)) {
            if (!userConfig.contains(key)) {
                userConfig.set(key, defaultConfig.get(key));
                keysAdded++;
            }
        }

        if (keysAdded > 0 || needsSave) {
            try {
                userConfig.options().copyHeader(true);
                userConfig.options().header(defaultConfig.options().header());

                userConfig.save(configFile);
                ConfigManager.logInfo("&aSuccessfully added " + keysAdded + " new key(s) to " + fileName + ".");
            } catch (IOException e) {
                ConfigManager.logInfo("&cFailed to save updated " + fileName + ": " + e.getMessage());
            }
        }
    }
}