package io.github.altkat.BuffedItems.manager.cost;

import java.util.Map;

public interface CostFactory {
    /**
     * Creates a new ICost object from the config data.
     * @param data Key-value data from the config (amount, message, material, etc.)
     * @return The created ICost object or null in case of an error.
     */
    ICost create(Map<String, Object> data);
}