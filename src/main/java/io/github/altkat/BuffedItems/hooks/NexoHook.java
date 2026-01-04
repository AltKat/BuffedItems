package io.github.altkat.BuffedItems.hooks;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import org.bukkit.inventory.ItemStack;

public class NexoHook {

    public NexoHook() {
        ConfigManager.logInfo("&aNexo hooked successfully!");
    }

    public Integer getCustomModelData(String itemId) {
        ItemStack item = getItemStack(itemId);
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            return item.getItemMeta().getCustomModelData();
        }
        return null;
    }

    public ItemStack getItemStack(String itemId) {
        ItemBuilder builder = NexoItems.itemFromId(itemId);
        if (builder != null) {
            return builder.build();
        }
        return null;
    }
}