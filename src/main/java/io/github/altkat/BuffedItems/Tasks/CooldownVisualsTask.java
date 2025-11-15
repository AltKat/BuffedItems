package io.github.altkat.BuffedItems.Tasks;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Managers.CooldownManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
                if (buffedItem != null && buffedItem.isActiveMode()) {
                    CooldownManager cm = plugin.getCooldownManager();
                    currentItem = buffedItem;
                    if (cm.isOnCooldown(player, itemId)) {
                        remaining = cm.getRemainingSeconds(player, itemId);
                        maxCooldown = buffedItem.getCooldown();

                        useActionBar = buffedItem.isVisualActionBar();
                        useBossBar = buffedItem.isVisualBossBar();
                        bbColorStr = buffedItem.getBossBarColor();
                        bbStyleStr = buffedItem.getBossBarStyle();

                        showVisuals = true;
                    }
                }
            }
        }

        if (useActionBar && showVisuals) {
            String rawMsg = null;
            if (currentItem != null) rawMsg = currentItem.getCustomActionBarMsg();

            if (rawMsg == null) {
                rawMsg = plugin.getConfig().getString("active-items.messages.cooldown-action-bar", "&fCD: {time}s");
            }

            String finalMsg = rawMsg.replace("{time}", String.format("%.1f", remaining));
            player.sendActionBar(ConfigManager.fromLegacy(finalMsg));
        }

        UUID uuid = player.getUniqueId();
        if (useBossBar && showVisuals) {
            if (!activeBossBars.containsKey(uuid)) {
                BarColor color;
                try { color = BarColor.valueOf(bbColorStr); } catch (Exception e) { color = BarColor.RED; }

                BarStyle style;
                try { style = BarStyle.valueOf(bbStyleStr); } catch (Exception e) { style = BarStyle.SOLID; }

                BossBar bar = Bukkit.createBossBar("Cooldown", color, style);
                bar.addPlayer(player);
                activeBossBars.put(uuid, bar);
            }

            BossBar bar = activeBossBars.get(uuid);
            try { bar.setColor(BarColor.valueOf(bbColorStr)); } catch(Exception ignored){}
            try { bar.setStyle(BarStyle.valueOf(bbStyleStr)); } catch(Exception ignored){}

            double progress = remaining / maxCooldown;
            if (progress < 0) progress = 0;
            if (progress > 1) progress = 1;
            bar.setProgress(progress);

            String rawTitle = null;
            if (currentItem != null) rawTitle = currentItem.getCustomBossBarMsg();

            if (rawTitle == null) {
                rawTitle = plugin.getConfig().getString("active-items.messages.cooldown-boss-bar", "CD: {time}s");
            }

            String finalTitle = rawTitle.replace("{time}", String.format("%.1f", remaining));
            bar.setTitle(ConfigManager.toSection(ConfigManager.fromLegacy(finalTitle)));
        } else {
            if (activeBossBars.containsKey(uuid)) {
                BossBar bar = activeBossBars.get(uuid);
                bar.removePlayer(player);
                activeBossBars.remove(uuid);
            }
        }
    }

    public void cleanup() {
        for (BossBar bar : activeBossBars.values()) {
            bar.removeAll();
        }
        activeBossBars.clear();
    }
}