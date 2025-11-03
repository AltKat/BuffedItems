package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class PlayerQuitListener implements Listener {

    private final BuffedItems plugin;

    public PlayerQuitListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Quit] Cleaning up player: " + player.getName());

        plugin.getEffectManager().clearAllAttributes(player);

        Set<PotionEffectType> effectsToClear = new HashSet<>(plugin.getEffectApplicatorTask().getManagedEffects(player.getUniqueId()));
        int effectCount = effectsToClear.size();
        plugin.getEffectApplicatorTask().playerQuit(player);
        plugin.getActiveAttributeManager().clearPlayer(player.getUniqueId());
        plugin.getDeathKeptItems().remove(player.getUniqueId());
        BuffedItems.removePlayerMenuUtility(player.getUniqueId());
        plugin.getInventoryChangeListener().clearPlayerData(player.getUniqueId());

        try {
            if (player.isOnline()) {
                effectsToClear.forEach(player::removePotionEffect);
            }
        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[Quit] Error while removing potion effects for " + player.getName() + " (player likely already offline): " + e.getMessage());
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Quit] Cleanup complete for " + player.getName() + " (removed " + effectCount + " potion effects)");
    }
}