package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.hooks.CoinsEngineHook;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import org.bukkit.entity.Player;
import su.nightexpress.coinsengine.api.currency.Currency;

import java.util.Map;

public class CoinsEngineCost implements ICost {

    private final double amount;
    private final String currencyId;
    private final String failureMessage;
    private final CoinsEngineHook hook;

    public CoinsEngineCost(Map<String, Object> data, CoinsEngineHook hook) {
        this.hook = hook;
        this.currencyId = (String) data.getOrDefault("currency_id", "coins");

        Currency currency = hook.getCurrency(this.currencyId);
        if (currency == null) {
            throw new IllegalArgumentException("Invalid CoinsEngine Currency ID: '" + this.currencyId + "'");
        }

        this.amount = ((Number) data.getOrDefault("amount", 0)).doubleValue();

        if (this.amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        String defaultMsg = ConfigManager.getDefaultCostMessage("COINSENGINE");
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);
    }

    @Override
    public String getDisplayString() {
        return amount + " " + currencyId;
    }

    @Override
    public boolean hasEnough(Player player) {
        Currency currency = hook.getCurrency(currencyId);
        if (currency == null) return false;

        return hook.getBalance(player, currency) >= amount;
    }

    @Override
    public void deduct(Player player) {
        Currency currency = hook.getCurrency(currencyId);
        if (currency != null) {
            hook.removeBalance(player, currency, amount);
        }
    }

    @Override
    public String getFailureMessage() {
        Currency currency = hook.getCurrency(currencyId);
        String currencyName = (currency != null) ? currency.getName() : currencyId;

        return ConfigManager.stripLegacy(failureMessage)
                .replace("{amount}", String.valueOf(amount))
                .replace("{currency_name}", currencyName);
    }
}