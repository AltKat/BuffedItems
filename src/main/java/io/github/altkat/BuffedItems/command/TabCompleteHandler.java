package io.github.altkat.BuffedItems.command;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TabCompleteHandler implements TabCompleter {

    private final BuffedItems plugin;

    public TabCompleteHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        final List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            if (sender.hasPermission("buffeditems.command.give")) {subcommands.add("give");}
            if (sender.hasPermission("buffeditems.command.reload")) {subcommands.add("reload");}
            if (sender.hasPermission("buffeditems.command.list")) {subcommands.add("list");}
            if (sender.hasPermission("buffeditems.command.menu")) {subcommands.add("menu");}
            if (sender.hasPermission("buffeditems.command.wiki")) subcommands.add("wiki");
            if (sender.hasPermission("buffeditems.command.update")) subcommands.add("update");
            if (sender.hasPermission("buffeditems.command.bypass")) subcommands.add("bypass");
            if (sender.hasPermission("buffeditems.command.upgrade")) {
                if (UpgradesConfig.get().getBoolean("settings.enabled", true)) {
                    subcommands.add("upgrade");
                }
            }
            if (sender.hasPermission("buffeditems.command.recipes")) {
                if (RecipesConfig.get().getBoolean("settings.enabled", true)) {
                    subcommands.add("recipes");
                }
            }
            if (sender.hasPermission("buffeditems.command.sets")) {
                if (SetsConfig.get().getBoolean("settings.enabled", true)) {
                    subcommands.add("sets");
                }
            }

            StringUtil.copyPartialMatches(args[0], subcommands, completions);

        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                if (sender.hasPermission("buffeditems.command.give")) {
                    List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList());
                    StringUtil.copyPartialMatches(args[1], playerNames, completions);
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("buffeditems.command.reload")) {
                    StringUtil.copyPartialMatches(args[1], List.of("all", "items", "recipes", "sets", "upgrades", "config"), completions);
                }
            }

        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                if (sender.hasPermission("buffeditems.command.give")) {
                    List<String> itemIds = new ArrayList<>(plugin.getItemManager().getLoadedItems().keySet());
                    StringUtil.copyPartialMatches(args[2], itemIds, completions);
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("buffeditems.command.reload")) {
                    String reloadType = args[1].toLowerCase();
                    if (reloadType.equals("all") || reloadType.equals("recipes")) {
                        StringUtil.copyPartialMatches(args[2], List.of("force"), completions);
                    }
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("give")) {
                if (sender.hasPermission("buffeditems.command.give")) {
                    StringUtil.copyPartialMatches(args[3], List.of("1", "64", "-s", "--silent"), completions);
                }
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("give")) {
                if (sender.hasPermission("buffeditems.command.give")) {
                    String prevArg = args[3];
                    if (prevArg.equalsIgnoreCase("-s") || prevArg.equalsIgnoreCase("--silent")) {
                        StringUtil.copyPartialMatches(args[4], List.of("1", "64"), completions);
                    } else {
                        StringUtil.copyPartialMatches(args[4], List.of("-s", "--silent"), completions);
                    }
                }
            }
        }

        return completions;
    }
}