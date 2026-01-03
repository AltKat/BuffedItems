package io.github.altkat.BuffedItems.utility.item.data;

import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;

import java.util.Map;

public class PassiveEffects {
    private final Map<String, BuffedItemEffect> effects;
    private final BuffedItem.AttributeMode attributeMode;
    private final String passivePermission;

    public PassiveEffects(Map<String, BuffedItemEffect> effects, BuffedItem.AttributeMode attributeMode, String passivePermission) {
        this.effects = effects;
        this.attributeMode = attributeMode;
        this.passivePermission = passivePermission;
    }

    public Map<String, BuffedItemEffect> getEffects() {
        return effects;
    }

    public BuffedItem.AttributeMode getAttributeMode() {
        return attributeMode;
    }

    public String getPassivePermission() {
        return passivePermission;
    }
}