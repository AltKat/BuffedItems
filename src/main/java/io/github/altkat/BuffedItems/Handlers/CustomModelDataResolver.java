package io.github.altkat.BuffedItems.Handlers;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomModelDataResolver {

    private final BuffedItems plugin;
    private boolean itemsAdderAvailable = false;
    private boolean nexoAvailable = false;
    private boolean oraxenAvailable = false;

    public CustomModelDataResolver(BuffedItems plugin) {
        this.plugin = plugin;
        detectExternalPlugins();
    }

    private void detectExternalPlugins() {
        itemsAdderAvailable = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
        nexoAvailable = Bukkit.getPluginManager().getPlugin("Nexo") != null;
        oraxenAvailable = Bukkit.getPluginManager().getPlugin("Oraxen") != null;

        if (itemsAdderAvailable) {
            ConfigManager.logInfo("&aItemsAdder detected - custom model data integration enabled");
        }
        if (nexoAvailable) {
            ConfigManager.logInfo("&aNexo detected - custom model data integration enabled");
        }
        if (oraxenAvailable) {
            ConfigManager.logInfo("&aOraxen detected - custom model data integration enabled");
        }
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
        } catch (NumberFormatException e) {
            //
        }

        if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":", 2);
            String pluginName = parts[0].toLowerCase();
            String externalItemId = parts[1];

            switch (pluginName) {
                case "itemsadder":
                    return resolveItemsAdder(externalItemId, trimmed, itemId);
                case "nexo":
                    return resolveNexo(externalItemId, trimmed, itemId);
                case "oraxen":
                    return resolveOraxen(externalItemId, trimmed, itemId);
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
        if (!itemsAdderAvailable) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] ItemsAdder format used but plugin not found for item: " + buffedItemId);
            return null;
        }

        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object customStack = customStackClass.getMethod("getInstance", String.class)
                    .invoke(null, externalItemId);

            if (customStack == null) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                        () -> "[CMD] ItemsAdder item not found: " + externalItemId + " (for BuffedItem: " + buffedItemId + ")");
                return null;
            }

            Object itemStackObj = customStackClass.getMethod("getItemStack").invoke(customStack);
            ItemStack itemStack = (ItemStack) itemStackObj;
            Integer cmd = extractCustomModelData(itemStack);

            if (cmd != null) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                        () -> "[CMD] Resolved ItemsAdder custom-model-data for '" + buffedItemId + "': " +
                                externalItemId + " -> " + cmd);
                return new CustomModelData(cmd, rawValue, CustomModelData.Source.ITEMSADDER);
            }

            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] ItemsAdder item '" + externalItemId + "' has no custom model data");
            return null;

        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] Failed to resolve ItemsAdder custom-model-data for '" + buffedItemId + "': " + e.getMessage());
            return null;
        }
    }

    private CustomModelData resolveNexo(String externalItemId, String rawValue, String buffedItemId) {
        if (!nexoAvailable) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] Nexo format used but plugin not found for item: " + buffedItemId);
            return null;
        }

        try {
            Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            Object itemBuilder = nexoItemsClass.getMethod("itemFromId", String.class)
                    .invoke(null, externalItemId);

            if (itemBuilder == null) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                        () -> "[CMD] Nexo item not found: " + externalItemId + " (for BuffedItem: " + buffedItemId + ")");
                return null;
            }

            Object itemStackObj = itemBuilder.getClass().getMethod("build").invoke(itemBuilder);
            ItemStack itemStack = (ItemStack) itemStackObj;
            Integer cmd = extractCustomModelData(itemStack);

            if (cmd != null) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                        () -> "[CMD] Resolved Nexo custom-model-data for '" + buffedItemId + "': " +
                                externalItemId + " -> " + cmd);
                return new CustomModelData(cmd, rawValue, CustomModelData.Source.NEXO);
            }

            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] Nexo item '" + externalItemId + "' has no custom model data");
            return null;

        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] Failed to resolve Nexo custom-model-data for '" + buffedItemId + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private CustomModelData resolveOraxen(String externalItemId, String rawValue, String buffedItemId) {
        if (Bukkit.getPluginManager().getPlugin("Oraxen") == null) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] Oraxen format used but plugin not found for item: " + buffedItemId);
            return null;
        }

        try {
            Class<?> oraxenItemsClass = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
            Object oraxenItem = oraxenItemsClass.getMethod("getItemById", String.class)
                    .invoke(null, externalItemId);

            if (oraxenItem == null) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                        () -> "[CMD] Oraxen item not found: " + externalItemId + " (for BuffedItem: " + buffedItemId + ")");
                return null;
            }

            Object itemStackObj = oraxenItem.getClass().getMethod("build").invoke(oraxenItem);
            ItemStack itemStack = (ItemStack) itemStackObj;
            Integer cmd = extractCustomModelData(itemStack);

            if (cmd != null) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                        () -> "[CMD] Resolved Oraxen custom-model-data for '" + buffedItemId + "': " +
                                externalItemId + " -> " + cmd);
                return new CustomModelData(cmd, rawValue, CustomModelData.Source.ORAXEN);
            }

            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] Oraxen item '" + externalItemId + "' has no custom model data");
            return null;

        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] Failed to resolve Oraxen custom-model-data for '" + buffedItemId + "': " + e.getMessage());
            return null;
        }
    }

    private Integer extractCustomModelData(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();

        if (meta.hasCustomModelData()) {
            return meta.getCustomModelData();
        }

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

        public int getValue() {
            return value;
        }

        public String getRawValue() {
            return rawValue;
        }

        public Source getSource() {
            return source;
        }

        public enum Source {
            DIRECT,
            ITEMSADDER,
            NEXO,
            ORAXEN
        }
    }

    public boolean isItemsAdderAvailable() {
        return itemsAdderAvailable;
    }

    public boolean isNexoAvailable() {
        return nexoAvailable;
    }
}