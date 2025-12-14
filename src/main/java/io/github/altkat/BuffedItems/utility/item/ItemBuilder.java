package io.github.altkat.BuffedItems.utility.item;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.utility.ItemUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
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

    protected static final EquipmentSlot[] VALID_SLOTS = {
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public ItemBuilder(BuffedItem buffedItem, Plugin plugin) {
        this.buffedItem = buffedItem;
        this.plugin = plugin;
        this.itemStack = new ItemStack(buffedItem.getMaterial());
    }

    public ItemStack build() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {return itemStack;}

        meta.displayName(ConfigManager.fromLegacy(buffedItem.getDisplayName()));
        List<Component> coloredLore = ConfigManager.loreFromLegacy(buffedItem.getLore());
        meta.lore(coloredLore);

        if (buffedItem.getCustomModelData().isPresent()) {
            int cmdValue = buffedItem.getCustomModelData().get();
            meta.setCustomModelData(cmdValue);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                    () -> "[ItemBuilder] Applied CMD (Integer): " + cmdValue + " to " + buffedItem.getId());
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

        ItemUtils.applyAttributes(buffedItem, meta);

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

        NamespacedKey versionKey = new NamespacedKey(plugin, "buffeditem_version");
        meta.getPersistentDataContainer().set(versionKey, PersistentDataType.INTEGER, buffedItem.getUpdateHash());

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
}