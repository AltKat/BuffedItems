package io.github.altkat.BuffedItems.utility.item.data;

public class BossBarCooldownVisuals {
    private final boolean enabled;
    private final String style;
    private final String color;
    private final String message;

    public BossBarCooldownVisuals(boolean enabled, String style, String color, String message) {
        this.enabled = enabled;
        this.style = style;
        this.color = color;
        this.message = message;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getStyle() {
        return style;
    }

    public String getColor() {
        return color;
    }

    public String getMessage() {
        return message;
    }
}
