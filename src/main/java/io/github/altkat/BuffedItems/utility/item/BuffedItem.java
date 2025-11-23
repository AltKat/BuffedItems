package io.github.altkat.BuffedItems.utility.item;

import io.github.altkat.BuffedItems.manager.cost.ICost;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.*;

public class BuffedItem {
    private final String id;
    private final String displayName;
    private final List<String> lore;
    private Material material;
    private final boolean glow;
    private final Map<String, BuffedItemEffect> effects;
    private final String permission;
    private final String activePermission;
    private final String passivePermission;
    private final Map<Enchantment, Integer> enchantments;
    private final Integer customModelData;
    private final String customModelDataRaw;

    private final boolean activeMode;
    private final int cooldown;
    private final int activeDuration;
    private final List<String> activeCommands;
    private final boolean visualChat;
    private final boolean visualTitle;
    private final boolean visualActionBar;
    private final boolean visualBossBar;
    private final String bossBarColor;
    private final String bossBarStyle;
    private final BuffedItemEffect activeEffects;
    private final String customChatMsg;
    private final String customTitleMsg;
    private final String customSubtitleMsg;
    private final String customActionBarMsg;
    private final String customBossBarMsg;
    private final String customSuccessSound;
    private final String customCooldownSound;
    private final String customCostFailSound;
    private final List<ICost> costs;

    private boolean isValid = true;
    private final List<String> errorMessages = new ArrayList<>();

    private final Map<String, Boolean> flags;

    public BuffedItem(String id, String displayName, List<String> lore, Material material,
                      boolean glow, Map<String, BuffedItemEffect> effects, String permission,
                      String activePermission, String passivePermission, Map<String, Boolean> flags,
                      Map<Enchantment, Integer> enchantments, Integer customModelData, String customModelDataRaw,
                      boolean activeMode, int cooldown, int activeDuration, List<String> activeCommands, boolean visualChat,
                      boolean visualTitle, boolean visualActionBar, boolean visualBossBar, String bossBarColor,
                      String bossBarStyle, BuffedItemEffect activeEffects, String customChatMsg, String customTitleMsg,
                      String customSubtitleMsg, String customActionBarMsg, String customBossBarMsg, String customSuccessSound,
                      String customCooldownSound, String customCostFailSound, List<ICost> costs) {
        this.id = id;
        this.displayName = displayName;
        this.lore = lore;
        this.material = material;
        this.glow = glow;
        this.effects = effects;
        this.permission = permission;
        this.activePermission = activePermission;
        this.passivePermission = passivePermission;
        this.flags = (flags != null) ? flags : new HashMap<>();
        this.enchantments = (enchantments != null) ? enchantments : new HashMap<>();
        this.customModelData = customModelData;
        this.customModelDataRaw = customModelDataRaw;
        this.activeMode = activeMode;
        this.cooldown = cooldown;
        this.activeDuration = activeDuration;
        this.activeCommands = (activeCommands != null) ? activeCommands : new ArrayList<>();
        this.visualChat = visualChat;
        this.visualTitle = visualTitle;
        this.visualActionBar = visualActionBar;
        this.visualBossBar = visualBossBar;
        this.bossBarColor = bossBarColor;
        this.bossBarStyle = bossBarStyle;
        this.activeEffects = activeEffects;
        this.customChatMsg = customChatMsg;
        this.customTitleMsg = customTitleMsg;
        this.customSubtitleMsg = customSubtitleMsg;
        this.customActionBarMsg = customActionBarMsg;
        this.customBossBarMsg = customBossBarMsg;
        this.customSuccessSound = customSuccessSound;
        this.customCooldownSound = customCooldownSound;
        this.customCostFailSound = customCostFailSound;
        this.costs = (costs != null) ? costs : new ArrayList<>();
    }



    private static final Set<String> DEFAULT_TRUE_FLAGS;
    static {
        DEFAULT_TRUE_FLAGS = new HashSet<>(Arrays.asList(
                //"UNBREAKABLE",
                "HIDE_ATTRIBUTES",
                "HIDE_ENCHANTS",
                "HIDE_UNBREAKABLE",
                "HIDE_ADDITIONAL_TOOLTIP",
                "HIDE_DESTROYS",
                "HIDE_PLACED_ON",
                //"PREVENT_ANVIL_USE",
                //"PREVENT_ENCHANT_TABLE",
                //"PREVENT_SMITHING_USE",
                "PREVENT_CRAFTING_USE",
                //"PREVENT_DROP",
                //"PREVENT_CONSUME"
                "PREVENT_PLACEMENT"
                //"PREVENT_DEATH_DROP",
                //"PREVENT_INTERACT"
                //"LOST_ON_DEATH"
        ));
    }

    public boolean getFlag(String flagName) {
        String id = flagName.toUpperCase();

        if (flags.containsKey(id)) {
            return flags.getOrDefault(id, false);
        }
        return DEFAULT_TRUE_FLAGS.contains(id);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public Material getMaterial() {
        return material;
    }

    public boolean hasGlow() {
        return glow;
    }

    public Map<String, BuffedItemEffect> getEffects() {
        return effects;
    }

    public String getPermission() {
        return permission;
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

    public Map<String, Boolean> getFlags() {
        return flags;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public Optional<Integer> getCustomModelData() {
        return Optional.ofNullable(customModelData);
    }

    public Optional<String> getCustomModelDataRaw() {
        return Optional.ofNullable(customModelDataRaw);
    }

    public boolean isActiveMode() {
        return activeMode;
    }

    public int getCooldown() {
        return cooldown;
    }

    public int getActiveDuration() {
        return activeDuration;
    }

    public List<String> getActiveCommands() {
        return activeCommands;
    }

    public boolean isVisualChat() { return visualChat; }
    public boolean isVisualTitle() { return visualTitle; }
    public boolean isVisualActionBar() { return visualActionBar; }
    public boolean isVisualBossBar() { return visualBossBar; }
    public String getBossBarColor() { return bossBarColor; }
    public String getBossBarStyle() { return bossBarStyle; }
    public BuffedItemEffect getActiveEffects() {
        return activeEffects;
    }
    public String getCustomChatMsg() { return customChatMsg; }
    public String getCustomTitleMsg() { return customTitleMsg; }
    public String getCustomSubtitleMsg() { return customSubtitleMsg; }
    public String getCustomActionBarMsg() { return customActionBarMsg; }
    public String getCustomBossBarMsg() { return customBossBarMsg; }
    public String getCustomSuccessSound() { return customSuccessSound; }
    public String getCustomCooldownSound() { return customCooldownSound; }
    public String getCustomCostFailSound() { return customCostFailSound; }
    public String getActivePermissionRaw() { return activePermission; }
    public String getPassivePermissionRaw() { return passivePermission; }

    public List<ICost> getCosts() {
        return costs;
    }

    public boolean hasActivePermission(org.bukkit.entity.Player player) {
        if (activePermission != null && !activePermission.equalsIgnoreCase("NONE")) {
            return player.hasPermission(activePermission);
        }
        if (permission != null) {
            return player.hasPermission(permission);
        }
        return true;
    }

    public boolean hasPassivePermission(org.bukkit.entity.Player player) {
        if (passivePermission != null && !passivePermission.equalsIgnoreCase("NONE")) {
            return player.hasPermission(passivePermission);
        }
        if (permission != null) {
            return player.hasPermission(permission);
        }
        return true;
    }
}