package io.github.altkat.BuffedItems.manager.config;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public abstract class BaseConfig {

    protected final BuffedItems plugin;
    protected final File file;
    protected FileConfiguration config;
    protected boolean isDirty = false;
    protected BukkitTask pendingSaveTask = null;

    public BaseConfig(BuffedItems plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        createFileIfNotExists(fileName);
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    protected void createFileIfNotExists(String resourceName) {
        if (!file.exists()) {
            try {
                if (plugin.getResource(resourceName) != null) {
                    plugin.saveResource(resourceName, false);
                } else {
                    if (!file.createNewFile()) {
                        plugin.getLogger().warning("Failed to create " + resourceName + " file!");
                    }
                }
                ConfigManager.logInfo("&aCreated new " + resourceName + " file.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create " + resourceName + "!");
                e.printStackTrace();
            }
        }
    }

    public void saveFileAsync() {
        isDirty = true;

        if (pendingSaveTask != null && !pendingSaveTask.isCancelled()) {
            pendingSaveTask.cancel();
        }

        pendingSaveTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            final String data = config.saveToString();
            isDirty = false;

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Files.writeString(file.toPath(), data);
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not save " + file.getName() + " asynchronously!");
                    e.printStackTrace();
                    isDirty = true;
                }
            });

        }, 20L);
    }

    public void saveFile() {
        try {
            config.save(file);
            isDirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save " + file.getName() + " file!");
            e.printStackTrace();
        }
    }

    public void reloadFile() {
        if (pendingSaveTask != null && !pendingSaveTask.isCancelled()) {
            pendingSaveTask.cancel();
        }
        config = YamlConfiguration.loadConfiguration(file);
        isDirty = false;
    }

    public FileConfiguration getConfigData() {
        return config;
    }

    public boolean hasUnsavedChanges() {
        return isDirty;
    }
}