package io.github.altkat.BuffedItems.utils;

import io.github.altkat.BuffedItems.Managers.ConfigManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;

public class ItemBuilder {

    private final BuffedItem buffedItem;
    private final ItemStack itemStack;
    private final Plugin plugin;
    private final int serverVersion;

    public ItemBuilder(BuffedItem buffedItem, Plugin plugin) {
        this.buffedItem = buffedItem;
        this.plugin = plugin;
        this.itemStack = new ItemStack(buffedItem.getMaterial());
        this.serverVersion = getMinecraftVersion();
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
            plugin.getLogger().warning("Could not parse MC version: " + version);
        }
        return 1165;
    }

    public ItemStack build() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        meta.displayName(ConfigManager.fromLegacy(buffedItem.getDisplayName()));

        List<Component> coloredLore = ConfigManager.loreFromLegacy(buffedItem.getLore());
        meta.lore(coloredLore);

        if (buffedItem.getCustomModelData().isPresent()) {
            int cmdValue = buffedItem.getCustomModelData().get();

            if (serverVersion < 1210) {
                meta.setCustomModelData(cmdValue);
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                        () -> "[ItemBuilder] Applied CMD (legacy): " + cmdValue + " to " + buffedItem.getId());
            } else {
                try {
                    meta.setCustomModelData(cmdValue);
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                            () -> "[ItemBuilder] Applied CMD (Paper): " + cmdValue + " to " + buffedItem.getId());
                } catch (Exception e) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                            () -> "[ItemBuilder] Legacy CMD failed, trying NMS for " + buffedItem.getId());
                    try {
                        applyCustomModelDataNMS(cmdValue);
                    } catch (Exception nmsException) {
                        plugin.getLogger().warning("[ItemBuilder] Failed to apply CMD via NMS: " + nmsException.getMessage());
                    }
                }
            }
        }

        if (buffedItem.getFlag("UNBREAKABLE")) {
            meta.setUnbreakable(true);
        }

        boolean hasRealEnchants = !buffedItem.getEnchantments().isEmpty();

        if (hasRealEnchants) {
            for (Map.Entry<Enchantment, Integer> entry : buffedItem.getEnchantments().entrySet()) {
                Enchantment enchantment = entry.getKey();
                int level = entry.getValue();
                try {
                    meta.addEnchant(enchantment, level, true);
                } catch (IllegalArgumentException e) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                            () -> "[ItemBuilder] Failed to apply enchantment " + enchantment.getKey().getKey() +
                                    " with level " + level + " to item " + buffedItem.getId() + ": " + e.getMessage());
                }
            }
        }

        if (buffedItem.hasGlow()) {
            if (!hasRealEnchants || buffedItem.getFlag("HIDE_ENCHANTS")) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, false);
            }
        }

        if (buffedItem.getFlag("HIDE_ENCHANTS")) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (buffedItem.getFlag("HIDE_ATTRIBUTES")) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        if (buffedItem.getFlag("HIDE_UNBREAKABLE")) {
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        if (buffedItem.getFlag("HIDE_DESTROYS")) {
            meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        }
        if (buffedItem.getFlag("HIDE_PLACED_ON")) {
            meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        }
        if (buffedItem.getFlag("HIDE_ADDITIONAL_TOOLTIP")) {
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }

        NamespacedKey key = new NamespacedKey(plugin, "buffeditem_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, buffedItem.getId());

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private void applyCustomModelDataNMS(int cmdValue) throws Exception {
        Class<?> craftItemStackClass = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");

        Object nmsStack = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class)
                .invoke(null, itemStack);

        if (nmsStack == null) {
            throw new IllegalStateException("NMS ItemStack is null");
        }

        Class<?> dataComponentTypeClass = Class.forName("net.minecraft.core.component.DataComponentType");
        Class<?> dataComponentsClass = Class.forName("net.minecraft.core.component.DataComponents");

        Object customModelDataComponent = dataComponentsClass.getField("CUSTOM_MODEL_DATA").get(null);

        Class<?> customModelDataClass = Class.forName("net.minecraft.world.item.component.CustomModelData");
        Object customModelDataValue = customModelDataClass.getConstructor(int.class).newInstance(cmdValue);

        nmsStack.getClass()
                .getMethod("b", dataComponentTypeClass, Object.class).invoke(nmsStack, customModelDataComponent, customModelDataValue);

        ItemStack resultStack = (ItemStack) craftItemStackClass
                .getMethod("asBukkitCopy", nmsStack.getClass())
                .invoke(null, nmsStack);

        itemStack.setItemMeta(resultStack.getItemMeta());

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                () -> "[ItemBuilder] Applied CMD via NMS: " + cmdValue);
    }
}