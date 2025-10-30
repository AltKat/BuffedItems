package io.github.altkat.BuffedItems.Managers;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class EffectManager {

    private final BuffedItems plugin;
    private static final int REFRESH_THRESHOLD_TICKS = 440;
    private static final int EFFECT_DURATION_TICKS = 600;

    public EffectManager(BuffedItems plugin) {
        this.plugin = plugin;
    }

    public void applyOrRefreshPotionEffects(Player player, Map<PotionEffectType, Integer> desiredEffects, boolean debugTick) {
        final boolean showIcon = ConfigManager.shouldShowPotionIcons();

        for (Map.Entry<PotionEffectType, Integer> entry : desiredEffects.entrySet()) {
            PotionEffectType type = entry.getKey();
            int amplifier = entry.getValue() - 1;

            PotionEffect existingEffect = player.getPotionEffect(type);

            if (existingEffect == null) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Potion] Applying new effect to " + player.getName() + ": " + type.getName() + " " + (amplifier + 1));
                player.addPotionEffect(new PotionEffect(type, EFFECT_DURATION_TICKS, amplifier, true, false, showIcon));
            } else if (existingEffect.getAmplifier() < amplifier || (existingEffect.getAmplifier() == amplifier && existingEffect.getDuration() < REFRESH_THRESHOLD_TICKS)) {
                if (existingEffect.getAmplifier() < amplifier) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Potion] Upgrading effect for " + player.getName() + ": " + type.getName() + " " + (existingEffect.getAmplifier() + 1) + " -> " + (amplifier + 1));
                } else {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Potion] Refreshing effect for " + player.getName() + ": " + type.getName() + " (duration: " + existingEffect.getDuration() + " -> " + EFFECT_DURATION_TICKS + ")");
                }
                player.addPotionEffect(new PotionEffect(type, EFFECT_DURATION_TICKS, amplifier, true, false, showIcon));
            } else {
                if(debugTick) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Potion] Effect already optimal for " + player.getName() + ": " + type.getName() + " (skipping)");
                }
            }
        }
    }

    public void removeObsoletePotionEffects(Player player, Set<PotionEffectType> lastAppliedEffects, Set<PotionEffectType> desiredEffects, boolean debugTick) {
        Set<PotionEffectType> effectsToRemove = new HashSet<>(lastAppliedEffects);
        effectsToRemove.removeAll(desiredEffects);

        for (PotionEffectType type : effectsToRemove) {
            PotionEffect currentEffect = player.getPotionEffect(type);
            if (currentEffect != null && currentEffect.getDuration() <= EFFECT_DURATION_TICKS) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Potion] Removing obsolete effect from " + player.getName() + ": " + type.getName());
                player.removePotionEffect(type);
            } else if (currentEffect != null && debugTick) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Potion] Not removing effect " + type.getName() + " from " + player.getName() + " as its duration ("+currentEffect.getDuration()+") suggests it's not managed by BuffedItems.");
            }
        }
    }


    public void applyAttributeEffects(Player player, String itemId, String slot, List<String> attributes) {
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Processing " + attributes.size() + " attributes for " + player.getName() + " (item: " + itemId + ", slot: " + slot + ")");

        EquipmentSlot equipmentSlot = getEquipmentSlot(slot);

        for (String attrString : attributes) {
            Attribute attribute;
            AttributeModifier.Operation operation;
            double amount;
            UUID modifierUUID;

            try {
                String[] parts = attrString.split(";");
                if (parts.length != 3) throw new IllegalArgumentException("Attribute string must have 3 parts separated by ';'. Found: " + attrString);
                attribute = Attribute.valueOf(parts[0].toUpperCase());
                operation = AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                amount = Double.parseDouble(parts[2]);
                modifierUUID = UUID.nameUUIDFromBytes(("buffeditems." + itemId + "." + slot + "." + attribute.name()).getBytes());
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Parsed: " + attribute.name() + " " + operation.name() + " " + amount + " UUID: " + modifierUUID);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid attribute format for item '" + itemId + "' in slot '" + slot + "': " + attrString + " | Error: " + e.getMessage());
                continue;
            }

            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                plugin.getLogger().warning("Player " + player.getName() + " does not have attribute '" + attribute.name() + "' (item: " + itemId + ")");
                continue;
            }

            if (plugin.getActiveAttributeManager().hasModifier(player.getUniqueId(), attribute, modifierUUID)) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Modifier " + modifierUUID + " already tracked for " + player.getName() + " on " + attribute.name() + " (skipping apply)");
                continue;
            }

            AttributeModifier existingMod = null;
            for (AttributeModifier mod : instance.getModifiers()) {
                if (mod.getUniqueId().equals(modifierUUID)) {
                    existingMod = mod;
                    break;
                }
            }
            boolean alreadyApplied = (existingMod != null);

            if (alreadyApplied) {
                plugin.getActiveAttributeManager().addModifier(player.getUniqueId(), attribute, existingMod);
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Modifier " + modifierUUID + " found on player but wasn't tracked. Re-tracking now.");
            } else {

                String modifierName = "buffeditems." + itemId + "." + slot;
                AttributeModifier modifier = new AttributeModifier(
                        modifierUUID,
                        modifierName,
                        amount,
                        operation,
                        equipmentSlot
                );

                try {
                    instance.addModifier(modifier);
                    plugin.getActiveAttributeManager().addModifier(player.getUniqueId(), attribute, modifier);
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Applied modifier " + modifierUUID + " to " + player.getName() + ": " + attribute.name() + " " + operation.name() + " " + amount);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Attempted to add duplicate attribute modifier UUID: " + modifierUUID + " for " + attribute.name() + " on player " + player.getName() + ". " + e.getMessage());
                } catch (Exception e) {
                    plugin.getLogger().severe("CRITICAL: Failed to apply attribute modifier!");
                    plugin.getLogger().severe("  Item: " + itemId + ", Slot: " + slot + ", Attribute String: " + attrString);
                    plugin.getLogger().severe("  Player: " + player.getName());
                    plugin.getLogger().severe("  Error: " + e.getMessage());
                    plugin.getLogger().severe("Stack trace for modifier application failure:");
                    for(StackTraceElement element : e.getStackTrace()) {
                        plugin.getLogger().severe("    at " + element.toString());
                    }
                }
            }
        }
    }

    public void clearAllAttributes(Player player) {
        UUID playerUUID = player.getUniqueId();
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Attribute] Clearing all tracked attributes for " + player.getName());

        Map<Attribute, List<AttributeModifier>> trackedModifiersMap = plugin.getActiveAttributeManager().getAndClearModifiers(playerUUID);

        if (trackedModifiersMap == null || trackedModifiersMap.isEmpty()) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] No tracked modifiers to remove for " + player.getName());
            cleanupOrphanedModifiers(player);
            return;
        }

        int attemptedRemoveCount = 0;
        for (Map.Entry<Attribute, List<AttributeModifier>> entry : trackedModifiersMap.entrySet()) {
            Attribute attribute = entry.getKey();
            List<AttributeModifier> modifiersToRemove = entry.getValue();
            AttributeInstance instance = player.getAttribute(attribute);

            if (instance == null) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Player " + player.getName() + " missing attribute " + attribute.name() + " during clear operation.");
                continue;
            }

            for (AttributeModifier modifier : modifiersToRemove) {
                try {
                    instance.removeModifier(modifier);
                    attemptedRemoveCount++;
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Attempted removal of tracked modifier " + modifier.getUniqueId() + " from " + attribute.name());
                } catch (Exception e) {
                    plugin.getLogger().warning("Error removing modifier " + modifier.getUniqueId() + " for attribute " + attribute.name() + " from player " + player.getName() + ": " + e.getMessage());
                }
            }
        }

        final int finalAttemptedRemoveCount = attemptedRemoveCount;
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Attribute] Finished clearing tracked attributes for " + player.getName() + " (Attempted removals: " + finalAttemptedRemoveCount + ")");
        cleanupOrphanedModifiers(player);
    }

    public void removeAttributeModifier(Player player, Attribute attribute, UUID modifierUUID) {
        AttributeInstance instance = player.getAttribute(attribute);
        AttributeModifier modifierToRemove = null;

        if (instance != null) {
            for (AttributeModifier mod : instance.getModifiers()) {
                if (mod.getUniqueId().equals(modifierUUID)) {
                    modifierToRemove = mod;
                    break;
                }
            }

            if (modifierToRemove != null) {
                try {
                    instance.removeModifier(modifierToRemove);
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Attempted removal of modifier " + modifierUUID + " from " + attribute.name() + " instance for player " + player.getName());
                } catch (Exception e){
                    plugin.getLogger().warning("Error removing modifier " + modifierUUID + " for attribute " + attribute.name() + " from player instance " + player.getName() + ": " + e.getMessage());
                }
            } else {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Modifier " + modifierUUID + " for " + attribute.name() + " not found on player instance " + player.getName() + " during specific removal attempt.");
            }
        } else {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Player " + player.getName() + " missing attribute " + attribute.name() + " during specific modifier removal attempt.");
        }

        boolean removedFromTracking = plugin.getActiveAttributeManager().removeModifier(player.getUniqueId(), attribute, modifierUUID);
        if(removedFromTracking) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Removed modifier " + modifierUUID + " from tracking for attribute " + attribute.name() + " for player " + player.getName());
        }
    }


    private void cleanupOrphanedModifiers(Player player) {
        Set<UUID> managedUUIDs = plugin.getItemManager().getManagedAttributeUUIDs();
        if (managedUUIDs.isEmpty()) return;

        int orphanAttemptCount = 0;
        UUID playerUUID = player.getUniqueId();

        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;

            List<AttributeModifier> orphansToRemove = new ArrayList<>();
            for (AttributeModifier modifier : instance.getModifiers()) {
                UUID currentUUID = modifier.getUniqueId();
                if (managedUUIDs.contains(currentUUID) &&
                        !plugin.getActiveAttributeManager().hasModifier(playerUUID, attribute, currentUUID)) {
                    orphansToRemove.add(modifier);
                }
            }

            for (AttributeModifier orphan : orphansToRemove) {
                try {
                    instance.removeModifier(orphan);
                    orphanAttemptCount++;
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute] Attempted cleanup of orphaned modifier from " + attribute.name() + ": " + orphan.getUniqueId());
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to remove orphaned modifier " + orphan.getUniqueId() + " for attribute " + attribute.name() + " from player " + player.getName() + ": " + e.getMessage());
                }
            }
        }

        if (orphanAttemptCount > 0) {
            final int finalOrphanAttemptCount = orphanAttemptCount;
            String playerName = player.getName();
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Attribute] Attempted to clean " + finalOrphanAttemptCount + " orphaned modifier(s) for player " + playerName);
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
            case "INVENTORY":
            default: return null;
        }
    }
}