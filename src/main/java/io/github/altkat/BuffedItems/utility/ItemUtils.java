package io.github.altkat.BuffedItems.utility;

import io.github.altkat.BuffedItems.utility.attribute.ParsedAttribute;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class ItemUtils {

    public static final EquipmentSlot[] VALID_SLOTS = {
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * Applies attributes (modifiers) to the ItemMeta based on the BuffedItem configuration.
     * Also handles the "Dummy Attribute" logic for hiding vanilla stats.
     *
     * @param item The BuffedItem template.
     * @param meta The ItemMeta to modify.
     */
    public static void applyAttributes(BuffedItem item, ItemMeta meta) {
        // 1. Clear existing attributes first
        meta.setAttributeModifiers(null);

        boolean forceHideAttributes = false;

        if (item.getPassiveEffects().getAttributeMode() == BuffedItem.AttributeMode.STATIC) {
            boolean hasAnyAttribute = false;

            for (Map.Entry<String, BuffedItemEffect> effectEntry : item.getPassiveEffects().getEffects().entrySet()) {
                String slotKey = effectEntry.getKey().toUpperCase();
                BuffedItemEffect itemEffect = effectEntry.getValue();

                EquipmentSlot equipmentSlot = getEquipmentSlot(slotKey);

                if (equipmentSlot != null) {
                    for (ParsedAttribute parsedAttr : itemEffect.getParsedAttributes()) {
                        AttributeModifier modifier = new AttributeModifier(
                                parsedAttr.getUuid(),
                                "buffeditems." + item.getId() + "." + slotKey,
                                parsedAttr.getAmount(),
                                parsedAttr.getOperation(),
                                equipmentSlot
                        );
                        meta.addAttributeModifier(parsedAttr.getAttribute(), modifier);
                        hasAnyAttribute = true;
                    }
                }
            }

            // If STATIC mode but no attributes are defined, add dummies to force HIDE_ATTRIBUTES to work properly
            if (!hasAnyAttribute) {
                applyDummyAttributes(item, meta);
                forceHideAttributes = true;
            }

        } else {
            // DYNAMIC Mode: Attributes are handled by Task, so only add dummies to item
            // to allow HIDE_ATTRIBUTES to function (hiding vanilla damage text etc.)
            applyDummyAttributes(item, meta);
            forceHideAttributes = true;
        }

        // Apply HIDE_ATTRIBUTES flag if configured OR if we forced it via dummies
        if (item.getFlag("HIDE_ATTRIBUTES") || forceHideAttributes) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
    }

    private static void applyDummyAttributes(BuffedItem item, ItemMeta meta) {
        Attribute dummyAttr = Attribute.GENERIC_LUCK;
        for (EquipmentSlot slot : VALID_SLOTS) {
            UUID dummyUUID = UUID.nameUUIDFromBytes(
                    ("buffeditems.dummy.static." + item.getId() + "." + slot.name()).getBytes(StandardCharsets.UTF_8)
            );

            AttributeModifier dummyMod = new AttributeModifier(
                    dummyUUID,
                    "buffeditems.dummy." + slot.name(),
                    0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    slot
            );
            meta.addAttributeModifier(dummyAttr, dummyMod);
        }
    }

    public static EquipmentSlot getEquipmentSlot(String slot) {
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