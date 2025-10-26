package io.github.altkat.BuffedItems.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.*;

public class BuffedItem {
    private final String id;
    private final String displayName;
    private final List<String> lore;
    private Material material;
    private final boolean glow;
    private final Map<String, BuffedItemEffect> effects;
    private final Optional<String> permission;
    private final Map<Enchantment, Integer> enchantments;

    private boolean isValid = true;
    private final List<String> errorMessages = new ArrayList<>();

    private final Map<String, Boolean> flags;

    public BuffedItem(String id, String displayName, List<String> lore, Material material, boolean glow, Map<String, BuffedItemEffect> effects, String permission, Map<String, Boolean> flags, Map<Enchantment, Integer> enchantments) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore;
        this.material = material;
        this.glow = glow;
        this.effects = effects;
        this.permission = Optional.ofNullable(permission);
        this.flags = (flags != null) ? flags : new HashMap<>();
        this.enchantments = (enchantments != null) ? enchantments : new HashMap<>();
    }

    public Map<String, Boolean> getFlags() {
        return flags;
    }

    private static final Set<String> DEFAULT_TRUE_FLAGS;
    static {
        DEFAULT_TRUE_FLAGS = new HashSet<>(Arrays.asList(
                //"UNBREAKABLE",
                "HIDE_ATTRIBUTES",
                //"HIDE_ENCHANTS",
                "HIDE_UNBREAKABLE",
                "HIDE_POTION_EFFECTS",
                "HIDE_DESTROYS",
                "HIDE_PLACED_ON",
                //"PREVENT_ANVIL_USE",
                //"PREVENT_ENCHANT_TABLE",
                //"PREVENT_SMITHING_USE",
                "PREVENT_CRAFTING_USE",
                //"PREVENT_DROP",
                //"PREVENT_CONSUME"
                "PREVENT_PLACEMENT"
                //"PREVENT_DEATH_DROP",
                //"PREVENT_INTERACT"
        ));
    }

    public boolean getFlag(String flagName) {
        String id = flagName.toUpperCase();

        if (flags.containsKey(id)) {
            return flags.getOrDefault(id, false);
        }
        return DEFAULT_TRUE_FLAGS.contains(id);
    }

    public BuffedItem(String id, String displayName, List<String> lore, Material material, boolean glow, Map<String, BuffedItemEffect> effects, String permission) {
        this(id, displayName, lore, material, glow, effects, permission, null, null);
    }

    public BuffedItem(String id, String displayName, List<String> lore, Material material, boolean glow, Map<String, BuffedItemEffect> effects, String permission, Map<String, Boolean> flags) {
        this(id, displayName, lore, material, glow, effects, permission, flags, null);
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

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }
}