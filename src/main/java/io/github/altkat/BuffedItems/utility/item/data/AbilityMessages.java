package io.github.altkat.BuffedItems.utility.item.data;

public class AbilityMessages {
    private final String cooldownChat;
    private final String cooldownTitle;
    private final String cooldownSubtitle;
    private final String cooldownActionBar;
    private final String cooldownBossBar;

    public AbilityMessages(String cooldownChat, String cooldownTitle, String cooldownSubtitle, String cooldownActionBar, String cooldownBossBar) {
        this.cooldownChat = cooldownChat;
        this.cooldownTitle = cooldownTitle;
        this.cooldownSubtitle = cooldownSubtitle;
        this.cooldownActionBar = cooldownActionBar;
        this.cooldownBossBar = cooldownBossBar;
    }

    public String getCooldownChat() {
        return cooldownChat;
    }

    public String getCooldownTitle() {
        return cooldownTitle;
    }

    public String getCooldownSubtitle() {
        return cooldownSubtitle;
    }

    public String getCooldownActionBar() {
        return cooldownActionBar;
    }

    public String getCooldownBossBar() {
        return cooldownBossBar;
    }
}