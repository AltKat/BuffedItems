package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.menu.set.SetBonusesMenu;
import io.github.altkat.BuffedItems.menu.set.SetEditorMenu;
import io.github.altkat.BuffedItems.menu.set.SetItemsMenu;
import io.github.altkat.BuffedItems.menu.set.SetListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.entity.Player;

import java.util.List;

public class SetInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public SetInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean shouldHandle(String path) {
        return path.equals("create_set") || path.equals("set_display_name") ||
                path.equals("create_bonus_tier") || path.equals("set_add_item");
    }

    @Override
    public void onCancel(Player player, PlayerMenuUtility pmu, String path) {
        if (path.equals("create_set")) {
            new SetListMenu(pmu, plugin).open();
        } else if (path.equals("set_display_name")) {
            new SetEditorMenu(pmu, plugin).open();
        } else if (path.equals("create_bonus_tier")) {
            new SetBonusesMenu(pmu, plugin).open();
        } else if (path.equals("set_add_item")) {
            new SetItemsMenu(pmu, plugin).open();
        } else {
            new SetListMenu(pmu, plugin).open();
        }
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path.equals("create_set")) {
            String newSetId = input.toLowerCase().replace(" ", "_");

            if (ConfigManager.createNewSet(newSetId)) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSet '" + newSetId + "' created successfully!"));
                new SetListMenu(pmu, plugin).open();
            } else {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: A set with that ID already exists."));
                new SetListMenu(pmu, plugin).open();
            }

            pmu.setWaitingForChatInput(false);
            pmu.setChatInputPath(null);
        }

        if (path.equals("set_display_name")) {
            String setId = pmu.getItemToEditId();
            SetsConfig.get().set("sets." + setId + ".display_name", input);
            SetsConfig.saveAsync();
            plugin.getSetManager().loadSets(true);

            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSet display name updated!"));
            pmu.setWaitingForChatInput(false);
            pmu.setChatInputPath(null);
            new io.github.altkat.BuffedItems.menu.set.SetEditorMenu(pmu, plugin).open();
        }
        else if (path.equals("create_bonus_tier")) {
            try {
                int count = Integer.parseInt(input);
                if (count <= 0) throw new NumberFormatException();

                String setId = pmu.getItemToEditId();
                String basePath = "sets." + setId + ".bonuses." + count;

                if (SetsConfig.get().contains(basePath)) {
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§cBonus tier " + count + " already exists."));
                } else {
                    SetsConfig.get().createSection(basePath);
                    SetsConfig.saveAsync();
                    plugin.getSetManager().loadSets(true);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aBonus tier " + count + " created!"));
                }

                pmu.setWaitingForChatInput(false);
                pmu.setChatInputPath(null);
                new SetBonusesMenu(pmu, plugin).open();

            } catch (NumberFormatException e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number. Enter a positive integer."));
                pmu.setWaitingForChatInput(false);
                pmu.setChatInputPath(null);
                new SetBonusesMenu(pmu, plugin).open();
            }
        }
        if (path.equals("set_add_item")) {
            String newItemId = input;
            String setId = pmu.getItemToEditId();
            List<String> items = SetsConfig.get().getStringList("sets." + setId + ".items");

            if (items.contains(newItemId)) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cItem already in set."));
            } else {
                items.add(newItemId);
                SetsConfig.get().set("sets." + setId + ".items", items);
                SetsConfig.saveAsync();
                plugin.getSetManager().loadSets(true);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aAdded " + newItemId + " to set."));
            }
            pmu.setWaitingForChatInput(false);
            pmu.setChatInputPath(null);
            new SetItemsMenu(pmu, plugin).open();
        }
    }
}