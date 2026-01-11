package io.github.altkat.BuffedItems.utility.item;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.utility.ItemUtils;
import io.github.altkat.BuffedItems.utility.item.data.ActiveAbility;
import io.github.altkat.BuffedItems.utility.item.data.ItemDisplay;
import io.github.altkat.BuffedItems.utility.item.data.UsageDetails;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemBuilder {

    private final BuffedItem buffedItem;
    private final ItemStack itemStack;
    private final Plugin plugin;

    protected static final EquipmentSlot[] VALID_SLOTS = {
            EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public ItemBuilder(BuffedItem buffedItem, Plugin plugin) {
        this.buffedItem = buffedItem;
        this.plugin = plugin;
        
        if (buffedItem.getBaseItem() != null) {
            this.itemStack = buffedItem.getBaseItem().clone();
        } else {
            this.itemStack = new ItemStack(buffedItem.getMaterial());
        }
    }

    public ItemStack build() {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {return itemStack;}

        ItemDisplay display = buffedItem.getItemDisplay();

        meta.displayName(ConfigManager.fromLegacy(display.getDisplayName()));
        List<Component> coloredLore = ConfigManager.loreFromLegacy(display.getLore());
        meta.lore(coloredLore);

        display.getCustomModelData().ifPresent(meta::setCustomModelData);

        if (display.getDurability() > 0 && meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
            damageable.setDamage(display.getDurability());
        }

        if (buffedItem.getFlag("UNBREAKABLE")) {
            meta.setUnbreakable(true);
        }

        boolean templateHasEnchants = !buffedItem.getEnchantments().isEmpty();

        if (templateHasEnchants) {
            for (Map.Entry<Enchantment, Integer> entry : buffedItem.getEnchantments().entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }

        boolean dummyEnchantAdded = false;
        if (display.hasGlow()) {
            if (!meta.hasEnchants()) {
                meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
                dummyEnchantAdded = true;
            }
        }

        ItemUtils.applyAttributes(buffedItem, meta);

        if (buffedItem.getFlag("HIDE_ENCHANTS") || dummyEnchantAdded) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        if (buffedItem.getFlag("HIDE_ATTRIBUTES")) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }
        if (buffedItem.getFlag("HIDE_UNBREAKABLE")) {
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }
        if (buffedItem.getFlag("HIDE_DESTROYS")) {
            meta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        }
        if (buffedItem.getFlag("HIDE_PLACED_ON")) {
            meta.addItemFlags(ItemFlag.HIDE_PLACED_ON);
        }
        if (buffedItem.getFlag("HIDE_ADDITIONAL_TOOLTIP")) {
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        }
        if (buffedItem.getFlag("HIDE_ARMOR_TRIM")) {
            meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        }
        if (buffedItem.getFlag("HIDE_DYE")) {
            meta.addItemFlags(ItemFlag.HIDE_DYE);
        }

        NamespacedKey key = new NamespacedKey(plugin, "buffeditem_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, buffedItem.getId());

        NamespacedKey versionKey = new NamespacedKey(plugin, "buffeditem_version");
        meta.getPersistentDataContainer().set(versionKey, PersistentDataType.INTEGER, buffedItem.getUpdateHash());

        ActiveAbility activeAbility = buffedItem.getActiveAbility();
        UsageDetails usageDetails = buffedItem.getUsageDetails();

        if(activeAbility.isEnabled() && usageDetails.getMaxUses() > 0){
            NamespacedKey maxKey = new NamespacedKey(plugin, "remaining_active_uses");
            meta.getPersistentDataContainer().set(maxKey, PersistentDataType.INTEGER, usageDetails.getMaxUses());

            List<Component> lore = meta.lore();
            if(lore == null){
                lore = new ArrayList<>();
            }
            String dynamicLore = buffedItem.getUsageLore(usageDetails.getMaxUses());
            if (dynamicLore != null && !dynamicLore.isEmpty()) {
                lore.add(ConfigManager.fromLegacy(dynamicLore));
            }

            meta.lore(lore);
        }
        
        if (meta instanceof org.bukkit.inventory.meta.LeatherArmorMeta leatherMeta) {
            buffedItem.getItemDisplay().getColor().ifPresent(leatherMeta::setColor);
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }
}