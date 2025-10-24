package io.github.altkat.BuffedItems.Managers;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.function.Supplier;

public class ConfigManager {

    private static BuffedItems plugin;
    public static final String NO_PERMISSION = "NONE";
    private static boolean debugMode = false;
    private static boolean showPotionIcons = true;

    public static void setup(BuffedItems pluginInstance) {
        plugin = pluginInstance;
    }

    public static void reloadConfig() {
        long startTime = System.currentTimeMillis();

        sendDebugMessage(() -> "[Config] Reloading configuration...");

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
        sendDebugMessage(() -> "[Config] Reload complete in " + elapsedTime + "ms");
    }

    public static void loadGlobalSettings() {
        debugMode = plugin.getConfig().getBoolean("debug-mode", false);
        showPotionIcons = plugin.getConfig().getBoolean("show-potion-icons", true);

        if (debugMode) {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&9[&6BuffedItems&9] §e[Debug Mode] Enabled - Detailed logs will be shown."));
        }
    }

    @Deprecated
    public static void sendDebugMessage(String message) {
        if (debugMode) {
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&9[&6BuffedItems&9] &e[Debug] &r" + message));
        }
    }

    public static void sendDebugMessage(Supplier<String> messageSupplier) {
        if (debugMode) {
            String message = messageSupplier.get();
            plugin.getServer().getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&9[&6BuffedItems&9] &e[Debug] &r" + message));
        }
    }

    public static void setItemValue(String itemId, String path, Object value) {
        sendDebugMessage(() -> "[Config] Setting value: items." + itemId + "." + path + " = " + value);

        String fullPath = (path == null) ? "items." + itemId : "items." + itemId + "." + path;

        if ("permission".equals(path) && value == null) {
            plugin.getConfig().set(fullPath, NO_PERMISSION);
        } else {
            plugin.getConfig().set(fullPath, value);
        }
        plugin.getItemManager().reloadSingleItem(itemId);
        plugin.getEffectApplicatorTask().invalidateCacheForHolding(itemId);
    }

    public static boolean createNewItem(String itemId) {
        sendDebugMessage(() -> "[Config] Creating new item: " + itemId);

        FileConfiguration config = plugin.getConfig();
        if (config.contains("items." + itemId)) {
            sendDebugMessage(() -> "[Config] Item already exists: " + itemId);
            return false;
        }

        config.set("items." + itemId + ".display_name", "&f" + itemId);
        config.set("items." + itemId + ".material", "STONE");
        config.set("items." + itemId + ".lore", Collections.singletonList("&7A new BuffedItem."));

        plugin.getItemManager().reloadSingleItem(itemId);

        plugin.getLogger().info("Created new item: " + itemId);
        return true;
    }

    private static void invalidateAllPlayerCaches() {
        if (plugin.getEffectApplicatorTask() == null) {
            return;
        }

        int playerCount = org.bukkit.Bukkit.getOnlinePlayers().size();
        sendDebugMessage(() -> "[Config] Invalidating cache for all (" + playerCount + ") online players after config change...");

        for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            plugin.getEffectApplicatorTask().markPlayerForUpdate(player.getUniqueId());
        }

        sendDebugMessage(() -> "[Config] Cache invalidation complete. Changes will apply on next tick.");
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static boolean shouldShowPotionIcons() {
        return showPotionIcons;
    }
}