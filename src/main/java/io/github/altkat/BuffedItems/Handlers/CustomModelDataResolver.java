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
    private final int serverVersion;

    public CustomModelDataResolver(BuffedItems plugin) {
        this.plugin = plugin;
        this.serverVersion = getMinecraftVersion();
        detectExternalPlugins();
    }

    private void detectExternalPlugins() {
        itemsAdderAvailable = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
        nexoAvailable = Bukkit.getPluginManager().getPlugin("Nexo") != null;

        if (itemsAdderAvailable) {
            ConfigManager.logInfo("&aItemsAdder detected - custom model data integration enabled (Version: " + serverVersion + ")");
        }
        if (nexoAvailable) {
            ConfigManager.logInfo("&aNexo detected - custom model data integration enabled (Version: " + serverVersion + ")");
        }
    }

    private int getMinecraftVersion() {
        String version = Bukkit.getBukkitVersion();
        try {
            String[] parts = version.split("-")[0].split("\\.");
            if (parts.length >= 2) {
                int major = Integer.parseInt(parts[0]);
                int minor = Integer.parseInt(parts[1]);
                int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                return (major * 100 + minor * 10 + patch);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not parse Minecraft version: " + version);
        }
        return 1165;
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
                                externalItemId + " -> " + cmd + " (Version: " + serverVersion + ")");
                return new CustomModelData(cmd, rawValue, CustomModelData.Source.NEXO);
            }

            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] Nexo item '" + externalItemId + "' has no custom model data (Version: " + serverVersion + ")");
            return null;

        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] Failed to resolve Nexo custom-model-data for '" + buffedItemId + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Integer extractCustomModelData(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();

        if (serverVersion < 1210) {
            if (meta.hasCustomModelData()) {
                return meta.getCustomModelData();
            }
            return null;
        }

        try {
            if (meta.hasCustomModelData()) {
                Integer oldValue = meta.getCustomModelData();
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                        () -> "[CMD] Found CMD via legacy method: " + oldValue);
                return oldValue;
            }

            Class<?> itemMetaClass = meta.getClass();

            try {
                Object cmdValue = itemMetaClass.getMethod("getCustomModelData").invoke(meta);
                if (cmdValue instanceof Integer) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                            () -> "[CMD] Found CMD via Paper API: " + cmdValue);
                    return (Integer) cmdValue;
                }
            } catch (NoSuchMethodException ignored) {
            }

            Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit." +
                    Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3] +
                    ".inventory.CraftItemStack");

            Object nmsStack = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class)
                    .invoke(null, itemStack);

            if (nmsStack != null) {
                Object components = nmsStack.getClass().getMethod("a").invoke(nmsStack);

                if (components != null) {
                    Object cmdComponent = components.getClass()
                            .getMethod("a", Class.forName("net.minecraft.core.component.DataComponentType"))
                            .invoke(components,
                                    Class.forName("net.minecraft.core.component.DataComponents")
                                            .getField("CUSTOM_MODEL_DATA").get(null));

                    if (cmdComponent != null) {
                        Integer value = (Integer) cmdComponent.getClass()
                                .getMethod("value").invoke(cmdComponent);
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                                () -> "[CMD] Found CMD via NMS components: " + value);
                        return value;
                    }
                }
            }

        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[CMD] Failed to extract CMD using reflection (1.21+): " + e.getMessage());
        }

        return null;
    }

    private CustomModelData resolveOraxen(String externalItemId, String rawValue, String buffedItemId) {
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[CMD] Oraxen integration not yet implemented for item: " + buffedItemId);
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