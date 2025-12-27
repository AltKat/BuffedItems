package io.github.altkat.BuffedItems.utility.item.data.visual;

public class SoundSettings {
    private final boolean enabled;
    private final String sound;
    private final int delay;

    public SoundSettings(boolean enabled, String sound, int delay) {
        this.enabled = enabled;
        this.sound = sound;
        this.delay = delay;
    }

    public boolean isEnabled() { return enabled; }
    public String getSound() { return sound; }
    public int getDelay() { return delay; }
}
