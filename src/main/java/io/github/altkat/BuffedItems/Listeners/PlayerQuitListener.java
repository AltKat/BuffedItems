package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final BuffedItems plugin;

    public PlayerQuitListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getEffectManager().clearAllAttributes(player);
        plugin.getEffectApplicatorTask().playerQuit(player);
        plugin.getActiveAttributeManager().clearPlayer(player.getUniqueId());
        plugin.getDeathKeptItems().remove(player.getUniqueId());
        BuffedItems.removePlayerMenuUtility(player.getUniqueId());
        plugin.getEffectApplicatorTask().getManagedEffects(player.getUniqueId()).forEach(player::removePotionEffect);
    }
}