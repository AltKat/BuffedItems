package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import java.util.Map;

public class MoneyCost implements ICost {
    private final double amount;
    private final String failureMessage;
    private final Economy economy;

    public MoneyCost(Map<String, Object> data, Economy economy) {
        this.economy = economy;
        this.amount = ((Number) data.getOrDefault("amount", 0)).doubleValue();
        String defaultMsg = ConfigManager.getDefaultCostMessage("MONEY");
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);
    }

    @Override
    public String getDisplayString() {
        return "$" + amount;
    }

    @Override
    public boolean hasEnough(Player player) {
        return economy.has(player, amount);
    }

    @Override
    public void deduct(Player player) {
        economy.withdrawPlayer(player, amount);
    }

    @Override
    public String getFailureMessage() {
        return ConfigManager.stripLegacy(failureMessage).replace("{amount}", String.valueOf(amount));
    }
}