package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.menu.set.SetListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.entity.Player;

public class SetInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public SetInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
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
                new io.github.altkat.BuffedItems.menu.set.SetBonusesMenu(pmu, plugin).open();

            } catch (NumberFormatException e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number. Enter a positive integer."));
                pmu.setWaitingForChatInput(false);
                pmu.setChatInputPath(null);
                new io.github.altkat.BuffedItems.menu.set.SetBonusesMenu(pmu, plugin).open();
            }
        }
    }
}