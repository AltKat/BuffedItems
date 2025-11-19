package io.github.altkat.BuffedItems.listener;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

/**
 * Effect type enumeration for generic handling
 */
public enum EffectType {
    POTION_EFFECT("potion effect", PotionEffectType.class),
    ATTRIBUTE("attribute", org.bukkit.attribute.Attribute.class),
    ENCHANTMENT("enchantment", Enchantment.class);

    private final String displayName;
    private final Class<?> validationClass;

    EffectType(String displayName, Class<?> validationClass) {
        this.displayName = displayName;
        this.validationClass = validationClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Class<?> getValidationClass() {
        return validationClass;
    }
}