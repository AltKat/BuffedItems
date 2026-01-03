package io.github.altkat.BuffedItems.utility.item.data;

public class CooldownVisuals {
    private final ChatCooldownVisuals chat;
    private final TitleCooldownVisuals title;
    private final ActionBarCooldownVisuals actionBar;
    private final BossBarCooldownVisuals bossBar;

    public CooldownVisuals(ChatCooldownVisuals chat, TitleCooldownVisuals title, ActionBarCooldownVisuals actionBar, BossBarCooldownVisuals bossBar) {
        this.chat = chat;
        this.title = title;
        this.actionBar = actionBar;
        this.bossBar = bossBar;
    }

    public ChatCooldownVisuals getChat() {
        return chat;
    }

    public TitleCooldownVisuals getTitle() {
        return title;
    }

    public ActionBarCooldownVisuals getActionBar() {
        return actionBar;
    }

    public BossBarCooldownVisuals getBossBar() {
        return bossBar;
    }
}
