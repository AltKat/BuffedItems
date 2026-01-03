package io.github.altkat.BuffedItems.utility.item.data.visual;

public class TitleSettings {
    private final boolean enabled;
    private final String header;
    private final String subtitle;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;
    private final int delay;

    public TitleSettings(boolean enabled, String header, String subtitle, int fadeIn, int stay, int fadeOut, int delay) {
        this.enabled = enabled;
        this.header = header;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
        this.delay = delay;
    }

    public boolean isEnabled() { return enabled; }
    public String getHeader() { return header; }
    public String getSubtitle() { return subtitle; }
    public int getFadeIn() { return fadeIn; }
    public int getStay() { return stay; }
    public int getFadeOut() { return fadeOut; }
    public int getDelay() { return delay; }
}
