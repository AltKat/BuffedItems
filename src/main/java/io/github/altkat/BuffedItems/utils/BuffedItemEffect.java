package io.github.altkat.BuffedItems.utils;

import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

public class BuffedItemEffect {

    private final Map<PotionEffectType, Integer> potionEffects;
    private final List<String> attributes;

    public BuffedItemEffect(Map<PotionEffectType, Integer> potionEffects, List<String> attributes) {
        this.potionEffects = potionEffects;
        this.attributes = attributes;
    }

    public Map<PotionEffectType, Integer> getPotionEffects() {
        return potionEffects;
    }

    public List<String> getAttributes() {
        return attributes;
    }
}