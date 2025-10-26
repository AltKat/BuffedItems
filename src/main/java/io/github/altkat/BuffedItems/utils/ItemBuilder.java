package io.github.altkat.BuffedItems.utils;

import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemBuilder {

    private final BuffedItem buffedItem;
    private final ItemStack itemStack;
    private final Plugin plugin;

    public ItemBuilder(BuffedItem buffedItem, Plugin plugin) {
        this.buffedItem = buffedItem;
        this.plugin = plugin;
        this.itemStack = new ItemStack(buffedItem.getMaterial());
    }

    public ItemStack build() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', buffedItem.getDisplayName()));

        List<String> coloredLore = buffedItem.getLore().stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
        meta.setLore(coloredLore);

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
                    ConfigManager.sendDebugMessage(() ->"Failed to apply enchantment " + enchantment.getKey().getKey() +
                            " with level " + level + " to item " + buffedItem.getId() + ": " + e.getMessage());
                }
            }
        }

        if (buffedItem.hasGlow()) {
            if (!hasRealEnchants || buffedItem.getFlag("HIDE_ENCHANTS")) {
                meta.addEnchant(Enchantment.LUCK, 1, false);
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
        if (buffedItem.getFlag("HIDE_POTION_EFFECTS")) {
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        }

        NamespacedKey key = new NamespacedKey(plugin, "buffeditem_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, buffedItem.getId());

        itemStack.setItemMeta(meta);
        return itemStack;
    }
}