package io.github.altkat.BuffedItems.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;

public class CustomModelDataHandler {

    private final BuffedItems plugin;
    private boolean itemsAdderAvailable = false;
    private boolean nexoAvailable = false;

    public CustomModelDataHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    public CustomModelData resolve(String input, String itemId) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();

        try {
            int value = Integer.parseInt(trimmed);
            if (value < 0) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                        () -> "[CMD] Invalid custom-model-data for item '" + itemId + "': negative value " + value);
                return null;
            }
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[CMD] Resolved direct integer custom-model-data for '" + itemId + "': " + value);
            return new CustomModelData(value, trimmed, CustomModelData.Source.DIRECT);
        } catch (NumberFormatException ignored) {}

        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 2);
            String pluginName = parts[0].toLowerCase();
            String externalItemId = parts[1];

            switch (pluginName) {
                case "itemsadder":
                    return resolveItemsAdder(externalItemId, trimmed, itemId);
                case "nexo":
                    return resolveNexo(externalItemId, trimmed, itemId);
                default:
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                            () -> "[CMD] Unknown plugin prefix for item '" + itemId + "': " + pluginName);
                    return null;
            }
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[CMD] Invalid custom-model-data format for item '" + itemId + "': " + trimmed);
        return null;
    }

    private CustomModelData resolveItemsAdder(String externalItemId, String rawValue, String buffedItemId) {
        if (!plugin.getHookManager().isItemsAdderLoaded()) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] ItemsAdder format used but plugin not hooked: " + buffedItemId);
            return null;
        }

        Integer cmd = plugin.getHookManager().getItemsAdderHook().getCustomModelData(externalItemId);

        if (cmd != null) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[CMD] Resolved ItemsAdder CMD for '" + buffedItemId + "': " + externalItemId + " -> " + cmd);
            return new CustomModelData(cmd, rawValue, CustomModelData.Source.ITEMSADDER);
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[CMD] ItemsAdder item '" + externalItemId + "' not found or has no CMD.");
        return null;
    }

    private CustomModelData resolveNexo(String externalItemId, String rawValue, String buffedItemId) {
        if (!plugin.getHookManager().isNexoLoaded()) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] Nexo format used but plugin not hooked: " + buffedItemId);
            return null;
        }

        Integer cmd = plugin.getHookManager().getNexoHook().getCustomModelData(externalItemId);

        if (cmd != null) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[CMD] Resolved Nexo CMD for '" + buffedItemId + "': " + externalItemId + " -> " + cmd);
            return new CustomModelData(cmd, rawValue, CustomModelData.Source.NEXO);
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[CMD] Nexo item '" + externalItemId + "' not found or has no CMD.");
        return null;
    }

    public static class CustomModelData {
        private final int value;
        private final String rawValue;
        private final Source source;

        public CustomModelData(int value, String rawValue, Source source) {
            this.value = value;
            this.rawValue = rawValue;
            this.source = source;
        }

        public int getValue() { return value; }
        public String getRawValue() { return rawValue; }
        public Source getSource() { return source; }

        public enum Source { DIRECT, ITEMSADDER, NEXO }
    }
}