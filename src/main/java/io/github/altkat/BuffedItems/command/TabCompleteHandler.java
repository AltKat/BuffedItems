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
            if (UpgradesConfig.get().getBoolean("settings.enabled", true)) {
                if (sender.hasPermission("buffeditems.command.upgrade")) {subcommands.add("upgrade");}
            }
            if (RecipesConfig.get().getBoolean("settings.enabled", true)) {
                if (sender.hasPermission("buffeditems.command.recipes")) subcommands.add("recipes");
            }
            if (SetsConfig.get().getBoolean("settings.enabled", true)) {
                if (sender.hasPermission("buffeditems.command.sets")) subcommands.add("sets");
            }

            StringUtil.copyPartialMatches(args[0], subcommands, completions);

        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
            StringUtil.copyPartialMatches(args[1], playerNames, completions);

        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> itemIds = new ArrayList<>(plugin.getItemManager().getLoadedItems().keySet());
            StringUtil.copyPartialMatches(args[2], itemIds, completions);
        }

        return completions;
    }
}