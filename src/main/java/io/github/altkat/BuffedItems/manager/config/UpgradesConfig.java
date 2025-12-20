package io.github.altkat.BuffedItems.manager.config;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class UpgradesConfig extends BaseConfig{

    private static UpgradesConfig instance;

    public UpgradesConfig(BuffedItems plugin) {
        super(plugin, "upgrades.yml");
        instance = this;
    }

    public static void setup(BuffedItems plugin) {
        new UpgradesConfig(plugin);
    }

    public static FileConfiguration get() {
        return instance.getConfigData();
    }

    public static void saveAsync() {
        if (instance != null) instance.saveFileAsync();
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
}