package io.github.altkat.BuffedItems.utility.item;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.utility.attribute.ParsedAttribute;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
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
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () ->
                            "[ItemBuilder] Applied enchantment: " + enchantment.getKey().getKey() +
                                    " Level: " + level + " to item " + buffedItem.getId());
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
        if (buffedItem.getFlag("HIDE_ARMOR_TRIM")) {
            meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        }

        NamespacedKey key = new NamespacedKey(plugin, "buffeditem_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, buffedItem.getId());

        if(buffedItem.isActiveMode() && buffedItem.getMaxUses() > 0){
            NamespacedKey maxKey = new NamespacedKey(plugin, "remaining_active_uses");
            meta.getPersistentDataContainer().set(maxKey, PersistentDataType.INTEGER, buffedItem.getMaxUses());

            List<Component> lore = meta.lore();
            if(lore == null){
                lore = new ArrayList<>();
            }
            String dynamicLore = buffedItem.getUsageLore(buffedItem.getMaxUses());
            lore.add(ConfigManager.fromLegacy(dynamicLore));

            meta.lore(lore);
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private void applyCustomModelDataNMS(int cmdValue) throws Exception {
        try {
            Class<?> itemMetaClass = ItemMeta.class;
            java.lang.reflect.Method setCustomModelData =
                    itemMetaClass.getMethod("setCustomModelData", int.class);

            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                setCustomModelData.invoke(meta, cmdValue);
                itemStack.setItemMeta(meta);
            }
        } catch (NoSuchMethodException e) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(cmdValue);
                itemStack.setItemMeta(meta);
            }
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                () -> "[ItemBuilder] Applied CMD: " + cmdValue);
    }

    private EquipmentSlot getEquipmentSlot(String slot) {
        switch (slot.toUpperCase()) {
            case "MAIN_HAND": return EquipmentSlot.HAND;
            case "OFF_HAND": return EquipmentSlot.OFF_HAND;
            case "HELMET": return EquipmentSlot.HEAD;
            case "CHESTPLATE": return EquipmentSlot.CHEST;
            case "LEGGINGS": return EquipmentSlot.LEGS;
            case "BOOTS": return EquipmentSlot.FEET;
            case "INVENTORY":
            default: return null;
        }
    }
}