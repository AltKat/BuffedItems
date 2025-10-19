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

public class Commands implements CommandExecutor {

    private final BuffedItems plugin;
    private final String noPermissionMessage = ChatColor.RED + "You do not have permission to use this command.";

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
            sender.sendMessage(ChatColor.GOLD + "/bi reload");
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

        plugin.getLogger().fine("[Command] Give command: player=" + args[1] + ", item=" + args[2] + ", amount=" + (args.length >= 4 ? args[3] : "1"));

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            plugin.getLogger().fine("[Command] Player not found: " + args[1]);
            return true;
        }

        String itemId = args[2];
        BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
        if (buffedItem == null) {
            sender.sendMessage(ChatColor.RED + "Item not found in config: " + itemId);
            plugin.getLogger().fine("[Command] Item not found: " + itemId);
            return true;
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[3]);
                plugin.getLogger().fine("[Command] Invalid amount: " + args[3]);
                return true;
            }
        }

        ItemStack itemStack = new ItemBuilder(buffedItem, plugin).build();
        itemStack.setAmount(amount);
        target.getInventory().addItem(itemStack);

        sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + buffedItem.getDisplayName() + ChatColor.GREEN + " to " + target.getName());
        target.sendMessage(ChatColor.GREEN + "You have received " + amount + "x " + buffedItem.getDisplayName());

        plugin.getLogger().info("Gave " + amount + "x " + itemId + " to " + target.getName() + " (by: " + sender.getName() + ")");

        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        ConfigManager.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "BuffedItems configuration has been reloaded.");
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