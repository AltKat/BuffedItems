package io.github.altkat.BuffedItems.manager.effect;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.utility.attribute.ParsedAttribute;
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

    public static UUID getUuidForItem(String itemId, String slot, Attribute attribute) {
        String uniqueKey = "buffeditems." + itemId + "." + slot + "." + attribute.name();
        return UUID.nameUUIDFromBytes(uniqueKey.getBytes());
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

    public void removeObsoletePotionEffects(Player player, Set<PotionEffectType> lastAppliedEffects,
                                            Set<PotionEffectType> desiredEffects, boolean debugTick) {
        Set<PotionEffectType> effectsToRemove = new HashSet<>(lastAppliedEffects);
        effectsToRemove.removeAll(desiredEffects);

        for (PotionEffectType type : effectsToRemove) {
            PotionEffect currentEffect = player.getPotionEffect(type);

            if (currentEffect != null) {
                boolean isManagedByBuffedItems = currentEffect.getDuration() <= EFFECT_DURATION_TICKS;

                if (isManagedByBuffedItems) {
                    player.removePotionEffect(type);
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () ->
                            "[Potion] REMOVED obsolete effect from " + player.getName() + ": " + type.getName() +
                                    " (duration was: " + currentEffect.getDuration() + ")");
                } else {
                    if (debugTick) {
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () ->
                                "[Potion] NOT removing effect " + type.getName() + " from " + player.getName() +
                                        " (duration: " + currentEffect.getDuration() + " suggests external source)");
                    }
                }
            }
        }
    }



    public void applySingleAttribute(Player player, ParsedAttribute parsedAttr, String slot) {
        UUID modifierUUID = parsedAttr.getUuid();
        Attribute attribute = parsedAttr.getAttribute();

        if (plugin.getActiveAttributeManager().hasModifier(player.getUniqueId(), attribute, modifierUUID)) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute-Fast Path] Modifier " + modifierUUID + " already tracked for " + player.getName() + " (skipping apply)");
            return;
        }

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            plugin.getLogger().warning("Player " + player.getName() + " does not have attribute '" + attribute.name() + "'");
            return;
        }

        AttributeModifier existingMod = null;
        for (AttributeModifier mod : instance.getModifiers()) {
            if (mod.getUniqueId().equals(modifierUUID)) {
                existingMod = mod;
                break;
            }
        }
        if (existingMod != null) {
            plugin.getActiveAttributeManager().addModifier(player.getUniqueId(), attribute, existingMod);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute-Fast Path] Modifier " + modifierUUID + " found on player but wasn't tracked. Re-tracking now.");
            return;
        }

        EquipmentSlot equipmentSlot = getEquipmentSlot(slot);
        String modifierName = "buffeditems." + modifierUUID;

        AttributeModifier modifier = new AttributeModifier(
                modifierUUID,
                modifierName,
                parsedAttr.getAmount(),
                parsedAttr.getOperation(),
                equipmentSlot
        );

        try {
            instance.addModifier(modifier);
            plugin.getActiveAttributeManager().addModifier(player.getUniqueId(), attribute, modifier);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Attribute-Fast Path] Applied modifier " + modifierUUID + " to " + player.getName() + ": " + attribute.name());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply single attribute modifier " + modifierUUID + ": " + e.getMessage());
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
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () ->
                            "[Attribute] REMOVED modifier " + modifierUUID + " from " + attribute.name() +
                                    " for player " + player.getName());
                } catch (Exception e) {
                    plugin.getLogger().warning("Error removing modifier " + modifierUUID + " for attribute " +
                            attribute.name() + " from player " + player.getName() + ": " + e.getMessage());
                }
            }
        }

        boolean removedFromTracking = plugin.getActiveAttributeManager().removeModifier(
                player.getUniqueId(), attribute, modifierUUID);
        if (removedFromTracking) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () ->
                    "[Attribute] Removed modifier " + modifierUUID + " from tracking for attribute " +
                            attribute.name() + " for player " + player.getName());
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