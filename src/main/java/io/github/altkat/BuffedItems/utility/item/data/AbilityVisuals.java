package io.github.altkat.BuffedItems.utility.item.data;

import io.github.altkat.BuffedItems.utility.item.data.visual.CastVisuals;
import io.github.altkat.BuffedItems.utility.item.data.CooldownVisuals;

public class AbilityVisuals {
    private final CooldownVisuals cooldown;
    private final CastVisuals cast;

    public AbilityVisuals(CooldownVisuals cooldown, CastVisuals cast) {
        this.cooldown = cooldown;
        this.cast = cast;
    }

    public CooldownVisuals getCooldown() {
        return cooldown;
    }

    public CastVisuals getCast() {
        return cast;
    }
}
