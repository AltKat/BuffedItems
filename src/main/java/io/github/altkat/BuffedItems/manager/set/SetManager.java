package io.github.altkat.BuffedItems.manager.set;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.utility.attribute.ParsedAttribute;
import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;
import io.github.altkat.BuffedItems.utility.set.BuffedSet;
import io.github.altkat.BuffedItems.utility.set.SetBonus;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SetManager {

    private final BuffedItems plugin;
    private final Map<String, BuffedSet> sets = new HashMap<>();
    private final Map<String, String> itemToSetMap = new HashMap<>();

    public SetManager(BuffedItems plugin) {
        this.plugin = plugin;
    }

    public void loadSets(boolean silent) {
        long startTime = System.currentTimeMillis();
        sets.clear();
        itemToSetMap.clear();

        if (!SetsConfig.get().getBoolean("settings.enabled", true)) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "Item Sets system is disabled in sets.yml");
            return;
        }

        ConfigurationSection section = SetsConfig.get().getConfigurationSection("sets");
        if (section == null) {
            if (!silent) ConfigManager.logInfo("&eNo item sets found in sets.yml.");
            return;
        }

        if (!silent) ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[SetManager] Loading sets...");

        int validCount = 0;
        int invalidCount = 0;
        List<String> setsWithErrors = new ArrayList<>();

        for (String setId : section.getKeys(false)) {
            ConfigurationSection setSection = section.getConfigurationSection(setId);
            if (setSection == null) continue;

            List<String> errors = new ArrayList<>();
            boolean isValid = true;

            String displayName = setSection.getString("display_name", setId);
            List<String> items = setSection.getStringList("items");

            if (items.isEmpty()) {
                isValid = false;
                errors.add("Set has no items defined.");
            }

            // Validate Items
            for (String itemId : items) {
                if (plugin.getItemManager().getBuffedItem(itemId) == null) {
                    isValid = false;
                    errors.add("Item '" + itemId + "' does not exist (not loaded in ItemManager).");
                } else {
                    // Check duplicate assignment (One item can only be in one set)
                    if (itemToSetMap.containsKey(itemId)) {
                        isValid = false;
                        errors.add("Item '" + itemId + "' is already assigned to set '" + itemToSetMap.get(itemId) + "'.");
                    }
                }
            }

            // Only map items if the set is valid so far to prevent partial mapping
            if (isValid) {
                for (String itemId : items) {
                    itemToSetMap.put(itemId, setId);
                }
            }

            Map<Integer, SetBonus> bonuses = new HashMap<>();
            ConfigurationSection bonusesSection = setSection.getConfigurationSection("bonuses");

            List<Integer> exceedingBonuses = new ArrayList<>();

            if (bonusesSection != null) {
                for (String countStr : bonusesSection.getKeys(false)) {
                    try {
                        int count = Integer.parseInt(countStr);
                        if (count <= 0) {
                            errors.add("Invalid bonus count '" + countStr + "'. Must be positive.");
                            isValid = false;
                            continue;
                        }

                        if (count > items.size()) {
                            exceedingBonuses.add(count);
                            isValid = false;
                            // Do NOT continue here, allow the bonus to be loaded for editing
                        }

                        ConfigurationSection bonusSection = bonusesSection.getConfigurationSection(countStr);
                        Map<PotionEffectType, Integer> potions = new HashMap<>();
                        List<ParsedAttribute> attributes = new ArrayList<>();

                        if (bonusSection != null) {
                            for (String s : bonusSection.getStringList("potion_effects")) {
                                try {
                                    String[] parts = s.split(";");
                                    PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                                    if (type == null) throw new IllegalArgumentException("Invalid potion type: " + parts[0]);
                                    potions.put(type, Integer.parseInt(parts[1]));
                                } catch (Exception e) {
                                    errors.add("Set Bonus (" + count + " pcs) Potion Error: " + e.getMessage() + " in '" + s + "'");
                                }
                            }

                            for (String s : bonusSection.getStringList("attributes")) {
                                try {
                                    String[] parts = s.split(";");
                                    Attribute attr = Attribute.valueOf(parts[0].toUpperCase());
                                    AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                                    double amount = Double.parseDouble(parts[2]);
                                    UUID uuid = UUID.nameUUIDFromBytes(("buffeditems.set." + setId + "." + count + "." + attr.name()).getBytes());
                                    attributes.add(new ParsedAttribute(attr, op, amount, uuid));
                                } catch (Exception e) {
                                    errors.add("Set Bonus (" + count + " pcs) Attribute Error: " + e.getMessage() + " in '" + s + "'");
                                }
                            }
                        }
                        bonuses.put(count, new SetBonus(count, new BuffedItemEffect(potions, attributes)));

                    } catch (NumberFormatException e) {
                        errors.add("Invalid bonus key '" + countStr + "'. Must be a number.");
                        isValid = false;
                    }
                }
            }

            if (!exceedingBonuses.isEmpty()) {
                Collections.sort(exceedingBonuses);
                String countsStr = exceedingBonuses.toString().replace("[", "").replace("]", "");
                errors.add("Bonuses configured for pieces " + countsStr + " exceed set size (Total items: " + items.size() + ").");
            }

            if (isValid) {
                validCount++;
            } else {
                invalidCount++;
                setsWithErrors.add(setId);
            }

            sets.put(setId, new BuffedSet(setId, displayName, items, bonuses, isValid, errors));
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        if (!silent) {
            ConfigManager.logInfo("&aLoaded &e" + sets.size() + "&a item sets (&e" + validCount + "&a valid, &e" + invalidCount + "&c with errors&a) in &e" + elapsedTime + "&ams");

            if (invalidCount > 0) {
                String separator = "============================================================";
                plugin.getLogger().warning(separator);
                plugin.getLogger().warning("⚠ " + invalidCount + " item set(s) have configuration errors:");
                for (String setId : setsWithErrors) {
                    BuffedSet set = sets.get(setId);
                    plugin.getLogger().warning("  • " + setId + " (" + set.getErrorMessages().size() + " error(s))");

                    for (String error : set.getErrorMessages()) {
                        plugin.getLogger().warning("    - " + ConfigManager.stripLegacy(error));
                    }

                }
                plugin.getLogger().warning(separator);
            }
        }
    }

    private BuffedItemEffect parseEffects(ConfigurationSection section, String setId, int count, List<String> errors) {
        Map<PotionEffectType, Integer> potions = new HashMap<>();
        List<ParsedAttribute> attributes = new ArrayList<>();

        if (section == null) return new BuffedItemEffect(potions, attributes);

        // Potions
        for (String s : section.getStringList("potion_effects")) {
            try {
                String[] parts = s.split(";");
                if (parts.length < 2) throw new IllegalArgumentException("Missing level");

                PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                if (type == null) throw new IllegalArgumentException("Invalid potion type: " + parts[0]);

                int level = Integer.parseInt(parts[1]);
                potions.put(type, level);
            } catch (Exception e) {
                errors.add("Set Bonus (" + count + " pcs) Potion Error: " + e.getMessage() + " in '" + s + "'");
            }
        }

        // Attributes
        for (String s : section.getStringList("attributes")) {
            try {
                String[] parts = s.split(";");
                if (parts.length < 3) throw new IllegalArgumentException("Invalid format");

                Attribute attr = Attribute.valueOf(parts[0].toUpperCase());
                AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                double amount = Double.parseDouble(parts[2]);

                UUID uuid = UUID.nameUUIDFromBytes(("buffeditems.set." + setId + "." + count + "." + attr.name()).getBytes());

                attributes.add(new ParsedAttribute(attr, op, amount, uuid));
            } catch (IllegalArgumentException e) {
                errors.add("Set Bonus (" + count + " pcs) Attribute Error: Invalid Enum/Number in '" + s + "'");
            } catch (Exception e) {
                errors.add("Set Bonus (" + count + " pcs) Attribute Error: " + e.getMessage());
            }
        }

        return new BuffedItemEffect(potions, attributes);
    }

    public BuffedSet getSet(String setId) {
        return sets.get(setId);
    }

    public String getSetIdByItem(String itemId) {
        return itemToSetMap.get(itemId);
    }

    public Map<String, BuffedSet> getSets() {
        return Collections.unmodifiableMap(sets);
    }
}