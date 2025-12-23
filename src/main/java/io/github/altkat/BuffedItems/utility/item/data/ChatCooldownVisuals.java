package io.github.altkat.BuffedItems.utility.item.data;

public class ChatCooldownVisuals {
    private final boolean enabled;
    private final String message;

    public ChatCooldownVisuals(boolean enabled, String message) {
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
