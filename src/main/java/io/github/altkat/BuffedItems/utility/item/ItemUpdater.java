package io.github.altkat.BuffedItems.utility.item;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemUpdater {

    private final BuffedItems plugin;
    private final NamespacedKey idKey;
    private final NamespacedKey usesKey;

    public ItemUpdater(BuffedItems plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "buffeditem_id");
        this.usesKey = new NamespacedKey(plugin, "remaining_active_uses");
    }

    public ItemStack updateItem(ItemStack oldItem, Player player) {
        if (oldItem == null || !oldItem.hasItemMeta()) return null;

        String itemId = oldItem.getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        if (itemId == null) return null;

        BuffedItem template = plugin.getItemManager().getBuffedItem(itemId);
        if (template == null) return null;

        if (template.getCachedItem() == null) {
            template.setCachedItem(new ItemBuilder(template, plugin).build());
        }

        ItemStack newItem = template.getCachedItem().clone();
        newItem.setAmount(oldItem.getAmount());

        ItemMeta oldMeta = oldItem.getItemMeta();
        ItemMeta newMeta = newItem.getItemMeta();

        if (newMeta.hasDisplayName()) {
            String rawName = template.getDisplayName();
            String parsedName = plugin.getHookManager().processPlaceholders(player, rawName);
            newMeta.displayName(ConfigManager.fromLegacy(parsedName));
        }

        if (newMeta.hasLore()) {
            List<Component> parsedLore = new ArrayList<>();
            for (String line : template.getLore()) {
                String parsedLine = plugin.getHookManager().processPlaceholders(player, line);
                parsedLore.add(ConfigManager.fromLegacy(parsedLine));
            }
            newMeta.lore(parsedLore);
        }

        if (oldMeta.hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : oldMeta.getEnchants().entrySet()) {
                newMeta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
        }

        if (oldMeta instanceof Damageable && newMeta instanceof Damageable) {
            ((Damageable) newMeta).setDamage(((Damageable) oldMeta).getDamage());
        }
        if (oldMeta instanceof Repairable && newMeta instanceof Repairable) {
            int oldCost = ((Repairable) oldMeta).getRepairCost();
            if (oldCost > 0) ((Repairable) newMeta).setRepairCost(oldCost);
        }
        if (oldMeta instanceof LeatherArmorMeta && newMeta instanceof LeatherArmorMeta) {
            LeatherArmorMeta oldLeather = (LeatherArmorMeta) oldMeta;
            LeatherArmorMeta newLeather = (LeatherArmorMeta) newMeta;
            if (!oldLeather.getColor().equals(plugin.getServer().getItemFactory().getDefaultLeatherColor())) {
                newLeather.setColor(oldLeather.getColor());
            }
        }
        if (oldMeta instanceof ArmorMeta && newMeta instanceof ArmorMeta) {
            ArmorMeta oldArmor = (ArmorMeta) oldMeta;
            ArmorMeta newArmor = (ArmorMeta) newMeta;

            if (oldArmor.hasTrim()) {
                newArmor.setTrim(oldArmor.getTrim());
            }
        }

        if (template.getMaxUses() > 0) {
            int usesToSet = template.getMaxUses();

            if (oldMeta.getPersistentDataContainer().has(usesKey, PersistentDataType.INTEGER)) {
                usesToSet = oldMeta.getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER);
            }

            newMeta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, usesToSet);
            updateLoreWithCurrentUses(newMeta, template, usesToSet, player);
        }
        else {
            newMeta.getPersistentDataContainer().remove(usesKey);
        }

        newItem.setItemMeta(newMeta);
        return newItem;
    }

    private void updateLoreWithCurrentUses(ItemMeta meta, BuffedItem item, int currentUses, Player player) {
        List<Component> lore = meta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        String rawMaxLine = item.getUsageLore(item.getMaxUses());
        String parsedMaxLine = plugin.getHookManager().processPlaceholders(player, rawMaxLine);
        String cleanMaxLine = ConfigManager.toPlainText(ConfigManager.fromLegacy(parsedMaxLine));

        boolean found = false;

        String rawNewLine;
        if (currentUses > 0) {
            rawNewLine = item.getUsageLore(currentUses);
        } else {
            rawNewLine = item.getDepletedLore();
        }

        String parsedNewLine = plugin.getHookManager().processPlaceholders(player, rawNewLine);
        Component finalComponent = ConfigManager.fromLegacy(parsedNewLine);

        for (int i = 0; i < lore.size(); i++) {
            String cleanCurrentLine = ConfigManager.toPlainText(lore.get(i));

            if (cleanCurrentLine.equals(cleanMaxLine)) {
                lore.set(i, finalComponent);
                found = true;
                break;
            }
        }

        if (!found) {
            lore.add(finalComponent);
        }

        meta.lore(lore);
    }
}