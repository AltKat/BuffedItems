package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
        Set<UUID> managedUUIDs = plugin.getItemManager().getManagedAttributeUUIDs();

        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;

            instance.getModifiers().stream()
                    .filter(mod -> managedUUIDs.contains(mod.getUniqueId()))
                    .forEach(mod -> {
                        try {
                            instance.removeModifier(mod);
                            plugin.getLogger().fine("Cleaned stale modifier on join: " + mod.getUniqueId() + " from " + player.getName());
                        } catch (Exception e) {
                            // Ignore
                        }
                    });
        }
    }
}