package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.listener.EffectType;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.editor.EnchantmentListMenu;
import io.github.altkat.BuffedItems.menu.passive.EffectListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class EffectInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public EffectInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path.startsWith("active.potion_effects.add.")) {
            handleAddGenericEffect(player, pmu, input, path, itemId,
                    "items." + itemId + ".active_effects.potion_effects", EffectType.POTION_EFFECT,
                    p -> p.substring(26), "ACTIVE", null);
        } else if (path.equals("active.potion_effects.edit")) {
            handleEditGenericEffect(player, pmu, input, itemId, EffectType.POTION_EFFECT,
                    "items." + itemId + ".active_effects.potion_effects", "ACTIVE", null);
        } else if (path.startsWith("active.attributes.add.")) {
            handleAddGenericEffect(player, pmu, input, path, itemId,
                    "items." + itemId + ".active_effects.attributes", EffectType.ATTRIBUTE,
                    p -> p.substring(22), "ACTIVE", null);
        } else if (path.equals("active.attributes.edit")) {
            handleEditGenericEffect(player, pmu, input, itemId, EffectType.ATTRIBUTE,
                    "items." + itemId + ".active_effects.attributes", "ACTIVE", null);
        } else if (path.startsWith("potion_effects.add.")) {
            String slot = pmu.getTargetSlot();
            handleAddGenericEffect(player, pmu, input, path, itemId,
                    "items." + itemId + ".effects." + slot + ".potion_effects", EffectType.POTION_EFFECT,
                    p -> p.substring(19), slot, slot);
        } else if (path.equals("potion_effects.edit")) {
            String slot = pmu.getTargetSlot();
            handleEditGenericEffect(player, pmu, input, itemId, EffectType.POTION_EFFECT,
                    "items." + itemId + ".effects." + slot + ".potion_effects", slot, slot);
        } else if (path.startsWith("attributes.add.")) {
            String slot = pmu.getTargetSlot();
            handleAddGenericEffect(player, pmu, input, path, itemId,
                    "items." + itemId + ".effects." + slot + ".attributes", EffectType.ATTRIBUTE,
                    p -> p.substring(15), slot, slot);
        } else if (path.equals("attributes.edit")) {
            String slot = pmu.getTargetSlot();
            handleEditGenericEffect(player, pmu, input, itemId, EffectType.ATTRIBUTE,
                    "items." + itemId + ".effects." + slot + ".attributes", slot, slot);
        } else if (path.equals("enchantments.edit")) {
            handleEditEnchantment(player, pmu, input, itemId);
        } else if (path.startsWith("enchantments.add.")) {
            handleAddEnchantment(player, pmu, input, path, itemId);
        }
    }


    // ==================== GENERIC EFFECT HANDLERS ====================

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
                    () -> "[EffectHandler] Added " + effectType.getDisplayName() + ": " + finalConfigString + " for " + itemId);

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
                    () -> "[EffectHandler] Failed to add " + effectType.getDisplayName() + ": " + rawName + " - " + ex.getMessage());
            closeChatInput(pmu);
            openAppropriateMenu(player, pmu, effectType, context, slot);
        }
    }

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
                return;
            }

            String[] parts = enchantments.get(index).split(";");
            if (parts.length == 2) {
                String enchantName = parts[0];
                if (Enchantment.getByName(enchantName.toUpperCase()) == null) {
                    player.sendMessage(ConfigManager.fromSection("§cError: Corrupted enchantment name '" + enchantName + "' found in config."));
                    closeChatInput(pmu);
                    new EnchantmentListMenu(pmu, plugin).open();
                    return;
                }

                String newEnchantString = enchantName + ";" + newLevel;
                enchantments.set(index, newEnchantString);
                ConfigManager.setItemValue(itemId, "enchantments", enchantments);
                player.sendMessage(ConfigManager.fromSection("§aEnchantment level has been updated!"));
            } else {
                player.sendMessage(ConfigManager.fromSection("§cError: Corrupted enchantment data found at index " + index));
            }
        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a whole number (e.g., 1, 5, 10)."));
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
                return;
            }

            if (enchantmentExists(enchantments, enchantName)) {
                player.sendMessage(ConfigManager.fromSection("§cError: This enchantment already exists on the item."));
                closeChatInput(pmu);
                new EnchantmentListMenu(pmu, plugin).open();
                return;
            }

            String newEnchantString = enchantName + ";" + level;
            enchantments.add(newEnchantString);
            ConfigManager.setItemValue(itemId, "enchantments", enchantments);
            player.sendMessage(ConfigManager.fromSection("§aEnchantment has been added!"));

        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSection("§cInvalid number format: " + input));
            return;
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ConfigManager.fromSection("§cError: Invalid enchantment name: " + enchantName));
            closeChatInput(pmu);
            new EnchantmentListMenu(pmu, plugin).open();
            return;
        }

        closeChatInput(pmu);
        new EnchantmentListMenu(pmu, plugin).open();
    }



    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }

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

    private void openAppropriateMenu(Player player, PlayerMenuUtility pmu, EffectType effectType, String context, String slot) {
        EffectListMenu.EffectType menuEffectType =
                effectType == EffectType.POTION_EFFECT ?
                        EffectListMenu.EffectType.POTION_EFFECT :
                        EffectListMenu.EffectType.ATTRIBUTE;

        new EffectListMenu(pmu, plugin, menuEffectType, context).open();
    }

    private boolean effectExists(List<String> effects, String effectName) {
        return effects.stream()
                .anyMatch(s -> s.toUpperCase().startsWith(effectName.toUpperCase() + ";"));
    }

    private boolean enchantmentExists(List<String> enchantments, String enchantName) {
        return enchantments.stream()
                .anyMatch(s -> s.toUpperCase().startsWith(enchantName.toUpperCase() + ";"));
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}