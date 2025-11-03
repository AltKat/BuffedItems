package io.github.altkat.BuffedItems.Managers;

import com.google.common.base.Charsets;
import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConfigUpdater {

    /**
     * Compares the user's config.yml file with the default config.yml
     * from the latest plugin version and adds any missing keys.
     * This process does not touch the 'items' section.
     *
     * @param plugin Instance of the main BuffedItems class.
     */
    public static void update(BuffedItems plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration defaultConfig = null;

        // Load the default config.yml from within the plugin (JAR)
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource("config.yml"), Charsets.UTF_8)) {
            defaultConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            ConfigManager.logInfo("&cFailed to load default config.yml for update check: " + e.getMessage());
            return;
        }

        // Load the user's current config.yml file
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);

        // Copy the header section from the default config
        userConfig.options().copyHeader(true);
        userConfig.options().header(defaultConfig.options().header());

        int keysAdded = 0;

        // Check all keys in the default config
        for (String key : defaultConfig.getKeys(false)) {

            // !!! IMPORTANT !!!
            // If the key starts with 'items.', DO NOT TOUCH this section.
            // This section contains user-created items and must not be overwritten.
            if (key.startsWith("items.")) {
                continue;
            }

            // If this key does not exist in the user's config,
            // add it from the default config.
            if (!userConfig.contains(key)) {
                userConfig.set(key, defaultConfig.get(key));
                keysAdded++;
            }
        }

        // If at least one new key was added, save the config.
        if (keysAdded > 0) {
            try {
                userConfig.save(configFile);
                ConfigManager.logInfo("&aSuccessfully added " + keysAdded + " new key(s) to config.yml (e.g., new messages).");
            } catch (IOException e) {
                ConfigManager.logInfo("&cFailed to save updated config.yml: " + e.getMessage());
            }
        } else {
            // This debug message indicates that everything is okay and no update was needed.
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[ConfigUpdater] config.yml is already up-to-date.");
        }
    }
}