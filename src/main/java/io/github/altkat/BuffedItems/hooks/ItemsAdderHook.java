package io.github.altkat.BuffedItems.hooks;

import dev.lone.itemsadder.api.CustomStack;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderHook {

    public ItemsAdderHook() {
        ConfigManager.logInfo("&aItemsAdder hooked successfully!");
    }

    public Integer getCustomModelData(String itemId) {
        CustomStack stack = CustomStack.getInstance(itemId);
        if (stack != null) {
            ItemStack item = stack.getItemStack();
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                return item.getItemMeta().getCustomModelData();
            }
        }
        return null;
    }
}