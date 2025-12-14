package io.github.altkat.BuffedItems.command;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.hooks.HookManager;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import io.github.altkat.BuffedItems.menu.crafting.PublicRecipeListMenu;
import io.github.altkat.BuffedItems.menu.set.PublicSetListMenu;
import io.github.altkat.BuffedItems.menu.utility.MainMenu;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BuffedItemCommand implements CommandExecutor {

    private final BuffedItems plugin;
    private final Component noPermissionMessage = ConfigManager.fromSection("§cYou do not have permission to use this command.");
    private final HookManager hooks;

    public BuffedItemCommand(BuffedItems plugin) {
        this.plugin = plugin;
        this.hooks = plugin.getHookManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                if (!sender.hasPermission("buffeditems.command.give")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }
                return handleGiveCommand(sender, args);
            case "reload":
                if (!sender.hasPermission("buffeditems.command.reload")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }
                return handleReloadCommand(sender, args);
            case "list":
                if (!sender.hasPermission("buffeditems.command.list")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }
                return handleListCommand(sender);
            case "menu":
                if (!sender.hasPermission("buffeditems.command.menu")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    BuffedItems.getPlayerMenuUtility(p).flushData();
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Command] Opening main menu for " + p.getName());
                    new MainMenu(BuffedItems.getPlayerMenuUtility(p), plugin).open();
                } else {
                    sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cThis command can only be used by players."));
                }
                return true;
            case "upgrade":
                if (!sender.hasPermission("buffeditems.command.upgrade")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }

                if (!UpgradesConfig.get().getBoolean("settings.enabled", true)) {
                    sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cThe Upgrade system is currently disabled."));
                    return true;
                }

                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    new io.github.altkat.BuffedItems.menu.upgrade.UpgradeMenu(BuffedItems.getPlayerMenuUtility(p), plugin).open();
                } else {
                    sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cOnly players can use this command."));
                }
                return true;
            case "wiki":
                if (!sender.hasPermission("buffeditems.command.wiki")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }
                handleWikiCommand(sender);
                return true;

            case "recipes":
                if (!sender.hasPermission("buffeditems.command.recipes")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }

                if (!RecipesConfig.get().getBoolean("settings.enabled", true)) {
                    sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cThe Crafting system is currently disabled."));
                    return true;
                }

                if (sender instanceof Player p) {
                    new PublicRecipeListMenu(BuffedItems.getPlayerMenuUtility(p), plugin).open();
                } else {
                    sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cOnly players can use this command."));
                }
                return true;

            case "sets":
                if (!sender.hasPermission("buffeditems.command.sets")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }

                if (!SetsConfig.get().getBoolean("settings.enabled", true)) {
                    sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cThe Item Set system is currently disabled."));
                    return true;
                }

                if (sender instanceof Player p) {
                    new PublicSetListMenu(BuffedItems.getPlayerMenuUtility(p), plugin).open();
                } else {
                    sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cOnly players can use this command."));
                }
                return true;

            case "update":
                if (!sender.hasPermission("buffeditems.command.update")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }
                if (plugin.getUpdateHandler() != null) {
                    plugin.getUpdateHandler().checkManually(sender);
                } else {
                    sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cUpdate handler is not initialized."));
                }
                return true;
            default:
                sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cUnknown subcommand. Use /buffeditems for help."));
                return true;
        }
    }

    private void handleWikiCommand(CommandSender sender) {
        Component message = ConfigManager.fromSectionWithPrefix("§bOfficial Wiki & Documentation:")
                .append(Component.text(" [Click to Open]", net.kyori.adventure.text.format.NamedTextColor.YELLOW, net.kyori.adventure.text.format.TextDecoration.BOLD)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl("https://github.com/AltKat/BuffedItems/wiki"))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Go to https://github.com/AltKat/BuffedItems/wiki"))));

        sender.sendMessage(message);
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ConfigManager.fromSection("§#FFD700--- §#FF6347BuffedItems Help §#FFD700---"));
        if (sender.hasPermission("buffeditems.command.give")) {
            sender.sendMessage(ConfigManager.fromSection("§6/bi give <player> <item_id> [amount]"));
        }
        if (sender.hasPermission("buffeditems.command.reload")) {
            sender.sendMessage(ConfigManager.fromSection("§6/bi reload (Reloads all config files from disk)"));
        }
        if (sender.hasPermission("buffeditems.command.list")) {
            sender.sendMessage(ConfigManager.fromSection("§6/bi list"));
        }
        if (sender.hasPermission("buffeditems.command.menu")) {
            sender.sendMessage(ConfigManager.fromSection("§6/bi menu"));
        }
        if (sender.hasPermission("buffeditems.command.upgrade")) {
            sender.sendMessage(ConfigManager.fromSection("§6/bi upgrade §7(Upgrade Station)"));
        }
        if (sender.hasPermission("buffeditems.command.recipes")) {
            sender.sendMessage(ConfigManager.fromSection("§6/bi recipes §7(View Crafting Recipes)"));
        }
        if (sender.hasPermission("buffeditems.command.sets")) {
            sender.sendMessage(ConfigManager.fromSection("§6/bi sets §7(View Item Sets)"));
        }
        if (sender.hasPermission("buffeditems.command.wiki")) {
            sender.sendMessage(ConfigManager.fromSection("§6/bi wiki §7(Get Wiki Link)"));
        }
        if (sender.hasPermission("buffeditems.command.update")) {
            sender.sendMessage(ConfigManager.fromSection("§6/bi update §7(Check Updates)"));
        }
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cUsage: /buffeditems give <player> <item_id> [amount]"));
            return true;
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Command] Give command: player=" + args[1] + ", item=" + args[2] + ", amount=" + (args.length >= 4 ? args[3] : "1"));

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cPlayer not found: " + args[1]));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Command] Player not found: " + args[1]);
            return true;
        }

        String itemId = args[2];
        BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
        if (buffedItem == null) {
            sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cItem not found in config: " + itemId));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Command] Item not found: " + itemId);
            return true;
        }

        if (!buffedItem.isValid()) {
            sender.sendMessage(ConfigManager.fromSection("§c⚠ WARNING: Item '" + itemId + "' has configuration errors!"));
            sender.sendMessage(ConfigManager.fromSection("§eErrors:"));
            for (String error : buffedItem.getErrorMessages()) {
                sender.sendMessage(ConfigManager.fromSection("§7  • " + error));
            }
            sender.sendMessage(ConfigManager.fromSection("§eThe item will still be given, but may not work as intended."));
            sender.sendMessage(ConfigManager.fromSection("§ePlease fix errors via /bi menu"));
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid amount: " + args[3]));
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Command] Invalid amount: " + args[3]);
                return true;
            }
        }

        ItemStack itemStack = new ItemBuilder(buffedItem, plugin).build();
        itemStack.setAmount(amount);


        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                Component originalName = meta.displayName();
                if (originalName != null) {
                    String legacyNameWithSection = ConfigManager.toSection(originalName);
                    String parsedName = hooks.processPlaceholders(target, legacyNameWithSection);
                    meta.displayName(ConfigManager.fromSection(parsedName));
                }
            }

            if (meta.hasLore()) {
                List<Component> originalLore = meta.lore();
                if (originalLore != null) {
                    List<Component> parsedLore = originalLore.stream()
                            .map(ConfigManager::toSection)
                            .map(line -> hooks.processPlaceholders(target, line))
                            .map(ConfigManager::fromSection)
                            .collect(Collectors.toList());
                    meta.lore(parsedLore);
                }
            }
            itemStack.setItemMeta(meta);
        }


        target.getInventory().addItem(itemStack);

        sender.sendMessage(ConfigManager.fromLegacyWithPrefix("&aGave &e" + amount + "x &r" + buffedItem.getDisplayName() + "&a to " + target.getName()));

        String rawMsg = plugin.getConfig().getString("messages.give-success-receiver", "&#00FF00You have received &#FFD700{amount}x &#00FF00{item_name}&#00FF00.");

        String papiMsg = hooks.processPlaceholders(target, rawMsg);

        Component baseComp = ConfigManager.fromLegacyWithPrefix(papiMsg);

        int finalAmount = amount;
        Component finalMessage = baseComp
                .replaceText(builder -> builder.matchLiteral("{amount}").replacement(String.valueOf(finalAmount)))
                .replaceText(builder -> builder.matchLiteral("{item_name}").replacement(ConfigManager.fromLegacy(buffedItem.getDisplayName())));

        target.sendMessage(finalMessage);

        ConfigManager.logInfo("&aGave &e" + amount + "x " + itemId + "&a to &e" + target.getName() + "&a (by: &e" + sender.getName() + "&a)");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getEffectApplicatorTask().markPlayerForUpdate(target.getUniqueId());
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Command] Marked " + target.getName() + " for update after receiving item via /bi give.");
        }, 1L);

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender, String[] args) {

        boolean isForce = args.length > 1 && args[1].equalsIgnoreCase("force");

        if (!isForce) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (BuffedItems.getPlayerMenuUtility(p).hasUnsavedChanges()) {
                    sender.sendMessage(ConfigManager.fromSectionWithPrefix("§c§lWARNING: §r§cPlayer '" + p.getName() + "' is currently editing a recipe!"));
                    sender.sendMessage(ConfigManager.fromSection("§cReloading now will discard their unsaved changes."));
                    sender.sendMessage(ConfigManager.fromSection("§eType §6/bi reload force §eto ignore this warning."));
                    return true;
                }
            }
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Reload] Reload triggered by " + sender.getName());

        ConfigManager.reloadConfig();

        sender.sendMessage(ConfigManager.fromSectionWithPrefix("§aConfigurations has been reloaded."));
        return true;
    }

    private boolean handleListCommand(CommandSender sender) {
        Map<String, BuffedItem> items = plugin.getItemManager().getLoadedItems();

        sender.sendMessage(ConfigManager.fromSection("§#FFD700--- §#FF6347Available Buffed Items §#FFD700---"));

        int validCount = 0;
        int errorCount = 0;

        for (Map.Entry<String, BuffedItem> entry : items.entrySet()) {
            String itemId = entry.getKey();
            BuffedItem item = entry.getValue();

            if (item.isValid()) {
                sender.sendMessage(ConfigManager.fromSection("§6✓ " + itemId));
                validCount++;
            } else {
                sender.sendMessage(ConfigManager.fromSection("§c✗ " + itemId + "§8 (" + item.getErrorMessages().size() + " error(s))"));
                errorCount++;
            }
        }

        sender.sendMessage(ConfigManager.fromSection(""));
        sender.sendMessage(ConfigManager.fromSection("§7Total: " + items.size() + " | " +
                "§aValid: " + validCount + " | " +
                "§cErrors: " + errorCount));

        if (errorCount > 0) {
            sender.sendMessage(ConfigManager.fromSection("§eUse §6/bi menu§e to view and fix errors."));
        }

        return true;
    }
}