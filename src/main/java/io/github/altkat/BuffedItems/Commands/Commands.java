package io.github.altkat.BuffedItems.Commands;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Menu.MainMenu;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.github.altkat.BuffedItems.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Commands implements CommandExecutor {

    private final BuffedItems plugin;
    private final String noPermissionMessage = ChatColor.RED + "You do not have permission to use this command.";
    private final Map<String, Long> reloadConfirmations = new HashMap<>();
    private static final long CONFIRM_TIMEOUT_MS = 5000;

    public Commands(BuffedItems plugin) {
        this.plugin = plugin;
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
            case "save":
                if (!sender.hasPermission("buffeditems.command.reload")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }
                return handleSaveCommand(sender);
            case "reload":
                if (!sender.hasPermission("buffeditems.command.reload")) {
                    sender.sendMessage(noPermissionMessage);
                    return true;
                }
                return handleReloadCommand(sender);
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
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Command] Opening main menu for " + p.getName());
                    new MainMenu(BuffedItems.getPlayerMenuUtility(p), plugin).open();
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                }
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /buffeditems for help.");
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "--- BuffedItems Help ---");
        if (sender.hasPermission("buffeditems.command.give")) {
            sender.sendMessage(ChatColor.GOLD + "/bi give <player> <item_id> [amount]");
        }
        if (sender.hasPermission("buffeditems.command.reload")) {
            sender.sendMessage(ChatColor.GOLD + "/bi save (Saves menu changes to config.yml)");
            sender.sendMessage(ChatColor.GOLD + "/bi reload (Loads config.yml, discards unsaved in-game made changes)");
        }
        if (sender.hasPermission("buffeditems.command.list")) {
            sender.sendMessage(ChatColor.GOLD + "/bi list");
        }
        if (sender.hasPermission("buffeditems.command.menu")) {
            sender.sendMessage(ChatColor.GOLD + "/bi menu");
        }
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /buffeditems give <player> <item_id> [amount]");
            return true;
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Command] Give command: player=" + args[1] + ", item=" + args[2] + ", amount=" + (args.length >= 4 ? args[3] : "1"));

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Command] Player not found: " + args[1]);
            return true;
        }

        String itemId = args[2];
        BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
        if (buffedItem == null) {
            sender.sendMessage(ChatColor.RED + "Item not found in config: " + itemId);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Command] Item not found: " + itemId);
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Command] Invalid amount: " + args[3]);
                return true;
            }
        }

        ItemStack itemStack = new ItemBuilder(buffedItem, plugin).build();
        itemStack.setAmount(amount);
        target.getInventory().addItem(itemStack);

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aGave &e" + amount + "x &r" + buffedItem.getDisplayName() + "&a to " + target.getName()));
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&aYou have received &e" + amount + "x &r" + buffedItem.getDisplayName()));

        plugin.getLogger().info("Gave " + amount + "x " + itemId + " to " + target.getName() + " (by: " + sender.getName() + ")");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getEffectApplicatorTask().markPlayerForUpdate(target.getUniqueId());
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Command] Marked " + target.getName() + " for update after receiving item via /bi give.");
        }, 1L);

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        String senderName = sender.getName();
        long currentTime = System.currentTimeMillis();
        Long expiryTime = reloadConfirmations.get(senderName);

        if (expiryTime != null && currentTime < expiryTime) {
            reloadConfirmations.remove(senderName);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Reload] Reload confirmed by " + senderName + ". Reloading from disk.");
            ConfigManager.reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "BuffedItems configuration has been reloaded from config.yml.");
            sender.sendMessage(ChatColor.GREEN + "Unsaved changes made via GUI (if any) have been discarded.");

        } else {
            reloadConfirmations.put(senderName, currentTime + CONFIRM_TIMEOUT_MS);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Reload] Confirmation requested by " + senderName + ".");
            sender.sendMessage(ChatColor.YELLOW + "==================[ " + ChatColor.RED + "WARNING" + ChatColor.YELLOW + " ]==================");
            sender.sendMessage(ChatColor.RED + "You are about to reload from 'config.yml'.");
            sender.sendMessage(ChatColor.RED + "Any changes made via " + ChatColor.GOLD + "/bi menu" + ChatColor.RED + " that were");
            sender.sendMessage(ChatColor.RED + "NOT saved with " + ChatColor.GOLD + "/bi save" + ChatColor.RED + " WILL BE LOST.");
            sender.sendMessage(ChatColor.YELLOW + "Type " + ChatColor.GOLD + "/bi reload" + ChatColor.YELLOW + " again within 5 seconds to confirm.");
            sender.sendMessage(ChatColor.YELLOW + "===============================================");
        }
        return true;
    }

    private boolean handleSaveCommand(CommandSender sender) {
        try {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Save] Saving pending changes to disk triggered by " + sender.getName() + "...");
            plugin.saveConfig();
            plugin.restartAutoSaveTask();
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Save] Save complete.");
            sender.sendMessage(ChatColor.GREEN + "BuffedItems configuration has been saved to config.yml.");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error: Could not save config.yml. Check console.");
            plugin.getLogger().severe("[Save] Failed to save config: " + e.getMessage());
        }
        return true;
    }

    private boolean handleListCommand(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "--- Available Buffed Items ---");
        plugin.getItemManager().getLoadedItems().keySet().forEach(itemId -> {
            sender.sendMessage(ChatColor.GOLD + "- " + itemId);
        });
        return true;
    }
}