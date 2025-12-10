package io.github.altkat.BuffedItems.utility.item;

import com.google.common.collect.Multimap;
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

import static io.github.altkat.BuffedItems.utility.item.ItemBuilder.VALID_SLOTS;

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

        for (Map.Entry<Enchantment, Integer> entry : template.getEnchantments().entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        meta.setUnbreakable(template.getFlag("UNBREAKABLE"));

        meta.setAttributeModifiers(null);

        boolean forceHideAttributes = false;

        if (template.getAttributeMode() == BuffedItem.AttributeMode.STATIC) {

            boolean hasAnyAttribute = false;

            for (Map.Entry<String, BuffedItemEffect> effectEntry : template.getEffects().entrySet()) {
                String slotKey = effectEntry.getKey().toUpperCase();
                BuffedItemEffect itemEffect = effectEntry.getValue();
                EquipmentSlot equipmentSlot = getEquipmentSlot(slotKey);

                if (equipmentSlot != null) {
                    for (ParsedAttribute parsedAttr : itemEffect.getParsedAttributes()) {

                        if (meta.hasAttributeModifiers()) {
                            Collection<AttributeModifier> mods = meta.getAttributeModifiers(parsedAttr.getAttribute());
                            if (mods != null) {
                                for (AttributeModifier existing : new ArrayList<>(mods)) {
                                    if (existing.getUniqueId().equals(parsedAttr.getUuid())) {
                                        meta.removeAttributeModifier(parsedAttr.getAttribute(), existing);
                                    }
                                }
                            }
                        }

                        AttributeModifier modifier = new AttributeModifier(
                                parsedAttr.getUuid(),
                                "buffeditems." + template.getId() + "." + slotKey,
                                parsedAttr.getAmount(),
                                parsedAttr.getOperation(),
                                equipmentSlot
                        );
                        meta.addAttributeModifier(parsedAttr.getAttribute(), modifier);
                        hasAnyAttribute = true;
                    }
                }
            }

            if (!hasAnyAttribute) {
                Attribute dummyAttr = Attribute.GENERIC_LUCK;
                for (EquipmentSlot slot : VALID_SLOTS) {
                    AttributeModifier dummyMod = new AttributeModifier(
                            UUID.randomUUID(),
                            "buffeditems.dummy.static." + slot.name(),
                            0,
                            AttributeModifier.Operation.ADD_NUMBER,
                            slot
                    );
                    meta.addAttributeModifier(dummyAttr, dummyMod);
                }
                forceHideAttributes = true;
            }

        } else {
            Attribute dummyAttr = Attribute.GENERIC_LUCK;
            for (EquipmentSlot slot : VALID_SLOTS) {
                AttributeModifier dummyMod = new AttributeModifier(
                        UUID.randomUUID(),
                        "buffeditems.dummy." + slot.name(),
                        0,
                        AttributeModifier.Operation.ADD_NUMBER,
                        slot
                );
                meta.addAttributeModifier(dummyAttr, dummyMod);
            }
            forceHideAttributes = true;
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

        meta.removeItemFlags(ItemFlag.values());

        if (template.getFlag("HIDE_ENCHANTS")) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        if (template.getFlag("HIDE_ATTRIBUTES") || forceHideAttributes) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        if (template.getFlag("HIDE_UNBREAKABLE")) meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        if (template.getFlag("HIDE_DESTROYS")) meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        if (template.getFlag("HIDE_PLACED_ON")) meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        if (template.getFlag("HIDE_ADDITIONAL_TOOLTIP")) meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        if (template.getFlag("HIDE_ARMOR_TRIM")) meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);

        if (template.hasGlow() && !meta.hasEnchants()) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        if (template.getAttributeMode() == BuffedItem.AttributeMode.DYNAMIC) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
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

    private EquipmentSlot getEquipmentSlot(String slot) {
        switch (slot.toUpperCase()) {
            case "MAIN_HAND": return EquipmentSlot.HAND;
            case "OFF_HAND": return EquipmentSlot.OFF_HAND;
            case "HELMET": return EquipmentSlot.HEAD;
            case "CHESTPLATE": return EquipmentSlot.CHEST;
            case "LEGGINGS": return EquipmentSlot.LEGS;
            case "BOOTS": return EquipmentSlot.FEET;
            default: return null;
        }
    }
}