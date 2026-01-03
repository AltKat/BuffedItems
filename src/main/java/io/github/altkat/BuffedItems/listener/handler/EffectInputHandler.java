package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.menu.editor.EnchantmentListMenu;
import io.github.altkat.BuffedItems.menu.passive.EffectListMenu;
import io.github.altkat.BuffedItems.menu.set.SetBonusEffectSelectorMenu;
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
    public boolean shouldHandle(String path) {
        return path.startsWith("active_ability.actions.effects.") ||
                path.startsWith("potion_effects") ||
                path.startsWith("attributes") ||
                path.startsWith("enchantments.") ||
                path.startsWith("set.potion.") ||
                path.startsWith("set.attribute.");
    }

    @Override
    public void onCancel(Player player, PlayerMenuUtility pmu, String path) {
        if (path.startsWith("active_ability.actions.effects.potion_effects")) {
            new EffectListMenu(pmu, plugin, EffectListMenu.EffectType.POTION_EFFECT, "ACTIVE").open();
        } else if (path.startsWith("active_ability.actions.effects.attributes")) {
            new EffectListMenu(pmu, plugin, EffectListMenu.EffectType.ATTRIBUTE, "ACTIVE").open();
        } else if (path.startsWith("potion_effects.")) {
            String slot = pmu.getTargetSlot();
            new EffectListMenu(pmu, plugin, EffectListMenu.EffectType.POTION_EFFECT, slot).open();
        } else if (path.startsWith("attributes.")) {
            String slot = pmu.getTargetSlot();
            new EffectListMenu(pmu, plugin, EffectListMenu.EffectType.ATTRIBUTE, slot).open();
        } else if (path.startsWith("enchantments.")) {
            new EnchantmentListMenu(pmu, plugin).open();
        } else if (path.startsWith("set.potion.") || path.startsWith("set.attribute.")) {
            new SetBonusEffectSelectorMenu(pmu, plugin).open();
        }
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {

        String slot = pmu.getTargetSlot();
        EffectListMenu.EffectType effectType;
        String context;

        if (path.startsWith("active_ability.actions.effects.potion_effects")) {
            effectType = EffectListMenu.EffectType.POTION_EFFECT;
            context = "ACTIVE";
        } else if (path.startsWith("active_ability.actions.effects.attributes")) {
            effectType = EffectListMenu.EffectType.ATTRIBUTE;
            context = "ACTIVE";
        } else if (path.startsWith("potion_effects")) {
            effectType = EffectListMenu.EffectType.POTION_EFFECT;
            context = slot;
        } else if (path.startsWith("attributes")) {
            effectType = EffectListMenu.EffectType.ATTRIBUTE;
            context = slot;
        } else if (path.startsWith("enchantments.")) {
            if (path.contains(".add")) {
                handleAddEnchantment(player, pmu, input, path.replace(".add", ""), itemId);
            } else { // .edit
                handleEditEnchantment(player, pmu, input, itemId);
            }
            return;
        } else if (path.startsWith("set.potion.add.")) {
            handleAddSetEffect(player, pmu, input, path, EffectListMenu.EffectType.POTION_EFFECT);
            return;
        } else if (path.equals("set.potion.edit")) {
            handleEditSetEffect(player, pmu, input, EffectListMenu.EffectType.POTION_EFFECT);
            return;
        } else if (path.startsWith("set.attribute.add.")) {
            handleAddSetEffect(player, pmu, input, path, EffectListMenu.EffectType.ATTRIBUTE);
            return;
        } else if (path.equals("set.attribute.edit")) {
            handleEditSetEffect(player, pmu, input, EffectListMenu.EffectType.ATTRIBUTE);
            return;
        } else {
            player.sendMessage("Unknown input path: " + path);
            return;
        }

        String configPath = buildConfigPath(itemId, context, effectType);

        if (path.contains(".add.")) {
            String name;
            int addIndex = path.indexOf(".add.");
            if (addIndex != -1) {
                name = path.substring(addIndex + 5);
            } else {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cCould not parse effect name from path: " + path));
                return;
            }
            handleAddGenericEffect(player, pmu, input, name, itemId, configPath, effectType, context);
        } else if (path.endsWith(".edit")) {
            handleEditGenericEffect(player, pmu, input, itemId, effectType, configPath, context);
        }
    }


    private String buildConfigPath(String itemId, String context, EffectListMenu.EffectType effectType) {
        String effectPath = (effectType == EffectListMenu.EffectType.POTION_EFFECT) ? "potion_effects" : "attributes";
        if ("ACTIVE".equals(context)) {
            return "active_ability.actions.effects." + effectPath;
        } else {
            return "passive_effects.slots." + context + "." + effectPath;
        }
    }

    private void handleAddGenericEffect(
            Player player,
            PlayerMenuUtility pmu,
            String input,
            String rawName,
            String itemId,
            String configPath,
            EffectListMenu.EffectType effectType,
            String context
    ) {
        String fullConfigPath = "items." + itemId + "." + configPath;
        List<String> effects = new ArrayList<>(ItemsConfig.get().getStringList(fullConfigPath));

        try {
            String finalConfigString;
            if (effectType == EffectListMenu.EffectType.ATTRIBUTE) {
                String[] parts = rawName.split("\\.");
                if (parts.length < 2) throw new IllegalArgumentException("Invalid attribute path format: " + rawName);

                String attrName = parts[0];
                String opName = parts[1];
                validateEffectName(EffectListMenu.EffectType.ATTRIBUTE, attrName);
                double amount = Double.parseDouble(input);

                if (effects.stream().anyMatch(s -> s.toUpperCase().startsWith(attrName.toUpperCase() + ";" + opName.toUpperCase() + ";"))) {
                    throw new IllegalStateException("Duplicate attribute");
                }
                finalConfigString = attrName + ";" + opName + ";" + amount;
            } else { // Potion
                validateEffectName(EffectListMenu.EffectType.POTION_EFFECT, rawName);
                int level = Integer.parseInt(input);
                if (level <= 0) throw new NumberFormatException();

                if (effects.stream().anyMatch(s -> s.toUpperCase().startsWith(rawName.toUpperCase() + ";"))) {
                    throw new IllegalStateException("Duplicate effect");
                }
                finalConfigString = rawName + ";" + level;
            }

            effects.add(finalConfigString);
            ConfigManager.setItemValue(itemId, configPath, effects);

            player.sendMessage(ConfigManager.fromSectionWithPrefix("§a" + capitalizeFirstLetter(effectType.name()) + " has been added!"));
            closeChatInput(pmu);
            new EffectListMenu(pmu, plugin, effectType, context).open();

        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number format."));
        } catch (IllegalStateException ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: This " + effectType.name().toLowerCase() + " already exists on the item."));
            closeChatInput(pmu);
            new EffectListMenu(pmu, plugin, effectType, context).open();
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Invalid " + effectType.name().toLowerCase() + " name: " + rawName));
            closeChatInput(pmu);
            new EffectListMenu(pmu, plugin, effectType, context).open();
        }
    }


    private void handleEditGenericEffect(
            Player player,
            PlayerMenuUtility pmu,
            String input,
            String itemId,
            EffectListMenu.EffectType effectType,
            String configPath,
            String context
    ) {
        String fullConfigPath = "items." + itemId + "." + configPath;
        List<String> effects = ItemsConfig.get().getStringList(fullConfigPath);
        // Sort the effects list to match the order in EffectListMenu
        effects.sort(String::compareTo);
        int index = pmu.getEditIndex();

        if (index < 0 || index >= effects.size()) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Invalid effect index."));
            closeChatInput(pmu);
            new EffectListMenu(pmu, plugin, effectType, context).open();
            return;
        }

        try {
            String[] parts = effects.get(index).split(";");
            String newEffectString;

            if (effectType == EffectListMenu.EffectType.ATTRIBUTE) {
                double newAmount = Double.parseDouble(input);
                newEffectString = parts[0] + ";" + parts[1] + ";" + newAmount;
            } else { // Potion
                int newLevel = Integer.parseInt(input);
                if (newLevel <= 0) throw new NumberFormatException();
                newEffectString = parts[0] + ";" + newLevel;
            }

            effects.set(index, newEffectString);
            ConfigManager.setItemValue(itemId, configPath, effects);

            // This is the targeted "nuke" fix. It only runs for attribute edits.
            if (effectType == EffectListMenu.EffectType.ATTRIBUTE) {
                plugin.getEffectApplicatorTask().forceAttributeRefreshForHolding(itemId);
            }

            player.sendMessage(ConfigManager.fromSectionWithPrefix("§a" + capitalizeFirstLetter(effectType.name()) + " has been updated!"));
            closeChatInput(pmu);
            new EffectListMenu(pmu, plugin, effectType, context).open();

        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number format."));
        } catch (Exception ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cAn unexpected error occurred."));
            closeChatInput(pmu);
            new EffectListMenu(pmu, plugin, effectType, context).open();
        }
    }


    // ==================== ENCHANTMENT HANDLERS ====================

    private void handleEditEnchantment(Player player, PlayerMenuUtility pmu, String input, String itemId) {
        String configPath = "enchantments";
        List<String> enchantments = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + "." + configPath));
        int index = pmu.getEditIndex();

        if (index < 0 || index >= enchantments.size()) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Invalid enchantment index."));
            closeChatInput(pmu);
            new EnchantmentListMenu(pmu, plugin).open();
            return;
        }

        try {
            int newLevel = Integer.parseInt(input);
            if (newLevel <= 0) throw new NumberFormatException();

            String[] parts = enchantments.get(index).split(";");
            String newEnchantString = parts[0] + ";" + newLevel;
            enchantments.set(index, newEnchantString);

            ConfigManager.setItemValue(itemId, configPath, enchantments);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnchantment level has been updated!"));
        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid level. Please enter a positive whole number."));
            return;
        }

        closeChatInput(pmu);
        new EnchantmentListMenu(pmu, plugin).open();
    }

    private void handleAddEnchantment(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        String configPath = "enchantments";
        String enchantName = path.substring("enchantments.".length());
        List<String> enchantments = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + "." + configPath));

        try {
            validateEffectName(EffectListMenu.EffectType.ENCHANTMENT, enchantName);
            int level = Integer.parseInt(input);
            if (level <= 0) throw new NumberFormatException();

            if (enchantments.stream().anyMatch(s -> s.toUpperCase().startsWith(enchantName.toUpperCase() + ";"))) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: This enchantment already exists on the item."));
                closeChatInput(pmu);
                new EnchantmentListMenu(pmu, plugin).open();
                return;
            }

            enchantments.add(enchantName + ";" + level);
            ConfigManager.setItemValue(itemId, configPath, enchantments);
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnchantment has been added!"));

        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number format."));
            return;
        } catch (IllegalArgumentException ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Invalid enchantment name: " + enchantName));
        }

        closeChatInput(pmu);
        new EnchantmentListMenu(pmu, plugin).open();
    }
    private void handleAddSetEffect(Player player, PlayerMenuUtility pmu, String input, String path, EffectListMenu.EffectType effectType) {
        String setId = pmu.getTempSetId();
        int count = pmu.getTempBonusCount();
        String basePath = "sets." + setId + ".bonuses." + count;
        String listKey = (effectType == EffectListMenu.EffectType.POTION_EFFECT) ? "potion_effects" : "attributes";
        String configPath = basePath + "." + listKey;

        String rawName = path.substring(effectType == EffectListMenu.EffectType.POTION_EFFECT ? 15 : 18);

        List<String> effects = new ArrayList<>(SetsConfig.get().getStringList(configPath));

        try {
            String finalConfigString;

            if (effectType == EffectListMenu.EffectType.ATTRIBUTE) {
                String[] parts = rawName.split("\\.");
                if (parts.length < 2) {
                    throw new IllegalArgumentException("Invalid attribute path format");
                }
                String attrName = parts[0];
                String opName = parts[1];

                validateEffectName(EffectListMenu.EffectType.ATTRIBUTE, attrName);
                double amount = Double.parseDouble(input);

                String checkStr = attrName + ";" + opName;
                if (effects.stream().anyMatch(s -> s.toUpperCase().startsWith(checkStr.toUpperCase()))) {
                    throw new IllegalStateException("Duplicate attribute");
                }

                finalConfigString = attrName + ";" + opName + ";" + amount;

            } else {
                String potionName = rawName;
                validateEffectName(EffectListMenu.EffectType.POTION_EFFECT, potionName);

                int level = Integer.parseInt(input);
                if (level <= 0) throw new NumberFormatException();

                if (effects.stream().anyMatch(s -> s.toUpperCase().startsWith(potionName.toUpperCase() + ";"))) {
                    throw new IllegalStateException("Duplicate effect");
                }

                finalConfigString = potionName + ";" + level;
            }

            effects.add(finalConfigString);
            SetsConfig.get().set(configPath, effects);
            SetsConfig.saveAsync();
            plugin.getSetManager().loadSets(true);

            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSet bonus updated!"));
            closeChatInput(pmu);

            openSetEffectMenu(pmu, effectType);

        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number. Please enter a valid number."));
        } catch (IllegalStateException ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: This effect already exists on the set bonus."));
            closeChatInput(pmu);
            openSetEffectMenu(pmu, effectType);
        } catch (Exception ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: " + ex.getMessage()));
            closeChatInput(pmu);
            openSetEffectMenu(pmu, effectType);
        }
    }

    private void handleEditSetEffect(Player player, PlayerMenuUtility pmu, String input, EffectListMenu.EffectType effectType) {
        String setId = pmu.getTempSetId();
        int count = pmu.getTempBonusCount();
        String basePath = "sets." + setId + ".bonuses." + count;
        String listKey = (effectType == EffectListMenu.EffectType.POTION_EFFECT) ? "potion_effects" : "attributes";
        String configPath = basePath + "." + listKey;

        List<String> effects = SetsConfig.get().getStringList(configPath);
        // Sort the effects list to match the order in EffectListMenu
        effects.sort(String::compareTo);
        int index = pmu.getEditIndex();

        if (index == -1 || index >= effects.size()) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Invalid index."));
            closeChatInput(pmu);
            openSetEffectMenu(pmu, effectType);
            return;
        }

        try {
            String currentString = effects.get(index);
            String[] parts = currentString.split(";");
            String newEffectString;

            if (effectType == EffectListMenu.EffectType.ATTRIBUTE) {
                if (parts.length < 3) throw new IllegalStateException("Corrupt attribute data");
                double newAmount = Double.parseDouble(input);
                newEffectString = parts[0] + ";" + parts[1] + ";" + newAmount;
            } else {
                if (parts.length < 2) throw new IllegalStateException("Corrupt potion data");
                int newLevel = Integer.parseInt(input);
                if (newLevel <= 0) throw new NumberFormatException();
                newEffectString = parts[0] + ";" + newLevel;
            }

            effects.set(index, newEffectString);
            SetsConfig.get().set(configPath, effects);
            SetsConfig.saveAsync();
            plugin.getSetManager().loadSets(true);

            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSet bonus updated!"));
            pmu.setEditIndex(-1);
            closeChatInput(pmu);
            openSetEffectMenu(pmu, effectType);

        } catch (NumberFormatException ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number."));
        } catch (Exception ex) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError updating effect: " + ex.getMessage()));
            closeChatInput(pmu);
            openSetEffectMenu(pmu, effectType);
        }
    }

    private void openSetEffectMenu(PlayerMenuUtility pmu, EffectListMenu.EffectType effectType) {
        new EffectListMenu(pmu, plugin,
                effectType,
                "SET_BONUS").open();
    }


    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
        pmu.setEditIndex(-1);
    }

    private void validateEffectName(EffectListMenu.EffectType effectType, String name) throws IllegalArgumentException {
        switch (effectType) {
            case POTION_EFFECT:
                if (PotionEffectType.getByName(name.toUpperCase()) == null) {
                    throw new IllegalArgumentException("Invalid potion effect name");
                }
                break;
            case ATTRIBUTE:
                org.bukkit.attribute.Attribute.valueOf(name.toUpperCase());
                break;
            case ENCHANTMENT:
                if (Enchantment.getByName(name.toUpperCase()) == null) {
                    throw new IllegalArgumentException("Invalid enchantment name");
                }
                break;
        }
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase().replace("_", " ");
    }
}