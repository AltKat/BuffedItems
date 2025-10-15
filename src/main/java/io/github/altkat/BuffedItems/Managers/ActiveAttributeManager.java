package io.github.altkat.BuffedItems.Managers;

import org.bukkit.attribute.AttributeModifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveAttributeManager {

    private final Map<UUID, List<AttributeModifier>> activeModifiers = new ConcurrentHashMap<>();


    public void addModifier(UUID playerUUID, AttributeModifier modifier) {
        activeModifiers.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(modifier);
    }

    public List<AttributeModifier> getAndClearModifiers(UUID playerUUID) {
        return activeModifiers.remove(playerUUID);
    }

    public void clearPlayer(UUID playerUUID) {
        activeModifiers.remove(playerUUID);
    }
}