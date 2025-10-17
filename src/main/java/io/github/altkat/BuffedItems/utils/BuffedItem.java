package io.github.altkat.BuffedItems.utils;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BuffedItem {
    private final String id;
    private final String displayName;
    private final List<String> lore;
    private Material material;
    private final boolean glow;
    private final Map<String, BuffedItemEffect> effects;
    private final Optional<String> permission;
    private boolean isValid = true;
    private final List<String> errorMessages = new ArrayList<>();

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

    public boolean isValid() {
        return isValid;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void addErrorMessage(String message) {
        this.isValid = false;
        this.errorMessages.add(message);
    }

    public void setMaterial(Material material) {
        this.material = material;
    }
}