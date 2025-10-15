package io.github.altkat.BuffedItems.Handlers;
import com.google.common.base.Charsets;
import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConfigUpdater {

    public static void update(BuffedItems plugin) throws IOException {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(plugin.getResource("config.yml"), Charsets.UTF_8));
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);

        boolean needsSave = false;

        for (String key : defaultConfig.getKeys(true)) {
            if (!userConfig.contains(key)) {
                userConfig.set(key, defaultConfig.get(key));
                needsSave = true;
            }
        }

        if (needsSave) {
            plugin.getLogger().info("Updating config.yml with new settings...");
            userConfig.options().copyHeader(true);
            userConfig.options().header(defaultConfig.options().header());
            userConfig.save(configFile);
        }
    }
}
