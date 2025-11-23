package io.github.altkat.BuffedItems.manager.item;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.handler.CustomModelDataHandler;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.manager.effect.EffectManager;
import io.github.altkat.BuffedItems.menu.selector.EnchantmentFinder;
import io.github.altkat.BuffedItems.utility.attribute.ParsedAttribute;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ItemManager {

    private final BuffedItems plugin;
    private final Map<String, BuffedItem> buffedItems = new HashMap<>();
    private final Set<UUID> managedAttributeUUIDs = new HashSet<>();
    private final CustomModelDataHandler cmdResolver;


    public ItemManager(BuffedItems plugin) {
        this.plugin = plugin;
        this.cmdResolver = new CustomModelDataHandler(plugin);
    }

    public void loadItems(boolean silent) {
        long startTime = System.currentTimeMillis();

        buffedItems.clear();
        managedAttributeUUIDs.clear();

        ConfigurationSection itemsSection = ItemsConfig.get().getConfigurationSection("items");
        if (itemsSection == null) {
            if (!silent) ConfigManager.logInfo("&eNo items found in items.yml yet.");
            return;
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[ItemManager] Loading items from config...");
        int validItems = 0;
        int invalidItems = 0;
        List<String> itemsWithErrors = new ArrayList<>();

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[ItemManager] Skipping null section for: " + itemId);
                continue;
            }

            BuffedItem loadedItem = parseItem(itemSection, itemId);
            if (!loadedItem.isValid()) {
                invalidItems++;
                itemsWithErrors.add(itemId);
            } else {
                validItems++;
            }
            buffedItems.put(itemId, loadedItem);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        if (!silent) {
            ConfigManager.logInfo("&aLoaded &e" + buffedItems.size() + "&a buffed items from config (&e" + validItems + "&a valid, &e" + invalidItems + "&c with errors&a) in &e" + elapsedTime + "&ams");

            if (invalidItems > 0) {
                String separator = "============================================================";
                plugin.getLogger().warning(separator);
                plugin.getLogger().warning("⚠ " + invalidItems + " item(s) have configuration errors:");
                for (String itemId : itemsWithErrors) {
                    BuffedItem item = buffedItems.get(itemId);
                    plugin.getLogger().warning("  • " + itemId + " (" + item.getErrorMessages().size() + " error(s))");

                    if (ConfigManager.isDebugLevelEnabled(ConfigManager.DEBUG_INFO)) {
                        for (String error : item.getErrorMessages()) {
                            plugin.getLogger().warning("    - " + ConfigManager.stripLegacy(error));
                        }
                    }
                }
                plugin.getLogger().warning("Use /bi menu to view and fix these errors in-game.");
                plugin.getLogger().warning(separator);
            }
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[ItemManager] Tracking " + managedAttributeUUIDs.size() + " attribute UUIDs");
    }

    public void reloadSingleItem(String itemId) {
        ConfigurationSection itemSection = ItemsConfig.get().getConfigurationSection("items." + itemId);

        BuffedItem oldItem = buffedItems.get(itemId);
        if (oldItem != null) {
            cleanupOldUUIDs(oldItem);
        }

        if (itemSection == null) {
            buffedItems.remove(itemId);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[ItemManager] Unloaded deleted item: " + itemId);
            return;
        }

        BuffedItem newItem = parseItem(itemSection, itemId);
        buffedItems.put(itemId, newItem);
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[ItemManager] Reloaded single item: " + itemId + " (Valid: " + newItem.isValid() + ")");
    }

    private BuffedItem parseItem(ConfigurationSection itemSection, String itemId) {
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[ItemManager] Processing item: " + itemId);

        String displayName = itemSection.getString("display_name", "Default Name");
        List<String> lore = itemSection.getStringList("lore");
        String materialName = itemSection.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        boolean glow = itemSection.getBoolean("glow", false);
        String permission = itemSection.getString("permission");

        if (permission != null && (permission.equals(ConfigManager.NO_PERMISSION) || permission.trim().isEmpty())) {
            permission = null;
        }

        String activePerm = itemSection.getString("active_permission");
        if (activePerm != null && (activePerm.equals(ConfigManager.NO_PERMISSION) || activePerm.trim().isEmpty())) {
            activePerm = null;
        }

        String passivePerm = itemSection.getString("passive_permission");
        if (passivePerm != null && (passivePerm.equals(ConfigManager.NO_PERMISSION) || passivePerm.trim().isEmpty())) {
            passivePerm = null;
        }

        List<String> errorMessages = new ArrayList<>();

        Integer customModelData;
        String customModelDataRaw;

        if (itemSection.contains("custom-model-data")) {
            Object cmdValue = itemSection.get("custom-model-data");

            if (cmdValue instanceof Integer) {
                customModelDataRaw = String.valueOf(cmdValue);
            } else if (cmdValue instanceof String) {
                customModelDataRaw = (String) cmdValue;
            } else {
                customModelDataRaw = null;
            }

            if (customModelDataRaw != null) {
                CustomModelDataHandler.CustomModelData resolved =
                        cmdResolver.resolve(customModelDataRaw, itemId);

                if (resolved != null) {
                    customModelData = resolved.getValue();
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                            () -> "[ItemManager] Item " + itemId + " resolved custom-model-data: " +
                                    customModelDataRaw + " -> " + customModelData +
                                    " (source: " + resolved.getSource() + ")");
                } else {
                    customModelData = null;
                    String errorMsg = "Invalid custom-model-data: '" + customModelDataRaw + "'";
                    errorMessages.add("§c" + errorMsg);
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                            () -> "[Item: " + itemId + "] " + errorMsg);
                }
            } else {
                customModelData = null;
            }
        } else {
            customModelDataRaw = null;
            customModelData = null;
        }

        if (material == null) {
            String errorMsg = "Invalid Material: '" + materialName + "'";
            errorMessages.add("§c" + errorMsg);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Item: " + itemId + "] " + errorMsg);
            material = Material.BARRIER;
        }

        Map<String, Boolean> flags = new HashMap<>();
        ConfigurationSection flagsSection = itemSection.getConfigurationSection("flags");
        if (flagsSection != null) {
            for (String flagKey : flagsSection.getKeys(false)) {
                flags.put(flagKey.toUpperCase(), flagsSection.getBoolean(flagKey, false));
            }
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[ItemManager] Item " + itemId + " has " + flags.size() + " custom flags");
        }

        Map<String, BuffedItemEffect> effects = new HashMap<>();
        ConfigurationSection effectsSection = itemSection.getConfigurationSection("effects");

        if (effectsSection != null) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[ItemManager] Item " + itemId + " has effects section");

            for (String slot : effectsSection.getKeys(false)) {
                ConfigurationSection slotSection = effectsSection.getConfigurationSection(slot);
                if (slotSection == null) continue;

                Map<PotionEffectType, Integer> potionEffects = new HashMap<>();
                List<ParsedAttribute> parsedAttributes = new ArrayList<>();
                List<String> potionEffectStrings = slotSection.getStringList("potion_effects");
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[ItemManager] Item " + itemId + " slot " + slot + " has " + potionEffectStrings.size() + " potion effects");

                for (String effectString : potionEffectStrings) {
                    try {
                        String[] parts = effectString.split(";");
                        String effectName = parts[0].toUpperCase();
                        PotionEffectType type = PotionEffectType.getByName(effectName);

                        if (type == null) {
                            String errorMsg = "Invalid PotionEffect: '" + effectName + "'";
                            errorMessages.add("§c" + errorMsg);
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Item: " + itemId + "] " + errorMsg);
                            continue;
                        }

                        int level = Integer.parseInt(parts[1]);
                        potionEffects.put(type, level);
                    } catch (Exception e) {
                        String errorMsg = "Corrupt PotionEffect format: §e'" + effectString + "'";
                        errorMessages.add("§c" + errorMsg);
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Item: " + itemId + "] " + errorMsg + " | Error: " + e.getMessage());
                    }
                }

                List<String> originalAttributeStrings = slotSection.getStringList("attributes");
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[ItemManager] Item " + itemId + " slot " + slot + " has " + originalAttributeStrings.size() + " attributes");

                for (String attrString : originalAttributeStrings) {
                    try {
                        String[] parts = attrString.split(";");
                        if (parts.length != 3) {
                            throw new IllegalArgumentException("Attribute string must have 3 parts separated by ';'. Found: " + attrString);
                        }

                        Attribute attribute = Attribute.valueOf(parts[0].toUpperCase());
                        AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                        double amount = Double.parseDouble(parts[2]);

                        UUID modifierUUID = EffectManager.getUuidForItem(itemId, slot.toUpperCase(), attribute);

                        parsedAttributes.add(new ParsedAttribute(attribute, operation, amount, modifierUUID));
                        managedAttributeUUIDs.add(modifierUUID);

                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[ItemManager] Pre-parsed and cached attribute UUID: " + modifierUUID + " for " + attribute.name());

                    } catch (IllegalArgumentException e) {
                        String errorMsg = "Invalid Attribute or Operation: '" + attrString + "'. Error: " + e.getMessage();
                        errorMessages.add("§c" + errorMsg);
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Item: " + itemId + "] " + errorMsg);
                    } catch (Exception e) {
                        String errorMsg = "Corrupt Attribute format: §e'" + attrString + "'";
                        errorMessages.add("§c" + errorMsg);
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Item: " + itemId + "] " + errorMsg + " | Error: " + e.getMessage());
                    }
                }

                effects.put(slot.toUpperCase(), new BuffedItemEffect(potionEffects, parsedAttributes));
            }
        }

        Map<Enchantment, Integer> enchantments = new HashMap<>();
        List<String> enchantmentStrings = itemSection.getStringList("enchantments");
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[ItemManager] Item " + itemId + " has " + enchantmentStrings.size() + " enchantments listed.");

        for (String enchString : enchantmentStrings) {
            try {
                String[] parts = enchString.split(";");
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Must be format ENCHANTMENT_NAME;LEVEL");
                }

                String enchName = parts[0].toUpperCase();
                Enchantment enchantment = EnchantmentFinder.findEnchantment(enchName, plugin);

                if (enchantment == null) {
                    String errorMsg = "Invalid Enchantment name: '" + enchName + "'";
                    errorMessages.add("§c" + errorMsg);
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Item: " + itemId + "] " + errorMsg);
                    continue;
                }

                int level = Integer.parseInt(parts[1]);

                if (level <= 0) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Item: " + itemId + "] Enchantment level for " + enchName + " must be positive, found: " + level + ". Skipping.");
                    continue;
                }

                if (enchantments.containsKey(enchantment)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Item: " + itemId + "] Duplicate enchantment found: '" + enchName + "'. Using the first definition.");
                    continue;
                }

                enchantments.put(enchantment, level);
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[ItemManager] Parsed enchantment: " + enchantment.getKey().getKey() + " Level: " + level);

            } catch (NumberFormatException e) {
                String errorMsg = "Invalid Enchantment level format: §e'" + enchString + "'";
                errorMessages.add("§c" + errorMsg);
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Item: " + itemId + "] " + errorMsg);
            } catch (Exception e) {
                String errorMsg = "Corrupt Enchantment format: §e'" + enchString + "' Error: " + e.getMessage();
                errorMessages.add("§c" + errorMsg);
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Item: " + itemId + "] " + errorMsg);
            }
        }

        ConfigurationSection activeModeSection = itemSection.getConfigurationSection("active-mode");

        boolean activeMode = false;
        int cooldown;
        int activeDuration = 0;
        List<String> activeCommands = new ArrayList<>();

        boolean vChat = true;
        boolean vTitle = true;
        boolean vActionBar = true;
        boolean vBossBar = true;
        String bbColor = "RED";
        String bbStyle = "SOLID";
        String msgChat = null;
        String msgTitle = null;
        String msgSubtitle = null;
        String msgActionBar = null;
        String msgBossBar = null;

        String soundSuccess = null;
        String soundCooldown = null;
        String soundCostFail = null;

        List<ICost> costs = new ArrayList<>();
        BuffedItemEffect activeEffectsObj = null;

        if (activeModeSection != null) {
            activeMode = activeModeSection.getBoolean("enabled", false);
            cooldown = activeModeSection.getInt("cooldown", 0);
            activeDuration = activeModeSection.getInt("duration", 0);
            activeCommands = activeModeSection.getStringList("commands");

            if (cooldown < 0) {
                errorMessages.add("§cActive Mode: Cooldown cannot be negative.");
            }
            if (activeDuration < 0) {
                errorMessages.add("§cActive Mode: Duration cannot be negative.");
            }

            if (activeMode) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[ItemManager] Item " + itemId + " is loaded as ACTIVE item (CD: " + cooldown + "s)");
            }

            ConfigurationSection visualsSection = activeModeSection.getConfigurationSection("visuals");
            if (visualsSection != null) {
                vChat = visualsSection.getBoolean("chat", true);
                vTitle = visualsSection.getBoolean("title", true);
                vActionBar = visualsSection.getBoolean("action-bar", true);
                vBossBar = visualsSection.getBoolean("boss-bar", true);

                bbColor = visualsSection.getString("boss-bar-color", "RED");
                try {
                    org.bukkit.boss.BarColor.valueOf(bbColor);
                } catch (IllegalArgumentException e) {
                    errorMessages.add("§cActive Mode Error: Invalid BossBar Color '" + bbColor + "'. Valid: RED, BLUE, GREEN, etc.");
                }

                bbStyle = visualsSection.getString("boss-bar-style", "SOLID");
                try {
                    org.bukkit.boss.BarStyle.valueOf(bbStyle);
                } catch (IllegalArgumentException e) {
                    errorMessages.add("§cActive Mode Error: Invalid BossBar Style '" + bbStyle + "'. Valid: SOLID, SEGMENTED_6, etc.");
                }

                msgChat = visualsSection.getString("messages.cooldown-chat");
                msgTitle = visualsSection.getString("messages.cooldown-title");
                msgSubtitle = visualsSection.getString("messages.cooldown-subtitle");
                msgActionBar = visualsSection.getString("messages.cooldown-action-bar");
                msgBossBar = visualsSection.getString("messages.cooldown-boss-bar");
            }

            ConfigurationSection soundsSection = activeModeSection.getConfigurationSection("sounds");
            if (soundsSection != null) {
                soundSuccess = validateSound(soundsSection.getString("success"), "success", errorMessages);
                soundCooldown = validateSound(soundsSection.getString("cooldown"), "cooldown", errorMessages);
                soundCostFail = validateSound(soundsSection.getString("cost-fail"), "cost-fail", errorMessages);
            }

            for (int i = 0; i < activeCommands.size(); i++) {
                String cmd = activeCommands.get(i);
                if (cmd.contains("[chance:")) {
                    try {
                        int start = cmd.indexOf("[chance:") + 8;
                        int end = cmd.indexOf("]", start);
                        if (end != -1) {
                            double chanceVal = Double.parseDouble(cmd.substring(start, end));
                            if (chanceVal < 0 || chanceVal > 100) {
                                errorMessages.add("§cActive Mode Error: Command chance must be 0-100. Found: " + chanceVal + " in command #" + (i+1));
                            }
                        }
                    } catch (NumberFormatException e) {
                        errorMessages.add("§cActive Mode Error: Invalid chance number format in command #" + (i+1));
                    }
                }
                if (cmd.contains("[delay:")) {
                    try {
                        int start = cmd.indexOf("[delay:") + 7;
                        int end = cmd.indexOf("]", start);
                        if (end != -1) {
                            long delayVal = Long.parseLong(cmd.substring(start, end));
                            if (delayVal < 0) {
                                errorMessages.add("§cActive Mode Error: Command delay cannot be negative. Found: " + delayVal + " in command #" + (i+1));
                            }
                        }
                    } catch (NumberFormatException e) {
                        errorMessages.add("§cActive Mode Error: Invalid delay number format in command #" + (i+1));
                    }
                }
            }

            if (activeModeSection.contains("costs")) {
                List<Map<?, ?>> costList = activeModeSection.getMapList("costs");
                for (int i = 0; i < costList.size(); i++) {
                    Map<?, ?> rawMap = costList.get(i);
                    try {
                        ICost cost = plugin.getCostManager().parseCost(rawMap);
                        if (cost != null) {
                            costs.add(cost);
                        } else {
                            String type = String.valueOf(rawMap.get("type"));
                            errorMessages.add("§cActive Mode Error: Invalid Cost at index " + (i + 1) + " (Unknown Type: " + type + ")");
                        }
                    } catch (Exception e) {
                        String type = String.valueOf(rawMap.get("type"));
                        errorMessages.add("§cActive Mode Error: " + e.getMessage() + " (Type: " + type + ")");
                    }
                }

                List<ICost> finalCosts = costs;
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[ItemManager] Loaded " + finalCosts.size() + " costs for item: " + itemId);
            }

            ConfigurationSection activeEffectsSection = activeModeSection.getConfigurationSection("effects");
            if (activeEffectsSection != null) {
                Map<PotionEffectType, Integer> activePotions = new HashMap<>();

                for (String effectString : activeEffectsSection.getStringList("potion_effects")) {
                    try {
                        String[] parts = effectString.split(";");
                        if (parts.length < 2) throw new IllegalArgumentException("Missing level");

                        PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                        if (type == null) throw new IllegalArgumentException("Invalid potion type: " + parts[0]);

                        int level = Integer.parseInt(parts[1]);
                        if (level <= 0) throw new IllegalArgumentException("Level must be positive");

                        activePotions.put(type, level);
                    } catch (Exception e) {
                        errorMessages.add("§cActive Mode Error: " + e.getMessage() + " in '" + effectString + "'");
                    }
                }

                List<ParsedAttribute> activeAttributes = new ArrayList<>();

                for (String attrString : activeEffectsSection.getStringList("attributes")) {
                    try {
                        String[] parts = attrString.split(";");
                        if (parts.length < 3) throw new IllegalArgumentException("Invalid format");

                        Attribute attribute = Attribute.valueOf(parts[0].toUpperCase());
                        AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                        double amount = Double.parseDouble(parts[2]);

                        activeAttributes.add(new ParsedAttribute(attribute, operation, amount, UUID.randomUUID()));
                    } catch (IllegalArgumentException e) {
                        errorMessages.add("§cActive Mode Error: Invalid Attribute/Operation in '" + attrString + "'");
                    } catch (Exception e) {
                        errorMessages.add("§cActive Mode Error: Corrupt attribute format '" + attrString + "'");
                    }
                }
                activeEffectsObj = new BuffedItemEffect(activePotions, activeAttributes);
            }
        } else {
            cooldown = 0;
        }

        if (activeEffectsObj == null) {
            activeEffectsObj = new BuffedItemEffect(new HashMap<>(), new ArrayList<>());
        }

        BuffedItem finalBuffedItem = new BuffedItem(
                itemId,
                displayName,
                lore,
                material,
                glow,
                effects,
                permission,
                activePerm,
                passivePerm,
                flags,
                enchantments,
                customModelData,
                customModelDataRaw,
                activeMode,
                cooldown,
                activeDuration,
                activeCommands,
                vChat,
                vTitle,
                vActionBar,
                vBossBar,
                bbColor,
                bbStyle,
                activeEffectsObj,
                msgChat,
                msgTitle,
                msgSubtitle,
                msgActionBar,
                msgBossBar,
                soundSuccess,
                soundCooldown,
                soundCostFail,
                costs
        );

        for (String errorMsg : errorMessages) {
            finalBuffedItem.addErrorMessage(errorMsg);
        }

        return finalBuffedItem;
    }

    private void cleanupOldUUIDs(BuffedItem item) {
        if (item == null || item.getEffects() == null) return;

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[ItemManager] Cleaning up old UUIDs for: " + item.getId());
        for (BuffedItemEffect effect : item.getEffects().values()) {
            if (effect.getParsedAttributes() == null) continue;
            for (ParsedAttribute attr : effect.getParsedAttributes()) {
                managedAttributeUUIDs.remove(attr.getUuid());
            }
        }
    }

    private String validateSound(String soundString, String type, List<String> errors) {
        if (soundString == null || soundString.equalsIgnoreCase("NONE")) return null;

        String[] parts = soundString.split(";");
        String soundName = parts[0];

        if (!soundName.contains(":")) {
            try {
                org.bukkit.Sound.valueOf(soundName.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("§cActive Mode Error: Invalid Sound name '" + soundName + "' in '" + type + "'.");
            }
        }

        if (parts.length > 1) {
            try {
                Float.parseFloat(parts[1]);
            } catch (NumberFormatException e) {
                errors.add("§cActive Mode Error: Invalid volume number in '" + type + "' sound.");
            }
        }

        if (parts.length > 2) {
            try {
                Float.parseFloat(parts[2]);
            } catch (NumberFormatException e) {
                errors.add("§cActive Mode Error: Invalid pitch number in '" + type + "' sound.");
            }
        }

        return soundString;
    }

    public BuffedItem getBuffedItem(String itemId) {
        return buffedItems.get(itemId);
    }

    public Map<String, BuffedItem> getLoadedItems() {
        return new HashMap<>(buffedItems);
    }

    public Set<UUID> getManagedAttributeUUIDs() {
        return managedAttributeUUIDs;
    }
}