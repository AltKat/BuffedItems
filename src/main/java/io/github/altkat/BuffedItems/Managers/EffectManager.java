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
            Attribute attribute;
            AttributeModifier.Operation operation;
            double amount;

            try {
                String[] parts = attrString.split(";");
                attribute = Attribute.valueOf(parts[0].toUpperCase());
                operation = AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                amount = Double.parseDouble(parts[2]);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid attribute format for item '" + itemId + "': " + attrString);
                continue;
            }

            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                plugin.getLogger().warning("Player does not have attribute '" + attribute.name() + "' (item: " + itemId + ")");
                continue;
            }

            UUID modifierUUID = UUID.nameUUIDFromBytes(("buffeditems." + itemId + "." + slot + "." + attribute.name()).getBytes());
            String modifierName = "buffeditems." + itemId;
            AttributeModifier modifier = new AttributeModifier(modifierUUID, modifierName, amount, operation, equipmentSlot);

            boolean alreadyApplied = instance.getModifiers().stream()
                    .anyMatch(m -> m.getUniqueId().equals(modifierUUID));

            if (!alreadyApplied) {
                try {
                    instance.addModifier(modifier);
                    plugin.getActiveAttributeManager().addModifier(player.getUniqueId(), modifier);
                } catch (Exception e) {
                    plugin.getLogger().severe("CRITICAL: Failed to apply attribute modifier!");
                    plugin.getLogger().severe("  Item: " + itemId);
                    plugin.getLogger().severe("  Attribute: " + attribute.name());
                    plugin.getLogger().severe("  Player: " + player.getName());
                    plugin.getLogger().severe("  Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void clearAllAttributes(Player player) {
        List<AttributeModifier> modifiersToRemove = plugin.getActiveAttributeManager()
                .getAndClearModifiers(player.getUniqueId());

        Set<UUID> managedUUIDs = plugin.getItemManager().getManagedAttributeUUIDs();
        boolean hadOrphans = false;

        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;

            if (modifiersToRemove != null) {
                for (AttributeModifier modifier : modifiersToRemove) {
                    try {
                        instance.removeModifier(modifier);
                    } catch (Exception e) {
                        plugin.getLogger().fine("Could not remove modifier (already removed?): " + e.getMessage());
                    }
                }
            }

            List<AttributeModifier> orphanedModifiers = instance.getModifiers().stream()
                    .filter(mod -> managedUUIDs.contains(mod.getUniqueId()))
                    .collect(java.util.stream.Collectors.toList());

            for (AttributeModifier orphan : orphanedModifiers) {
                try {
                    instance.removeModifier(orphan);
                    hadOrphans = true;
                    plugin.getLogger().fine("Cleaned orphaned modifier: " + orphan.getUniqueId());
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to remove orphaned modifier: " + e.getMessage());
                }
            }
        }

        if (hadOrphans) {
            plugin.getLogger().info("Cleaned stale attribute modifiers for player: " + player.getName());
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