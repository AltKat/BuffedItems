package io.github.altkat.BuffedItems.Managers;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Handlers.CustomModelDataResolver;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.github.altkat.BuffedItems.utils.BuffedItemEffect;
import io.github.altkat.BuffedItems.utils.ParsedAttribute;
import org.bukkit.ChatColor;
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
    private final CustomModelDataResolver cmdResolver;


    public ItemManager(BuffedItems plugin) {
        this.plugin = plugin;
        this.cmdResolver = new CustomModelDataResolver(plugin);
    }

    public void loadItems(boolean silent) {
        long startTime = System.currentTimeMillis();

        buffedItems.clear();
        managedAttributeUUIDs.clear();

        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("No 'items' section found in config.yml.");
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
                            plugin.getLogger().warning("    - " + ChatColor.stripColor(error));
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
        ConfigurationSection itemSection = plugin.getConfig().getConfigurationSection("items." + itemId);

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
                CustomModelDataResolver.CustomModelData resolved =
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

                        UUID modifierUUID = UUID.nameUUIDFromBytes(("buffeditems." + itemId + "." + slot + "." + attribute.name()).getBytes());

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
                Enchantment enchantment = Enchantment.getByName(enchName);

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
        BuffedItem finalBuffedItem = new BuffedItem(
                itemId,
                displayName,
                lore,
                material,
                glow,
                effects,
                permission,
                flags,
                enchantments,
                customModelData,
                customModelDataRaw
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