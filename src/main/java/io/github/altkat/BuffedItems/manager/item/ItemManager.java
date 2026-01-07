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
import io.github.altkat.BuffedItems.utility.item.DepletionAction;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import io.github.altkat.BuffedItems.utility.item.data.*;
import io.github.altkat.BuffedItems.utility.item.data.visual.*;
import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleDisplay;
import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleShape;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class ItemManager {

    private final BuffedItems plugin;
    private final Map<String, BuffedItem> buffedItems = new HashMap<>();
    private final Set<UUID> managedAttributeUUIDs = new HashSet<>();
    private final CustomModelDataHandler cmdResolver;
    private final NamespacedKey nbtKey;


    public ItemManager(BuffedItems plugin) {
        this.plugin = plugin;
        this.cmdResolver = new CustomModelDataHandler(plugin);
        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
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

        List<String> errorMessages = new ArrayList<>();

        // Basic Info
        String materialName = itemSection.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            String errorMsg = "Invalid Material: '" + materialName + "'";
            errorMessages.add("§c" + errorMsg);
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Item: " + itemId + "] " + errorMsg);
            material = Material.BARRIER;
        }

        String permission = itemSection.getString("permission");
        if (permission != null && (permission.equals(ConfigManager.NO_PERMISSION) || permission.trim().isEmpty())) {
            permission = null;
        }

        // Parse sections
        Map<String, Boolean> flags = new HashMap<>();
        ConfigurationSection flagsSection = itemSection.getConfigurationSection("flags");
        if (flagsSection != null) {
            for (String flagKey : flagsSection.getKeys(false)) {
                flags.put(flagKey.toUpperCase(), flagsSection.getBoolean(flagKey, false));
            }
        }
        Map<Enchantment, Integer> enchantments = parseEnchantments(itemSection, errorMessages);

        java.util.concurrent.atomic.AtomicReference<ItemStack> baseItemRef = new java.util.concurrent.atomic.AtomicReference<>();
        ItemDisplay itemDisplay = parseDisplay(itemSection.getConfigurationSection("display"), itemId, errorMessages, baseItemRef);
        
        // Force material match if we have a base item from Nexo/ItemsAdder
        if (baseItemRef.get() != null) {
            material = baseItemRef.get().getType();
        }

        PassiveEffects passiveEffects = parsePassiveEffects(itemSection.getConfigurationSection("passive_effects"), itemId, errorMessages);
        PassiveVisuals passiveVisuals = parsePassiveVisuals(itemSection.getConfigurationSection("passive_effects.visuals"), errorMessages);
        ActiveAbility activeAbility = parseActiveAbility(itemSection.getConfigurationSection("active_ability"), itemId, errorMessages);
        UsageDetails usageDetails = parseUsageDetails(itemSection.getConfigurationSection("active_ability.usage"), itemId, errorMessages);

        // Improved placeholder check using Regex to avoid false positives with "20%"
        java.util.regex.Pattern placeholderPattern = java.util.regex.Pattern.compile("%.+%");
        boolean hasPlaceholders = placeholderPattern.matcher(itemDisplay.getDisplayName()).find() ||
                                  itemDisplay.getLore().stream().anyMatch(line -> placeholderPattern.matcher(line).find());

        // --- Create Stable Representations for Hashing ---

        // 1. Enchantments (Stable)
        Map<String, Integer> stableEnchants = new TreeMap<>();
        enchantments.forEach((enchant, level) -> stableEnchants.put(enchant.getKey().toString(), level));

        // 2. Attributes (Stable - Only Attributes are physical, Potion Effects are dynamic logic)
        Map<String, String> stableAttributes = new TreeMap<>();
        passiveEffects.getEffects().forEach((slot, effect) -> {
            List<String> attributeStrings = effect.getParsedAttributes().stream()
                    .map(attr -> attr.getAttribute().name() + ";" + attr.getOperation().name() + ";" + attr.getAmount())
                    .sorted()
                    .collect(Collectors.toList());
            if (!attributeStrings.isEmpty()) {
                stableAttributes.put(slot, String.join(",", attributeStrings));
            }
        });

        // 3. Flags (Stable)
        Map<String, Boolean> stableFlags = new TreeMap<>(flags);

        // Calculate Hash based ONLY on physical item properties
        // Using toString() on collections ensures stability
        int updateHash = Objects.hash(
                material.name(),
                itemDisplay.getDisplayName(),
                itemDisplay.getLore().toString(),
                itemDisplay.hasGlow(),
                itemDisplay.getCustomModelData().orElse(-1),
                itemDisplay.getDurability(),
                itemDisplay.getColor().map(Color::asRGB).orElse(-1),
                stableFlags.toString(),
                stableEnchants.toString(),
                stableAttributes.toString(), // Only attributes
                usageDetails.getMaxUses(),
                usageDetails.getUsageLore(),
                usageDetails.getDepletedLore()
        );

        BuffedItem finalBuffedItem = new BuffedItem(
                itemId,
                material,
                permission,
                updateHash,
                hasPlaceholders,
                itemDisplay,
                passiveEffects,
                passiveVisuals,
                activeAbility,
                usageDetails,
                flags,
                enchantments,
                baseItemRef.get()
        );

        for (String errorMsg : errorMessages) {
            finalBuffedItem.addErrorMessage(errorMsg);
        }

        ItemStack stack = new ItemBuilder(finalBuffedItem, plugin).build();
        finalBuffedItem.setCachedItem(stack);

        return finalBuffedItem;
    }

    private PassiveVisuals parsePassiveVisuals(ConfigurationSection section, List<String> errorMessages) {
        if (section == null) {
            return new PassiveVisuals(
                    new ActionBarSettings(VisualTriggerMode.CONTINUOUS, false, null, 0, 0),
                    new BossBarSettings(VisualTriggerMode.CONTINUOUS, false, null, BarColor.WHITE, BarStyle.SOLID, 0, 0),
                    new TitleSettings(false, null, null, 10, 70, 20, 0),
                    new SoundSettings(false, null, 0),
                    new ArrayList<>());
        }

        VisualTriggerMode globalMode = VisualTriggerMode.CONTINUOUS;
        try {
            globalMode = VisualTriggerMode.valueOf(section.getString("mode", "CONTINUOUS").toUpperCase());
        } catch (IllegalArgumentException e) {
            errorMessages.add("§cInvalid passive visual mode: " + section.getString("mode"));
        }

        // Action Bar
        ConfigurationSection abSection = section.getConfigurationSection("action-bar");
        ActionBarSettings abSettings = new ActionBarSettings(globalMode, false, null, 0, 0);
        if (abSection != null) {
            VisualTriggerMode abMode = globalMode;
            if (abSection.contains("mode")) {
                 try { abMode = VisualTriggerMode.valueOf(abSection.getString("mode").toUpperCase()); } catch(Exception ignored){}
            }
            abSettings = new ActionBarSettings(
                    abMode,
                    abSection.getBoolean("enabled", false),
                    abSection.getString("message"),
                    abSection.getInt("duration", 3),
                    abSection.getInt("delay", 0)
            );
        }

        // Title
        ConfigurationSection titleSection = section.getConfigurationSection("title");
        TitleSettings titleSettings = new TitleSettings(false, null, null, 10, 70, 20, 0);
        if (titleSection != null) {
            titleSettings = new TitleSettings(
                    titleSection.getBoolean("enabled", false),
                    titleSection.getString("header"),
                    titleSection.getString("subtitle"),
                    titleSection.getInt("fade-in", 10),
                    titleSection.getInt("stay", 70),
                    titleSection.getInt("fade-out", 20),
                    titleSection.getInt("delay", 0)
            );
        }
        
        // Sound
        ConfigurationSection soundSection = section.getConfigurationSection("sound");
        SoundSettings soundSettings = new SoundSettings(false, null, 0);
        if (soundSection != null) {
            soundSettings = new SoundSettings(
                    soundSection.getBoolean("enabled", false),
                    validateSound(soundSection.getString("sound"), "passive-visual-sound", errorMessages),
                    soundSection.getInt("delay", 0)
            );
        }

        // Boss Bar
        ConfigurationSection bbSection = section.getConfigurationSection("boss-bar");
        BossBarSettings bbSettings = new BossBarSettings(globalMode, false, null, BarColor.WHITE, BarStyle.SOLID, 3, 0);
        if (bbSection != null) {
            BarColor color = BarColor.WHITE;
            try {
                color = BarColor.valueOf(bbSection.getString("color", "WHITE").toUpperCase());
            } catch (IllegalArgumentException e) {
                errorMessages.add("§cInvalid boss-bar color: " + bbSection.getString("color"));
            }

            BarStyle style = BarStyle.SOLID;
            try {
                style = BarStyle.valueOf(bbSection.getString("style", "SOLID").toUpperCase());
            } catch (IllegalArgumentException e) {
                errorMessages.add("§cInvalid boss-bar style: " + bbSection.getString("style"));
            }

            VisualTriggerMode bbMode = globalMode;
            if (bbSection.contains("mode")) {
                try { bbMode = VisualTriggerMode.valueOf(bbSection.getString("mode").toUpperCase()); } catch(Exception ignored){}
            }

            bbSettings = new BossBarSettings(
                    bbMode,
                    bbSection.getBoolean("enabled", false),
                    bbSection.getString("title"),
                    color,
                    style,
                    bbSection.getInt("duration", 3),
                    bbSection.getInt("delay", 0)
            );
        }
        
        List<ParticleDisplay> particles = parseParticles(section, errorMessages, globalMode);
        
        return new PassiveVisuals(abSettings, bbSettings, titleSettings, soundSettings, particles);
    }

    private Map<Enchantment, Integer> parseEnchantments(ConfigurationSection section, List<String> errorMessages) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        List<String> enchantmentStrings = section.getStringList("enchantments");
        for (String enchString : enchantmentStrings) {
            try {
                String[] parts = enchString.split(";");
                if (parts.length != 2) throw new IllegalArgumentException("Must be format ENCHANTMENT_NAME;LEVEL");

                Enchantment enchantment = EnchantmentFinder.findEnchantment(parts[0].toUpperCase(), plugin);
                if (enchantment == null) {
                    errorMessages.add("§cInvalid Enchantment name: '" + parts[0] + "'");
                    continue;
                }

                int level = Integer.parseInt(parts[1]);
                if (level <= 0) continue;

                if (enchantments.containsKey(enchantment)) continue;

                enchantments.put(enchantment, level);
            } catch (Exception e) {
                errorMessages.add("§cCorrupt Enchantment format: §e'" + enchString + "'");
            }
        }
        return enchantments;
    }

    private ItemDisplay parseDisplay(ConfigurationSection section, String itemId, List<String> errorMessages, java.util.concurrent.atomic.AtomicReference<ItemStack> baseItemRef) {
        if (section == null) {
            return new ItemDisplay("Default Name", new ArrayList<>(), false, null, null, 0, null);
        }
        String displayName = section.getString("name", "Default Name");
        List<String> lore = section.getStringList("lore");
        boolean glow = section.getBoolean("glow", false);
        int durability = section.getInt("durability", 0);
        org.bukkit.Color color = null;
        String colorStr = section.getString("color");
        if (colorStr != null) {
            try {
                if (colorStr.startsWith("#")) {
                    colorStr = colorStr.substring(1);
                }
                int red = Integer.valueOf(colorStr.substring(0, 2), 16);
                int green = Integer.valueOf(colorStr.substring(2, 4), 16);
                int blue = Integer.valueOf(colorStr.substring(4, 6), 16);
                color = org.bukkit.Color.fromRGB(red, green, blue);
            } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                errorMessages.add("§cInvalid hex color code: '" + colorStr + "' for item '" + itemId + "'.");
            }
        }

        Integer customModelData;
        String customModelDataRaw;

        if (section.contains("custom-model-data")) {
            Object cmdValue = section.get("custom-model-data");

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
                    if (resolved.getItemStack() != null) {
                        baseItemRef.set(resolved.getItemStack());
                    }
                } else {
                    customModelData = null;
                    errorMessages.add("§cInvalid custom-model-data: '" + customModelDataRaw + "'");
                }
            } else {
                customModelData = null;
            }
        } else {
            customModelDataRaw = null;
            customModelData = null;
        }

        return new ItemDisplay(displayName, lore, glow, customModelData, customModelDataRaw, durability, color);
    }

    private PassiveEffects parsePassiveEffects(ConfigurationSection section, String itemId, List<String> errorMessages) {
        if (section == null) {
            return new PassiveEffects(new HashMap<>(), BuffedItem.AttributeMode.STATIC, null);
        }
        String passivePerm = section.getString("permission");
        if (passivePerm != null && (passivePerm.equals(ConfigManager.NO_PERMISSION) || passivePerm.trim().isEmpty())) {
            passivePerm = null;
        }

        BuffedItem.AttributeMode attributeMode;
        try {
            attributeMode = BuffedItem.AttributeMode.valueOf(section.getString("attribute_mode", "STATIC").toUpperCase());
        } catch (IllegalArgumentException e) {
            attributeMode = BuffedItem.AttributeMode.STATIC;
            errorMessages.add("§cInvalid attribute_mode. Defaulting to STATIC.");
        }

        Map<String, BuffedItemEffect> effects = new HashMap<>();
        ConfigurationSection effectsSection = section.getConfigurationSection("slots");

        if (effectsSection != null) {
            for (String slot : effectsSection.getKeys(false)) {
                ConfigurationSection slotSection = effectsSection.getConfigurationSection(slot);
                if (slotSection == null) continue;

                Map<PotionEffectType, Integer> potionEffects = new HashMap<>();
                for (String effectString : slotSection.getStringList("potion_effects")) {
                    try {
                        String[] parts = effectString.split(";");
                        PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                        if (type == null) {
                            errorMessages.add("§cInvalid PotionEffect: '" + parts[0] + "'");
                            continue;
                        }
                        potionEffects.put(type, Integer.parseInt(parts[1]));
                    } catch (Exception e) {
                        errorMessages.add("§cCorrupt PotionEffect format: §e'" + effectString + "'");
                    }
                }

                List<ParsedAttribute> parsedAttributes = new ArrayList<>();
                for (String attrString : slotSection.getStringList("attributes")) {
                    try {
                        String[] parts = attrString.split(";");
                        Attribute attribute = Attribute.valueOf(parts[0].toUpperCase());
                        AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                        double amount = Double.parseDouble(parts[2]);
                        UUID modifierUUID = EffectManager.getUuidForItem(itemId, slot.toUpperCase(), attribute);
                        parsedAttributes.add(new ParsedAttribute(attribute, operation, amount, modifierUUID));

                        if (attributeMode == BuffedItem.AttributeMode.DYNAMIC || slot.equalsIgnoreCase("INVENTORY")) {
                            managedAttributeUUIDs.add(modifierUUID);
                        }
                    } catch (Exception e) {
                        errorMessages.add("§cCorrupt Attribute format: §e'" + attrString + "'");
                    }
                }
                effects.put(slot.toUpperCase(), new BuffedItemEffect(potionEffects, parsedAttributes));
            }
        }
        return new PassiveEffects(effects, attributeMode, passivePerm);
    }

    private ActiveAbility parseActiveAbility(ConfigurationSection section, String itemId, List<String> errorMessages) {
        if (section == null) {
            return new ActiveAbility(false, 0, 0, null, new ArrayList<>(), new BuffedItemEffect(new HashMap<>(), new ArrayList<>()), new AbilityVisuals(null, null), new AbilitySounds(null,null,null), new ArrayList<>());
        }

        boolean enabled = section.getBoolean("enabled", false);
        int cooldown = section.getInt("cooldown", 0);
        int duration = section.getInt("duration", 0);
        
        String activePerm = section.getString("permission");
        if (activePerm != null && (activePerm.equals(ConfigManager.NO_PERMISSION) || activePerm.trim().isEmpty())) {
            activePerm = null;
        }

        // Parse Costs
        List<ICost> costs = new ArrayList<>();
        if (section.contains("costs")) {
            List<Map<?, ?>> costList = section.getMapList("costs");
            for (Map<?, ?> rawMap : costList) {
                try {
                    ICost cost = plugin.getCostManager().parseCost(rawMap);
                    if (cost != null) costs.add(cost);
                    else errorMessages.add("§cUnknown cost type: " + rawMap.get("type"));
                } catch (Exception e) {
                    errorMessages.add("§cError parsing cost: " + e.getMessage());
                }
            }
        }
        
        ConfigurationSection actionsSection = section.getConfigurationSection("actions");
        List<String> commands = new ArrayList<>();
        BuffedItemEffect activeEffects = new BuffedItemEffect(new HashMap<>(), new ArrayList<>());

        if(actionsSection != null) {
            commands = actionsSection.getStringList("commands");
            // Parse Active Effects
            ConfigurationSection activeEffectsSection = actionsSection.getConfigurationSection("effects");
            if (activeEffectsSection != null) {
                Map<PotionEffectType, Integer> activePotions = new HashMap<>();
                for (String effectString : activeEffectsSection.getStringList("potion_effects")) {
                    try {
                        String[] parts = effectString.split(";");
                        PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
                        if (type == null) {
                            errorMessages.add("§cInvalid PotionEffect in active-mode: '" + parts[0] + "'");
                            continue;
                        }
                        activePotions.put(type, Integer.parseInt(parts[1]));
                    } catch (Exception e) {
                        errorMessages.add("§cCorrupt active-mode PotionEffect: §e'" + effectString + "'");
                    }
                }
                List<ParsedAttribute> activeAttributes = new ArrayList<>();
                for (String attrString : activeEffectsSection.getStringList("attributes")) {
                    try {
                        String[] parts = attrString.split(";");
                        Attribute attribute = Attribute.valueOf(parts[0].toUpperCase());
                        AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                        double amount = Double.parseDouble(parts[2]);
                        activeAttributes.add(new ParsedAttribute(attribute, operation, amount, UUID.randomUUID()));
                    } catch (Exception e) {
                        errorMessages.add("§cCorrupt active-mode Attribute: §e'" + attrString + "'");
                    }
                }
                activeEffects = new BuffedItemEffect(activePotions, activeAttributes);
            }
        }

        // Parse Visuals and Sounds
        AbilityVisuals visuals = parseAbilityVisuals(section.getConfigurationSection("visuals"), errorMessages);
        AbilitySounds sounds = parseAbilitySounds(section.getConfigurationSection("sounds"), errorMessages);


        return new ActiveAbility(enabled, cooldown, duration, activePerm, commands, activeEffects, visuals, sounds, costs);
    }
    
    private AbilityVisuals parseAbilityVisuals(ConfigurationSection visualsSection, List<String> errorMessages) {
        if (visualsSection == null) {
            return new AbilityVisuals(new CooldownVisuals(
                    new ChatCooldownVisuals(ConfigManager.isVisualChatEnabled(), null),
                    new TitleCooldownVisuals(ConfigManager.isVisualTitleEnabled(), null, null),
                    new ActionBarCooldownVisuals(ConfigManager.isVisualActionBarEnabled(), null),
                    new BossBarCooldownVisuals(ConfigManager.isVisualBossBarEnabled(), ConfigManager.getBossBarStyle(), ConfigManager.getBossBarColor(), null)
            ), new CastVisuals(
                    new ActionBarSettings(VisualTriggerMode.CONTINUOUS, false, null, 0, 0),
                    new BossBarSettings(VisualTriggerMode.CONTINUOUS, false, null, BarColor.WHITE, BarStyle.SOLID, 0, 0),
                    new TitleSettings(false, null, null, 10, 70, 20, 0),
                    new SoundSettings(false, null, 0),
                    new ArrayList<>()
            ));
        }
        CooldownVisuals cooldownVisuals = parseCooldownVisuals(visualsSection.getConfigurationSection("cooldown"));
        CastVisuals castVisuals = parseCastVisuals(visualsSection.getConfigurationSection("cast"), errorMessages);
        return new AbilityVisuals(cooldownVisuals, castVisuals);
    }

    private CastVisuals parseCastVisuals(ConfigurationSection section, List<String> errorMessages) {
        if (section == null) {
            return new CastVisuals(
                    new ActionBarSettings(VisualTriggerMode.CONTINUOUS, false, null, 0, 0),
                    new BossBarSettings(VisualTriggerMode.CONTINUOUS, false, null, BarColor.WHITE, BarStyle.SOLID, 0, 0),
                    new TitleSettings(false, null, null, 10, 70, 20, 0),
                    new SoundSettings(false, null, 0),
                    new ArrayList<>()
            );
        }

        // Action Bar
        ConfigurationSection abSection = section.getConfigurationSection("action-bar");
        ActionBarSettings abSettings = new ActionBarSettings(VisualTriggerMode.CONTINUOUS, false, null, 0, 0);
        if (abSection != null) {
            abSettings = new ActionBarSettings(
                    VisualTriggerMode.CONTINUOUS,
                    abSection.getBoolean("enabled", false),
                    abSection.getString("message"),
                    abSection.getInt("duration", 3),
                    abSection.getInt("delay", 0)
            );
        }

        // Title
        ConfigurationSection titleSection = section.getConfigurationSection("title");
        TitleSettings titleSettings = new TitleSettings(false, null, null, 10, 70, 20, 0);
        if (titleSection != null) {
            titleSettings = new TitleSettings(
                    titleSection.getBoolean("enabled", false),
                    titleSection.getString("header"),
                    titleSection.getString("subtitle"),
                    titleSection.getInt("fade-in", 10),
                    titleSection.getInt("stay", 70),
                    titleSection.getInt("fade-out", 20),
                    titleSection.getInt("delay", 0)
            );
        }

        // Sound
        ConfigurationSection soundSection = section.getConfigurationSection("sound");
        SoundSettings soundSettings = new SoundSettings(false, null, 0);
        if (soundSection != null) {
            soundSettings = new SoundSettings(
                    soundSection.getBoolean("enabled", false),
                    validateSound(soundSection.getString("sound"), "cast-visual-sound", errorMessages),
                    soundSection.getInt("delay", 0)
            );
        }

        // Boss Bar
        ConfigurationSection bbSection = section.getConfigurationSection("boss-bar");
        BossBarSettings bbSettings = new BossBarSettings(VisualTriggerMode.CONTINUOUS, false, null, BarColor.WHITE, BarStyle.SOLID, 3, 0);
        if (bbSection != null) {
            BarColor color = BarColor.WHITE;
            try {
                color = BarColor.valueOf(bbSection.getString("color", "WHITE").toUpperCase());
            } catch (IllegalArgumentException e) {
                errorMessages.add("§cInvalid boss-bar color: " + bbSection.getString("color"));
            }

            BarStyle style = BarStyle.SOLID;
            try {
                style = BarStyle.valueOf(bbSection.getString("style", "SOLID").toUpperCase());
            } catch (IllegalArgumentException e) {
                errorMessages.add("§cInvalid boss-bar style: " + bbSection.getString("style"));
            }

            bbSettings = new BossBarSettings(
                    VisualTriggerMode.CONTINUOUS,
                    bbSection.getBoolean("enabled", false),
                    bbSection.getString("title"),
                    color,
                    style,
                    bbSection.getInt("duration", 3),
                    bbSection.getInt("delay", 0)
            );
        }

        List<ParticleDisplay> particles = parseParticles(section, errorMessages, VisualTriggerMode.CONTINUOUS);

        return new CastVisuals(abSettings, bbSettings, titleSettings, soundSettings, particles);
    }


    private CooldownVisuals parseCooldownVisuals(ConfigurationSection cooldownSection) {
        if (cooldownSection == null) {
            return new CooldownVisuals(
                    new ChatCooldownVisuals(ConfigManager.isVisualChatEnabled(), null),
                    new TitleCooldownVisuals(ConfigManager.isVisualTitleEnabled(), null, null),
                    new ActionBarCooldownVisuals(ConfigManager.isVisualActionBarEnabled(), null),
                    new BossBarCooldownVisuals(ConfigManager.isVisualBossBarEnabled(), ConfigManager.getBossBarStyle(), ConfigManager.getBossBarColor(), null)
            );
        }
        ChatCooldownVisuals chat = parseChatCooldownVisuals(cooldownSection.getConfigurationSection("chat"));
        TitleCooldownVisuals title = parseTitleCooldownVisuals(cooldownSection.getConfigurationSection("title"));
        ActionBarCooldownVisuals actionBar = parseActionBarCooldownVisuals(cooldownSection.getConfigurationSection("action-bar"));
        BossBarCooldownVisuals bossBar = parseBossBarCooldownVisuals(cooldownSection.getConfigurationSection("boss-bar"));
        return new CooldownVisuals(chat, title, actionBar, bossBar);
    }

    private ChatCooldownVisuals parseChatCooldownVisuals(ConfigurationSection section) {
        if (section == null) return new ChatCooldownVisuals(ConfigManager.isVisualChatEnabled(), null);
        return new ChatCooldownVisuals(section.getBoolean("enabled", ConfigManager.isVisualChatEnabled()), section.getString("message"));
    }

    private TitleCooldownVisuals parseTitleCooldownVisuals(ConfigurationSection section) {
        if (section == null) return new TitleCooldownVisuals(ConfigManager.isVisualTitleEnabled(), null, null);
        return new TitleCooldownVisuals(section.getBoolean("enabled", ConfigManager.isVisualTitleEnabled()), section.getString("message"), section.getString("subtitle"));
    }

    private ActionBarCooldownVisuals parseActionBarCooldownVisuals(ConfigurationSection section) {
        if (section == null) return new ActionBarCooldownVisuals(ConfigManager.isVisualActionBarEnabled(), null);
        return new ActionBarCooldownVisuals(section.getBoolean("enabled", ConfigManager.isVisualActionBarEnabled()), section.getString("message"));
    }

    private BossBarCooldownVisuals parseBossBarCooldownVisuals(ConfigurationSection section) {
        if (section == null) return new BossBarCooldownVisuals(ConfigManager.isVisualBossBarEnabled(), ConfigManager.getBossBarStyle(), ConfigManager.getBossBarColor(), null);
        return new BossBarCooldownVisuals(section.getBoolean("enabled", ConfigManager.isVisualBossBarEnabled()), section.getString("style", ConfigManager.getBossBarStyle()), section.getString("color", ConfigManager.getBossBarColor()), section.getString("message"));
    }


    private AbilitySounds parseAbilitySounds(ConfigurationSection soundsSection, List<String> errorMessages) {
        if (soundsSection == null) return new AbilitySounds(null,null,null);

        String success = validateSound(soundsSection.getString("success"), "success", errorMessages);
        String costFail = validateSound(soundsSection.getString("cost-fail"), "cost-fail", errorMessages);
        String cooldown = validateSound(soundsSection.getString("cooldown"), "cooldown", errorMessages);

        return new AbilitySounds(success, costFail, cooldown);
    }

    private UsageDetails parseUsageDetails(ConfigurationSection section, String itemId, List<String> errorMessages) {
        if(section == null) {
            return new UsageDetails(-1, DepletionAction.DISABLE, null, new ArrayList<>(), null, null, null, null, null, null, null);
        }

        int maxUses = section.getInt("limit", -1);
        String usageLore = section.getString("lore_format", ConfigManager.getGlobalUsageLore());
        String depletedLore = section.getString("depleted_lore", ConfigManager.getGlobalDepletedLore());
        String depletedMessage = section.getString("depleted_message", ConfigManager.getGlobalDepletedMessage());
        String depletionNotification = section.getString("depletion_notification", ConfigManager.getGlobalDepletionNotification());
        String transformMessage = section.getString("transform_message", ConfigManager.getGlobalDepletionTransformMessage());
        List<String> depletionCommands = section.getStringList("commands");
        String depletionSound = validateSound(section.getString("depletion_sound", ConfigManager.getGlobalDepletionSound()), "depletion", errorMessages);
        String depletedTrySound = validateSound(section.getString("depleted_try_sound", ConfigManager.getGlobalDepletedTrySound()), "depleted-try", errorMessages);


        DepletionAction action = DepletionAction.DISABLE;
        try {
            action = DepletionAction.valueOf(section.getString("action", "DISABLE").toUpperCase());
        } catch (IllegalArgumentException e) {
            errorMessages.add("§cInvalid Depletion Action: " + section.getString("action") + ".");
        }

        String transformId = section.getString("transform_item");

        return new UsageDetails(maxUses, action, transformId, depletionCommands, usageLore, depletedLore, depletedMessage, depletionNotification, transformMessage, depletionSound, depletedTrySound);
    }

    private void cleanupOldUUIDs(BuffedItem item) {
        if (item == null || item.getPassiveEffects() == null || item.getPassiveEffects().getEffects() == null) return;

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[ItemManager] Cleaning up old UUIDs for: " + item.getId());
        for (BuffedItemEffect effect : item.getPassiveEffects().getEffects().values()) {
            if (effect.getParsedAttributes() == null) continue;
            for (ParsedAttribute attr : effect.getParsedAttributes()) {
                managedAttributeUUIDs.remove(attr.getUuid());
            }
        }
    }

    private String validateSound(String soundString, String type, List<String> errors) {
        if (soundString == null) return null;
        if (soundString.equalsIgnoreCase("NONE")) return "NONE";

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

    private List<ParticleDisplay> parseParticles(ConfigurationSection section, List<String> errorMessages, VisualTriggerMode defaultMode) {
        List<ParticleDisplay> particles = new ArrayList<>();
        if (section == null || !section.contains("particles")) return particles;

        List<?> list = section.getList("particles");
        if (list == null) return particles;

        for (Object obj : list) {
            if (!(obj instanceof Map)) continue;
            Map<?, ?> map = (Map<?, ?>) obj;
            try {
                Object typeObj = map.get("type");
                if (typeObj == null) continue;
                String typeStr = typeObj.toString();
                Particle particle = Particle.valueOf(typeStr.toUpperCase());
                
                VisualTriggerMode pMode = defaultMode;
                if (map.containsKey("mode")) {
                    try { pMode = VisualTriggerMode.valueOf(map.get("mode").toString().toUpperCase()); } catch(Exception ignored){}
                }

                String shapeStr = map.get("shape") != null ? map.get("shape").toString() : "POINT";
                ParticleShape shape;
                try {
                    shape = ParticleShape.valueOf(shapeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    shape = ParticleShape.POINT;
                    errorMessages.add("§cInvalid Particle Shape: " + shapeStr);
                }

                int count = map.get("count") instanceof Number ? ((Number) map.get("count")).intValue() : 1;
                double speed = map.get("speed") instanceof Number ? ((Number) map.get("speed")).doubleValue() : 0.0;
                
                double ox = map.get("offset_x") instanceof Number ? ((Number) map.get("offset_x")).doubleValue() : 0.0;
                double oy = map.get("offset_y") instanceof Number ? ((Number) map.get("offset_y")).doubleValue() : 0.0;
                double oz = map.get("offset_z") instanceof Number ? ((Number) map.get("offset_z")).doubleValue() : 0.0;
                Vector offset = new Vector(ox, oy, oz);

                double radius = map.get("radius") instanceof Number ? ((Number) map.get("radius")).doubleValue() : 1.0;
                double height = map.get("height") instanceof Number ? ((Number) map.get("height")).doubleValue() : 1.0;
                double period = map.get("period") instanceof Number ? ((Number) map.get("period")).doubleValue() : 20.0;
                int duration = map.get("duration") instanceof Number ? ((Number) map.get("duration")).intValue() : 0;
                int delay = map.get("delay") instanceof Number ? ((Number) map.get("delay")).intValue() : 0;

                org.bukkit.Color color = null;
                if (map.containsKey("color")) {
                    String cStr = map.get("color").toString();
                    try {
                        if (cStr.startsWith("#")) cStr = cStr.substring(1);
                        int r = Integer.valueOf(cStr.substring(0, 2), 16);
                        int g = Integer.valueOf(cStr.substring(2, 4), 16);
                        int b = Integer.valueOf(cStr.substring(4, 6), 16);
                        color = org.bukkit.Color.fromRGB(r, g, b);
                    } catch (Exception e) {
                        errorMessages.add("§cInvalid Particle Color: " + cStr);
                    }
                }

                String materialData = map.get("material_data") != null ? map.get("material_data").toString() : null;

                particles.add(new ParticleDisplay(pMode, particle, shape, count, speed, offset, radius, height, period, duration, delay, color, materialData));

            } catch (Exception e) {
                 errorMessages.add("§cError parsing particle: " + e.getMessage());
            }
        }
        return particles;
    }

    public boolean isBuffedItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(nbtKey, PersistentDataType.STRING);
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