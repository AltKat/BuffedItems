package io.github.altkat.BuffedItems.Managers;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EffectManager {

    private final BuffedItems plugin;
    private static final int REFRESH_THRESHOLD_TICKS = 440;
    private static final int EFFECT_DURATION_TICKS = 600;

    public EffectManager(BuffedItems plugin) {
        this.plugin = plugin;
    }

    public void applyOrRefreshPotionEffects(Player player, Map<PotionEffectType, Integer> desiredEffects) {
        for (Map.Entry<PotionEffectType, Integer> entry : desiredEffects.entrySet()) {
            PotionEffectType type = entry.getKey();
            int amplifier = entry.getValue() - 1;

            PotionEffect existingEffect = player.getPotionEffect(type);

            if (existingEffect == null || existingEffect.getAmplifier() < amplifier || existingEffect.getDuration() < REFRESH_THRESHOLD_TICKS) {
                player.addPotionEffect(new PotionEffect(type, EFFECT_DURATION_TICKS, amplifier, true, false));
            }
        }
    }

    public void removeObsoletePotionEffects(Player player, Set<PotionEffectType> lastAppliedEffects, Set<PotionEffectType> desiredEffects) {
        for (PotionEffectType type : lastAppliedEffects) {
            if (!desiredEffects.contains(type)) {
                player.removePotionEffect(type);
            }
        }
    }

    public void applyAttributeEffects(Player player, String itemId, String slot, List<String> attributes) {
        EquipmentSlot equipmentSlot = getEquipmentSlot(slot);
        for (String attrString : attributes) {
            try {
                String[] parts = attrString.split(";");
                Attribute attribute = Attribute.valueOf(parts[0].toUpperCase());
                AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                double amount = Double.parseDouble(parts[2]);
                AttributeInstance instance = player.getAttribute(attribute);
                if (instance == null) continue;

                UUID modifierUUID = UUID.nameUUIDFromBytes(("buffeditems." + itemId + "." + slot + "." + attribute.name()).getBytes());
                String modifierName = "buffeditems." + itemId;
                AttributeModifier modifier = new AttributeModifier(modifierUUID, modifierName, amount, operation, equipmentSlot);

                plugin.getActiveAttributeManager().addModifier(player.getUniqueId(), modifier);
                if (!instance.getModifiers().contains(modifier)) {
                    instance.addModifier(modifier);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid attribute format for item '" + itemId + "': " + attrString);
            }
        }
    }

    public void clearAllAttributes(Player player) {
        List<AttributeModifier> modifiersToRemove = plugin.getActiveAttributeManager().getAndClearModifiers(player.getUniqueId());
        if (modifiersToRemove == null || modifiersToRemove.isEmpty()) return;

        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                for (AttributeModifier modifier : modifiersToRemove) {
                    instance.removeModifier(modifier);
                }
            }
        }
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