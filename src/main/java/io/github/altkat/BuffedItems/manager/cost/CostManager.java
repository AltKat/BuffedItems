package io.github.altkat.BuffedItems.manager.cost;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CostManager {

    private final BuffedItems plugin;
    private final Map<String, CostFactory> factories = new HashMap<>();

    public CostManager(BuffedItems plugin) {
        this.plugin = plugin;
        registerDefaults();
    }

    /**
     * Registers a new Cost type in the system.
     */
    public void registerFactory(String type, CostFactory factory) {
        factories.put(type.toUpperCase(), factory);
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[CostManager] Registered cost type: " + type.toUpperCase());
    }

    private void registerDefaults() {
        registerFactory("EXPERIENCE", ExperienceCost::new);
        registerFactory("LEVEL", LevelCost::new);
        registerFactory("HUNGER", HungerCost::new);
        registerFactory("HEALTH", HealthCost::new);
        registerFactory("ITEM", ItemCost::new);
        registerFactory("BUFFED_ITEM", data -> new BuffedItemCost(data, plugin));

        if (plugin.getHookManager().isCoinsEngineLoaded()) {
            registerFactory("COINSENGINE", data -> new CoinsEngineCost(data, plugin.getHookManager().getCoinsEngineHook()));
        }

        if (plugin.getHookManager().isVaultLoaded()) {
            registerFactory("MONEY", data -> new MoneyCost(data, plugin.getHookManager().getVaultHook()));
        }

        if (plugin.getHookManager().isAuraSkillsLoaded()) {
            registerFactory("AURASKILLS_MANA", data -> new AuraSkillsManaCost(data, plugin.getHookManager().getAuraSkillsHook()));
        }
    }

    /**
     * Generates Cost objects from the config list.
     */
    public ICost parseCost(Map<?, ?> rawMap) {
        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            data.put(entry.getKey().toString(), entry.getValue());
        }

        String type = (String) data.get("type");
        if (type == null) return null;

        CostFactory factory = factories.get(type.toUpperCase());
        if (factory != null) {
            return factory.create(data);
        } else {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[CostManager] Unknown cost type: " + type);
            return null;
        }
    }

    /**
     * Generates Cost objects from the config list.
     */
    public List<ICost> parseCosts(List<Map<?, ?>> configList) {
        List<ICost> costs = new ArrayList<>();
        if (configList == null) return costs;

        for (Map<?, ?> rawMap : configList) {
            ICost cost = parseCost(rawMap);
            if (cost != null) {
                costs.add(cost);
            }
        }
        return costs;
    }
}