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

            if (existingEffect == null) {
                plugin.getLogger().fine("[Potion] Applying new effect to " + player.getName() + ": " + type.getName() + " " + (amplifier + 1));
                player.addPotionEffect(new PotionEffect(type, EFFECT_DURATION_TICKS, amplifier, true, false));
            } else if (existingEffect.getAmplifier() < amplifier) {
                plugin.getLogger().fine("[Potion] Upgrading effect for " + player.getName() + ": " + type.getName() + " " + (existingEffect.getAmplifier() + 1) + " → " + (amplifier + 1));
                player.addPotionEffect(new PotionEffect(type, EFFECT_DURATION_TICKS, amplifier, true, false));
            } else if (existingEffect.getDuration() < REFRESH_THRESHOLD_TICKS) {
                plugin.getLogger().fine("[Potion] Refreshing effect for " + player.getName() + ": " + type.getName() + " (duration: " + existingEffect.getDuration() + " → " + EFFECT_DURATION_TICKS + ")");
                player.addPotionEffect(new PotionEffect(type, EFFECT_DURATION_TICKS, amplifier, true, false));
            } else {
                plugin.getLogger().fine("[Potion] Effect already optimal for " + player.getName() + ": " + type.getName() + " (skipping)");
            }
        }
    }

    public void removeObsoletePotionEffects(Player player, Set<PotionEffectType> lastAppliedEffects, Set<PotionEffectType> desiredEffects) {
        for (PotionEffectType type : lastAppliedEffects) {
            if (!desiredEffects.contains(type)) {
                plugin.getLogger().fine("[Potion] Removing obsolete effect from " + player.getName() + ": " + type.getName());
                player.removePotionEffect(type);
            }
        }
    }

    public void applyAttributeEffects(Player player, String itemId, String slot, List<String> attributes) {
        plugin.getLogger().fine("[Attribute] Processing " + attributes.size() + " attributes for " + player.getName() + " (item: " + itemId + ", slot: " + slot + ")");

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

                plugin.getLogger().fine("[Attribute] Parsed: " + attribute.name() + " " + operation.name() + " " + amount);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid attribute format for item '" + itemId + "': " + attrString);
                continue;
            }

            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                plugin.getLogger().warning("Player " + player.getName() + " does not have attribute '" + attribute.name() + "' (item: " + itemId + ")");
                continue;
            }

            UUID modifierUUID = UUID.nameUUIDFromBytes(("buffeditems." + itemId + "." + slot + "." + attribute.name()).getBytes());
            String modifierName = "buffeditems." + itemId;
            AttributeModifier modifier = new AttributeModifier(modifierUUID, modifierName, amount, operation, equipmentSlot);

            boolean alreadyApplied = instance.getModifiers().stream()
                    .anyMatch(m -> m.getUniqueId().equals(modifierUUID));

            if (alreadyApplied) {
                plugin.getLogger().fine("[Attribute] Modifier already exists for " + player.getName() + ": " + attribute.name() + " (skipping)");
            } else {
                try {
                    instance.addModifier(modifier);
                    plugin.getActiveAttributeManager().addModifier(player.getUniqueId(), modifier);
                    plugin.getLogger().fine("[Attribute] Applied modifier to " + player.getName() + ": " + attribute.name() + " " + operation.name() + " " + amount);
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
        plugin.getLogger().fine("[Attribute] Clearing all attributes for " + player.getName());

        List<AttributeModifier> modifiersToRemove = plugin.getActiveAttributeManager()
                .getAndClearModifiers(player.getUniqueId());

        if (modifiersToRemove == null || modifiersToRemove.isEmpty()) {
            plugin.getLogger().fine("[Attribute] No registered modifiers to remove for " + player.getName());
            return;
        }

        plugin.getLogger().fine("[Attribute] Removing " + modifiersToRemove.size() + " registered modifiers from " + player.getName());

        Set<UUID> managedUUIDs = plugin.getItemManager().getManagedAttributeUUIDs();
        int removedCount = 0;
        int orphanCount = 0;

        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;

            for (AttributeModifier modifier : modifiersToRemove) {
                try {
                    instance.removeModifier(modifier);
                    removedCount++;
                    plugin.getLogger().fine("[Attribute] Removed modifier from " + attribute.name() + ": " + modifier.getUniqueId());
                } catch (Exception e) {
                    plugin.getLogger().fine("[Attribute] Could not remove modifier (already removed?): " + e.getMessage());
                }
            }

            List<AttributeModifier> orphanedModifiers = instance.getModifiers().stream()
                    .filter(mod -> managedUUIDs.contains(mod.getUniqueId()))
                    .collect(java.util.stream.Collectors.toList());

            for (AttributeModifier orphan : orphanedModifiers) {
                try {
                    instance.removeModifier(orphan);
                    orphanCount++;
                    plugin.getLogger().fine("[Attribute] Cleaned orphaned modifier from " + attribute.name() + ": " + orphan.getUniqueId());
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to remove orphaned modifier: " + e.getMessage());
                }
            }
        }

        if (orphanCount > 0) {
            plugin.getLogger().info("Cleaned " + orphanCount + " stale attribute modifier(s) for player: " + player.getName());
        }

        plugin.getLogger().fine("[Attribute] Cleanup complete for " + player.getName() + " (removed: " + removedCount + ", orphaned: " + orphanCount + ")");
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