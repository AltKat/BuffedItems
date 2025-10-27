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

    public CustomModelDataResolver(BuffedItems plugin) {
        this.plugin = plugin;
        detectExternalPlugins();
    }

    private void detectExternalPlugins() {
        itemsAdderAvailable = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
        nexoAvailable = Bukkit.getPluginManager().getPlugin("Nexo") != null;

        if (itemsAdderAvailable) {
            plugin.getLogger().info("ItemsAdder detected - custom model data integration enabled");
        }
        if (nexoAvailable) {
            plugin.getLogger().info("Nexo detected - custom model data integration enabled");
        }
    }

    /**
     * Resolves custom model data from various formats:
     * - Direct integer: 100001
     * - ItemsAdder: "itemsadder:fire_sword"
     * - Nexo: "nexo:custom_helmet"
     * - Oraxen: "oraxen:magic_wand" (future support)
     *
     * @param input The raw string from config
     * @param itemId The BuffedItem ID (for logging)
     * @return Resolved CustomModelData object or null if invalid
     */
    public CustomModelData resolve(String input, String itemId) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();

        // Try direct integer first
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
            // Not a direct integer, try plugin formats
        }

        // Check for plugin format (plugin:item_id)
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
            // ItemsAdder API integration via Reflection
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
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null && itemMeta.hasCustomModelData()) {
                Integer cmd = itemMeta.getCustomModelData();

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
            e.printStackTrace();
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
            // Nexo API integration via Reflection
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
            ItemMeta itemMeta = itemStack.getItemMeta();

            if (itemMeta != null && itemMeta.hasCustomModelData()) {
                Integer cmd = itemMeta.getCustomModelData();

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
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[CMD] Oraxen integration not yet implemented for item: " + buffedItemId);
        return null;
    }

    /**
     * Container class for resolved custom model data
     */
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
            DIRECT,     // Direct integer: 100001
            ITEMSADDER, // ItemsAdder: itemsadder:fire_sword
            NEXO,       // Nexo: nexo:custom_helmet
            ORAXEN      // Oraxen: oraxen:magic_wand (future)
        }
    }

    public boolean isItemsAdderAvailable() {
        return itemsAdderAvailable;
    }

    public boolean isNexoAvailable() {
        return nexoAvailable;
    }
}