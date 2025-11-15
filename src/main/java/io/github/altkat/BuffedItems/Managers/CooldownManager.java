package io.github.altkat.BuffedItems.Managers;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public boolean isOnCooldown(Player player, String itemId) {
        if (!cooldowns.containsKey(player.getUniqueId())) return false;

        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (!playerCooldowns.containsKey(itemId)) return false;

        return playerCooldowns.get(itemId) > System.currentTimeMillis();
    }

    public void setCooldown(Player player, String itemId, int seconds) {
        long expiry = System.currentTimeMillis() + (seconds * 1000L);

        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(itemId, expiry);
    }

    public double getRemainingSeconds(Player player, String itemId) {
        if (!isOnCooldown(player, itemId)) return 0;

        long expiry = cooldowns.get(player.getUniqueId()).get(itemId);
        return (expiry - System.currentTimeMillis()) / 1000.0;
    }

    public void clearPlayer(Player player) {
        cooldowns.remove(player.getUniqueId());
    }
}