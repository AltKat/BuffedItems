package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Set;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final BuffedItems plugin;

    public PlayerJoinListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        ConfigManager.sendDebugMessage("[Join] Checking for stale modifiers on " + player.getName());

        Set<UUID> managedUUIDs = plugin.getItemManager().getManagedAttributeUUIDs();
        int cleanedCount = 0;

        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;

            for (AttributeModifier mod : instance.getModifiers()) {
                if (managedUUIDs.contains(mod.getUniqueId())) {
                    try {
                        instance.removeModifier(mod);
                        cleanedCount++;
                        ConfigManager.sendDebugMessage("[Join] Cleaned stale modifier: " + mod.getUniqueId() + " from " + attribute.name());
                    } catch (Exception e) {
                        ConfigManager.sendDebugMessage("[Join] Could not remove stale modifier: " + e.getMessage());
                    }
                }
            }
        }

        if (cleanedCount > 0) {
            plugin.getLogger().info("Cleaned " + cleanedCount + " stale modifier(s) from " + player.getName() + " on join");
        } else {
            ConfigManager.sendDebugMessage("[Join] No stale modifiers found for " + player.getName());
        }
    }
}