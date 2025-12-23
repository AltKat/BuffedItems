package io.github.altkat.BuffedItems.utility.item.data;

public class AbilityVisuals {
    private final CooldownVisuals cooldown;

    public AbilityVisuals(CooldownVisuals cooldown) {
        this.cooldown = cooldown;
    }

    public CooldownVisuals getCooldown() {
        return cooldown;
    }
}
