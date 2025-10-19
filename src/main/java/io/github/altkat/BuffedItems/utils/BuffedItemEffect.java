package io.github.altkat.BuffedItems.utils;

import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;

public class BuffedItemEffect {

    private final Map<PotionEffectType, Integer> potionEffects;
    private final List<ParsedAttribute> parsedAttributes;

    public BuffedItemEffect(Map<PotionEffectType, Integer> potionEffects, List<ParsedAttribute> parsedAttributes) {
        this.potionEffects = potionEffects;
        this.parsedAttributes = parsedAttributes;
    }

    public Map<PotionEffectType, Integer> getPotionEffects() {
        return potionEffects;
    }

    public List<ParsedAttribute> getParsedAttributes() {
        return parsedAttributes;
    }
}