package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class ItemCost implements ICost {
    private final Material material;
    private final int amount;
    private final String failureMessage;

    public ItemCost(Map<String, Object> data) {
        String matName = (String) data.getOrDefault("material", "STONE");
        this.material = Material.matchMaterial(matName);
        this.amount = ((Number) data.getOrDefault("amount", 1)).intValue();
        String defaultMsg = ConfigManager.getDefaultCostMessage("ITEM");
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);
    }

    @Override
    public boolean hasEnough(Player player) {
        if (material == null) return false;
        return player.getInventory().contains(material, amount);
    }

    @Override
    public void deduct(Player player) {
        if (material == null) return;
        player.getInventory().removeItem(new ItemStack(material, amount));
    }

    @Override
    public String getFailureMessage() {
        String matName = (material != null) ? material.name() : "INVALID_ITEM";
        return ConfigManager.stripLegacy(failureMessage)
                .replace("{amount}", String.valueOf(amount))
                .replace("{material}", matName);
    }
}