package io.github.altkat.BuffedItems.utility.item.data.visual;

public class ActionBarSettings {
    private final VisualTriggerMode triggerMode;
    private final boolean enabled;
    private final String message;
    private final int duration; // in seconds
    private final int delay;    // in ticks

    public ActionBarSettings(VisualTriggerMode triggerMode, boolean enabled, String message, int duration, int delay) {
        this.triggerMode = triggerMode != null ? triggerMode : VisualTriggerMode.CONTINUOUS;
        this.enabled = enabled;
        this.message = message;
        this.duration = duration;
        this.delay = delay;
    }

    public VisualTriggerMode getTriggerMode() { return triggerMode; }
    public boolean isEnabled() { return enabled; }
    public String getMessage() { return message; }
    public int getDuration() { return duration; }
    public int getDelay() { return delay; }
}
