package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.active.ActiveItemSettingsMenu;
import io.github.altkat.BuffedItems.menu.active.ActiveItemVisualsMenu;
import io.github.altkat.BuffedItems.menu.active.CommandListMenu;
import io.github.altkat.BuffedItems.menu.active.CostListMenu;
import io.github.altkat.BuffedItems.menu.editor.EnchantmentListMenu;
import io.github.altkat.BuffedItems.menu.editor.ItemEditorMenu;
import io.github.altkat.BuffedItems.menu.editor.LoreEditorMenu;
import io.github.altkat.BuffedItems.menu.passive.EffectListMenu;
import io.github.altkat.BuffedItems.menu.selector.CostTypeSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.MainMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.menu.utility.SoundSettingsMenu;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Handles all chat-based input for the BuffedItems in-game editor.
 * Optimized with DRY principles to minimize code duplication.
 */
public class ChatListener implements Listener {

    private final BuffedItems plugin;
    private static final Pattern PERMISSION_NODE_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9._-]*[a-z0-9])?$", Pattern.CASE_INSENSITIVE);

    /**
     * Effect type enumeration for generic handling
     */
    public enum EffectType {
        POTION_EFFECT("potion effect", PotionEffectType.class),
        ATTRIBUTE("attribute", org.bukkit.attribute.Attribute.class),
        ENCHANTMENT("enchantment", Enchantment.class);

        private final String displayName;
        private final Class<?> validationClass;

        EffectType(String displayName, Class<?> validationClass) {
            this.displayName = displayName;
            this.validationClass = validationClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Class<?> getValidationClass() {
            return validationClass;
        }
    }

    public ChatListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent e) {
        Player player = e.getPlayer();
        PlayerMenuUtility pmu = BuffedItems.getPlayerMenuUtility(player);

        if (!pmu.isWaitingForChatInput()) {
            return;
        }

        e.setCancelled(true);

        String input = PlainTextComponentSerializer.plainText().serialize(e.message());
        String path = pmu.getChatInputPath();
        String itemId = pmu.getItemToEditId();

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                () -> "[Chat] Processing input from " + player.getName() + ": path=" + path + ", input=" + input);

        Bukkit.getScheduler().runTask(plugin, () -> {
            handleChatInput(player, pmu, input, path, itemId);
        });
    }

    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }

    /**
     * Main chat input handler - routes all paths to appropriate methods
     */
    private void handleChatInput(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path == null) {
            closeChatInput(pmu);
            return;
        }

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ConfigManager.fromSection("§cOperation cancelled."));
            handleCancelAction(player, pmu, path);
            closeChatInput(pmu);
            return;
        }

        if (path.equals("createnewitem")) {
            handleCreateNewItem(player, pmu, input);
        } else if (path.equals("duplicateitem")) {
            handleDuplicateItem(player, pmu, input);
        } else if (path.startsWith("lore.")) {
            handleLoreEdit(player, pmu, input, path, itemId);
        } else if (path.equals("display_name")) {
            handleDisplayNameEdit(player, pmu, input, itemId);
        } else if (path.equals("permission")) {
            handlePermissionEdit(player, pmu, input, itemId);
        } else if (path.equals("material.manual")) {
            handleMaterialManualEdit(player, pmu, input, itemId);
        } else if (path.equals("custom_model_data")) {
            handleCustomModelDataEdit(player, pmu, input, itemId);
        } else if (path.equals("active.cooldown")) {
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
        } else if (path.equals("active.potion_effects.edit")) {
            handleEditGenericEffect(player, pmu, input, itemId, EffectType.POTION_EFFECT,
                    "items." + itemId + ".active_effects.potion_effects", "ACTIVE", null);
        } else if (path.startsWith("active.potion_effects.add.")) {
            handleAddGenericEffect(player, pmu, input, path, itemId,
                    "items." + itemId + ".active_effects.potion_effects", EffectType.POTION_EFFECT,
                    p -> p.substring(26), "ACTIVE", null);
        } else if (path.equals("active.attributes.edit")) {
            handleEditGenericEffect(player, pmu, input, itemId, EffectType.ATTRIBUTE,
                    "items." + itemId + ".active_effects.attributes", "ACTIVE", null);
        } else if (path.startsWith("active.attributes.add.")) {
            handleAddGenericEffect(player, pmu, input, path, itemId,
                    "items." + itemId + ".active_effects.attributes", EffectType.ATTRIBUTE,
                    p -> p.substring(22), "ACTIVE", null);
        } else if (path.equals("potion_effects.edit")) {
            String slot = pmu.getTargetSlot();
            handleEditGenericEffect(player, pmu, input, itemId, EffectType.POTION_EFFECT,
                    "items." + itemId + ".effects." + slot + ".potion_effects", slot, slot);
        } else if (path.startsWith("potion_effects.add.")) {
            String slot = pmu.getTargetSlot();
            handleAddGenericEffect(player, pmu, input, path, itemId,
                    "items." + itemId + ".effects." + slot + ".potion_effects", EffectType.POTION_EFFECT,
                    p -> p.substring(19), slot, slot);
        } else if (path.equals("attributes.edit")) {
            String slot = pmu.getTargetSlot();
            handleEditGenericEffect(player, pmu, input, itemId, EffectType.ATTRIBUTE,
                    "items." + itemId + ".effects." + slot + ".attributes", slot, slot);
        } else if (path.startsWith("attributes.add.")) {
            String slot = pmu.getTargetSlot();
            handleAddGenericEffect(player, pmu, input, path, itemId,
                    "items." + itemId + ".effects." + slot + ".attributes", EffectType.ATTRIBUTE,
                    p -> p.substring(15), slot, slot);
        } else if (path.equals("enchantments.edit")) {
            handleEditEnchantment(player, pmu, input, itemId);
        } else if (path.startsWith("enchantments.add.")) {
            handleAddEnchantment(player, pmu, input, path, itemId);
        } else if (path.startsWith("active.costs.add.")) {
            String type = path.substring(17);
            handleAddCost(player, pmu, input, type, itemId);
        } else if (path.equals("active.costs.edit.amount")) {
            handleEditCostAmount(player, pmu, input, itemId);
        } else if (path.equals("active.costs.edit.message")) {
            handleEditCostMessage(player, pmu, input, itemId);
        } else {
            player.sendMessage(ConfigManager.fromSection("§cError: Unknown input path: " + path));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[Chat] Attempted to set unknown path via chat: " + path);
            closeChatInput(pmu);
        }
    }

    /**
     * Handles cancel action - returns to appropriate menu based on path type
     */
    private void handleCancelAction(Player player, PlayerMenuUtility pmu, String path) {
        if (path.equals("createnewitem") || path.equals("duplicateitem")) {
            new MainMenu(pmu, plugin).open();
        } else if (path.startsWith("lore.")) {
            new LoreEditorMenu(pmu, plugin).open();
        } else if (path.equals("display_name") || path.equals("permission") ||
                path.equals("material.manual") || path.equals("custom_model_data")) {
            new ItemEditorMenu(pmu, plugin).open();
        } else if (path.startsWith("active.")) {
            if (path.startsWith("active.msg.") || path.startsWith("active.sounds.")) {
                new ActiveItemVisualsMenu(pmu, plugin).open();
            } else if (path.equals("active.cooldown") || path.equals("active.duration") ||
                    path.equals("active.commands.add") || path.startsWith("active.commands.edit.")) {
                new ActiveItemSettingsMenu(pmu, plugin).open();
            } else if (path.startsWith("active.potion_effects")) {
                new EffectListMenu(pmu, plugin,
                        EffectListMenu.EffectType.POTION_EFFECT, "ACTIVE").open();
            } else if (path.startsWith("active.attributes")) {
                new EffectListMenu(pmu, plugin,
                        EffectListMenu.EffectType.ATTRIBUTE, "ACTIVE").open();
            }
        } else if (path.startsWith("potion_effects")) {
            String slot = pmu.getTargetSlot();
            new EffectListMenu(pmu, plugin,
                    EffectListMenu.EffectType.POTION_EFFECT, slot).open();
        } else if (path.startsWith("attributes")) {
            String slot = pmu.getTargetSlot();
            new EffectListMenu(pmu, plugin,
                    EffectListMenu.EffectType.ATTRIBUTE, slot).open();
        } else if (path.startsWith("enchantments")) {
            new EnchantmentListMenu(pmu, plugin).open();
        }
    }

    // ==================== ITEM CREATION & DUPLICATION ====================

    private void handleCreateNewItem(Player player, PlayerMenuUtility pmu, String input) {
        String newItemId = input.toLowerCase().replaceAll("\\s+", "_");
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[Chat] Creating new item: " + newItemId);

        if (ConfigManager.createNewItem(newItemId)) {
            player.sendMessage(ConfigManager.fromSection("§aNew item '" + newItemId + "' created. Now editing..."));
            pmu.setItemToEditId(newItemId);
            closeChatInput(pmu);
            new ItemEditorMenu(pmu, plugin).open();
        } else {
            player.sendMessage(ConfigManager.fromSection("§cError: An item with the ID '" + newItemId + "' already exists."));
            closeChatInput(pmu);
            new MainMenu(pmu, plugin).open();
        }
    }

    private void handleDuplicateItem(Player player, PlayerMenuUtility pmu, String input) {
        String sourceItemId = pmu.getItemToEditId();
        String newItemId = input.toLowerCase().replaceAll("\\s+", "_");

        if (newItemId.equals(sourceItemId)) {
            player.sendMessage(ConfigManager.fromSection("§cError: New ID cannot be the same as the source ID."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("duplicateitem");
            return;
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[Chat] Attempting to duplicate '" + sourceItemId + "' as '" + newItemId + "'");

        String createdId = ConfigManager.duplicateItem(sourceItemId, newItemId);

        if (createdId != null) {
            player.sendMessage(ConfigManager.fromSection("§aItem '§e" + sourceItemId + "§a' successfully duplicated as '§e" + createdId + "§a'."));
            closeChatInput(pmu);
            new MainMenu(pmu, plugin).open();
        } else {
            player.sendMessage(ConfigManager.fromSection("§cError: An item with the ID '§e" + newItemId + "§c' already exists."));
            player.sendMessage(ConfigManager.fromSection("§aPlease try a different ID."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("duplicateitem");
        }
    }

    // ==================== BASIC ITEM PROPERTIES ====================

    private void handleDisplayNameEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        ConfigManager.setItemValue(itemId, "display_name", input);
        player.sendMessage(ConfigManager.fromSection("§aDisplay name has been updated!"));
        closeChatInput(pmu);
        new ItemEditorMenu(pmu, plugin).open();
    }

    private void handlePermissionEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        if ("none".equalsIgnoreCase(input) || "remove".equalsIgnoreCase(input)) {
            ConfigManager.setItemValue(itemId, "permission", null);
            player.sendMessage(ConfigManager.fromSection("§aPermission has been removed."));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[Chat] Removed permission for " + itemId);
        } else if (isValidPermissionNode(input)) {
            ConfigManager.setItemValue(itemId, "permission", input);
            player.sendMessage(ConfigManager.fromSection("§aPermission has been set!"));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[Chat] Set permission for " + itemId + " to: " + input);
        } else {
            player.sendMessage(ConfigManager.fromSection("§cInvalid permission node! Permissions can only contain letters, numbers, dots (.), hyphens (-), and underscores (_)."));
            player.sendMessage(ConfigManager.fromSection("§cYour input was: §e" + input));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("permission");
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
            player.sendMessage(ConfigManager.fromSection("§aMaterial has been updated to " + selectedMaterial.name()));
            closeChatInput(pmu);
            new ItemEditorMenu(pmu, plugin).open();
        } else {
            player.sendMessage(ConfigManager.fromSection("§cError: Invalid material name: '" + input + "'"));
            player.sendMessage(ConfigManager.fromSection("§cMaterial not found or is not an item. Please try again."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("material.manual");
        }
    }

    private void handleCustomModelDataEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        if ("none".equalsIgnoreCase(input) || "remove".equalsIgnoreCase(input)) {
            ConfigManager.setItemValue(itemId, "custom-model-data", null);
            player.sendMessage(ConfigManager.fromSection("§aCustom Model Data has been removed."));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[Chat] Removed custom-model-data for " + itemId);
            closeChatInput(pmu);
        } else {
            try {
                int directValue = Integer.parseInt(input);
                if (directValue < 0) {
                    throw new NumberFormatException();
                }
                ConfigManager.setItemValue(itemId, "custom-model-data", directValue);
                player.sendMessage(ConfigManager.fromSection("§aCustom Model Data set to: §e" + directValue));
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                        () -> "[Chat] Set direct custom-model-data for " + itemId + ": " + directValue);
                closeChatInput(pmu);
            } catch (NumberFormatException ex) {
                if (input.contains(":")) {
                    ConfigManager.setItemValue(itemId, "custom-model-data", input);
                    player.sendMessage(ConfigManager.fromSection("§aCustom Model Data set to: §e" + input));
                    player.sendMessage(ConfigManager.fromSection("§7It may be resolved on next reload."));
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                            () -> "[Chat] Set external custom-model-data for " + itemId + ": " + input);
                    closeChatInput(pmu);
                } else {
                    player.sendMessage(ConfigManager.fromSection("§cInvalid format! Use:"));
                    player.sendMessage(ConfigManager.fromSection("§e100001 §7(direct integer)"));
                    player.sendMessage(ConfigManager.fromSection("§eitemsadder:item_id"));
                    player.sendMessage(ConfigManager.fromSection("§enexo:item_id"));
                    player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                            () -> "[Chat] Invalid custom-model-data format from " + player.getName() + ": " + input);
                    pmu.setWaitingForChatInput(true);
                    pmu.setChatInputPath("custom_model_data");
                    return;
                }
            }
        }
        new ItemEditorMenu(pmu, plugin).open();
    }

    // ==================== LORE EDITING ====================

    private void handleLoreEdit(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) {
            player.sendMessage(ConfigManager.fromSection("§cError: Item '" + itemId + "' not found in memory."));
            closeChatInput(pmu);
            new MainMenu(pmu, plugin).open();
            return;
        }

        List<String> currentLore = new ArrayList<>(item.getLore());

        if (path.equals("lore.add")) {
            currentLore.add(input);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                    () -> "[Chat] Added new lore line to " + itemId);
        } else {
            try {
                int index = Integer.parseInt(path.substring(5));
                if (index >= 0 && index < currentLore.size()) {
                    currentLore.set(index, input);
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                            () -> "[Chat] Updated lore line " + index + " for " + itemId);
                } else {
                    player.sendMessage(ConfigManager.fromSection("§cError: Invalid lore index."));
                    closeChatInput(pmu);
                    new LoreEditorMenu(pmu, plugin).open();
                    return;
                }
            } catch (NumberFormatException ex) {
                player.sendMessage(ConfigManager.fromSection("§cError: Could not parse lore index from path: " + path));
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                        () -> "[Chat] Could not parse lore index from path: " + path);
                closeChatInput(pmu);
                new LoreEditorMenu(pmu, plugin).open();
                return;
            }
        }

        ConfigManager.setItemValue(itemId, "lore", currentLore);
        player.sendMessage(ConfigManager.fromSection("§aLore has been updated!"));
        closeChatInput(pmu);
        new LoreEditorMenu(pmu, plugin).open();
    }

    // ==================== ACTIVE ITEM: COOLDOWN & DURATION ====================

    private void handleActiveCooldownEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        try {
            int val = Integer.parseInt(input);
            if (val < 0) throw new NumberFormatException();
            ConfigManager.setItemValue(itemId, "cooldown", val);
            player.sendMessage(ConfigManager.fromSection("§aCooldown updated to: §e" + val + "s"));
            closeChatInput(pmu);
            new ActiveItemSettingsMenu(pmu, plugin).open();
        } catch (NumberFormatException error) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid number! Please enter a positive integer (e.g. 10)."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("active.cooldown");
        }
    }

    private void handleActiveDurationEdit(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        try {
            int val = Integer.parseInt(input);
            if (val < 0) throw new NumberFormatException();
            ConfigManager.setItemValue(itemId, "effect_duration", val);
            player.sendMessage(ConfigManager.fromSection("§aEffect Duration updated to: §e" + val + "s"));
            closeChatInput(pmu);
            new ActiveItemSettingsMenu(pmu, plugin).open();
        } catch (NumberFormatException error) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid number! Please enter a positive integer (e.g. 5)."));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("active.duration");
        }
    }

    // ==================== ACTIVE ITEM: COMMANDS ====================

    private void handleAddCommand(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        List<String> commands = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + ".commands"));
        commands.add(input);
        ConfigManager.setItemValue(itemId, "commands", commands);
        player.sendMessage(ConfigManager.fromSection("§aCommand added!"));
        closeChatInput(pmu);
        new CommandListMenu(pmu, plugin).open();
    }

    private void handleEditCommand(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        try {
            int index = Integer.parseInt(path.substring(21));
            List<String> commands = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + ".commands"));

            if (index >= 0 && index < commands.size()) {
                commands.set(index, input);
                ConfigManager.setItemValue(itemId, "commands", commands);
                player.sendMessage(ConfigManager.fromSection("§aCommand updated!"));
            } else {
                player.sendMessage(ConfigManager.fromSection("§cError: Command index out of bounds."));
            }
            closeChatInput(pmu);
            new CommandListMenu(pmu, plugin).open();
        } catch (Exception error) {
            player.sendMessage(ConfigManager.fromSection("§cError processing edit."));
            closeChatInput(pmu);
            new CommandListMenu(pmu, plugin).open();
        }
    }

    // ==================== ACTIVE ITEM: MESSAGES & SOUNDS ====================

    private void handleActiveMessageEdit(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        String messageType = path.substring(11);
        String configPath = getMessageConfigPath(messageType);

        if (configPath == null) {
            player.sendMessage(ConfigManager.fromSection("§cError: Unknown message type: " + messageType));
            closeChatInput(pmu);
            new ActiveItemVisualsMenu(pmu, plugin).open();
            return;
        }

        if ("default".equalsIgnoreCase(input)) {
            if ("title".equals(messageType)) {
                ConfigManager.setItemValue(itemId, "visuals.messages.subtitle", null);
            }
            ConfigManager.setItemValue(itemId, configPath, null);
            player.sendMessage(ConfigManager.fromSection("§aReset to default."));
        } else if ("title".equals(messageType) && input.contains("|")) {
            String[] parts = input.split("\\|", 2);
            ConfigManager.setItemValue(itemId, configPath, parts[0].trim());
            ConfigManager.setItemValue(itemId, "visuals.messages.subtitle", parts[1].trim());
            player.sendMessage(ConfigManager.fromSection("§aTitle and Subtitle updated!"));
        } else {
            ConfigManager.setItemValue(itemId, configPath, input);
            player.sendMessage(ConfigManager.fromSection("§aMessage updated!"));
        }
        closeChatInput(pmu);
        new ActiveItemVisualsMenu(pmu, plugin).open();
    }

    private void handleActiveSoundEdit(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        String soundType = path.substring(14);

        if (!isValidSoundFormat(input)) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid format! Use: NAME;VOLUME;PITCH"));
            player.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("active.sounds." + soundType);
            return;
        }

        ConfigManager.setItemValue(itemId, "sounds." + soundType, input);
        player.sendMessage(ConfigManager.fromSection("§aSound updated!"));

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

    // ==================== GENERIC EFFECT HANDLERS ====================
    /**
     * Universal handler for adding any type of effect (Potion, Attribute)
     */
    private void handleAddGenericEffect(
            Player player,
            PlayerMenuUtility pmu,
            String input,
            String path,
            String itemId,
            String configPath,
            EffectType effectType,
            Function<String, String> nameExtractor,
            String context,
            String slot
    ) {
        String rawName = nameExtractor.apply(path);
        List<String> effects = new ArrayList<>(ItemsConfig.get().getStringList(configPath));

        try {
            String finalConfigString;

            if (effectType == EffectType.ATTRIBUTE) {
                String[] parts = rawName.split("\\.");
                if (parts.length < 2) {
                    throw new IllegalArgumentException("Invalid attribute path format: " + rawName);
                }
                String attrName = parts[0];
                String opName = parts[1];

                validateEffectName(EffectType.ATTRIBUTE, attrName);

                double amount = Double.parseDouble(input);

                String checkStr = attrName + ";" + opName;
                if (effects.stream().anyMatch(s -> s.toUpperCase().startsWith(checkStr.toUpperCase()))) {
                    throw new IllegalStateException("Duplicate attribute");
                }
                finalConfigString = attrName + ";" + opName + ";" + amount;

            } else {
                String potionName = rawName;
                validateEffectName(effectType, potionName);

                int level = Integer.parseInt(input);
                if (level <= 0) throw new NumberFormatException();

                if (effectExists(effects, potionName)) {
                    throw new IllegalStateException("Duplicate effect");
                }

                finalConfigString = potionName + ";" + level;
            }

            effects.add(finalConfigString);

            String configKey = configPath.substring(configPath.indexOf(".") + itemId.length() + 2);
            ConfigManager.setItemValue(itemId, configKey, effects);

            player.sendMessage(ConfigManager.fromSection("§a" + capitalizeFirstLetter(effectType.getDisplayName()) + " has been added!"));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[Chat] Added " + effectType.getDisplayName() + ": " + finalConfigString + " for " + itemId);

            closeChatInput(pmu);
            openAppropriateMenu(player, pmu, effectType, context, slot);

        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid number format. Please enter a valid number."));

        } catch (IllegalStateException ex) {
            player.sendMessage(ConfigManager.fromSection("§cError: This " + effectType.getDisplayName() + " already exists on the item."));
            closeChatInput(pmu);
            openAppropriateMenu(player, pmu, effectType, context, slot);

        } catch (IllegalArgumentException ex) {
            player.sendMessage(ConfigManager.fromSection("§cError: Invalid " + effectType.getDisplayName() + " format: " + rawName));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[Chat] Failed to add " + effectType.getDisplayName() + ": " + rawName + " - " + ex.getMessage());
            closeChatInput(pmu);
            openAppropriateMenu(player, pmu, effectType, context, slot);
        }
    }

    /**
     * Universal handler for editing effects (Potion, Attribute, Enchantment)
     * Faz 2 Optimization: Consolidates edit methods for Potion and Attribute
     */
    private void handleEditGenericEffect(
            Player player,
            PlayerMenuUtility pmu,
            String input,
            String itemId,
            EffectType effectType,
            String configPath,
            String context,
            String slot
    ) {
        List<String> effects = ItemsConfig.get().getStringList(configPath);
        int index = pmu.getEditIndex();

        if (index == -1 || index >= effects.size()) {
            player.sendMessage(ConfigManager.fromSection("§cError: Invalid " + effectType.getDisplayName() + " index."));
            closeChatInput(pmu);
            openAppropriateMenu(player, pmu, effectType, context, slot);
            return;
        }

        try {
            String currentString = effects.get(index);
            String[] parts = currentString.split(";");
            String newEffectString;

            if (effectType == EffectType.ATTRIBUTE) {
                if (parts.length < 3) throw new IllegalStateException("Corrupt attribute data");
                double newAmount = Double.parseDouble(input);
                newEffectString = parts[0] + ";" + parts[1] + ";" + newAmount;
            } else {
                if (parts.length < 2) throw new IllegalStateException("Corrupt effect data");
                int newLevel = Integer.parseInt(input);
                if (newLevel <= 0) throw new NumberFormatException();
                newEffectString = parts[0] + ";" + newLevel;
            }

            effects.set(index, newEffectString);
            String configKey = configPath.substring(configPath.indexOf(".") + itemId.length() + 2);
            ConfigManager.setItemValue(itemId, configKey, effects);
            player.sendMessage(ConfigManager.fromSection("§a" + capitalizeFirstLetter(effectType.getDisplayName()) + " has been updated!"));

            pmu.setEditIndex(-1);
            closeChatInput(pmu);
            openAppropriateMenu(player, pmu, effectType, context, slot);

        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid number. Please enter a valid number."));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath(effectType == EffectType.POTION_EFFECT ? "potion_effects.edit" : "attributes.edit");
        } catch (Exception ex) {
            player.sendMessage(ConfigManager.fromSection("§cError updating " + effectType.getDisplayName() + ". Data might be corrupt."));
            closeChatInput(pmu);
            openAppropriateMenu(player, pmu, effectType, context, slot);
        }
    }

    // ==================== ENCHANTMENT HANDLERS ====================

    private void handleEditEnchantment(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        String configPath = "items." + itemId + ".enchantments";
        List<String> enchantments = new ArrayList<>(ItemsConfig.get().getStringList(configPath));
        int index = pmu.getEditIndex();

        if (index == -1 || index >= enchantments.size()) {
            player.sendMessage(ConfigManager.fromSection("§cError: Invalid enchantment index."));
            closeChatInput(pmu);
            new EnchantmentListMenu(pmu, plugin).open();
            return;
        }

        try {
            int newLevel = Integer.parseInt(input);
            if (newLevel <= 0) {
                player.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a positive whole number (e.g., 1, 5, 10)."));
                pmu.setWaitingForChatInput(true);
                pmu.setChatInputPath("enchantments.edit");
                return;
            }

            String[] parts = enchantments.get(index).split(";");
            if (parts.length == 2) {
                String enchantName = parts[0];
                if (Enchantment.getByName(enchantName.toUpperCase()) == null) {
                    player.sendMessage(ConfigManager.fromSection("§cError: Corrupted enchantment name '" + enchantName + "' found in config."));
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                            () -> "[Chat] Corrupted enchantment name: " + enchantName);
                    closeChatInput(pmu);
                    new EnchantmentListMenu(pmu, plugin).open();
                    return;
                }

                String newEnchantString = enchantName + ";" + newLevel;
                enchantments.set(index, newEnchantString);
                ConfigManager.setItemValue(itemId, "enchantments", enchantments);
                player.sendMessage(ConfigManager.fromSection("§aEnchantment level has been updated!"));
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                        () -> "[Chat] Updated enchantment: " + newEnchantString);
            } else {
                player.sendMessage(ConfigManager.fromSection("§cError: Corrupted enchantment data found at index " + index));
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                        () -> "[Chat] Corrupted enchantment data at index " + index);
            }
        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a whole number (e.g., 1, 5, 10)."));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[Chat] Invalid number format: " + input);
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("enchantments.edit");
            return;
        }

        pmu.setEditIndex(-1);
        closeChatInput(pmu);
        new EnchantmentListMenu(pmu, plugin).open();
    }

    private void handleAddEnchantment(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        String configPath = "items." + itemId + ".enchantments";
        List<String> enchantments = new ArrayList<>(ItemsConfig.get().getStringList(configPath));
        String enchantName = path.substring(17);

        try {
            if (Enchantment.getByName(enchantName.toUpperCase()) == null) {
                throw new IllegalArgumentException("Invalid Enchantment name");
            }

            int level = Integer.parseInt(input);
            if (level <= 0) {
                player.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a positive whole number (e.g., 1, 5, 10)."));
                closeChatInput(pmu);
                new EnchantmentListMenu(pmu, plugin).open();
                return;
            }

            if (enchantmentExists(enchantments, enchantName)) {
                player.sendMessage(ConfigManager.fromSection("§cError: This enchantment already exists on the item."));
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                        () -> "[Chat] Enchantment " + enchantName + " already exists");
                closeChatInput(pmu);
                new EnchantmentListMenu(pmu, plugin).open();
                return;
            }

            String newEnchantString = enchantName + ";" + level;
            enchantments.add(newEnchantString);
            ConfigManager.setItemValue(itemId, "enchantments", enchantments);
            player.sendMessage(ConfigManager.fromSection("§aEnchantment has been added!"));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[Chat] Added enchantment: " + newEnchantString);
        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a whole number (e.g., 1, 5, 10)."));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                    () -> "[Chat] Invalid number format: " + input);
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath(path);
            return;
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ConfigManager.fromSection("§cError: Invalid enchantment name: " + enchantName));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[Chat] Invalid enchantment: " + enchantName);
            closeChatInput(pmu);
            new EnchantmentListMenu(pmu, plugin).open();
            return;
        }
        closeChatInput(pmu);
        new EnchantmentListMenu(pmu, plugin).open();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Validates an effect name based on its type
     */
    private void validateEffectName(EffectType effectType, String name) throws IllegalArgumentException {
        switch (effectType) {
            case POTION_EFFECT:
                if (PotionEffectType.getByName(name.toUpperCase()) == null) {
                    throw new IllegalArgumentException("Invalid potion effect");
                }
                break;
            case ATTRIBUTE:
                org.bukkit.attribute.Attribute.valueOf(name.toUpperCase());
                break;
            case ENCHANTMENT:
                if (Enchantment.getByName(name.toUpperCase()) == null) {
                    throw new IllegalArgumentException("Invalid enchantment");
                }
                break;
        }
    }

    /**
     * Opens the appropriate menu based on effect type and context
     * Uses generic EffectListMenu for all effect types
     */
    private void openAppropriateMenu(Player player, PlayerMenuUtility pmu, EffectType effectType, String context, String slot) {
        EffectListMenu.EffectType menuEffectType =
                effectType == EffectType.POTION_EFFECT ?
                        EffectListMenu.EffectType.POTION_EFFECT :
                        EffectListMenu.EffectType.ATTRIBUTE;

        new EffectListMenu(pmu, plugin, menuEffectType, context).open();
    }

    /**
     * Converts message type to config path
     */
    private String getMessageConfigPath(String messageType) {
        return switch (messageType) {
            case "chat" -> "visuals.messages.cooldown-chat";
            case "title" -> "visuals.messages.cooldown-title";
            case "actionbar" -> "visuals.messages.cooldown-action-bar";
            case "bossbar" -> "visuals.messages.cooldown-boss-bar";
            default -> null;
        };
    }

    /**
     * Validates sound format: NAME;VOLUME;PITCH
     */
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

    /**
     * Validates permission node format
     */
    private boolean isValidPermissionNode(String input) {
        if (input.contains("..") || input.startsWith(".") || input.endsWith(".")) {
            return false;
        }
        return PERMISSION_NODE_PATTERN.matcher(input).matches();
    }

    /**
     * Checks if effect already exists (case-insensitive)
     */
    private boolean effectExists(List<String> effects, String effectName) {
        return effects.stream()
                .anyMatch(s -> s.toUpperCase().startsWith(effectName.toUpperCase() + ";"));
    }

    /**
     * Checks if enchantment already exists (case-insensitive)
     */
    private boolean enchantmentExists(List<String> enchantments, String enchantName) {
        return enchantments.stream()
                .anyMatch(s -> s.toUpperCase().startsWith(enchantName.toUpperCase() + ";"));
    }

    /**
     * Capitalizes the first letter of a string
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void handleAddCost(Player player, PlayerMenuUtility pmu, String input, String type, String itemId) {
        Map<String, Object> newCost = new java.util.HashMap<>();
        newCost.put("type", type);

        try {
            if (type.equals("BUFFED_ITEM")) {
                String[] parts = input.split(";");
                if (parts.length != 2) throw new IllegalArgumentException("Invalid format");

                int amount = Integer.parseInt(parts[0]);
                String buffedItemId = parts[1];

                if (plugin.getItemManager().getBuffedItem(buffedItemId) == null) {
                    player.sendMessage(ConfigManager.fromSection("§eWarning: Item ID '" + buffedItemId + "' is not loaded yet."));
                }

                newCost.put("amount", amount);
                newCost.put("item_id", buffedItemId);
            }
            else if (type.equals("COINSENGINE")) {
                double amount;
                String currencyId = "coins";

                if (input.contains(";")) {
                    String[] parts = input.split(";");
                    if (parts.length != 2) throw new IllegalArgumentException("Invalid format");
                    amount = Double.parseDouble(parts[0]);
                    currencyId = parts[1];
                } else {
                    amount = Double.parseDouble(input);
                }

                if (amount <= 0) throw new NumberFormatException();

                if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") != null) {
                    if (su.nightexpress.coinsengine.api.CoinsEngineAPI.getCurrency(currencyId) == null) {
                        player.sendMessage(ConfigManager.fromSection("§eWarning: Currency ID '" + currencyId + "' not found in CoinsEngine."));
                    }
                }

                newCost.put("amount", amount);
                newCost.put("currency_id", currencyId);
            }
            else if (type.equals("ITEM")) {
                String[] parts = input.split(";");
                if (parts.length != 2) throw new IllegalArgumentException("Invalid format");

                int amount = Integer.parseInt(parts[0]);
                String material = parts[1].toUpperCase();

                if (Material.matchMaterial(material) == null) {
                    player.sendMessage(ConfigManager.fromSection("§cInvalid material: " + material));
                    closeChatInput(pmu);
                    new CostTypeSelectorMenu(pmu, plugin).open();
                    return;
                }

                newCost.put("amount", amount);
                newCost.put("material", material);
            } else {
                double amount = Double.parseDouble(input);
                if (amount <= 0) throw new NumberFormatException();

                if (amount == (int) amount) {
                    newCost.put("amount", (int) amount);
                } else {
                    newCost.put("amount", amount);
                }
            }

            List<Map<?, ?>> costs = ItemsConfig.get().getMapList("items." + itemId + ".costs");
            costs.add(newCost);
            ConfigManager.setItemValue(itemId, "costs", costs);

            player.sendMessage(ConfigManager.fromSection("§aCost added successfully!"));
            closeChatInput(pmu);
            new CostListMenu(pmu, plugin).open();

        } catch (Exception e) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid input."));
            if (type.equals("ITEM")) player.sendMessage(ConfigManager.fromSection("§7Use format: AMOUNT;MATERIAL"));
            else player.sendMessage(ConfigManager.fromSection("§7Please enter a valid positive number."));

            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("active.costs.add." + type);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleEditCostAmount(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        int index = pmu.getEditIndex();
        List<Map<?, ?>> costList = ItemsConfig.get().getMapList("items." + itemId + ".costs");

        if (index < 0 || index >= costList.size()) {
            player.sendMessage(ConfigManager.fromSection("§cError: Cost index out of bounds."));
            closeChatInput(pmu);
            new CostListMenu(pmu, plugin).open();
            return;
        }

        List<Map<String, Object>> editableList = new ArrayList<>();
        for (Map<?, ?> map : costList) {
            editableList.add((java.util.Map<String, Object>) map);
        }

        Map<String, Object> targetCost = editableList.get(index);
        String type = (String) targetCost.get("type");

        try {
            if ("ITEM".equals(type) || "LEVEL".equals(type) || "HUNGER".equals(type) || "BUFFED_ITEM".equals(type)) {
                int val = Integer.parseInt(input);
                if (val <= 0) throw new NumberFormatException();
                targetCost.put("amount", val);
            } else {
                double val = Double.parseDouble(input);
                if (val <= 0) throw new NumberFormatException();

                if (val == (int) val) targetCost.put("amount", (int) val);
                else targetCost.put("amount", val);
            }

            ConfigManager.setItemValue(itemId, "costs", editableList);
            player.sendMessage(ConfigManager.fromSection("§aCost amount updated!"));
            closeChatInput(pmu);
            new io.github.altkat.BuffedItems.menu.active.CostListMenu(pmu, plugin).open();

        } catch (NumberFormatException e) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid amount. Please enter a positive number."));
            pmu.setWaitingForChatInput(true);
            pmu.setChatInputPath("active.costs.edit.amount");
        }
    }

    @SuppressWarnings("unchecked")
    private void handleEditCostMessage(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        int index = pmu.getEditIndex();
        List<Map<?, ?>> costList = ItemsConfig.get().getMapList("items." + itemId + ".costs");

        if (index < 0 || index >= costList.size()) {
            player.sendMessage(ConfigManager.fromSection("§cError: Cost index out of bounds."));
            closeChatInput(pmu);
            new CostListMenu(pmu, plugin).open();
            return;
        }

        List<Map<String, Object>> editableList = new ArrayList<>();
        for (Map<?, ?> map : costList) {
            editableList.add((java.util.Map<String, Object>) map);
        }

        Map<String, Object> targetCost = editableList.get(index);

        if ("default".equalsIgnoreCase(input) || "reset".equalsIgnoreCase(input)) {
            targetCost.remove("message");
            player.sendMessage(ConfigManager.fromSection("§aMessage reset to default config value."));
        } else {
            targetCost.put("message", input);
            player.sendMessage(ConfigManager.fromSection("§aFailure message updated!"));
        }

        ConfigManager.setItemValue(itemId, "costs", editableList);
        closeChatInput(pmu);
        new CostListMenu(pmu, plugin).open();
    }
}