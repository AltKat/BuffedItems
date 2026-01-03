package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.hooks.AuraSkillsHook;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import org.bukkit.entity.Player;

import java.util.Map;

public class AuraSkillsManaCost implements ICost {
    private final double amount;
    private final String failureMessage;
    private final AuraSkillsHook auraSkillsHook;

    public AuraSkillsManaCost(Map<String, Object> data, AuraSkillsHook hook) {
        this.auraSkillsHook = hook;
        if (hook == null) {
            throw new IllegalStateException("AuraSkills is not hooked/loaded.");
        }

        this.amount = ((Number) data.getOrDefault("amount", 0)).doubleValue();

        if (this.amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        String defaultMsg = ConfigManager.getDefaultCostMessage("AURASKILLS_MANA");
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);
    }

    @Override
    public boolean hasEnough(Player player) {
        return auraSkillsHook.checkMana(player, amount);
    }

    @Override
    public void deduct(Player player) {
        auraSkillsHook.consumeMana(player, amount);
    }

    @Override
    public String getFailureMessage() {
        return ConfigManager.stripLegacy(failureMessage).replace("{amount}", String.valueOf(amount));
    }

    @Override
    public String getDisplayString() {
        return amount + " Mana";
    }
}
