package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Managers.EffectManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlayerJoinListener implements Listener {

    private final BuffedItems plugin;
    private static final int MAX_MANAGED_DURATION = 600;

    public PlayerJoinListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Join] Cleaning up any potentially stale tracked/orphaned attributes for " + player.getName());
        plugin.getEffectManager().clearAllAttributes(player);
        try {
            Collection<PotionEffect> activeEffects = player.getActivePotionEffects();
            if (!activeEffects.isEmpty()) {
                List<PotionEffectType> staleEffectsToClear = new ArrayList<>();

                for (PotionEffect effect : activeEffects) {
                    if (effect.getDuration() <= MAX_MANAGED_DURATION) {
                        staleEffectsToClear.add(effect.getType());
                    }
                }

                if (!staleEffectsToClear.isEmpty()) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Join] Found " + staleEffectsToClear.size() + " stale managed potion effect(s) for " + player.getName() + ". Clearing them...");
                    staleEffectsToClear.forEach(player::removePotionEffect);
                }
            }
        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[Join] Error while clearing stale potion effects for " + player.getName() + ": " + e.getMessage());
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Join] Attribute cleanup check finished for " + player.getName());
        plugin.getEffectApplicatorTask().markPlayerForUpdate(player.getUniqueId());
    }
}