package io.github.altkat.BuffedItems.utility.item.data;

public class TitleCooldownVisuals {
    private final boolean enabled;
    private final String message;
    private final String subtitle;

    public TitleCooldownVisuals(boolean enabled, String message, String subtitle) {
        this.enabled = enabled;
        this.message = message;
        this.subtitle = subtitle;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMessage() {
        return message;
    }

    public String getSubtitle() {
        return subtitle;
    }
}
