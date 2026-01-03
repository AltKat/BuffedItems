package io.github.altkat.BuffedItems.utility.item.data;

import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;

import java.util.List;

public class ActiveAbility {
    private final boolean enabled;
    private final int cooldown;
    private final int duration;
    private final String activePermission;
    private final List<String> commands;
    private final BuffedItemEffect effects;
    private final AbilityVisuals visuals;
    private final AbilitySounds sounds;
    private final List<ICost> costs;

    public ActiveAbility(boolean enabled, int cooldown, int duration, String activePermission, List<String> commands, BuffedItemEffect effects, AbilityVisuals visuals, AbilitySounds sounds, List<ICost> costs) {
        this.enabled = enabled;
        this.cooldown = cooldown;
        this.duration = duration;
        this.activePermission = activePermission;
        this.commands = commands;
        this.effects = effects;
        this.visuals = visuals;
        this.sounds = sounds;
        this.costs = costs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getDuration() {
        return duration;
    }

    public String getActivePermission() {
        return activePermission;
    }

    public List<String> getCommands() {
        return commands;
    }

    public BuffedItemEffect getEffects() {
        return effects;
    }

    public AbilityVisuals getVisuals() {
        return visuals;
    }

    public AbilitySounds getSounds() {
        return sounds;
    }

    public List<ICost> getCosts() {
        return costs;
    }
}