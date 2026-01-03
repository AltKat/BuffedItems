package io.github.altkat.BuffedItems.utility.item.data.visual;

import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleDisplay;
import java.util.ArrayList;
import java.util.List;

public class CastVisuals {
    private final ActionBarSettings actionBar;
    private final BossBarSettings bossBar;
    private final TitleSettings title;
    private final SoundSettings sound;
    private final List<ParticleDisplay> particles;

    public CastVisuals(ActionBarSettings actionBar, BossBarSettings bossBar, TitleSettings title, SoundSettings sound, List<ParticleDisplay> particles) {
        this.actionBar = actionBar;
        this.bossBar = bossBar;
        this.title = title;
        this.sound = sound;
        this.particles = particles != null ? particles : new ArrayList<>();
    }

    public ActionBarSettings getActionBar() {
        return actionBar;
    }

    public BossBarSettings getBossBar() {
        return bossBar;
    }

    public TitleSettings getTitle() {
        return title;
    }

    public SoundSettings getSound() {
        return sound;
    }

    public List<ParticleDisplay> getParticles() {
        return particles;
    }
}
