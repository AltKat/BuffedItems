package io.github.altkat.BuffedItems.utility.item;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.utility.attribute.ParsedAttribute;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ItemUpdater {

    private final BuffedItems plugin;
    private final NamespacedKey idKey;
    private final NamespacedKey usesKey;

    public ItemUpdater(BuffedItems plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "buffeditem_id");
        this.usesKey = new NamespacedKey(plugin, "remaining_active_uses");
    }

    public ItemStack updateItem(ItemStack oldItem, Player player) {
        if (oldItem == null || !oldItem.hasItemMeta()) return null;

        String itemId = oldItem.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (itemId == null) return null;

        BuffedItem template = plugin.getItemManager().getBuffedItem(itemId);
        if (template == null) return null;

        ItemStack newItem = oldItem.clone();

        if (newItem.getType() != template.getMaterial()) {
            newItem.setType(template.getMaterial());
        }

        ItemMeta meta = newItem.getItemMeta();

        String rawName = template.getDisplayName();
        String parsedName = plugin.getHookManager().processPlaceholders(player, rawName);
        meta.displayName(ConfigManager.fromLegacy(parsedName));

        List<Component> parsedLore = new ArrayList<>();
        for (String line : template.getLore()) {
            String parsedLine = plugin.getHookManager().processPlaceholders(player, line);
            parsedLore.add(ConfigManager.fromLegacy(parsedLine));
        }
        meta.lore(parsedLore);

        if (template.getCustomModelData().isPresent()) {
            meta.setCustomModelData(template.getCustomModelData().get());
        } else {
            meta.setCustomModelData(null);
        }

        meta.removeItemFlags(ItemFlag.values());
        if (template.getFlag("HIDE_ENCHANTS")) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        if (template.getFlag("HIDE_ATTRIBUTES")) meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (template.getFlag("HIDE_UNBREAKABLE")) meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        if (template.getFlag("HIDE_DESTROYS")) meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        if (template.getFlag("HIDE_PLACED_ON")) meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        if (template.getFlag("HIDE_ADDITIONAL_TOOLTIP")) meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        if (template.getFlag("HIDE_ARMOR_TRIM")) meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);

        meta.setUnbreakable(template.getFlag("UNBREAKABLE"));

        for (Map.Entry<Enchantment, Integer> entry : template.getEnchantments().entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        if (meta.hasAttributeModifiers()) {
            List<Map.Entry<Attribute, AttributeModifier>> toRemove = new ArrayList<>();
            meta.getAttributeModifiers().forEach((attr, mod) -> {
                if (mod.getName().startsWith("buffeditems.")) {
                    toRemove.add(new AbstractMap.SimpleEntry<>(attr, mod));
                }
            });
            for (Map.Entry<Attribute, AttributeModifier> entry : toRemove) {
                meta.removeAttributeModifier(entry.getKey(), entry.getValue());
            }
        }

        boolean hasRealEnchants = meta.hasEnchants();
        if (template.hasGlow()) {
            if (!hasRealEnchants || template.getFlag("HIDE_ENCHANTS")) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, false);
            }
        }

        if (template.isActiveMode() && template.getMaxUses() > 0) {
            int usesToSet = template.getMaxUses();

            if (meta.getPersistentDataContainer().has(usesKey, PersistentDataType.INTEGER)) {
                usesToSet = meta.getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER);
            }

            meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, usesToSet);
            updateLoreWithCurrentUses(meta, template, usesToSet, player);
        } else {
            meta.getPersistentDataContainer().remove(usesKey);
        }

        newItem.setItemMeta(meta);
        return newItem;
    }

    private void updateLoreWithCurrentUses(ItemMeta meta, BuffedItem item, int currentUses, Player player) {
        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        String rawMaxLine = item.getUsageLore(item.getMaxUses());
        String parsedMaxLine = plugin.getHookManager().processPlaceholders(player, rawMaxLine);
        String cleanMaxLine = ConfigManager.toPlainText(ConfigManager.fromLegacy(parsedMaxLine));

        boolean found = false;

        String rawNewLine;
        if (currentUses > 0) {
            rawNewLine = item.getUsageLore(currentUses);
        } else {
            rawNewLine = item.getDepletedLore();
        }

        String parsedNewLine = plugin.getHookManager().processPlaceholders(player, rawNewLine);
        Component finalComponent = ConfigManager.fromLegacy(parsedNewLine);

        for (int i = 0; i < lore.size(); i++) {
            String cleanCurrentLine = ConfigManager.toPlainText(lore.get(i));
            if (cleanCurrentLine.equals(cleanMaxLine) || cleanCurrentLine.contains(String.valueOf(item.getMaxUses()))) {
                lore.set(i, finalComponent);
                found = true;
                break;
            }
        }

        if (!found) {
            lore.add(finalComponent);
        }

        meta.lore(lore);
    }
}