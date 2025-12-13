package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
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

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateAllItems(player);
            }
        }, 40L);

        if (RecipesConfig.get().getBoolean("settings.register-to-book", true)) {
            for (String recipeId : plugin.getCraftingManager().getRecipes().keySet()) {
                NamespacedKey key = new NamespacedKey(plugin, recipeId);
                try {
                    player.discoverRecipe(key);
                } catch (Exception ignored) {}
            }
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Join] Attribute cleanup check finished for " + player.getName());
        plugin.getEffectApplicatorTask().markPlayerForUpdate(player.getUniqueId());
    }

    private void updateAllItems(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        boolean changed = false;

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;

            ItemStack updated = plugin.getItemUpdater().updateItem(item, player);

            if (updated != null && !updated.isSimilar(item)) {
                contents[i] = updated;
                changed = true;
            }

        }

        if (changed) {
            player.getInventory().setContents(contents);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[LiveUpdate] Full inventory update for " + player.getName() + " on join (delayed).");
        }
    }
}