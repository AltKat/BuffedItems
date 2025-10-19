package io.github.altkat.BuffedItems.Managers;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.github.altkat.BuffedItems.utils.BuffedItemEffect;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ItemManager {

    private final BuffedItems plugin;
    private final Map<String, BuffedItem> buffedItems = new HashMap<>();
    private final Set<UUID> managedAttributeUUIDs = new HashSet<>();

    public ItemManager(BuffedItems plugin) {
        this.plugin = plugin;
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

        ConfigManager.sendDebugMessage("[ItemManager] Loading items from config...");
        int validItems = 0;
        int invalidItems = 0;

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigManager.sendDebugMessage("[ItemManager] Processing item: " + itemId);

            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null) {
                ConfigManager.sendDebugMessage("[ItemManager] Skipping null section for: " + itemId);
                continue;
            }

            String displayName = itemSection.getString("display_name", "Default Name");
            List<String> lore = itemSection.getStringList("lore");
            String materialName = itemSection.getString("material", "STONE");
            Material material = Material.matchMaterial(materialName);
            boolean glow = itemSection.getBoolean("glow", false);
            String permission = itemSection.getString("permission");

            if (permission != null && (permission.equals(ConfigManager.NO_PERMISSION) || permission.trim().isEmpty())) {
                permission = null;
            }

            Map<String, Boolean> flags = new HashMap<>();
            ConfigurationSection flagsSection = itemSection.getConfigurationSection("flags");
            if (flagsSection != null) {
                for (String flagKey : flagsSection.getKeys(false)) {
                    flags.put(flagKey.toUpperCase(), flagsSection.getBoolean(flagKey, false));
                }
                ConfigManager.sendDebugMessage("[ItemManager] Item " + itemId + " has " + flags.size() + " custom flags");
            }

            BuffedItem buffedItem = new BuffedItem(itemId, displayName, lore, material, glow, new HashMap<>(), permission);

            if (material == null) {
                String errorMsg = "Invalid Material: '" + materialName + "'";
                buffedItem.addErrorMessage("§c" + errorMsg);
                plugin.getLogger().warning("[Item: " + itemId + "] " + errorMsg);
                buffedItem.setMaterial(Material.BARRIER);
            }

            Map<String, BuffedItemEffect> effects = new HashMap<>();
            ConfigurationSection effectsSection = itemSection.getConfigurationSection("effects");
            if (effectsSection != null) {
                ConfigManager.sendDebugMessage("[ItemManager] Item " + itemId + " has effects section");

                for (String slot : effectsSection.getKeys(false)) {
                    ConfigurationSection slotSection = effectsSection.getConfigurationSection(slot);
                    if (slotSection == null) continue;

                    Map<PotionEffectType, Integer> potionEffects = new HashMap<>();
                    List<String> attributeStrings = new ArrayList<>();

                    List<String> potionEffectStrings = slotSection.getStringList("potion_effects");
                    ConfigManager.sendDebugMessage("[ItemManager] Item " + itemId + " slot " + slot + " has " + potionEffectStrings.size() + " potion effects");

                    for (String effectString : potionEffectStrings) {
                        try {
                            String[] parts = effectString.split(";");
                            String effectName = parts[0].toUpperCase();
                            PotionEffectType type = PotionEffectType.getByName(effectName);
                            if (type == null) {
                                String errorMsg = "Invalid PotionEffect: '" + effectName + "'";
                                buffedItem.addErrorMessage("§c" + errorMsg);
                                plugin.getLogger().warning("[Item: " + itemId + "] " + errorMsg);
                                continue;
                            }
                            int level = Integer.parseInt(parts[1]);
                            potionEffects.put(type, level);
                        } catch (Exception e) {
                            buffedItem.addErrorMessage("§cCorrupt PotionEffect format: §e'" + effectString + "'");
                            plugin.getLogger().warning("[Item: " + itemId + "] Corrupt effect format: " + effectString);
                        }
                    }

                    List<String> originalAttributeStrings = slotSection.getStringList("attributes");
                    ConfigManager.sendDebugMessage("[ItemManager] Item " + itemId + " slot " + slot + " has " + originalAttributeStrings.size() + " attributes");

                    for (String attrString : originalAttributeStrings) {
                        try {
                            String[] parts = attrString.split(";");
                            String attributeName = parts[0].toUpperCase();
                            Attribute.valueOf(attributeName);
                            attributeStrings.add(attrString);

                            UUID modifierUUID = UUID.nameUUIDFromBytes(("buffeditems." + itemId + "." + slot + "." + attributeName).getBytes());
                            managedAttributeUUIDs.add(modifierUUID);
                            ConfigManager.sendDebugMessage("[ItemManager] Registered attribute UUID: " + modifierUUID + " for " + attributeName);
                        } catch (IllegalArgumentException e) {
                            String errorMsg = "Invalid Attribute: '" + attrString.split(";")[0] + "'";
                            buffedItem.addErrorMessage("§c" + errorMsg);
                            plugin.getLogger().warning("[Item: " + itemId + "] " + errorMsg);
                        } catch (Exception e) {
                            buffedItem.addErrorMessage("§cCorrupt Attribute format: §e'" + attrString + "'");
                            plugin.getLogger().warning("[Item: " + itemId + "] Corrupt attribute format: " + attrString);
                        }
                    }
                    effects.put(slot.toUpperCase(), new BuffedItemEffect(potionEffects, attributeStrings));
                }
            }

            BuffedItem finalBuffedItem = new BuffedItem(itemId, displayName, lore, buffedItem.getMaterial(), glow, effects, permission, flags);
            if (!buffedItem.isValid()) {
                buffedItem.getErrorMessages().forEach(finalBuffedItem::addErrorMessage);
                invalidItems++;
            } else {
                validItems++;
            }

            buffedItems.put(itemId, finalBuffedItem);
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        if (!silent) {
            plugin.getLogger().info("Loaded " + buffedItems.size() + " buffed items from config (" + validItems + " valid, " + invalidItems + " with errors) in " + elapsedTime + "ms");
        }

        ConfigManager.sendDebugMessage("[ItemManager] Tracking " + managedAttributeUUIDs.size() + " attribute UUIDs");
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