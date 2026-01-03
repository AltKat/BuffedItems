package io.github.altkat.BuffedItems.utility.item.data;

public class ActionBarCooldownVisuals {
    private final boolean enabled;
    private final String message;

    public ActionBarCooldownVisuals(boolean enabled, String message) {
        this.enabled = enabled;
        this.message = message;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMessage() {
        return message;
    }
}
