package io.github.altkat.BuffedItems.Managers;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.github.altkat.BuffedItems.utils.BuffedItemEffect;
import org.bukkit.Material;
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

    public void loadItems() {
        buffedItems.clear();
        managedAttributeUUIDs.clear();
        ConfigurationSection itemsSection = plugin.getConfig().getConfigurationSection("items");
        if (itemsSection == null) {
            plugin.getLogger().warning("No 'items' section found in config.yml.");
            return;
        }

        for (String itemId : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemId);
            if (itemSection == null) continue;

            String displayName = itemSection.getString("display_name", "Default Name");
            List<String> lore = itemSection.getStringList("lore");
            Material material = Material.matchMaterial(itemSection.getString("material", "STONE"));
            boolean glow = itemSection.getBoolean("glow", false);
            String permission = itemSection.getString("permission");

            if (permission != null && (permission.equals(ConfigManager.NO_PERMISSION) || permission.trim().isEmpty())) {
                permission = null;
            }

            Map<String, BuffedItemEffect> effects = new HashMap<>();
            ConfigurationSection effectsSection = itemSection.getConfigurationSection("effects");
            if (effectsSection != null) {
                for (String slot : effectsSection.getKeys(false)) {
                    Map<PotionEffectType, Integer> potionEffects = new HashMap<>();
                    ConfigurationSection slotSection = effectsSection.getConfigurationSection(slot);
                    if(slotSection == null) continue;

                    List<String> potionEffectStrings = slotSection.getStringList("potion_effects");
                    for (String effectString : potionEffectStrings) {
                        try {
                            String[] parts = effectString.split(";");
                            PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                            int level = Integer.parseInt(parts[1]);
                            if (type != null) {
                                potionEffects.put(type, level);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Invalid potion effect format for item '" + itemId + "': " + effectString);
                        }
                    }
                    List<String> attributeStrings = slotSection.getStringList("attributes");

                    for (String attrString : attributeStrings) {
                        try {
                            String[] parts = attrString.split(";");
                            String attributeName = parts[0].toUpperCase();
                            UUID modifierUUID = UUID.nameUUIDFromBytes(("buffeditems." + itemId + "." + slot + "." + attributeName).getBytes());
                            managedAttributeUUIDs.add(modifierUUID);
                        } catch (Exception ignored) {
                        }
                    }

                    effects.put(slot.toUpperCase(), new BuffedItemEffect(potionEffects, attributeStrings));
                }
            }

            if (material == null) {
                plugin.getLogger().warning("Invalid material for item '" + itemId + "'. Skipping.");
                continue;
            }

            BuffedItem buffedItem = new BuffedItem(itemId, displayName, lore, material, glow, effects, permission);
            buffedItems.put(itemId, buffedItem);
        }
        plugin.getLogger().info("Loaded " + buffedItems.size() + " buffed items from config.");
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