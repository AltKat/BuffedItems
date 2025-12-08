package io.github.altkat.BuffedItems.manager.crafting;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class ItemMatcher {

    private final BuffedItems plugin;
    private final NamespacedKey nbtKey;

    public ItemMatcher(BuffedItems plugin) {
        this.plugin = plugin;
        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
    }

    public boolean matches(ItemStack inputItem, RecipeIngredient ingredient) {
        if (ingredient == null) {
            return inputItem == null || inputItem.getType().isAir();
        }

        if (inputItem == null || inputItem.getType().isAir()) {
            return false;
        }

        if (inputItem.getAmount() < ingredient.getAmount()) {
            return false;
        }

        if (inputItem.getType() != ingredient.getMaterial()) {
            return false;
        }

        switch (ingredient.getMatchType()) {
            case MATERIAL:
                return true;

            case BUFFED_ITEM:
                if (!inputItem.hasItemMeta()) return false;
                String inputId = inputItem.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
                return ingredient.getData().equals(inputId);

            case EXACT:
                ItemStack ref = ingredient.getExactReferenceItem();
                if (ref == null) return false;
                return inputItem.isSimilar(ref);

            default:
                return false;
        }
    }
}