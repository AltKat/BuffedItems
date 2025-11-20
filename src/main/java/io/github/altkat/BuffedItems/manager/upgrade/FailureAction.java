package io.github.altkat.BuffedItems.manager.upgrade;

public enum FailureAction {
    LOSE_EVERYTHING("Lose Everything"),
    KEEP_BASE_ONLY("Keep Base Item"),
    KEEP_EVERYTHING("Keep Everything");

    private final String displayName;

    FailureAction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}