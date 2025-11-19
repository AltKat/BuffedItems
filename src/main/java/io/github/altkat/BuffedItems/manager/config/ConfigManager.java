package io.github.altkat.BuffedItems.manager.config;

import io.github.altkat.BuffedItems.BuffedItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConfigManager {

    private static BuffedItems plugin;
    private static final Object CONFIG_LOCK = new Object();
    public static final String NO_PERMISSION = "NONE";
    private static int debugLevel = 0;
    private static boolean showPotionIcons = true;

    private static boolean visualChat;
    private static boolean visualTitle;
    private static boolean visualActionBar;
    private static boolean visualBossBar;
    private static String bossBarColor;
    private static String bossBarStyle;
    private static String globalSuccessSound;
    private static String globalCooldownSound;
    private static String globalCostFailSound;

    public static final int DEBUG_OFF = 0;
    public static final int DEBUG_INFO = 1;      // Basic plugin status
    public static final int DEBUG_TASK = 2;      // Core task loops, major events
    public static final int DEBUG_DETAILED = 3;  // Per-player effect details
    public static final int DEBUG_VERBOSE = 4;   // GUI, Chat, Inventory events (spammy)

    private static final String PLUGIN_PREFIX_CONFIG = "&#FFD700[&#FF6347BuffedItems&#FFD700] ";

    private static final PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();

    private static final LegacyComponentSerializer ampersandSerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private static final LegacyComponentSerializer sectionSerializer = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .build();

    public static Component fromLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        Component component = ampersandSerializer.deserialize(text);

        if (component.style().decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
            return component.style(style -> style.decoration(TextDecoration.ITALIC, false));
        }
        return component;
    }

    public static List<Component> loreFromLegacy(List<String> textLines) {
        if (textLines == null || textLines.isEmpty()) {
            return Collections.emptyList();
        }
        return textLines.stream()
                .map(ConfigManager::fromLegacy)
                .collect(Collectors.toList());
    }

    public static Component fromSection(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        Component component = sectionSerializer.deserialize(text);

        if (component.style().decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
            return component.style(style -> style.decoration(TextDecoration.ITALIC, false));
        }
        return component;
    }

    public static String toSection(Component component) {
        if (component == null) {
            return "";
        }
        return sectionSerializer.serialize(component);
    }

    public static String toPlainText(Component component) {
        if (component == null) {
            return "";
        }
        return plainTextSerializer.serialize(component);
    }

    public static String stripLegacy(String legacyText) {
        if (legacyText == null || legacyText.isEmpty()) {
            return "";
        }
        return plainTextSerializer.serialize(fromSection(legacyText));
    }

    public static void setup(BuffedItems pluginInstance) {
        plugin = pluginInstance;
    }

    public static void backupConfig() {
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        File configBackup = new File(backupDir, "config.yml.backup");

        if (configFile.exists()) {
            try {
                Files.copy(configFile.toPath(), configBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                sendDebugMessage(DEBUG_INFO, () -> "[Backup] config.yml backed up to backups/ folder.");
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create config.yml backup: " + e.getMessage());
            }
        }

        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        File itemsBackup = new File(backupDir, "items.yml.backup");

        if (itemsFile.exists()) {
            try {
                Files.copy(itemsFile.toPath(), itemsBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
                sendDebugMessage(DEBUG_INFO, () -> "[Backup] items.yml backed up to backups/ folder.");
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create items.yml backup: " + e.getMessage());
            }
        }
    }

    public static void reloadConfig() {
        reloadConfig(false);
    }

    public static void reloadConfig(boolean silent) {
        synchronized (CONFIG_LOCK) {
            long startTime = System.currentTimeMillis();

            if (!silent) {
                sendDebugMessage(DEBUG_INFO, () -> "[Config] Reloading configuration...");
            }

            plugin.reloadConfig();
            ItemsConfig.reload();

            plugin.getItemManager().loadItems(silent);

            loadGlobalSettings();
            invalidateAllPlayerCaches();

            if (!silent) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                sendDebugMessage(DEBUG_INFO, () -> "[Config] Reload complete in " + elapsedTime + "ms");
            }
        }
    }

    public static void loadGlobalSettings() {
        debugLevel = plugin.getConfig().getInt("debug-level", 0);
        if (debugLevel < 0) {
            debugLevel = 0;
        }

        showPotionIcons = plugin.getConfig().getBoolean("show-potion-icons", true);

        if (isDebugLevelEnabled(DEBUG_INFO)) {
            plugin.getServer().getConsoleSender().sendMessage(
                    fromLegacy("&#FFD700[&#FF6347BuffedItems&#FFD700] &e[Debug Level " + debugLevel + "] Enabled - Detailed logs will be shown according to level.")
            );
        }

        visualChat = plugin.getConfig().getBoolean("active-items.visuals.chat", true);
        visualTitle = plugin.getConfig().getBoolean("active-items.visuals.title", true);
        visualActionBar = plugin.getConfig().getBoolean("active-items.visuals.action-bar", true);
        visualBossBar = plugin.getConfig().getBoolean("active-items.visuals.boss-bar", true);

        bossBarColor = plugin.getConfig().getString("active-items.boss-bar-settings.color", "RED");
        bossBarStyle = plugin.getConfig().getString("active-items.boss-bar-settings.style", "SOLID");
        globalSuccessSound = plugin.getConfig().getString("active-items.sounds.success", "ENTITY_EXPERIENCE_ORB_PICKUP;1.0;1.0");
        globalCooldownSound = plugin.getConfig().getString("active-items.sounds.cooldown", "ENTITY_VILLAGER_NO;1.0;1.0");
        globalCostFailSound = plugin.getConfig().getString("active-items.sounds.cost-fail", "BLOCK_NOTE_BLOCK_HARP;1.0;0.5");
    }

    /**
     * Sends a debug message (with Lazy evaluation) if the specified level is enabled.
     * Prepends the message with "[L<level>]".
     *
     * @param level The debug level required to show this message.
     * @param messageSupplier The message supplier (only called if level is met).
     */
    public static void sendDebugMessage(int level, Supplier<String> messageSupplier) {
        if (debugLevel >= level) {
            String message = messageSupplier.get();
            String prefix = "&#FFD700[&#FF6347BuffedItems&#FFD700] &e[L" + level + "] &r";
            plugin.getServer().getConsoleSender().sendMessage(fromLegacy(prefix + message));
        }
    }

    public static void logInfo(String message) {
        if (plugin == null) return;
        plugin.getServer().getConsoleSender().sendMessage(fromLegacy(PLUGIN_PREFIX_CONFIG + message));
    }

    public static void setItemValue(String itemId, String path, Object value) {
        synchronized (CONFIG_LOCK) {
            sendDebugMessage(DEBUG_INFO, () -> "[Config] Setting value: items." + itemId + "." + path + " = " + value);

            String normalizedPath;

            if (path != null) {
                if (path.equals("active_mode")) {
                    normalizedPath = "active-mode.enabled";
                } else if (path.equals("cooldown")) {
                    normalizedPath = "active-mode.cooldown";
                } else if (path.equals("effect_duration")) {
                    normalizedPath = "active-mode.duration";
                } else if (path.equals("commands")) {
                    normalizedPath = "active-mode.commands";
                } else if (path.equals("costs")) {
                    normalizedPath = "active-mode.costs";
                } else if (path.startsWith("visuals")) {
                    normalizedPath = "active-mode." + path;
                } else if (path.startsWith("sounds")) {
                    normalizedPath = "active-mode." + path;
                } else if (path.startsWith("active_effects")) {
                    // active_effects.potion_effects -> active-mode.effects.potion_effects
                    normalizedPath = path.replace("active_effects", "active-mode.effects");
                } else {
                    normalizedPath = path;
                }
            } else {
                normalizedPath = null;
            }

            sendDebugMessage(DEBUG_INFO, () -> "[Config] Setting value: items." + itemId + "." + normalizedPath + " = " + value);

            String fullPath = (normalizedPath == null) ? "items." + itemId : "items." + itemId + "." + normalizedPath;

            if ("permission".equals(normalizedPath) && value == null) {
                ItemsConfig.get().set(fullPath, NO_PERMISSION);
            } else {
                ItemsConfig.get().set(fullPath, value);
            }

            ItemsConfig.save();

            plugin.getItemManager().reloadSingleItem(itemId);
            plugin.getEffectApplicatorTask().invalidateCacheForHolding(itemId);
        }
    }

    public static boolean createNewItem(String itemId) {
        synchronized (CONFIG_LOCK) {
            sendDebugMessage(DEBUG_INFO, () -> "[Config] Creating new item: " + itemId);

            FileConfiguration config = ItemsConfig.get();
            if (config.contains("items." + itemId)) {
                sendDebugMessage(DEBUG_INFO, () -> "[Config] Item already exists: " + itemId);
                return false;
            }

            config.set("items." + itemId + ".display_name", "&f" + itemId);
            config.set("items." + itemId + ".material", "STONE");
            config.set("items." + itemId + ".lore", List.of("", "&7A new BuffedItem."));

            ItemsConfig.save();

            plugin.getItemManager().reloadSingleItem(itemId);

            logInfo("&aCreated new item: &e" + itemId);
            return true;
        }
    }

    public static String duplicateItem(String sourceItemId, String newItemId) {
        synchronized (CONFIG_LOCK) {
            FileConfiguration config = ItemsConfig.get();
            ConfigurationSection sourceSection = config.getConfigurationSection("items." + sourceItemId);

            if (sourceSection == null) {
                sendDebugMessage(DEBUG_INFO, () -> "[Config] Duplicate failed: Source item '" + sourceItemId + "' not found.");
                return null;
            }

            if (config.getConfigurationSection("items." + newItemId) != null) {
                sendDebugMessage(DEBUG_INFO, () -> "[Config] Duplicate failed: Target ID '" + newItemId + "' already exists.");
                return null;
            }

            String originalDisplayName = sourceSection.getString("display_name", "&f" + sourceItemId);
            Map<String, Object> sourceData = sourceSection.getValues(true);

            config.createSection("items." + newItemId, sourceData);
            config.set("items." + newItemId + ".display_name", originalDisplayName + " &7(Copy)");

            ItemsConfig.save();

            plugin.getItemManager().reloadSingleItem(newItemId);

            sendDebugMessage(DEBUG_INFO, () -> "[Config] Duplicated item '" + sourceItemId + "' to '" + newItemId + "'");
            return newItemId;
        }
    }

    private static void invalidateAllPlayerCaches() {
        if (plugin.getEffectApplicatorTask() == null) {
            return;
        }

        int playerCount = org.bukkit.Bukkit.getOnlinePlayers().size();
        sendDebugMessage(DEBUG_TASK, () -> "[Config] Invalidating cache for all (" + playerCount + ") online players after config change...");

        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            plugin.getEffectApplicatorTask().markPlayerForUpdate(player.getUniqueId());
        }

        sendDebugMessage(DEBUG_TASK, () -> "[Config] Cache invalidation complete. Changes will apply on next tick.");
    }

    public static Component getPrefixedMessageAsComponent(String path) {
        String prefix = plugin.getConfig().getString("messages.prefix", PLUGIN_PREFIX_CONFIG);
        String msg = plugin.getConfig().getString("messages." + path, "&cMissing message: messages." + path);
        return fromLegacy(prefix + msg);
    }

    public static void setDebugLevel(int level) {
        if (level < DEBUG_OFF) level = DEBUG_OFF;
        if (level > DEBUG_VERBOSE) level = DEBUG_OFF;

        plugin.getConfig().set("debug-level", level);
        plugin.saveConfig();
        loadGlobalSettings();
    }

    public static void setShowPotionIcons(boolean show) {
        plugin.getConfig().set("show-potion-icons", show);
        plugin.saveConfig();
        loadGlobalSettings();
    }

    public static boolean isDebugLevelEnabled(int level) {
        return debugLevel >= level;
    }

    public static int getDebugLevel() {
        return debugLevel;
    }

    public static boolean shouldShowPotionIcons() {
        return showPotionIcons;
    }

    public static boolean isVisualChatEnabled() { return visualChat; }
    public static boolean isVisualTitleEnabled() { return visualTitle; }
    public static boolean isVisualActionBarEnabled() { return visualActionBar; }
    public static boolean isVisualBossBarEnabled() { return visualBossBar; }
    public static String getBossBarColor() { return bossBarColor; }
    public static String getBossBarStyle() { return bossBarStyle; }
    public static String getGlobalSuccessSound() { return globalSuccessSound; }
    public static String getGlobalCooldownSound() { return globalCooldownSound; }
    public static String getGlobalCostFailSound() { return globalCostFailSound; }

    public static String getDefaultCostMessage(String type) {
        String path = "active-items.costs.messages." + type.toLowerCase();

        if (plugin.getConfig().contains(path)) {
            return plugin.getConfig().getString(path);
        }
        return plugin.getConfig().getString("active-items.costs.messages.default", "&cCost not met.");
    }
}