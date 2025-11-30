package io.github.altkat.BuffedItems.utility.set;

import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;

public class SetBonus {
    private final int requiredPieces;
    private final BuffedItemEffect effects;

    public SetBonus(int requiredPieces, BuffedItemEffect effects) {
        this.requiredPieces = requiredPieces;
        this.effects = effects;
    }

    public int getRequiredPieces() {
        return requiredPieces;
    }

    public BuffedItemEffect getEffects() {
        return effects;
    }
}