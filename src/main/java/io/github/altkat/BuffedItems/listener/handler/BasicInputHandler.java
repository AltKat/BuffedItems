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
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#?[0-9a-fA-F]{6}$");


    public BasicInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean shouldHandle(String path) {
        // Exclude lore, which is handled by LoreInputHandler
        if (path.startsWith("display.lore.")) {
            return false;
        }
        return path.startsWith("display.") ||
                path.equals("permission") ||
                path.equals("active_ability.permission") ||
                path.equals("passive_effects.permission") ||
                path.equals("material.manual");
    }

    @Override
    public void onCancel(Player player, PlayerMenuUtility pmu, String path) {
        if (path.equals("permission") || path.equals("active_ability.permission") || path.equals("passive_effects.permission")) {
            new PermissionSettingsMenu(pmu, plugin).open();
        } else {
            // display.* (name, color, custom-model-data) and material.manual
            new ItemEditorMenu(pmu, plugin).open();
        }
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        switch (path) {
            case "display.name":
                handleDisplayNameEdit(player, pmu, input, itemId);
                break;
            case "display.color":
                handleColorEdit(player, pmu, input, itemId);
                break;
            case "permission":
                handlePermissionEdit(player, pmu, input, itemId, "permission");
                break;
            case "active_ability.permission":
                handlePermissionEdit(player, pmu, input, itemId, "active_ability.permission");
                break;
            case "passive_effects.permission":
                handlePermissionEdit(player, pmu, input, itemId, "passive_effects.permission");
                break;
            case "material.manual":
                handleMaterialManualEdit(player, pmu, input, itemId);
                break;
            case "display.custom-model-data":
                handleCustomModelDataEdit(player, pmu, input, itemId);
                break;
            case "display.durability":
                handleDurabilityEdit(player, pmu, input, itemId);
                break;
            default:
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cUnknown input path: " + path));
                closeChatInput(pmu);
                new ItemEditorMenu(pmu, plugin).open();
                break;
        }
    }

    private void handleDurabilityEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        try {
            int value = Integer.parseInt(input);
            if (value < 0) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cDurability damage cannot be negative."));
                return;
            }
            ConfigManager.setItemValue(itemId, "display.durability", value);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aDurability damage set to: §e" + value));
            closeChatInput(pmu);
            new ItemEditorMenu(pmu, plugin).open();
        } catch (NumberFormatException e) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number. Please enter a valid integer (e.g., 100)."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
        }
    }

    private void handleDisplayNameEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        ConfigManager.setItemValue(itemId, "display.name", input);
        player.sendMessage(ConfigManager.fromSectionWithPrefix("§aDisplay name has been updated!"));
        closeChatInput(pmu);
        new ItemEditorMenu(pmu, plugin).open();
    }

    private void handlePermissionEdit(Player player, PlayerMenuUtility pmu, String input, String itemId, String configKey) {
        if ("none".equalsIgnoreCase(input) || "remove".equalsIgnoreCase(input)) {
            ConfigManager.setItemValue(itemId, configKey, null);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§a" + configKey.replace("active_ability.permission", "active permission").replace("passive_effects.permission", "passive permission") + " has been removed/reset."));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[BasicHandler] Removed " + configKey + " for " + itemId);
        } else if (isValidPermissionNode(input)) {
            ConfigManager.setItemValue(itemId, configKey, input);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§a" + configKey.replace("active_ability.permission", "active permission").replace("passive_effects.permission", "passive permission") + " has been set to: " + input));
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
    
    private void handleColorEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        if ("none".equalsIgnoreCase(input) || "remove".equalsIgnoreCase(input)) {
            ConfigManager.setItemValue(itemId, "display.color", null);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aArmor color has been removed."));
        } else if (HEX_COLOR_PATTERN.matcher(input).matches()) {
            String hex = input.startsWith("#") ? input : "#" + input;
            ConfigManager.setItemValue(itemId, "display.color", hex);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aArmor color has been set to: §e" + hex));
        } else {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid hex color code! Format must be #RRGGBB."));
            player.sendMessage(ConfigManager.fromSection("§cYour input: §e" + input));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("display.color");
            return;
        }

        closeChatInput(pmu);
        new ItemEditorMenu(pmu, plugin).open();
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
            ConfigManager.setItemValue(itemId, "display.custom-model-data", null);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCustom Model Data has been removed."));
            closeChatInput(pmu);
        } else {
            try {
                int directValue = Integer.parseInt(input);
                if (directValue < 0) {
                    throw new NumberFormatException();
                }
                ConfigManager.setItemValue(itemId, "display.custom-model-data", directValue);
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCustom Model Data set to: §e" + directValue));
                closeChatInput(pmu);
            } catch (NumberFormatException ex) {
                if (input.contains(":")) {
                    ConfigManager.setItemValue(itemId, "display.custom-model-data", input);
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
                    pmu.setChatInputPath("display.custom-model-data");
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