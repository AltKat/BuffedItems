package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final BuffedItems plugin;

    public PlayerJoinListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigManager.sendDebugMessage("[Join] Cleaning up any potentially stale tracked/orphaned attributes for " + player.getName());
        plugin.getEffectManager().clearAllAttributes(player);
        ConfigManager.sendDebugMessage("[Join] Attribute cleanup check finished for " + player.getName());
    }
}