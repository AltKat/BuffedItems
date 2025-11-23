package io.github.altkat.BuffedItems.hooks;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import org.bukkit.entity.Player;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;

public class CoinsEngineHook {

    public CoinsEngineHook() {
        ConfigManager.logInfo("&aCoinsEngine hooked successfully!");
    }

    public Currency getCurrency(String currencyId) {
        return CoinsEngineAPI.getCurrency(currencyId);
    }

    public double getBalance(Player player, Currency currency) {
        return CoinsEngineAPI.getBalance(player, currency);
    }

    public void removeBalance(Player player, Currency currency, double amount) {
        CoinsEngineAPI.removeBalance(player, currency, amount);
    }
}