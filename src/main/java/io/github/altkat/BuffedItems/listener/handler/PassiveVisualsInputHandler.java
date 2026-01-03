package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.passive.PassiveItemVisualsMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.menu.utility.SoundSettingsMenu;
import org.bukkit.entity.Player;

public class PassiveVisualsInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public PassiveVisualsInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean shouldHandle(String path) {
        return path.startsWith("passive_effects.visuals.");
    }

    @Override
    public void onCancel(Player player, PlayerMenuUtility pmu, String path) {
        new PassiveItemVisualsMenu(pmu, plugin).open();
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        
        // Handle numeric inputs
        if (path.endsWith(".duration")) {
            try {
                double val = Double.parseDouble(input);
                if (val < 0) throw new NumberFormatException();
                ConfigManager.setItemValue(itemId, path, (int) val);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aValue updated to: §e" + (int) val));
            } catch (NumberFormatException e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number! Please enter a non-negative number."));
                return;
            }
        }
        else if (path.endsWith(".stay") || path.endsWith(".delay") || path.endsWith(".fade-in") || path.endsWith(".fade-out")) {
            try {
                double val = Double.parseDouble(input);
                if (val < 0) throw new NumberFormatException();
                int ticks = (int) (val * 20);
                ConfigManager.setItemValue(itemId, path, ticks);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aValue updated to: §e" + val + "s §7(" + ticks + " ticks)"));
            } catch (NumberFormatException e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number! Please enter a non-negative number (seconds)."));
                return;
            }
        } 
        // Handle string inputs
        else if (path.equals("passive_effects.visuals.title.header")) {
            if (input.contains("|")) {
                String[] parts = input.split("\\|", 2);
                ConfigManager.setItemValue(itemId, "passive_effects.visuals.title.header", parts[0].trim().equalsIgnoreCase("none") ? null : parts[0].trim());
                ConfigManager.setItemValue(itemId, "passive_effects.visuals.title.subtitle", parts[1].trim().equalsIgnoreCase("none") ? null : parts[1].trim());
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aTitle and Subtitle updated!"));
            } else {
                ConfigManager.setItemValue(itemId, path, input.equalsIgnoreCase("none") ? null : input);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aTitle header updated!"));
            }
        } else if (path.equals("passive_effects.visuals.sound.sound")) {
            if (!isValidSoundFormat(input)) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid format! Use: NAME;VOLUME;PITCH"));
                return;
            }
            ConfigManager.setItemValue(itemId, path, input.equalsIgnoreCase("none") ? null : input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSound updated!"));
            
            closeChatInput(pmu);
            new SoundSettingsMenu(pmu, plugin, "passive").open();
            return;
        }
        else if (path.equals("passive_effects.visuals.action-bar.message") || path.equals("passive_effects.visuals.boss-bar.title")) {
            ConfigManager.setItemValue(itemId, path, input.equalsIgnoreCase("none") ? null : input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSetting updated!"));
        } else {
             player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Unknown input path '" + path + "' in PassiveVisualsInputHandler."));
        }

        closeChatInput(pmu);
        new PassiveItemVisualsMenu(pmu, plugin).open();
    }

    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }

    private boolean isValidSoundFormat(String input) {
        if (input.equalsIgnoreCase("none")) return true;
        if (input.contains(";")) {
            String[] parts = input.split(";");
            try {
                if (parts.length > 1) Float.parseFloat(parts[1]);
                if (parts.length > 2) Float.parseFloat(parts[2]);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
}
