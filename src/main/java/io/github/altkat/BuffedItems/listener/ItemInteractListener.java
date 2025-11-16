package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.utility.attribute.ParsedAttribute;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.*;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles right-click interactions with active items
 */
public class ItemInteractListener implements Listener {

    private final BuffedItems plugin;
    private final NamespacedKey nbtKey;

    public ItemInteractListener(BuffedItems plugin) {
        this.plugin = plugin;
        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        String itemId = item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
        if (itemId == null) {
            return;
        }

        BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
        if (buffedItem == null || !buffedItem.isActiveMode()) {
            return;
        }

        // Permission check
        if (buffedItem.getPermission().isPresent()) {
            if (!player.hasPermission(buffedItem.getPermission().get())) {
                player.sendMessage(ConfigManager.getPrefixedMessageAsComponent("protection-prevent-interact"));
                return;
            }
        }

        // Cooldown check
        if (plugin.getCooldownManager().isOnCooldown(player, itemId)) {
            handleCooldownMessage(player, buffedItem);
            event.setCancelled(true);
            return;
        }

        // Set cooldown
        if (buffedItem.getCooldown() > 0) {
            plugin.getCooldownManager().setCooldown(player, itemId, buffedItem.getCooldown());
        }

        // Execute active effects
        executeCommands(player, buffedItem.getActiveCommands());
        applyActiveEffects(player, buffedItem);
        playConfiguredSound(player, buffedItem.getCustomSuccessSound(), ConfigManager.getGlobalSuccessSound());

        event.setCancelled(true);
    }

    /**
     * Handles cooldown messaging (chat, title, action bar, boss bar)
     */
    private void handleCooldownMessage(Player player, BuffedItem buffedItem) {
        double remaining = plugin.getCooldownManager().getRemainingSeconds(player, buffedItem.getId());

        if (buffedItem.isVisualChat()) {
            String rawMsg = buffedItem.getCustomChatMsg();
            if (rawMsg == null) {
                rawMsg = plugin.getConfig().getString("active-items.messages.cooldown-chat", "&cWait {time}s");
            }
            player.sendMessage(ConfigManager.fromLegacy(rawMsg.replace("{time}", String.format("%.1f", remaining))));
        }

        if (buffedItem.isVisualTitle()) {
            String title = buffedItem.getCustomTitleMsg();
            if (title == null) {
                title = plugin.getConfig().getString("active-items.messages.cooldown-title", "");
            }

            String subtitle = buffedItem.getCustomSubtitleMsg();
            if (subtitle == null) {
                subtitle = plugin.getConfig().getString("active-items.messages.cooldown-subtitle", "");
            }

            player.showTitle(net.kyori.adventure.title.Title.title(
                    ConfigManager.fromLegacy(title.replace("{time}", String.format("%.1f", remaining))),
                    ConfigManager.fromLegacy(subtitle.replace("{time}", String.format("%.1f", remaining)))
            ));
        }

        playConfiguredSound(player, buffedItem.getCustomCooldownSound(), ConfigManager.getGlobalCooldownSound());
    }


    /**
     * Main entry point: Iterates through the command list and sends each line to the processor.
     */
    private void executeCommands(Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;

        long cumulativeDelay = 0;

        for (String cmdLine : commands) {
            cumulativeDelay = processCommand(player, cmdLine, cumulativeDelay, false);
        }
    }

    /**
     * Smart Command Processor (Recursive)
     * @param delayOffset The accumulated delay to wait before executing this command.
     * @param inheritedConsole Console permission inherited from the parent command if chained.
     * @return The [delay] amount found in this command (to be added to the main loop).
     */
    private long processCommand(Player player, String commandLine, long delayOffset, boolean inheritedConsole) {
        String cmdToProcess = commandLine.trim();
        long localDelay = 0;
        double localChance = 100.0;
        boolean isConsole = inheritedConsole;

        boolean findingPrefixes = true;
        while (findingPrefixes) {
            findingPrefixes = false;
            String lowerCmd = cmdToProcess.toLowerCase();

            if (lowerCmd.startsWith("[delay:")) {
                try {
                    int closeIndex = cmdToProcess.indexOf("]");
                    if (closeIndex != -1) {
                        localDelay += Long.parseLong(cmdToProcess.substring(7, closeIndex));
                        cmdToProcess = cmdToProcess.substring(closeIndex + 1).trim();
                        findingPrefixes = true;
                    }
                } catch (Exception ignored) {}
            }
            else if (lowerCmd.startsWith("[chance:")) {
                try {
                    int closeIndex = cmdToProcess.indexOf("]");
                    if (closeIndex != -1) {
                        localChance = Double.parseDouble(cmdToProcess.substring(8, closeIndex));
                        cmdToProcess = cmdToProcess.substring(closeIndex + 1).trim();
                        findingPrefixes = true;
                    }
                } catch (Exception ignored) {}
            }
            else if (lowerCmd.startsWith("[console]")) {
                isConsole = true;
                cmdToProcess = cmdToProcess.substring(9).trim();
                findingPrefixes = true;
            }
        }

        if (cmdToProcess.isEmpty()) return localDelay;

        final String finalCmd = cmdToProcess;
        final double finalChance = localChance;
        final boolean finalIsConsole = isConsole;
        final long totalWaitTime = delayOffset + localDelay;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            if (finalChance < 100.0) {
                if (ThreadLocalRandom.current().nextDouble(100.0) > finalChance) return;
            }

            if (finalCmd.contains(";;")) {
                String[] subCommands = finalCmd.split(";;");

                long chainDelay = 0;
                for (String sub : subCommands) {
                    chainDelay = processCommand(player, sub, chainDelay, false);
                }
                return;
            }

            executeSingleCommand(player, finalCmd, finalIsConsole);

        }, totalWaitTime);

        return localDelay;
    }

    /**
     * Helper method that executes the final command (Placeholder + Dispatch).
     */
    private void executeSingleCommand(Player player, String cmd, boolean asConsole) {
        Location loc = player.getLocation();
        String parsedCmd = cmd
                .replace("%player%", player.getName())
                .replace("%player_name%", player.getName())
                .replace("%player_x%", String.format(Locale.US, "%.2f", loc.getX()))
                .replace("%player_y%", String.format(Locale.US, "%.2f", loc.getY()))
                .replace("%player_z%", String.format(Locale.US, "%.2f", loc.getZ()))
                .replace("%player_yaw%", String.format(Locale.US, "%.2f", loc.getYaw()))
                .replace("%player_pitch%", String.format(Locale.US, "%.2f", loc.getPitch()));

        if (plugin.isPlaceholderApiEnabled()) {
            parsedCmd = PlaceholderAPI.setPlaceholders(player, parsedCmd);
        }

        String lowerCmd = parsedCmd.toLowerCase().trim();

        if (lowerCmd.startsWith("[message] ") || lowerCmd.startsWith("[msg] ")) {
            String msgContent = parsedCmd.substring(parsedCmd.indexOf("] ") + 2);
            player.sendMessage(ConfigManager.fromLegacy(msgContent));
            return;
        }

        if (lowerCmd.startsWith("[actionbar] ") || lowerCmd.startsWith("[ab] ")) {
            String msgContent = parsedCmd.substring(parsedCmd.indexOf("] ") + 2);
            player.sendActionBar(ConfigManager.fromLegacy(msgContent));
            return;
        }

        if (lowerCmd.startsWith("[title] ")) {
            String fullContent = parsedCmd.substring(8);
            String titleText = fullContent;
            String subtitleText = "";

            if (fullContent.contains("|")) {
                String[] parts = fullContent.split("\\|", 2);
                titleText = parts[0].trim();
                subtitleText = parts[1].trim();
            }

            player.showTitle(net.kyori.adventure.title.Title.title(
                    ConfigManager.fromLegacy(titleText),
                    ConfigManager.fromLegacy(subtitleText)
            ));
            return;
        }

        Boolean rule = player.getWorld().getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
        player.getWorld().setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);

        try {
            if (asConsole) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
            } else {
                player.performCommand(parsedCmd);
            }
        } finally {
            if (rule != null) player.getWorld().setGameRule(GameRule.SEND_COMMAND_FEEDBACK, rule);
        }
    }


    private void applyActiveEffects(Player player, BuffedItem item) {
        BuffedItemEffect effects = item.getActiveEffects();
        if (effects == null) return;

        int durationTicks = item.getActiveDuration() > 0 ? item.getActiveDuration() * 20 : 100;

        for (java.util.Map.Entry<PotionEffectType, Integer> entry : effects.getPotionEffects().entrySet()) {
            player.addPotionEffect(new PotionEffect(entry.getKey(), durationTicks, entry.getValue() - 1));
        }

        for (ParsedAttribute attr : effects.getParsedAttributes()) {
            AttributeInstance instance = player.getAttribute(attr.getAttribute());
            if (instance != null) {
                UUID deterministicUUID = UUID.nameUUIDFromBytes(
                        ("buffeditems.ACTIVE." + player.getUniqueId() + "." + item.getId() + "." + attr.getAttribute().name()).getBytes()
                );

                AttributeModifier modifier = new AttributeModifier(
                        deterministicUUID,
                        "buffeditems.active." + item.getId(),
                        attr.getAmount(),
                        attr.getOperation()
                );

                instance.addModifier(modifier);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        AttributeInstance currentInstance = player.getAttribute(attr.getAttribute());
                        if (currentInstance != null) {
                            currentInstance.removeModifier(modifier);
                        }
                    }
                }, durationTicks);
            }
        }
    }

    /**
     * Plays a configured sound or uses global default
     * Format: "SOUND_NAME;VOLUME;PITCH"
     */
    private void playConfiguredSound(Player player, String itemSound, String globalSound) {
        String soundString = (itemSound != null) ? itemSound : globalSound;

        if (soundString == null || soundString.equalsIgnoreCase("NONE")) {
            return;
        }

        try {
            String[] parts = soundString.split(";");
            Sound sound = Sound.valueOf(parts[0].toUpperCase());
            float volume = (parts.length > 1) ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = (parts.length > 2) ? Float.parseFloat(parts[2]) : 1.0f;

            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[Sound] Invalid sound format: " + soundString);
        }
    }
}