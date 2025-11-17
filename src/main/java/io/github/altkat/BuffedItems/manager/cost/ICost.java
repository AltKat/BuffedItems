package io.github.altkat.BuffedItems.manager.cost;

import org.bukkit.entity.Player;

public interface ICost {
    /**
     * Checks whether the player can afford this cost.
     */
    boolean hasEnough(Player player);

    /**
     * The cost is deducted from the player.
     */
    void deduct(Player player);

    /**
     * Message to be sent in case of insufficient balance.
     */
    String getFailureMessage();
}