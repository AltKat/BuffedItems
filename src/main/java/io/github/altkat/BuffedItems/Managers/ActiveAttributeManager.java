package io.github.altkat.BuffedItems.Managers;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveAttributeManager {

    private final Map<UUID, Map<Attribute, List<AttributeModifier>>> activePlayerModifiers = new ConcurrentHashMap<>();

    /**
     * Adds an active modifier for a player, associated with its Attribute.
     * @param playerUUID The UUID of the player.
     * @param attribute The Attribute the modifier belongs to.
     * @param modifier The AttributeModifier applied.
     */
    public void addModifier(UUID playerUUID, Attribute attribute, AttributeModifier modifier) {
        activePlayerModifiers
                .computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(attribute, k -> new ArrayList<>())
                .add(modifier);
    }

    /**
     * Retrieves all active modifiers applied by this plugin for a specific player.
     * @param playerUUID The UUID of the player.
     * @return A map where keys are Attributes and values are lists of applied AttributeModifiers,
     * or an empty map if the player has no active modifiers tracked.
     */
    public Map<Attribute, List<AttributeModifier>> getActiveModifiers(UUID playerUUID) {
        return activePlayerModifiers.getOrDefault(playerUUID, Collections.emptyMap());
    }

    /**
     * Retrieves and removes all active modifiers tracked for a specific player.
     * Used typically when the player leaves or attributes need a full reset.
     * @param playerUUID The UUID of the player.
     * @return A map of Attribute to List of AttributeModifiers that were removed,
     * or null if the player wasn't tracked.
     */
    public Map<Attribute, List<AttributeModifier>> getAndClearModifiers(UUID playerUUID) {
        return activePlayerModifiers.remove(playerUUID);
    }

    /**
     * Removes tracking information for a specific player without returning the modifiers.
     * Useful for cleanup on player quit if modifiers are handled elsewhere.
     * @param playerUUID The UUID of the player to clear.
     */
    public void clearPlayer(UUID playerUUID) {
        activePlayerModifiers.remove(playerUUID);
    }

    /**
     * Removes a specific modifier for a player and attribute.
     * @param playerUUID The UUID of the player.
     * @param attribute The Attribute the modifier belongs to.
     * @param modifierUUID The UUID of the modifier to remove.
     * @return true if the modifier was found and removed, false otherwise.
     */
    public boolean removeModifier(UUID playerUUID, Attribute attribute, UUID modifierUUID) {
        Map<Attribute, List<AttributeModifier>> playerMap = activePlayerModifiers.get(playerUUID);
        if (playerMap != null) {
            List<AttributeModifier> attributeList = playerMap.get(attribute);
            if (attributeList != null) {
                boolean removed = attributeList.removeIf(mod -> mod.getUniqueId().equals(modifierUUID));
                if (attributeList.isEmpty()) {
                    playerMap.remove(attribute);
                    if (playerMap.isEmpty()) {
                        activePlayerModifiers.remove(playerUUID);
                    }
                }
                return removed;
            }
        }
        return false;
    }

    /**
     * Checks if a specific modifier is currently tracked as active for a player.
     * @param playerUUID The UUID of the player.
     * @param attribute The Attribute the modifier belongs to.
     * @param modifierUUID The UUID of the modifier.
     * @return true if the modifier is tracked, false otherwise.
     */
    public boolean hasModifier(UUID playerUUID, Attribute attribute, UUID modifierUUID) {
        Map<Attribute, List<AttributeModifier>> playerMap = activePlayerModifiers.get(playerUUID);
        if (playerMap != null) {
            List<AttributeModifier> attributeList = playerMap.get(attribute);
            if (attributeList != null) {
                return attributeList.stream().anyMatch(mod -> mod.getUniqueId().equals(modifierUUID));
            }
        }
        return false;
    }
}