package io.github.altkat.BuffedItems.utility.item.data;

import io.github.altkat.BuffedItems.utility.item.DepletionAction;

import java.util.List;

public class UsageDetails {
    private final int maxUses;
    private final DepletionAction depletionAction;
    private final String transformId;
    private final List<String> depletionCommands;
    private final String usageLore;
    private final String depletedLore;
    private final String depletedMessage;
    private final String depletionNotification;
    private final String depletionTransformMessage;
    private final String depletionSound;
    private final String depletedTrySound;

    public UsageDetails(int maxUses, DepletionAction depletionAction, String transformId, List<String> depletionCommands, String usageLore, String depletedLore, String depletedMessage, String depletionNotification, String depletionTransformMessage, String depletionSound, String depletedTrySound) {
        this.maxUses = maxUses;
        this.depletionAction = depletionAction;
        this.transformId = transformId;
        this.depletionCommands = depletionCommands;
        this.usageLore = usageLore;
        this.depletedLore = depletedLore;
        this.depletedMessage = depletedMessage;
        this.depletionNotification = depletionNotification;
        this.depletionTransformMessage = depletionTransformMessage;
        this.depletionSound = depletionSound;
        this.depletedTrySound = depletedTrySound;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public DepletionAction getDepletionAction() {
        return depletionAction;
    }

    public String getTransformId() {
        return transformId;
    }

    public List<String> getDepletionCommands() {
        return depletionCommands;
    }

    public String getUsageLore() {
        return usageLore;
    }

    public String getDepletedLore() {
        return depletedLore;
    }

    public String getDepletedMessage() {
        return depletedMessage;
    }

    public String getDepletionNotification() {
        return depletionNotification;
    }

    public String getDepletionTransformMessage() {
        return depletionTransformMessage;
    }

    public String getDepletionSound() {
        return depletionSound;
    }

    public String getDepletedTrySound() {
        return depletedTrySound;
    }
}
