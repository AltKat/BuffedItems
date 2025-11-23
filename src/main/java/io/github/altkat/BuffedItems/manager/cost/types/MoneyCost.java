package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.hooks.VaultHook;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import org.bukkit.entity.Player;

import java.util.Map;

public class MoneyCost implements ICost {
    private final double amount;
    private final String failureMessage;
    private final VaultHook vault;

    public MoneyCost(Map<String, Object> data, VaultHook vault) {
        this.vault = vault;
        if (vault == null || !vault.isHooked()) {
            throw new IllegalStateException("Vault economy is not hooked/loaded.");
        }

        this.amount = ((Number) data.getOrDefault("amount", 0)).doubleValue();

        if (this.amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        String defaultMsg = ConfigManager.getDefaultCostMessage("MONEY");
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);
    }

    @Override
    public String getDisplayString() {
        return "$" + amount;
    }

    @Override
    public boolean hasEnough(Player player) {
        return vault.has(player, amount);
    }

    @Override
    public void deduct(Player player) {
        vault.withdraw(player, amount);
    }

    @Override
    public String getFailureMessage() {
        return ConfigManager.stripLegacy(failureMessage).replace("{amount}", String.valueOf(amount));
    }
}