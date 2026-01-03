package io.github.altkat.BuffedItems.task;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cooldown.CooldownManager;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownVisualsTask extends BukkitRunnable {

    private final BuffedItems plugin;
    private final NamespacedKey nbtKey;
    private final Map<UUID, BossBar> activeBossBars = new HashMap<>();

    public CooldownVisualsTask(BuffedItems plugin) {
        this.plugin = plugin;
        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateVisuals(player);
        }
    }

    private void updateVisuals(Player player) {
        BuffedItem currentItem = null;
        ItemStack handItem = player.getInventory().getItemInMainHand();
        boolean showVisuals = false;
        double remaining = 0;
        double maxCooldown = 1;

        boolean useActionBar = false;
        boolean useBossBar = false;
        String bbColorStr = "RED";
        String bbStyleStr = "SOLID";

        if (handItem != null && handItem.hasItemMeta()) {
            String itemId = handItem.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
            if (itemId != null) {
                BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
                if (buffedItem != null && buffedItem.getActiveAbility().isEnabled()) {
                    CooldownManager cm = plugin.getCooldownManager();
                    currentItem = buffedItem;
                    if (cm.isOnCooldown(player, itemId)) {
                        remaining = cm.getRemainingSeconds(player, itemId);
                        maxCooldown = buffedItem.getActiveAbility().getCooldown();

                        useActionBar = buffedItem.getActiveAbility().getVisuals().getCooldown().getActionBar().isEnabled();
                        useBossBar = buffedItem.getActiveAbility().getVisuals().getCooldown().getBossBar().isEnabled();
                        bbColorStr = buffedItem.getActiveAbility().getVisuals().getCooldown().getBossBar().getColor();
                        bbStyleStr = buffedItem.getActiveAbility().getVisuals().getCooldown().getBossBar().getStyle();

                        showVisuals = true;
                    }
                }
            }
        }

        if (useActionBar && showVisuals) {
            String rawMsg = null;
            if (currentItem != null) rawMsg = currentItem.getActiveAbility().getVisuals().getCooldown().getActionBar().getMessage();

            if (rawMsg == null) {
                rawMsg = plugin.getConfig().getString("active-items.messages.cooldown-action-bar", "&fCD: {time}s");
            }

            String timeMsg = rawMsg.replace("{time}", String.format("%.1f", remaining));
            String finalMsg = plugin.getHookManager().processPlaceholders(player, timeMsg);
            player.sendActionBar(ConfigManager.fromLegacy(finalMsg));
        }

        UUID uuid = player.getUniqueId();
        if (useBossBar && showVisuals) {
            BossBar.Color color;
            try { color = BossBar.Color.valueOf(bbColorStr.toUpperCase()); } catch (Exception e) { color = BossBar.Color.RED; }

            BossBar.Overlay style;
            try {
                style = convertStyle(org.bukkit.boss.BarStyle.valueOf(bbStyleStr.toUpperCase()));
            } catch (Exception e) {
                style = BossBar.Overlay.PROGRESS;
            }

            String rawTitle = null;
            if (currentItem != null) rawTitle = currentItem.getActiveAbility().getVisuals().getCooldown().getBossBar().getMessage();

            if (rawTitle == null) {
                rawTitle = plugin.getConfig().getString("active-items.messages.cooldown-boss-bar", "CD: {time}s");
            }

            String timeTitle = rawTitle.replace("{time}", String.format("%.1f", remaining));
            String finalTitle = plugin.getHookManager().processPlaceholders(player, timeTitle);
            Component titleComp = ConfigManager.fromLegacy(finalTitle);

            float progress = (float) (remaining / maxCooldown);
            if (progress < 0f) progress = 0f;
            if (progress > 1f) progress = 1f;

            if (!activeBossBars.containsKey(uuid)) {
                BossBar bar = BossBar.bossBar(titleComp, progress, color, style);
                player.showBossBar(bar);
                activeBossBars.put(uuid, bar);
            } else {
                BossBar bar = activeBossBars.get(uuid);
                bar.name(titleComp);
                bar.progress(progress);
                bar.color(color);
                bar.overlay(style);
            }

        } else {
            if (activeBossBars.containsKey(uuid)) {
                BossBar bar = activeBossBars.get(uuid);
                player.hideBossBar(bar);
                activeBossBars.remove(uuid);
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                        () -> "[CooldownVisuals] Removed BossBar for player: " + player.getName());
            }
        }
    }

    private BossBar.Overlay convertStyle(org.bukkit.boss.BarStyle style) {
        return switch (style) {
            case SOLID -> BossBar.Overlay.PROGRESS;
            case SEGMENTED_6 -> BossBar.Overlay.NOTCHED_6;
            case SEGMENTED_10 -> BossBar.Overlay.NOTCHED_10;
            case SEGMENTED_12 -> BossBar.Overlay.NOTCHED_12;
            case SEGMENTED_20 -> BossBar.Overlay.NOTCHED_20;
        };
    }

    public void cleanup() {
        for (Map.Entry<UUID, BossBar> entry : activeBossBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        activeBossBars.clear();
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[CooldownVisuals] Cleaned up all BossBars during shutdown");
    }
}