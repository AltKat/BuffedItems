package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import org.bukkit.entity.Player;

import java.util.Map;

public class HealthCost implements ICost {
    private final double amount;
    private final String failureMessage;

    public HealthCost(Map<String, Object> data) {
        this.amount = ((Number) data.getOrDefault("amount", 2.0)).doubleValue();
        String defaultMsg = ConfigManager.getDefaultCostMessage("HEALTH");
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);
    }

    @Override
    public boolean hasEnough(Player player) {
        return player.getHealth() > amount;
    }

    @Override
    public String getDisplayString() {
        return amount + " Health (Hearts)";
    }

    @Override
    public void deduct(Player player) {
        double newHealth = player.getHealth() - amount;
        if (newHealth < 0) newHealth = 0;
        player.setHealth(newHealth);
    }

    @Override
    public String getFailureMessage() {
        return ConfigManager.stripLegacy(failureMessage).replace("{amount}", String.valueOf(amount));
    }
}