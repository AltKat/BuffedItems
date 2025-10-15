package io.github.altkat.BuffedItems.utils;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BuffedItem {
    private final String id;
    private final String displayName;
    private final List<String> lore;
    private final Material material;
    private final boolean glow;
    private final Map<String, BuffedItemEffect> effects;
    private final Optional<String> permission;

    public BuffedItem(String id, String displayName, List<String> lore, Material material, boolean glow, Map<String, BuffedItemEffect> effects, String permission) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore;
        this.material = material;
        this.glow = glow;
        this.effects = effects;
        this.permission = Optional.ofNullable(permission);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean hasGlow() {
        return glow;
    }

    public Map<String, BuffedItemEffect> getEffects() {
        return effects;
    }
    public Optional<String> getPermission() {
        return permission;
    }
}