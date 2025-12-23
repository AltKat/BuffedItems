package io.github.altkat.BuffedItems.utility.item.data;

public class AbilitySounds {
    private final String success;
    private final String costFail;
    private final String cooldown;

    public AbilitySounds(String success, String costFail, String cooldown) {
        this.success = success;
        this.costFail = costFail;
        this.cooldown = cooldown;
    }

    public String getSuccess() {
        return success;
    }

    public String getCostFail() {
        return costFail;
    }

    public String getCooldown() {
        return cooldown;
    }
}
