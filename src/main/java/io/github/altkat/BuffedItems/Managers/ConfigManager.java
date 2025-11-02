package io.github.altkat.BuffedItems.Managers;

import io.github.altkat.BuffedItems.BuffedItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConfigManager {

    private static BuffedItems plugin;
    private static final Object CONFIG_LOCK = new Object();
    public static final String NO_PERMISSION = "NONE";
    private static int debugLevel = 0;
    private static boolean showPotionIcons = true;

    public static final int DEBUG_OFF = 0;
    public static final int DEBUG_INFO = 1;      // Basic plugin status
    public static final int DEBUG_TASK = 2;      // Core task loops, major events
    public static final int DEBUG_DETAILED = 3;  // Per-player effect details
    public static final int DEBUG_VERBOSE = 4;   // GUI, Chat, Inventory events (spammy)

    private static final String PLUGIN_PREFIX_CONFIG = "&9[&6BuffedItems&9] ";

    private static final PlainTextComponentSerializer plainTextSerializer = PlainTextComponentSerializer.plainText();

    private static final LegacyComponentSerializer ampersandSerializer = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final LegacyComponentSerializer sectionSerializer = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
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
        sendDebugMessage(DEBUG_INFO, () -> "[Config] Creating config.yml backup to config.yml.backup...");
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        File backupFile = new File(plugin.getDataFolder(), "config.yml.backup");

        if (!configFile.exists()) {
            sendDebugMessage(DEBUG_INFO, () -> "[Config] config.yml does not exist, skipping backup.");
            return;
        }

        try {
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            sendDebugMessage(DEBUG_INFO, () -> "[Config] Backup created successfully.");
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create config backup: " + e.getMessage());
            sendDebugMessage(DEBUG_INFO, () -> "[Config] Backup creation FAILED. Error: " + e.getMessage());
        }
    }

    public static void reloadConfig() {
        synchronized (CONFIG_LOCK) {
            long startTime = System.currentTimeMillis();

            sendDebugMessage(DEBUG_INFO, () -> "[Config] Reloading configuration...");

            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                plugin.getLogger().warning("config.yml not found! Creating default config...");
                try {
                    plugin.saveDefaultConfig();
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to create default config: " + e.getMessage());
                    return;
                }
            }

            plugin.reloadConfig();
            plugin.getItemManager().loadItems(false);
            loadGlobalSettings();
            plugin.reloadConfigSettings();
            invalidateAllPlayerCaches();

            long elapsedTime = System.currentTimeMillis() - startTime;
            sendDebugMessage(DEBUG_INFO, () -> "[Config] Reload complete in " + elapsedTime + "ms");
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
                    fromLegacy("&9[&6BuffedItems&9] &e[Debug Level " + debugLevel + "] Enabled - Detailed logs will be shown according to level.")
            );
        }
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
            String prefix = "&9[&6BuffedItems&9] &e[L" + level + "] &r";
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

            String fullPath = (path == null) ? "items." + itemId : "items." + itemId + "." + path;

            if ("permission".equals(path) && value == null) {
                plugin.getConfig().set(fullPath, NO_PERMISSION);
            } else {
                plugin.getConfig().set(fullPath, value);
            }
            plugin.getItemManager().reloadSingleItem(itemId);
            plugin.getEffectApplicatorTask().invalidateCacheForHolding(itemId);
        }
    }

    public static boolean createNewItem(String itemId) {
        synchronized (CONFIG_LOCK) {
            sendDebugMessage(DEBUG_INFO, () -> "[Config] Creating new item: " + itemId);

            FileConfiguration config = plugin.getConfig();
            if (config.contains("items." + itemId)) {
                sendDebugMessage(DEBUG_INFO, () -> "[Config] Item already exists: " + itemId);
                return false;
            }

            config.set("items." + itemId + ".display_name", "&f" + itemId);
            config.set("items." + itemId + ".material", "STONE");
            config.set("items." + itemId + ".lore", List.of("", "&7A new BuffedItem."));

            plugin.getItemManager().reloadSingleItem(itemId);

            logInfo("&aCreated new item: &e" + itemId);
            return true;
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

    public static boolean isDebugLevelEnabled(int level) {
        return debugLevel >= level;
    }

    public static int getDebugLevel() {
        return debugLevel;
    }

    public static boolean shouldShowPotionIcons() {
        return showPotionIcons;
    }
}