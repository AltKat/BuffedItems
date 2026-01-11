package io.github.altkat.BuffedItems.utility.item;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.utility.item.data.ActiveAbility;
import io.github.altkat.BuffedItems.utility.item.data.ItemDisplay;
import io.github.altkat.BuffedItems.utility.item.data.PassiveEffects;
import io.github.altkat.BuffedItems.utility.item.data.UsageDetails;
import io.github.altkat.BuffedItems.utility.item.data.visual.PassiveVisuals;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import org.bukkit.enchantments.Enchantment;

import java.util.*;

public class BuffedItem {
    private final String id;
    private Material material;
    private final String permission;
    private final int updateHash;
    private final boolean hasPlaceholders;

    private final ItemDisplay itemDisplay;
    private final PassiveEffects passiveEffects;
    private final PassiveVisuals passiveVisuals;
    private final ActiveAbility activeAbility;
    private final UsageDetails usageDetails;
    private final Map<String, Boolean> flags;
    private final Map<Enchantment, Integer> enchantments;

    private boolean isValid = true;
    private final List<String> errorMessages = new ArrayList<>();
    private ItemStack cachedItem;
    private final ItemStack baseItem;

    public enum AttributeMode { STATIC, DYNAMIC }

    public BuffedItem(String id, Material material, String permission, int updateHash, boolean hasPlaceholders, ItemDisplay itemDisplay, PassiveEffects passiveEffects, PassiveVisuals passiveVisuals, ActiveAbility activeAbility, UsageDetails usageDetails, Map<String, Boolean> flags, Map<Enchantment, Integer> enchantments, ItemStack baseItem) {
        this.id = id;
        this.material = material;
        this.permission = permission;
        this.updateHash = updateHash;
        this.hasPlaceholders = hasPlaceholders;
        this.itemDisplay = itemDisplay;
        this.passiveEffects = passiveEffects;
        this.passiveVisuals = passiveVisuals;
        this.activeAbility = activeAbility;
        this.usageDetails = usageDetails;
        this.flags = flags;
        this.enchantments = enchantments;
        this.baseItem = baseItem;
    }

    public BuffedItem(String id, Material material, String permission, int updateHash, boolean hasPlaceholders, ItemDisplay itemDisplay, PassiveEffects passiveEffects, PassiveVisuals passiveVisuals, ActiveAbility activeAbility, UsageDetails usageDetails, Map<String, Boolean> flags, Map<Enchantment, Integer> enchantments) {
        this(id, material, permission, updateHash, hasPlaceholders, itemDisplay, passiveEffects, passiveVisuals, activeAbility, usageDetails, flags, enchantments, null);
    }

    private static final Set<String> DEFAULT_TRUE_FLAGS;
    static {
        DEFAULT_TRUE_FLAGS = new HashSet<>(Arrays.asList(
                "HIDE_ATTRIBUTES",
                "HIDE_UNBREAKABLE",
                "HIDE_ADDITIONAL_TOOLTIP",
                "HIDE_DESTROYS",
                "HIDE_PLACED_ON",
                "HIDE_ARMOR_TRIM",
                "HIDE_DYE",
                "PREVENT_PLACEMENT"
        ));
    }

    public boolean getFlag(String flagName) {
        String id = flagName.toUpperCase();

        if (flags.containsKey(id)) {
            return flags.getOrDefault(id, false);
        }
        return DEFAULT_TRUE_FLAGS.contains(id);
    }

    public Map<String, Boolean> getFlags() {
        return flags;
    }
    
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public String getPermission() {
        return permission;
    }

    public int getUpdateHash() {
        return updateHash;
    }

    public boolean hasPlaceholders() {
        return hasPlaceholders;
    }

    public ItemDisplay getItemDisplay() {
        return itemDisplay;
    }

    public PassiveEffects getPassiveEffects() {
        return passiveEffects;
    }

    public PassiveVisuals getPassiveVisuals() {
        return passiveVisuals;
    }

    public ActiveAbility getActiveAbility() {
        return activeAbility;
    }

    public UsageDetails getUsageDetails() {
        return usageDetails;
    }

    public boolean isValid() {
        return isValid;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void addErrorMessage(String message) {
        this.isValid = false;
        this.errorMessages.add(message);
    }

    public void setCachedItem(ItemStack cachedItem) {
        this.cachedItem = cachedItem;
    }

    public ItemStack getCachedItem() {
        return cachedItem;
    }

    public ItemStack getBaseItem() {
        return baseItem;
    }

    public String getUsageLore(int currentUses) {
        String format;
        if (usageDetails.getUsageLore() != null) {
            if (usageDetails.getUsageLore().isEmpty() || usageDetails.getUsageLore().equalsIgnoreCase("NONE")) {
                return null;
            }
            format = usageDetails.getUsageLore();
        } else {
            format = ConfigManager.getGlobalUsageLore();
        }

        if (format == null) return null;

        return format
                .replace("{remaining_uses}", String.valueOf(currentUses))
                .replace("{total_uses}", String.valueOf(usageDetails.getMaxUses()));
    }

    public String getDepletedLore() {
        String format;
        if (usageDetails.getDepletedLore() != null) {
            if (usageDetails.getDepletedLore().isEmpty() || usageDetails.getDepletedLore().equalsIgnoreCase("NONE")) {
                return null;
            }
            format = usageDetails.getDepletedLore();
        } else {
            format = ConfigManager.getGlobalDepletedLore();
        }
        
        return format;
    }
    
    public boolean hasActivePermission(org.bukkit.entity.Player player) {
        String activePermission = activeAbility.getActivePermission();
        if (activePermission != null && !activePermission.equalsIgnoreCase("NONE")) {
            return player.hasPermission(activePermission);
        }
        if (permission != null) {
            return player.hasPermission(permission);
        }
        return true;
    }

    public boolean hasPassivePermission(org.bukkit.entity.Player player) {
        String passivePermission = passiveEffects.getPassivePermission();
        if (passivePermission != null && !passivePermission.equalsIgnoreCase("NONE")) {
            return player.hasPermission(passivePermission);
        }
        if (permission != null) {
            return player.hasPermission(permission);
        }
        return true;
    }
}
