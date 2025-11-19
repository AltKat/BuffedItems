package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import org.bukkit.entity.Player;
import java.util.Map;

public class LevelCost implements ICost {
    private final int amount;
    private final String failureMessage;

    public LevelCost(Map<String, Object> data) {
        this.amount = ((Number) data.getOrDefault("amount", 1)).intValue();
        String defaultMsg = ConfigManager.getDefaultCostMessage("LEVEL");
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);
    }

    @Override
    public boolean hasEnough(Player player) {
        return player.getLevel() >= amount;
    }

    @Override
    public void deduct(Player player) {
        player.setLevel(Math.max(0, player.getLevel() - amount));
    }

    @Override
    public String getFailureMessage() {
        return ConfigManager.stripLegacy(failureMessage).replace("{amount}", String.valueOf(amount));
    }
}