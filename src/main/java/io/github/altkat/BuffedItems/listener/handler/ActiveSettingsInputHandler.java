package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.active.*;
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
    public boolean shouldHandle(String path) {
        if (path.startsWith("active_ability.actions.effects.") ||
                path.startsWith("active_ability.costs.") ||
                path.equals("active_ability.permission")) {
            return false;
        }
        return path.startsWith("active_ability.") || path.startsWith("usage.");
    }

    @Override
    public void onCancel(Player player, PlayerMenuUtility pmu, String path) {
        if (path.startsWith("usage.commands.")) {
            new CommandListMenu(pmu, plugin, CommandListMenu.CommandContext.DEPLETION).open();
            return;
        }

        if (path.startsWith("usage.")) {
            new UsageLimitSettingsMenu(pmu, plugin).open();
            return;
        }

        if (path.startsWith("active_ability.commands.")) {
            new CommandListMenu(pmu, plugin, CommandListMenu.CommandContext.ACTIVE).open();
            return;
        }

        if (path.startsWith("active_ability.visuals.cast.")) {
            if (path.contains("boss-bar") && path.contains("color")) {
                new io.github.altkat.BuffedItems.menu.selector.BossBarColorMenu(pmu, plugin).open();
                return;
            }
             if (path.contains("boss-bar")) {
                new ActiveCastBossBarSettingsMenu(pmu, plugin).open();
                return;
            }
            if (path.contains("sound")) {
                new SoundSettingsMenu(pmu, plugin, "cast").open();
                return;
            }
            new ActiveItemCastVisualsMenu(pmu, plugin).open();
            return;
        }

        if (path.startsWith("active_ability.visuals.cooldown.")) {
            new ActiveItemCooldownVisualsMenu(pmu, plugin).open();
            return;
        }

        if (path.startsWith("active_ability.visuals.")) {
            new ActiveItemVisualsMenu(pmu, plugin).open();
            return;
        }

        if (path.startsWith("active_ability.sounds.")) {
            new ActiveItemSoundsMenu(pmu, plugin).open();
            return;
        }

        new ActiveItemSettingsMenu(pmu, plugin).open();
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path.equals("active_ability.cooldown")) {
            handleActiveCooldownEdit(player, pmu, input, itemId);
        } else if (path.equals("active_ability.duration")) {
            handleActiveDurationEdit(player, pmu, input, itemId);
        } else if (path.equals("active_ability.commands.add")) {
            handleAddCommand(player, pmu, input, itemId);
        } else if (path.startsWith("active_ability.commands.edit.")) {
            handleEditCommand(player, pmu, input, path, itemId);
        } else if (path.startsWith("active_ability.visuals.cast.")) {
            handleActiveCastVisualEdit(player, pmu, input, path, itemId);
        } else if (path.startsWith("active_ability.visuals.cooldown.")) {
            handleActiveMessageEdit(player, pmu, input, path, itemId);
        } else if (path.startsWith("active_ability.sounds.")) {
            handleActiveSoundEdit(player, pmu, input, path, itemId);
        }else if (path.startsWith("usage.")) {
            handleUsageLimitEdit(player, pmu, input, path, itemId);
        } else {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cUnknown input path: " + path));
            closeChatInput(pmu);
            new ActiveItemSettingsMenu(pmu, plugin).open();
        }
    }

    private void handleActiveCastVisualEdit(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        // Numeric inputs
        if (path.endsWith(".duration")) {
            try {
                double val = Double.parseDouble(input);
                if (val < 0) throw new NumberFormatException();
                ConfigManager.setItemValue(itemId, path, (int) val);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aDuration updated to: §e" + (int) val + "s"));
            } catch (NumberFormatException e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number! Please enter a non-negative number."));
                return;
            }
        }
        else if (path.endsWith(".stay") || path.endsWith(".delay")) {
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
        // String inputs
        else if (path.endsWith(".title.header")) {
            if (input.contains("|")) {
                String[] parts = input.split("\\|", 2);
                ConfigManager.setItemValue(itemId, "active_ability.visuals.cast.title.header", parts[0].trim().equalsIgnoreCase("none") ? null : parts[0].trim());
                ConfigManager.setItemValue(itemId, "active_ability.visuals.cast.title.subtitle", parts[1].trim().equalsIgnoreCase("none") ? null : parts[1].trim());
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aTitle and Subtitle updated!"));
            } else {
                ConfigManager.setItemValue(itemId, path, input.equalsIgnoreCase("none") ? null : input);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aTitle header updated!"));
            }
        } else if (path.endsWith(".sound.sound")) {
            if (!isValidSoundFormat(input)) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid format! Use: NAME;VOLUME;PITCH"));
                return;
            }
            ConfigManager.setItemValue(itemId, path, input.equalsIgnoreCase("none") ? null : input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSound updated!"));

            closeChatInput(pmu);
            new SoundSettingsMenu(pmu, plugin, "cast").open();
            return;
        }
        else if (path.endsWith(".action-bar.message") || path.endsWith(".boss-bar.title")) {
            ConfigManager.setItemValue(itemId, path, input.equalsIgnoreCase("none") ? null : input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSetting updated!"));
        } else {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Unknown cast visual path '" + path + "'."));
        }

        closeChatInput(pmu);
        if (path.contains("boss-bar")) {
            new ActiveCastBossBarSettingsMenu(pmu, plugin).open();
        } else {
            new ActiveItemCastVisualsMenu(pmu, plugin).open();
        }
    }

    private void handleActiveCooldownEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        try {
            int val = Integer.parseInt(input);
            if (val < 0) throw new NumberFormatException();
            ConfigManager.setItemValue(itemId, "active_ability.cooldown", val);
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
            ConfigManager.setItemValue(itemId, "active_ability.duration", val);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aEffect Duration updated to: §e" + val + "s"));
            closeChatInput(pmu);
            new ActiveItemSettingsMenu(pmu, plugin).open();
        } catch (NumberFormatException error) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number! Please enter a positive integer (e.g. 5)."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
        }
    }


    private void handleAddCommand(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        List<String> commands = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + ".active_ability.actions.commands"));
        if (commands.isEmpty() && input.trim().toLowerCase().startsWith("[else]")) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: The VERY FIRST command cannot start with [else]!"));
            player.sendMessage(ConfigManager.fromSection("§7Logic requires a preceding command to fail first."));
            player.sendMessage(ConfigManager.fromSection("§ePlease enter the command again without [else]."));
            return;
        }

        commands.add(input);
        ConfigManager.setItemValue(itemId, "active_ability.actions.commands", commands);
        player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCommand added!"));
        closeChatInput(pmu);
        new CommandListMenu(pmu, plugin, CommandListMenu.CommandContext.ACTIVE).open();
    }

    private void handleEditCommand(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        try {
            int lastDotIndex = path.lastIndexOf('.');
            int index = Integer.parseInt(path.substring(lastDotIndex + 1));
            
            List<String> commands = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + ".active_ability.actions.commands"));

            if (index >= 0 && index < commands.size()) {
                commands.set(index, input);
                ConfigManager.setItemValue(itemId, "active_ability.actions.commands", commands);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCommand updated!"));
            } else {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Command index out of bounds."));
            }
            closeChatInput(pmu);
            new CommandListMenu(pmu, plugin, CommandListMenu.CommandContext.ACTIVE).open();
        } catch (Exception error) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError processing edit: " + error.getMessage()));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[ActiveInput] Error parsing index from path: " + path);
            closeChatInput(pmu);
            new CommandListMenu(pmu, plugin, CommandListMenu.CommandContext.ACTIVE).open();
        }
    }


    private void handleActiveMessageEdit(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        String messageType;
        try {
            int startIndex = path.indexOf("cooldown.") + 9;
            int endIndex = path.lastIndexOf(".message");
            messageType = path.substring(startIndex, endIndex);
        } catch (Exception e) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Could not parse message type from path: " + path));
            closeChatInput(pmu);
            new ActiveItemCooldownVisualsMenu(pmu, plugin).open();
            return;
        }

        String configPath = getMessageConfigPath(messageType);

        if (configPath == null) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Unknown message type: " + messageType));
            closeChatInput(pmu);
            new ActiveItemCooldownVisualsMenu(pmu, plugin).open();
            return;
        }

        if ("default".equalsIgnoreCase(input)) {
            if ("title".equals(messageType)) {
                ConfigManager.setItemValue(itemId, "active_ability.visuals.cooldown.title.subtitle", null);
            }
            ConfigManager.setItemValue(itemId, configPath, null);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aReset to default."));
        } else if ("title".equals(messageType) && input.contains("|")) {
            String[] parts = input.split("\\|", 2);
            ConfigManager.setItemValue(itemId, configPath, parts[0].trim());
            ConfigManager.setItemValue(itemId, "active_ability.visuals.cooldown.title.subtitle", parts[1].trim());
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aTitle and Subtitle updated!"));
        } else {
            ConfigManager.setItemValue(itemId, configPath, input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aMessage updated!"));
        }
        closeChatInput(pmu);
        new ActiveItemCooldownVisualsMenu(pmu, plugin).open();
    }

    private void handleActiveSoundEdit(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        String soundType = path.substring(path.lastIndexOf('.') + 1);

        if (!isValidSoundFormat(input)) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid format! Use: NAME;VOLUME;PITCH"));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath(path);
            return;
        }

        ConfigManager.setItemValue(itemId, "active_ability.sounds." + soundType, input);
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
        if (path.equals("usage.commands.add")) {
            List<String> commands = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + ".active_ability.usage.commands"));
            commands.add(input);
            ConfigManager.setItemValue(itemId, "usage.commands", commands);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aDepletion command added!"));
            closeChatInput(pmu);
            new CommandListMenu(pmu, plugin, CommandListMenu.CommandContext.DEPLETION).open();
            return;
        }
        else if (path.startsWith("usage.commands.edit.")) {
            try {
                int index = Integer.parseInt(path.substring(20));
                List<String> commands = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + ".active_ability.usage.commands"));

                if (index >= 0 && index < commands.size()) {
                    commands.set(index, input);
                    ConfigManager.setItemValue(itemId, "usage.commands", commands);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aDepletion command updated!"));
                } else {
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Command index out of bounds."));
                }
                closeChatInput(pmu);
                new CommandListMenu(pmu, plugin, CommandListMenu.CommandContext.DEPLETION).open();
            } catch (Exception e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError processing edit."));
                closeChatInput(pmu);
                new CommandListMenu(pmu, plugin, CommandListMenu.CommandContext.DEPLETION).open();
            }
            return;
        }
        else if (path.equals("usage.depletion_sound") || path.equals("usage.depleted_try_sound")) {
             if (!isValidSoundFormat(input)) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid format! Use: NAME;VOLUME;PITCH"));
                player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                pmu.setWaitingForChatInput(true);
                pmu.setChatInputPath(path);
                return;
            }
            ConfigManager.setItemValue(itemId, path, input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSound updated!"));
        }
        else if (path.equals("usage.limit")) {
            try {
                int val = Integer.parseInt(input);
                if (val < -1 || val == 0) throw new NumberFormatException();

                ConfigManager.setItemValue(itemId, "usage.limit", val);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aMax Usage updated to: §e" + (val == -1 ? "Unlimited" : val)));
            } catch (NumberFormatException e) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number! Enter a positive integer or -1 for unlimited."));
            }
        }
        else if (path.equals("usage.transform_item")) {
            if (plugin.getItemManager().getBuffedItem(input) == null) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§eWarning: Item ID '" + input + "' does not exist yet."));
            }
            ConfigManager.setItemValue(itemId, "usage.transform_item", input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aTransform Target updated to: §e" + input));
        }
        else if (path.startsWith("usage.")) {
             String key = path.substring(6);
             ConfigManager.setItemValue(itemId, "usage." + key, input);
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
            case "chat" -> "active_ability.visuals.cooldown.chat.message";
            case "title" -> "active_ability.visuals.cooldown.title.message";
            case "action-bar" -> "active_ability.visuals.cooldown.action-bar.message";
            case "boss-bar" -> "active_ability.visuals.cooldown.boss-bar.message";
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