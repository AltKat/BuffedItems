package io.github.altkat.BuffedItems.utility.item;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.utility.ItemUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemUpdater {

    private final BuffedItems plugin;
    private final NamespacedKey idKey;
    private final NamespacedKey usesKey;
    private final NamespacedKey versionKey;
    private final NamespacedKey updateFlagKey;
    private final Map<UUID, Map<String, Long>> updateCooldowns = new ConcurrentHashMap<>();

    public ItemUpdater(BuffedItems plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "buffeditem_id");
        this.usesKey = new NamespacedKey(plugin, "remaining_active_uses");
        this.versionKey = new NamespacedKey(plugin, "buffeditem_version");
        this.updateFlagKey = new NamespacedKey(plugin, "needs_lore_update");
    }

    public ItemStack updateItem(ItemStack oldItem, Player player) {
        if (oldItem == null || !oldItem.hasItemMeta()) return null;

        ItemMeta oldMeta = oldItem.getItemMeta();
        String itemId = oldMeta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (itemId == null) return null;

        BuffedItem template = plugin.getItemManager().getBuffedItem(itemId);
        if (template == null) return null;

        boolean hasUsageLimit = template.getActiveAbility().isEnabled() && template.getUsageDetails().getMaxUses() > 0;

        Integer currentHash = oldMeta.getPersistentDataContainer().get(versionKey, PersistentDataType.INTEGER);
        boolean hasUpdateFlag = oldMeta.getPersistentDataContainer().has(updateFlagKey, PersistentDataType.BYTE);

        // --- Simplified and Corrected Update Logic ---
        boolean needsUpdate = false;

        // 1. Core definition changed (color, name, stats, etc.)
        if (currentHash == null || !currentHash.equals(template.getUpdateHash())) {
            needsUpdate = true;
        }

        // 2. Lore needs to be updated after item usage (e.g. usage limit)
        if (!needsUpdate && hasUsageLimit && hasUpdateFlag) {
            needsUpdate = true;
        }

        // 3. Placeholders need refreshing
        if (!needsUpdate && template.hasPlaceholders()) {
            long now = System.currentTimeMillis();
            Map<String, Long> playerCooldowns = updateCooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
            long lastUpdate = playerCooldowns.getOrDefault(itemId, 0L);

            if (now - lastUpdate >= 3000) {
                needsUpdate = true;
                playerCooldowns.put(itemId, now);
            }
        }

        if (!needsUpdate) {
            return null;
        }

        ItemStack newItem = oldItem.clone();

        if (newItem.getType() != template.getMaterial()) {
            newItem.setType(template.getMaterial());
        }

        ItemMeta meta = newItem.getItemMeta();

        // 1. Display Name Update
        String rawName = template.getItemDisplay().getDisplayName();
        String parsedName = template.hasPlaceholders()
                ? plugin.getHookManager().processPlaceholders(player, rawName)
                : rawName;
        meta.displayName(ConfigManager.fromLegacy(parsedName));

        // 2. Base Lore Update
        List<Component> baseLore = new ArrayList<>();
        for (String line : template.getItemDisplay().getLore()) {
            String parsedLine = template.hasPlaceholders()
                    ? plugin.getHookManager().processPlaceholders(player, line)
                    : line;
            baseLore.add(ConfigManager.fromLegacy(parsedLine));
        }

        int currentUses = template.getUsageDetails().getMaxUses();
        if (template.getActiveAbility().isEnabled() && template.getUsageDetails().getMaxUses() > 0) {
            if (meta.getPersistentDataContainer().has(usesKey, PersistentDataType.INTEGER)) {
                currentUses = meta.getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER);
            } else {
                meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, currentUses);
            }

            String usageLineRaw = (currentUses > 0)
                    ? template.getUsageLore(currentUses)
                    : template.getDepletedLore();

            String parsedUsageLine = template.hasPlaceholders()
                    ? plugin.getHookManager().processPlaceholders(player, usageLineRaw)
                    : usageLineRaw;
            baseLore.add(ConfigManager.fromLegacy(parsedUsageLine));
        } else {
            meta.getPersistentDataContainer().remove(usesKey);
        }

        meta.lore(baseLore);

        // 3. Custom Model Data
        template.getItemDisplay().getCustomModelData().ifPresent(meta::setCustomModelData);

        // 4. Enchantments (Sync with template)
        // First, clear existing enchants to ensure sync
        for (Enchantment ench : meta.getEnchants().keySet()) {
            meta.removeEnchant(ench);
        }
        
        boolean hasRealEnchants = !template.getEnchantments().isEmpty();
        
        if (hasRealEnchants) {
            for (Map.Entry<Enchantment, Integer> entry : template.getEnchantments().entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }

        // 5. Unbreakable
        meta.setUnbreakable(template.getFlag("UNBREAKABLE"));

        // Glow Logic (Synced with ItemBuilder)
        if (template.getItemDisplay().hasGlow()) {
            if (!hasRealEnchants) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            }
        }

        meta.removeItemFlags(ItemFlag.values());

        // 6. Attributes
        ItemUtils.applyAttributes(template, meta);

        // 7. Flags
        if (template.getFlag("HIDE_ENCHANTS") || (template.getItemDisplay().hasGlow() && !hasRealEnchants)) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (template.getFlag("HIDE_ATTRIBUTES")) meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (template.getFlag("HIDE_UNBREAKABLE")) meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        if (template.getFlag("HIDE_DESTROYS")) meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        if (template.getFlag("HIDE_PLACED_ON")) meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        if (template.getFlag("HIDE_ADDITIONAL_TOOLTIP")) meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        if (template.getFlag("HIDE_ARMOR_TRIM")) meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        if (template.getFlag("HIDE_DYE")) meta.addItemFlags(ItemFlag.HIDE_DYE);

        // 8. Color for Leather Armor
        if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leatherMeta) {
            template.getItemDisplay().getColor().ifPresentOrElse(
                    leatherMeta::setColor,
                    () -> leatherMeta.setColor(null) // Reset color if not specified
            );
        }

        meta.getPersistentDataContainer().set(versionKey, PersistentDataType.INTEGER, template.getUpdateHash());
        meta.getPersistentDataContainer().remove(updateFlagKey);

        newItem.setItemMeta(meta);

        boolean flagStillExists = meta.getPersistentDataContainer().has(updateFlagKey, PersistentDataType.BYTE);
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () ->
                "[ItemUpdater] Flag cleanup: " + (flagStillExists ? "FAILED ❌" : "SUCCESS ✅"));

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () ->
                "[ItemUpdater] Lore updated for item: " + itemId + " | Current uses in NBT: " +
                        (newItem.getItemMeta().getPersistentDataContainer().has(usesKey, PersistentDataType.INTEGER)
                                ? newItem.getItemMeta().getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER)
                                : "NOT_SET"));

        return newItem;
    }
}