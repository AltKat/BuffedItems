package io.github.altkat.BuffedItems.utility.item.data.visual;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

public class BossBarSettings {
    private final boolean enabled;
    private final String title;
    private final BarColor color;
    private final BarStyle style;
    private final int duration; // in seconds, for ON_EQUIP
    private final int delay;    // in ticks

    public BossBarSettings(boolean enabled, String title, BarColor color, BarStyle style, int duration, int delay) {
        this.enabled = enabled;
        this.title = title;
        this.color = color;
        this.style = style;
        this.duration = duration;
        this.delay = delay;
    }

    public boolean isEnabled() { return enabled; }
    public String getTitle() { return title; }
    public BarColor getColor() { return color; }
    public BarStyle getStyle() { return style; }
    public int getDuration() { return duration; }
    public int getDelay() { return delay; }
}
