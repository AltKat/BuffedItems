package io.github.altkat.BuffedItems.manager.config;

import com.google.common.base.Charsets;
import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
                    "messages.protection-prevent-smithing-use",
                    "messages.protection-prevent-crafting-use"
            };

            for (String key : obsoleteKeys) {
                if (userConfig.contains(key)) {
                    userConfig.set(key, null);
                    needsSave = true;
                    ConfigManager.logInfo("&eRemoved obsolete key '" + key + "' from " + fileName);
                }
            }
        }

        String defaultHeader = defaultConfig.options().header();
        String userHeader = userConfig.options().header();

        if (!defaultHeader.equals(userHeader)) {
            needsSave = true;
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Config] Header update detected for: " + fileName);
        }

        int keysAdded = 0;

        for (String key : defaultConfig.getKeys(true)) {

            if (fileName.equals("items.yml") && key.startsWith("items.")) continue;
            if (fileName.equals("upgrades.yml") && key.startsWith("upgrades.")) continue;
            if(fileName.equals("sets.yml") && key.startsWith("sets.")) continue;
            if(fileName.equals("recipes.yml") && key.startsWith("recipes.")) continue;

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

    public static void migrateItems(FileConfiguration config, File configFile) {
        if (config.getInt("version", 0) >= 2) {
            return;
        }

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            config.set("version", 2);
            try {
                config.save(configFile);
            } catch (IOException e) {
                ConfigManager.logInfo("&cFailed to save migrated items.yml: " + e.getMessage());
            }
            return;
        }

        ConfigManager.logInfo("&eStarting migration of items.yml to new format with ordered keys...");

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null) {
                continue;
            }

            // Use LinkedHashMap to preserve insertion order for a clean output file.

            // --- Build Display Section ---
            Map<String, Object> display = new LinkedHashMap<>();
            move(itemSection, "display_name", display, "name");
            move(itemSection, "lore", display);
            move(itemSection, "custom-model-data", display);
            move(itemSection, "glow", display);


            // --- Build Passive Effects Section ---
            Map<String, Object> passiveEffects = new LinkedHashMap<>();
            move(itemSection, "passive_permission", passiveEffects, "permission");
            move(itemSection, "attribute_mode", passiveEffects);
            move(itemSection, "effects", passiveEffects, "slots");

            // --- Build Active Ability Section ---
            Map<String, Object> activeAbility = new LinkedHashMap<>();
            move(itemSection, "active-mode.enabled", activeAbility, "enabled");
            move(itemSection, "active_permission", activeAbility, "permission");
            move(itemSection, "active-mode.cooldown", activeAbility, "cooldown");
            move(itemSection, "active-mode.duration", activeAbility, "duration");
            move(itemSection, "active-mode.costs", activeAbility, "costs");

            // --- Active Ability Actions (Commands & Effects) ---
            Map<String, Object> actions = new LinkedHashMap<>();
            move(itemSection, "active-mode.commands", actions, "commands");
            move(itemSection, "active-mode.effects", actions, "effects");
            if (!actions.isEmpty()) {
                activeAbility.put("actions", actions);
            }

            // --- Active Ability Usage Details (Flattened) ---
            Map<String, Object> usage = new LinkedHashMap<>();
            move(itemSection, "active-mode.usage-limit.max-usage", usage, "limit");
            move(itemSection, "active-mode.usage-limit.action", usage, "action");
            move(itemSection, "active-mode.usage-limit.transform-item", usage, "transform_item");
            move(itemSection, "active-mode.usage-limit.lore", usage, "lore_format");
            move(itemSection, "active-mode.usage-limit.depleted-lore", usage, "depleted_lore");
            move(itemSection, "active-mode.usage-limit.depleted-message", usage, "depleted_message");
            move(itemSection, "active-mode.usage-limit.depletion-notification", usage, "depletion_notification");
            move(itemSection, "active-mode.usage-limit.depletion-transform-message", usage, "transform_message");
            move(itemSection, "active-mode.usage-limit.commands", usage, "commands");
            move(itemSection, "active-mode.sounds.depletion", usage, "depletion_sound");
            move(itemSection, "active-mode.sounds.depleted-try", usage, "depleted_try_sound");
            if (!usage.isEmpty()) {
                activeAbility.put("usage", usage);
            }

            // --- Active Ability Visuals ---
            Map<String, Object> visuals = new LinkedHashMap<>();
            Map<String, Object> cooldownVisuals = new LinkedHashMap<>();

            Map<String, Object> chat = new LinkedHashMap<>();
            move(itemSection, "active-mode.visuals.chat", chat, "enabled");
            move(itemSection, "active-mode.visuals.messages.cooldown-chat", chat, "message");
            if (!chat.isEmpty()) cooldownVisuals.put("chat", chat);

            Map<String, Object> title = new LinkedHashMap<>();
            move(itemSection, "active-mode.visuals.title", title, "enabled");
            move(itemSection, "active-mode.visuals.messages.cooldown-title", title, "message");
            move(itemSection, "active-mode.visuals.messages.subtitle", title, "subtitle");
            if (!title.isEmpty()) cooldownVisuals.put("title", title);

            Map<String, Object> actionBar = new LinkedHashMap<>();
            move(itemSection, "active-mode.visuals.action-bar", actionBar, "enabled");
            move(itemSection, "active-mode.visuals.messages.cooldown-action-bar", actionBar, "message");
            if (!actionBar.isEmpty()) cooldownVisuals.put("action-bar", actionBar);

            Map<String, Object> bossBar = new LinkedHashMap<>();
            move(itemSection, "active-mode.visuals.boss-bar", bossBar, "enabled");
            move(itemSection, "active-mode.visuals.boss-bar-style", bossBar, "style");
            move(itemSection, "active-mode.visuals.boss-bar-color", bossBar, "color");
            move(itemSection, "active-mode.visuals.messages.cooldown-boss-bar", bossBar, "message");
            if (!bossBar.isEmpty()) cooldownVisuals.put("boss-bar", bossBar);

            if (!cooldownVisuals.isEmpty()) {
                visuals.put("cooldown", cooldownVisuals);
                activeAbility.put("visuals", visuals);
            }

            // --- Active Ability Sounds ---
            Map<String, Object> sounds = new LinkedHashMap<>();
            move(itemSection, "active-mode.sounds.success", sounds, "success");
            move(itemSection, "active-mode.sounds.cost-fail", sounds, "cost-fail");
            move(itemSection, "active-mode.sounds.cooldown", sounds, "cooldown");
            if (!sounds.isEmpty()) {
                activeAbility.put("sounds", sounds);
            }

            // --- Set new structures in order ---
            if (!display.isEmpty()) itemSection.set("display", display);
            if (!passiveEffects.isEmpty()) itemSection.set("passive_effects", passiveEffects);
            if (!activeAbility.isEmpty()) itemSection.set("active_ability", activeAbility);

            // Remove all old top-level keys
            itemSection.set("display_name", null);
            itemSection.set("lore", null);
            itemSection.set("glow", null);
            itemSection.set("custom-model-data", null);
            itemSection.set("effects", null);
            itemSection.set("attribute_mode", null);
            itemSection.set("active-mode", null);
            itemSection.set("active_permission", null);
            itemSection.set("passive_permission", null);
            itemSection.set("usage", null);
        }

        config.set("version", 2);

        try {
            config.save(configFile);
            ConfigManager.logInfo("&aSuccessfully migrated items.yml to the new, ordered format!");
        } catch (IOException e) {
            ConfigManager.logInfo("&cFailed to save migrated items.yml: " + e.getMessage());
        }
    }

    private static void move(ConfigurationSection from, String oldKey, Map<String, Object> to, String newKey) {
        if (from.contains(oldKey)) {
            to.put(newKey, from.get(oldKey));
        }
    }

    private static void move(ConfigurationSection from, String oldKey, Map<String, Object> to) {
        move(from, oldKey, to, oldKey);
    }
}