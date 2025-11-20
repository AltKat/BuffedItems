package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import org.bukkit.entity.Player;

import java.util.Map;

public class ExperienceCost implements ICost {

    private final int amount;
    private final String failureMessage;

    public ExperienceCost(Map<String, Object> data) {
        this.amount = ((Number) data.getOrDefault("amount", 0)).intValue();
        String defaultMsg = ConfigManager.getDefaultCostMessage("EXPERIENCE");
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);
    }

    @Override
    public String getDisplayString() {
        return amount + " XP Points";
    }

    @Override
    public boolean hasEnough(Player player) {
        return getTotalExperience(player) >= amount;
    }

    @Override
    public void deduct(Player player) {
        int current = getTotalExperience(player);
        setTotalExperience(player, Math.max(0, current - amount));
    }

    @Override
    public String getFailureMessage() {
        return ConfigManager.stripLegacy(failureMessage).replace("{amount}", String.valueOf(amount));
    }

    /**
     * Calculates the player's total XP points.
     * Based on Minecraft's complex level formula.
     */
    private int getTotalExperience(Player player) {
        int experience = 0;
        int level = player.getLevel();

        if (level >= 0 && level <= 15) {
            experience = (int) Math.ceil(Math.pow(level, 2) + 6 * level);
            int requiredExperience = 2 * level + 7;
            double currentExp = Double.parseDouble(Float.toString(player.getExp()));
            experience += Math.ceil(currentExp * requiredExperience);
        } else if (level > 15 && level <= 30) {
            experience = (int) Math.ceil(2.5 * Math.pow(level, 2) - 40.5 * level + 360);
            int requiredExperience = 5 * level - 38;
            double currentExp = Double.parseDouble(Float.toString(player.getExp()));
            experience += Math.ceil(currentExp * requiredExperience);
        } else {
            experience = (int) Math.ceil(4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
            int requiredExperience = 9 * level - 158;
            double currentExp = Double.parseDouble(Float.toString(player.getExp()));
            experience += Math.ceil(currentExp * requiredExperience);
        }

        return experience;
    }

    /**
     * Sets the player's total XP to a specific value.
     */
    private void setTotalExperience(Player player, int amount) {
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);
        player.giveExp(amount);
    }
}