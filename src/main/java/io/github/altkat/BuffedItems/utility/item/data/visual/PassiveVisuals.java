package io.github.altkat.BuffedItems.utility.item.data.visual;

public class PassiveVisuals {
    
    private final VisualTriggerMode triggerMode;
    private final ActionBarSettings actionBar;
    private final BossBarSettings bossBar;
    private final TitleSettings title;
    private final SoundSettings sound;

    public PassiveVisuals(VisualTriggerMode triggerMode, ActionBarSettings actionBar, BossBarSettings bossBar, TitleSettings title, SoundSettings sound) {
        this.triggerMode = triggerMode;
        this.actionBar = actionBar;
        this.bossBar = bossBar;
        this.title = title;
        this.sound = sound;
    }

    public VisualTriggerMode getTriggerMode() { return triggerMode; }
    public ActionBarSettings getActionBar() { return actionBar; }
    public BossBarSettings getBossBar() { return bossBar; }
    public TitleSettings getTitle() { return title; }
    public SoundSettings getSound() { return sound; }
}
