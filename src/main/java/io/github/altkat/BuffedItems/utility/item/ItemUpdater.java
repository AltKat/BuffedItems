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

        boolean hasUsageLimit = template.isActiveMode() && template.getMaxUses() > 0;

        Integer currentHash = oldMeta.getPersistentDataContainer().get(versionKey, PersistentDataType.INTEGER);
        boolean hasUpdateFlag = oldMeta.getPersistentDataContainer().has(updateFlagKey, PersistentDataType.BYTE);

        // 1. Hash Check
        boolean needsUpdate = currentHash == null || currentHash != template.getUpdateHash();

        // 2. Placeholder Check
        if (template.hasPlaceholders()) {
            long now = System.currentTimeMillis();
            Map<String, Long> playerCooldowns = updateCooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
            long lastUpdate = playerCooldowns.getOrDefault(itemId, 0L);

            if (now - lastUpdate >= 3000) {
                needsUpdate = true;
                playerCooldowns.put(itemId, now);
            }
        }

        // 3. Usage Check
        if (hasUsageLimit && hasUpdateFlag) {
            needsUpdate = true;
        }

        if (hasUsageLimit && !hasUpdateFlag && currentHash != null && currentHash == template.getUpdateHash()) {
            needsUpdate = false;
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
        String rawName = template.getDisplayName();
        String parsedName = template.hasPlaceholders()
                ? plugin.getHookManager().processPlaceholders(player, rawName)
                : rawName;
        meta.displayName(ConfigManager.fromLegacy(parsedName));

        // 2. Base Lore Update
        List<Component> baseLore = new ArrayList<>();
        for (String line : template.getLore()) {
            String parsedLine = template.hasPlaceholders()
                    ? plugin.getHookManager().processPlaceholders(player, line)
                    : line;
            baseLore.add(ConfigManager.fromLegacy(parsedLine));
        }

        int currentUses = template.getMaxUses();
        if (template.isActiveMode() && template.getMaxUses() > 0) {
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
        if (template.getCustomModelData().isPresent()) {
            meta.setCustomModelData(template.getCustomModelData().get());
        } else {
            meta.setCustomModelData(null);
        }

        // 4. Enchantments (Sync with template)
        for (Map.Entry<Enchantment, Integer> entry : template.getEnchantments().entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        // 5. Unbreakable
        meta.setUnbreakable(template.getFlag("UNBREAKABLE"));

        meta.removeItemFlags(ItemFlag.values());

        // 6. Attributes
        ItemUtils.applyAttributes(template, meta);

        // 7. Flags
        if (template.getFlag("HIDE_ENCHANTS")) meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        if (template.getFlag("HIDE_ATTRIBUTES")) meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (template.getFlag("HIDE_UNBREAKABLE")) meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        if (template.getFlag("HIDE_DESTROYS")) meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        if (template.getFlag("HIDE_PLACED_ON")) meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        if (template.getFlag("HIDE_ADDITIONAL_TOOLTIP")) meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        if (template.getFlag("HIDE_ARMOR_TRIM")) meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);

        if (template.hasGlow() && !meta.hasEnchants()) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
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