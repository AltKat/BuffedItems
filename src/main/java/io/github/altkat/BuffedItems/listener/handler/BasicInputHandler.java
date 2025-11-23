package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.editor.ItemEditorMenu;
import io.github.altkat.BuffedItems.menu.editor.PermissionSettingsMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class BasicInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;
    private static final Pattern PERMISSION_NODE_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9._-]*[a-z0-9])?$", Pattern.CASE_INSENSITIVE);

    public BasicInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        switch (path) {
            case "display_name":
                handleDisplayNameEdit(player, pmu, input, itemId);
                break;
            case "permission":
                handlePermissionEdit(player, pmu, input, itemId, "permission");
                break;
            case "active_permission":
                handlePermissionEdit(player, pmu, input, itemId, "active_permission");
                break;
            case "passive_permission":
                handlePermissionEdit(player, pmu, input, itemId, "passive_permission");
                break;
            case "material.manual":
                handleMaterialManualEdit(player, pmu, input, itemId);
                break;
            case "custom_model_data":
                handleCustomModelDataEdit(player, pmu, input, itemId);
                break;
        }
    }

    private void handleDisplayNameEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        ConfigManager.setItemValue(itemId, "display_name", input);
        player.sendMessage(ConfigManager.fromSectionWithPrefix("§aDisplay name has been updated!"));
        closeChatInput(pmu);
        new ItemEditorMenu(pmu, plugin).open();
    }

    private void handlePermissionEdit(Player player, PlayerMenuUtility pmu, String input, String itemId, String configKey) {
        if ("none".equalsIgnoreCase(input) || "remove".equalsIgnoreCase(input)) {
            ConfigManager.setItemValue(itemId, configKey, null);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§a" + configKey + " has been removed/reset."));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[BasicHandler] Removed " + configKey + " for " + itemId);
        } else if (isValidPermissionNode(input)) {
            ConfigManager.setItemValue(itemId, configKey, input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§a" + configKey + " has been set to: " + input));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[BasicHandler] Set " + configKey + " for " + itemId + " to: " + input);
        } else {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid permission node! Permissions can only contain letters, numbers, dots (.), hyphens (-), and underscores (_)."));
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cYour input was: §e" + input));
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§7(Type 'cancel' to exit)"));
            return;
        }
        closeChatInput(pmu);
        new PermissionSettingsMenu(pmu, plugin).open();
    }

    private void handleMaterialManualEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        String materialName = input.toUpperCase().replaceAll("\\s+", "_");
        Material selectedMaterial = Material.matchMaterial(materialName);

        if (selectedMaterial != null && selectedMaterial.isItem()) {
            ConfigManager.setItemValue(itemId, "material", selectedMaterial.name());
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aMaterial has been updated to " + selectedMaterial.name()));
            closeChatInput(pmu);
            new ItemEditorMenu(pmu, plugin).open();
        } else {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Invalid material name: '" + input + "'"));
            player.sendMessage(ConfigManager.fromSection("§cMaterial not found or is not an item. Please try again."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
        }
    }

    private void handleCustomModelDataEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        if ("none".equalsIgnoreCase(input) || "remove".equalsIgnoreCase(input)) {
            ConfigManager.setItemValue(itemId, "custom-model-data", null);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCustom Model Data has been removed."));
            closeChatInput(pmu);
        } else {
            try {
                int directValue = Integer.parseInt(input);
                if (directValue < 0) {
                    throw new NumberFormatException();
                }
                ConfigManager.setItemValue(itemId, "custom-model-data", directValue);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCustom Model Data set to: §e" + directValue));
                closeChatInput(pmu);
            } catch (NumberFormatException ex) {
                if (input.contains(":")) {
                    ConfigManager.setItemValue(itemId, "custom-model-data", input);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCustom Model Data set to: §e" + input));
                    player.sendMessage(ConfigManager.fromSection("§7It will be resolved on next reload/save."));
                    closeChatInput(pmu);
                } else {
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid format! Use:"));
                    player.sendMessage(ConfigManager.fromSection("§e100001 §7(direct integer)"));
                    player.sendMessage(ConfigManager.fromSection("§eitemsadder:item_id"));
                    player.sendMessage(ConfigManager.fromSection("§enexo:item_id"));
                    player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                    pmu.setWaitingForChatInput(true);
                    pmu.setChatInputPath("custom_model_data");
                    return;
                }
            }
        }
        new ItemEditorMenu(pmu, plugin).open();
    }

    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }

    private boolean isValidPermissionNode(String input) {
        if (input.contains("..") || input.startsWith(".") || input.endsWith(".")) {
            return false;
        }
        return PERMISSION_NODE_PATTERN.matcher(input).matches();
    }
}