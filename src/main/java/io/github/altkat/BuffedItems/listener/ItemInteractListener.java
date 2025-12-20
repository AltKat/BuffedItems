package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.hooks.HookManager;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.utility.attribute.ParsedAttribute;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;
import io.github.altkat.BuffedItems.utility.item.DepletionAction;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles right-click interactions with active items
 */
public class ItemInteractListener implements Listener {

    private final BuffedItems plugin;
    private final NamespacedKey nbtKey;
    private final HookManager hooks;

    public ItemInteractListener(BuffedItems plugin) {
        this.plugin = plugin;
        this.hooks = plugin.getHookManager();
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

        // Live Update Check
        ItemStack updatedItem = plugin.getItemUpdater().updateItem(item, player);

        if (updatedItem != null && !updatedItem.isSimilar(item)) {
            if (event.getHand() == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(updatedItem);
            } else {
                player.getInventory().setItemInOffHand(updatedItem);
            }
            item = updatedItem;
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[LiveUpdate] Item updated in hand.");
        }

        String itemId = item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
        if (itemId == null) return;

        BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
        if (buffedItem == null || !buffedItem.isActiveMode()) {
            return;
        }

        if (!buffedItem.hasActivePermission(player)) {
            String rawMsg = plugin.getConfig().getString("messages.protection-prevent-use-no-permission", "&cYou do not have permission to use this item.");
            String parsedMsg = hooks.processPlaceholders(player, rawMsg);
            player.sendMessage(ConfigManager.fromLegacyWithPrefix(parsedMsg));
            return;
        }

        // Cooldown check
        if (plugin.getCooldownManager().isOnCooldown(player, itemId)) {
            handleCooldownMessage(player, buffedItem);
            event.setCancelled(true);
            return;
        }

        NamespacedKey durabilityKey = new NamespacedKey(plugin, "remaining_active_uses");
        Integer currentUses = null;

        if (item.getItemMeta().getPersistentDataContainer().has(durabilityKey, PersistentDataType.INTEGER)) {
            currentUses = item.getItemMeta().getPersistentDataContainer().get(durabilityKey, PersistentDataType.INTEGER);
            if (currentUses != null && currentUses <= 0) {
                event.setCancelled(true);
                String rawDepletedMsg = buffedItem.getDepletedMessage();
                String parsedDepletedMsg = hooks.processPlaceholders(player, rawDepletedMsg);
                player.sendMessage(ConfigManager.fromLegacyWithPrefix(parsedDepletedMsg));
                playConfiguredSound(player, buffedItem.getCustomDepletedTrySound(), ConfigManager.getGlobalDepletedTrySound());
                return;
            }
        }

        // Cost Checks
        List<ICost> costs = buffedItem.getCosts();
        if (costs != null && !costs.isEmpty()) {
            List<String> missingRequirements = new ArrayList<>();

            for (ICost cost : costs) {
                if (!cost.hasEnough(player)) {
                    missingRequirements.add(hooks.processPlaceholders(player, cost.getFailureMessage()));
                }
            }

            if (!missingRequirements.isEmpty()) {
                for (String msg : missingRequirements) {
                    player.sendMessage(ConfigManager.fromLegacy(msg));
                }
                playConfiguredSound(player, buffedItem.getCustomCostFailSound(), ConfigManager.getGlobalCostFailSound());
                event.setCancelled(true);
                return;
            }
        }

        // Deduct Costs
        if (costs != null && !costs.isEmpty()) {
            for (ICost cost : costs) {
                cost.deduct(player);
            }
        }

        // Set cooldown
        if (buffedItem.getCooldown() > 0) {
            plugin.getCooldownManager().setCooldown(player, itemId, buffedItem.getCooldown());
        }

        // Execute active effects and commands
        executeCommands(player, buffedItem.getActiveCommands());
        applyActiveEffects(player, buffedItem);
        playConfiguredSound(player, buffedItem.getCustomSuccessSound(), ConfigManager.getGlobalSuccessSound());

        if (currentUses != null && currentUses > 0) {
            int newUses = currentUses - 1;

            if (newUses == 0) {
                handleDepletion(player, buffedItem, item, event.getHand());
            } else {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);

                    ItemStack processedItem = item.clone();
                    processedItem.setAmount(1);
                    updateUsesNBT(processedItem, newUses, buffedItem, player);

                    giveItemToPlayer(player, processedItem);
                } else {
                    updateUsesNBT(item, newUses, buffedItem, player);
                }
            }
        }

        event.setCancelled(true);
    }

    private void updateUsesNBT(ItemStack item, int uses, BuffedItem buffedItem, Player player) {
        ItemMeta meta = item.getItemMeta();
        NamespacedKey durabilityKey = new NamespacedKey(plugin, "remaining_active_uses");
        NamespacedKey updateFlagKey = new NamespacedKey(plugin, "needs_lore_update");

        meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, uses);
        meta.getPersistentDataContainer().set(updateFlagKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);

        ItemStack updated = plugin.getItemUpdater().updateItem(item, player);
        if (updated != null) {
            item.setItemMeta(updated.getItemMeta());
        }
    }

    private void handleDepletion(Player player, BuffedItem buffedItem, ItemStack originalItem, EquipmentSlot hand) {
        String rawDepleteMsg = buffedItem.getDepletionNotification();
        String parsedDepleteMsg = hooks.processPlaceholders(player, rawDepleteMsg);
        player.sendMessage(ConfigManager.fromLegacyWithPrefix(parsedDepleteMsg));

        playConfiguredSound(player, buffedItem.getCustomDepletionSound(), ConfigManager.getGlobalDepletionSound());

        if (!buffedItem.getDepletionCommands().isEmpty()) {
            executeCommands(player, buffedItem.getDepletionCommands());
        }

        DepletionAction action = buffedItem.getDepletionAction();

        if (action == DepletionAction.DESTROY) {
            if (originalItem.getAmount() > 1) {
                originalItem.setAmount(originalItem.getAmount() - 1);
            } else {
                originalItem.setAmount(0);
                if (hand == EquipmentSlot.HAND) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }
        else if (action == DepletionAction.TRANSFORM) {
            if (originalItem.getAmount() > 1) {
                originalItem.setAmount(originalItem.getAmount() - 1);
                giveTransformedItem(player, buffedItem);
            } else {
                setTransformedItemInHand(player, buffedItem, hand);
            }
        }
        else {
            if (originalItem.getAmount() > 1) {
                originalItem.setAmount(originalItem.getAmount() - 1);

                ItemStack disabledItem = originalItem.clone();
                disabledItem.setAmount(1);
                updateUsesNBT(disabledItem, 0, buffedItem, player);
                giveItemToPlayer(player, disabledItem);
            } else {
                updateUsesNBT(originalItem, 0, buffedItem, player);
            }
        }
    }

    private void setTransformedItemInHand(Player player, BuffedItem buffedItem, EquipmentSlot hand) {
        String targetId = buffedItem.getDepletionTransformId();

        if (targetId == null) {
            plugin.getLogger().warning("[BuffedItems] Configuration Error: Item '" + buffedItem.getId() +
                    "' has DepletionAction set to TRANSFORM but no 'depletion.transform-to' ID is configured!");
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cItem transformation failed due to misconfiguration. Please contact an administrator."));
            if (hand == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(null);
            else player.getInventory().setItemInOffHand(null);
            return;
        }

        BuffedItem targetItem = plugin.getItemManager().getBuffedItem(targetId);
        if (targetItem != null) {
            ItemStack resultItem = new ItemBuilder(targetItem, plugin).build();
            processMetaPlaceholders(player, resultItem);

            if (hand == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(resultItem);
            else player.getInventory().setItemInOffHand(resultItem);

            String rawTransformMsg = buffedItem.getDepletionTransformMessage();
            String parsedTransformMsg = hooks.processPlaceholders(player, rawTransformMsg);
            player.sendMessage(ConfigManager.fromLegacyWithPrefix(parsedTransformMsg));
        } else {
            plugin.getLogger().warning("[BuffedItems] Transform failed. Target '" + targetId + "' not found.");
            if (hand == EquipmentSlot.HAND) player.getInventory().setItemInMainHand(null);
            else player.getInventory().setItemInOffHand(null);
        }
    }

    private void giveTransformedItem(Player player, BuffedItem buffedItem) {
        String targetId = buffedItem.getDepletionTransformId();
        if (targetId == null){
            plugin.getLogger().warning("[BuffedItems] Configuration Error: Item '" + buffedItem.getId() +
                    "' has DepletionAction set to TRANSFORM but no 'depletion.transform-to' ID is configured!");
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cItem transformation failed due to misconfiguration. Please contact an administrator."));
            return;
        }

        BuffedItem targetItem = plugin.getItemManager().getBuffedItem(targetId);
        if (targetItem != null) {
            ItemStack resultItem = new ItemBuilder(targetItem, plugin).build();
            processMetaPlaceholders(player, resultItem);
            giveItemToPlayer(player, resultItem);

            String rawTransformMsg = buffedItem.getDepletionTransformMessage();
            String parsedTransformMsg = hooks.processPlaceholders(player, rawTransformMsg);
            player.sendMessage(ConfigManager.fromLegacyWithPrefix(parsedTransformMsg));
        }else {
            plugin.getLogger().warning("[BuffedItems] Transform failed. Target '" + targetId + "' not found.");
        }
    }

    private void processMetaPlaceholders(Player player, ItemStack item) {
        ItemMeta newMeta = item.getItemMeta();
        if (newMeta != null) {
            if (newMeta.hasDisplayName()) {
                String rawName = ConfigManager.toSection(newMeta.displayName());
                newMeta.displayName(ConfigManager.fromSection(hooks.processPlaceholders(player, rawName)));
            }
            if (newMeta.hasLore()) {
                List<Component> parsedLore = new ArrayList<>();
                for (Component line : newMeta.lore()) {
                    String rawLine = ConfigManager.toSection(line);
                    parsedLore.add(ConfigManager.fromSection(hooks.processPlaceholders(player, rawLine)));
                }
                newMeta.lore(parsedLore);
            }
            item.setItemMeta(newMeta);
        }
    }

    private void giveItemToPlayer(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
        if (!leftOver.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftOver.get(0));
        }
    }

    private void handleCooldownMessage(Player player, BuffedItem buffedItem) {
        double remaining = plugin.getCooldownManager().getRemainingSeconds(player, buffedItem.getId());

        if (buffedItem.isVisualChat()) {
            String rawMsg = buffedItem.getCustomChatMsg();
            if (rawMsg == null) {
                rawMsg = plugin.getConfig().getString("active-items.messages.cooldown-chat", "&cWait {time}s");
            }
            String parsedMsg = hooks.processPlaceholders(player, rawMsg.replace("{time}", String.format("%.1f", remaining)));
            player.sendMessage(ConfigManager.fromLegacy(parsedMsg));
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

            String finalTitle = hooks.processPlaceholders(player, title.replace("{time}", String.format("%.1f", remaining)));
            String finalSubtitle = hooks.processPlaceholders(player, subtitle.replace("{time}", String.format("%.1f", remaining)));

            player.showTitle(Title.title(
                    ConfigManager.fromLegacy(finalTitle),
                    ConfigManager.fromLegacy(finalSubtitle)
            ));
        }

        playConfiguredSound(player, buffedItem.getCustomCooldownSound(), ConfigManager.getGlobalCooldownSound());
    }

    /**
     * Executes the list of commands with logic support ([chance], [delay], [else]).
     */
    private void executeCommands(Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;

        long cumulativeDelay = 0;
        boolean lastChainFailed = false;

        for (String cmdLine : commands) {
            String cmdToProcess = cmdLine.trim();

            boolean isElseBlock = false;
            if (cmdToProcess.toLowerCase().startsWith("[else]")) {
                isElseBlock = true;
                cmdToProcess = cmdToProcess.substring(6).trim();
            }

            if (isElseBlock && !lastChainFailed) {
                continue;
            }

            if (!isElseBlock) {
                lastChainFailed = false;
            }

            long localDelay = 0;
            double localChance = 100.0;
            boolean isConsole = false;

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

            if (localChance < 100.0) {
                if (ThreadLocalRandom.current().nextDouble(100.0) > localChance) {
                    lastChainFailed = true;
                    continue;
                }
            }

            lastChainFailed = false;

            if (cmdToProcess.isEmpty()) continue;

            final String finalCmd = cmdToProcess;
            final boolean finalIsConsole = isConsole;
            final long executionTime = cumulativeDelay + localDelay;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                if (finalCmd.contains(";;")) {
                    String[] subCommands = finalCmd.split(";;");
                    long chainDelay = 0;
                    for (String sub : subCommands) {
                        processSubCommand(player, sub, chainDelay, finalIsConsole);
                    }
                } else {
                    executeSingleCommand(player, finalCmd, finalIsConsole);
                }

            }, executionTime);

            cumulativeDelay += localDelay;
        }
    }

    private void processSubCommand(Player player, String commandPart, long delayOffset, boolean inheritedConsole) {
        String cmdToProcess = commandPart.trim();
        long localDelay = 0;
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
            else if (lowerCmd.startsWith("[console]")) {
                isConsole = true;
                cmdToProcess = cmdToProcess.substring(9).trim();
                findingPrefixes = true;
            }
            else if (lowerCmd.startsWith("[chance:")) {
                try {
                    int closeIndex = cmdToProcess.indexOf("]");
                    if (closeIndex != -1) {
                        double chance = Double.parseDouble(cmdToProcess.substring(8, closeIndex));
                        if (ThreadLocalRandom.current().nextDouble(100.0) > chance) {
                            return;
                        }
                        cmdToProcess = cmdToProcess.substring(closeIndex + 1).trim();
                        findingPrefixes = true;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (cmdToProcess.isEmpty()) return;

        final String finalCmd = cmdToProcess;
        final boolean finalIsConsole = isConsole;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                executeSingleCommand(player, finalCmd, finalIsConsole);
            }
        }, delayOffset + localDelay);
    }

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

        parsedCmd = hooks.processPlaceholders(player, parsedCmd);

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

        if (lowerCmd.startsWith("[sound] ")) {
            String soundData = parsedCmd.substring(parsedCmd.indexOf("] ") + 2);
            playConfiguredSound(player, soundData, "NONE");
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
        boolean showIcon = ConfigManager.shouldShowPotionIcons();

        for (Map.Entry<PotionEffectType, Integer> entry : effects.getPotionEffects().entrySet()) {
            player.addPotionEffect(new PotionEffect(
                    entry.getKey(),
                    durationTicks,
                    entry.getValue() - 1,
                    true,
                    false,
                    showIcon
            ));
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