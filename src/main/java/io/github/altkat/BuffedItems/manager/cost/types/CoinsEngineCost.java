package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import org.bukkit.entity.Player;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

import java.util.Map;

public class CoinsEngineCost implements ICost {

    private final double amount;
    private final String currencyId;
    private final String failureMessage;

    public CoinsEngineCost(Map<String, Object> data) {
        this.amount = ((Number) data.getOrDefault("amount", 0)).doubleValue();
        this.currencyId = (String) data.getOrDefault("currency_id", "coins");

        String defaultMsg = ConfigManager.getDefaultCostMessage("COINSENGINE");
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);
    }

    @Override
    public String getDisplayString() {
        return amount + " " + currencyId;
    }

    @Override
    public boolean hasEnough(Player player) {
        Currency currency = CoinsEngineAPI.getCurrency(currencyId);
        if (currency == null) return false;

        return CoinsEngineAPI.getBalance(player, currency) >= amount;
    }

    @Override
    public void deduct(Player player) {
        Currency currency = CoinsEngineAPI.getCurrency(currencyId);
        if (currency != null) {
            CoinsEngineAPI.removeBalance(player, currency, amount);
        }
    }

    @Override
    public String getFailureMessage() {
        Currency currency = CoinsEngineAPI.getCurrency(currencyId);
        String currencyName = (currency != null) ? currency.getName() : currencyId;

        return ConfigManager.stripLegacy(failureMessage)
                .replace("{amount}", String.valueOf(amount))
                .replace("{currency_name}", currencyName);
    }
}