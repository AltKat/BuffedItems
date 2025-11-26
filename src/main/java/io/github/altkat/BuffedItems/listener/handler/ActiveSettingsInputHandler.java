package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.active.ActiveItemSettingsMenu;
import io.github.altkat.BuffedItems.menu.active.ActiveItemVisualsMenu;
import io.github.altkat.BuffedItems.menu.active.CommandListMenu;
import io.github.altkat.BuffedItems.menu.active.UsageLimitSettingsMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.menu.utility.SoundSettingsMenu;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ActiveSettingsInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public ActiveSettingsInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path.equals("active.cooldown")) {
            handleActiveCooldownEdit(player, pmu, input, itemId);
        } else if (path.equals("active.duration")) {
            handleActiveDurationEdit(player, pmu, input, itemId);
        } else if (path.equals("active.commands.add")) {
            handleAddCommand(player, pmu, input, itemId);
        } else if (path.startsWith("active.commands.edit.")) {
            handleEditCommand(player, pmu, input, path, itemId);
        } else if (path.startsWith("active.msg.")) {
            handleActiveMessageEdit(player, pmu, input, path, itemId);
        } else if (path.startsWith("active.sounds.")) {
            handleActiveSoundEdit(player, pmu, input, path, itemId);
        }else if (path.startsWith("usage-limit.")) {
            handleUsageLimitEdit(player, pmu, input, path, itemId);
        }
    }

    private void handleActiveCooldownEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        try {
            int val = Integer.parseInt(input);
            if (val < 0) throw new NumberFormatException();
            ConfigManager.setItemValue(itemId, "cooldown", val);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCooldown updated to: §e" + val + "s"));
            closeChatInput(pmu);
            new ActiveItemSettingsMenu(pmu, plugin).open();
        } catch (NumberFormatException error) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number! Please enter a positive integer (e.g. 10)."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
        }
    }

    private void handleActiveDurationEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        try {
            int val = Integer.parseInt(input);
            if (val < 0) throw new NumberFormatException();
            ConfigManager.setItemValue(itemId, "effect_duration", val);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aEffect Duration updated to: §e" + val + "s"));
            closeChatInput(pmu);
            new ActiveItemSettingsMenu(pmu, plugin).open();
        } catch (NumberFormatException error) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number! Please enter a positive integer (e.g. 5)."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
        }
    }


    private void handleAddCommand(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        List<String> commands = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + ".active-mode.commands"));
        if (commands.isEmpty() && input.trim().toLowerCase().startsWith("[else]")) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: The VERY FIRST command cannot start with [else]!"));
            player.sendMessage(ConfigManager.fromSection("§7Logic requires a preceding command to fail first."));
            player.sendMessage(ConfigManager.fromSection("§ePlease enter the command again without [else]."));
            return;
        }

        commands.add(input);
        ConfigManager.setItemValue(itemId, "commands", commands);
        player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCommand added!"));
        closeChatInput(pmu);
        new CommandListMenu(pmu, plugin).open();
    }

    private void handleEditCommand(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        try {
            int index = Integer.parseInt(path.substring(21));
            List<String> commands = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + ".active-mode.commands"));

            if (index >= 0 && index < commands.size()) {
                commands.set(index, input);
                ConfigManager.setItemValue(itemId, "commands", commands);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCommand updated!"));
            } else {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Command index out of bounds."));
            }
            closeChatInput(pmu);
            new CommandListMenu(pmu, plugin).open();
        } catch (Exception error) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError processing edit."));
            closeChatInput(pmu);
            new CommandListMenu(pmu, plugin).open();
        }
    }


    private void handleActiveMessageEdit(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        String messageType = path.substring(11);
        String configPath = getMessageConfigPath(messageType);

        if (configPath == null) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Unknown message type: " + messageType));
            closeChatInput(pmu);
            new ActiveItemVisualsMenu(pmu, plugin).open();
            return;
        }

        if ("default".equalsIgnoreCase(input)) {
            if ("title".equals(messageType)) {
                ConfigManager.setItemValue(itemId, "visuals.messages.subtitle", null);
            }
            ConfigManager.setItemValue(itemId, configPath, null);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aReset to default."));
        } else if ("title".equals(messageType) && input.contains("|")) {
            String[] parts = input.split("\\|", 2);
            ConfigManager.setItemValue(itemId, configPath, parts[0].trim());
            ConfigManager.setItemValue(itemId, "visuals.messages.subtitle", parts[1].trim());
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aTitle and Subtitle updated!"));
        } else {
            ConfigManager.setItemValue(itemId, configPath, input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aMessage updated!"));
        }
        closeChatInput(pmu);
        new ActiveItemVisualsMenu(pmu, plugin).open();
    }

    private void handleActiveSoundEdit(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        String soundType = path.substring(14);

        if (!isValidSoundFormat(input)) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid format! Use: NAME;VOLUME;PITCH"));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("active.sounds." + soundType);
            return;
        }

        ConfigManager.setItemValue(itemId, "sounds." + soundType, input);
        player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSound updated!"));

        try {
            String soundName = input.split(";")[0];
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ignored) {
            // Custom sound, can't preview
        }

        closeChatInput(pmu);
        new SoundSettingsMenu(pmu, plugin, soundType).open();
    }

    private void handleUsageLimitEdit(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path.equals("usage-limit.max-usage")) {
            try {
                int val = Integer.parseInt(input);
                if (val < -1 || val == 0) throw new NumberFormatException();

                ConfigManager.setItemValue(itemId, "usage-limit.max-usage", val);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aMax Usage updated to: §e" + (val == -1 ? "Unlimited" : val)));
            } catch (NumberFormatException e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number! Enter a positive integer or -1 for unlimited."));
            }
        }
        else if (path.equals("usage-limit.transform-item")) {
            if (plugin.getItemManager().getBuffedItem(input) == null) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§eWarning: Item ID '" + input + "' does not exist yet."));
            }
            ConfigManager.setItemValue(itemId, "usage-limit.transform-item", input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aTransform Target updated to: §e" + input));
        }
        else {
            ConfigManager.setItemValue(itemId, path, input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aMessage setting updated!"));
        }

        closeChatInput(pmu);
        new UsageLimitSettingsMenu(pmu, plugin).open();
    }

    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }

    private String getMessageConfigPath(String messageType) {
        return switch (messageType) {
            case "chat" -> "visuals.messages.cooldown-chat";
            case "title" -> "visuals.messages.cooldown-title";
            case "actionbar" -> "visuals.messages.cooldown-action-bar";
            case "bossbar" -> "visuals.messages.cooldown-boss-bar";
            default -> null;
        };
    }

    private boolean isValidSoundFormat(String input) {
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