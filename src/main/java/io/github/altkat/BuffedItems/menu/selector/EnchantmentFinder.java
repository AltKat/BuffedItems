package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

public class EnchantmentFinder {

    /**
     * Find all enchantments - vanilla, custom plugin enchantments, etc.
     * @param enchantKey Enchantment key (e.g., “minecraft:sharpness” or “veinminer”)
     * @param plugin BuffedItems plugin instance
     * @return Found Enchantment object, null if not found
     */
    public static Enchantment findEnchantment(String enchantKey, BuffedItems plugin) {
        if (enchantKey == null || enchantKey.isEmpty()) {
            return null;
        }

        String key = enchantKey.toLowerCase().trim();

        try {
            Enchantment enchant = Enchantment.getByKey(NamespacedKey.fromString(key));
            if (enchant != null) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                        () -> "[EnchantmentFinder] Found enchantment via NamespacedKey.fromString(): " + key);
                return enchant;
            }
        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                    () -> "[EnchantmentFinder] NamespacedKey.fromString() failed for: " + key);
        }

        if (key.contains(":")) {
            String[] parts = key.split(":", 2);
            String namespace = parts[0];
            String name = parts[1];

            try {
                NamespacedKey nsKey = new NamespacedKey(namespace, name);
                Enchantment enchant = Enchantment.getByKey(nsKey);
                if (enchant != null) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                            () -> "[EnchantmentFinder] Found enchantment via custom NamespacedKey: " + namespace + ":" + name);
                    return enchant;
                }
            } catch (Exception e) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                        () -> "[EnchantmentFinder] Custom NamespacedKey failed for: " + namespace + ":" + name);
            }
        }

        if (!key.startsWith("minecraft:")) {
            try {
                NamespacedKey minecraftKey = NamespacedKey.minecraft(key);
                Enchantment enchant = Enchantment.getByKey(minecraftKey);
                if (enchant != null) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                            () -> "[EnchantmentFinder] Found vanilla enchantment: minecraft:" + key);
                    return enchant;
                }
            } catch (Exception e) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                        () -> "[EnchantmentFinder] Vanilla NamespacedKey failed for: " + key);
            }
        }

        try {
            Enchantment enchant = Enchantment.getByName(key.toUpperCase());
            if (enchant != null) {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                        () -> "[EnchantmentFinder] Found enchantment via getByName(): " + key.toUpperCase());
                return enchant;
            }
        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                    () -> "[EnchantmentFinder] getByName() failed for: " + key.toUpperCase());
        }

        try {
            String searchName;
            if (key.contains(":")) {
                searchName = key.split(":", 2)[1].toUpperCase();
            } else {
                searchName = key.toUpperCase();
            }

            for (Enchantment enchant : Enchantment.values()) {
                if (enchant == null) continue;

                if (enchant.getName().toUpperCase().equals(searchName)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                            () -> "[EnchantmentFinder] Found enchantment via getName() comparison: " + searchName);
                    return enchant;
                }

                String keyPart = enchant.getKey().getKey().toUpperCase();
                if (keyPart.equals(searchName)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                            () -> "[EnchantmentFinder] Found enchantment via key comparison: " + searchName);
                    return enchant;
                }

                if (keyPart.contains("_")) {
                    String[] keyParts = keyPart.split("_");
                    String lastPart = keyParts[keyParts.length - 1];
                    if (lastPart.equals(searchName)) {
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                                () -> "[EnchantmentFinder] Found enchantment via partial key match: " + searchName);
                        return enchant;
                    }
                }
            }
        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[EnchantmentFinder] Name search failed: " + e.getMessage());
        }

        try {
            for (Enchantment enchant : Enchantment.values()) {
                if (enchant == null) continue;

                String enchantFullKey = enchant.getKey().getKey();
                String enchantFullKeyLower = enchantFullKey.toLowerCase();

                if (enchantFullKeyLower.contains(key)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                            () -> "[EnchantmentFinder] Found enchantment via substring match: " + enchantFullKey + " matches " + key);
                    return enchant;
                }
            }
        } catch (Exception e) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                    () -> "[EnchantmentFinder] Substring search failed: " + e.getMessage());
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                () -> "[EnchantmentFinder] Could not find enchantment: " + enchantKey +
                        " - Available enchantments: " + getAvailableEnchantments());
        return null;
    }

    private static String getAvailableEnchantments() {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Enchantment e : Enchantment.values()) {
            if (e != null) {
                if (count > 0) sb.append(", ");
                sb.append(e.getKey().getKey());
                count++;
                if (count >= 5) {
                    sb.append("... (and ").append(Enchantment.values().length - 5).append(" more)");
                    break;
                }
            }
        }
        return sb.toString();
    }
}