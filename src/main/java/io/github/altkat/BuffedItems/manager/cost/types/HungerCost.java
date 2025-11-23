package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import org.bukkit.entity.Player;

import java.util.Map;

public class HungerCost implements ICost {
    private final int amount;
    private final String failureMessage;

    public HungerCost(Map<String, Object> data) {
        this.amount = ((Number) data.getOrDefault("amount", 1)).intValue();
        String defaultMsg = ConfigManager.getDefaultCostMessage("HUNGER");
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);
    }

    @Override
    public String getDisplayString() {
        return amount + " Food Level";
    }

    @Override
    public boolean hasEnough(Player player) {
        return player.getFoodLevel() >= amount;
    }

    @Override
    public void deduct(Player player) {
        player.setFoodLevel(Math.max(0, player.getFoodLevel() - amount));
    }

    @Override
    public String getFailureMessage() {
        return ConfigManager.stripLegacy(failureMessage).replace("{amount}", String.valueOf(amount));
    }
}