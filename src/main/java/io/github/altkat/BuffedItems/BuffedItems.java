package io.github.altkat.BuffedItems;

import io.github.altkat.BuffedItems.command.BuffedItemCommand;
import io.github.altkat.BuffedItems.command.TabCompleteHandler;
import io.github.altkat.BuffedItems.handler.UpdateHandler;
import io.github.altkat.BuffedItems.hooks.HookManager;
import io.github.altkat.BuffedItems.listener.*;
import io.github.altkat.BuffedItems.manager.attribute.ActiveAttributeManager;
import io.github.altkat.BuffedItems.manager.config.*;
import io.github.altkat.BuffedItems.manager.cooldown.CooldownManager;
import io.github.altkat.BuffedItems.manager.cost.CostManager;
import io.github.altkat.BuffedItems.manager.crafting.CraftingManager;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.manager.effect.EffectManager;
import io.github.altkat.BuffedItems.manager.item.ItemManager;
import io.github.altkat.BuffedItems.manager.set.SetManager;
import io.github.altkat.BuffedItems.manager.upgrade.UpgradeManager;
import io.github.altkat.BuffedItems.manager.upgrade.UpgradeRecipe;
import io.github.altkat.BuffedItems.menu.MenuListener;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.task.CooldownVisualsTask;
import io.github.altkat.BuffedItems.task.EffectApplicatorTask;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemUpdater;
import io.github.altkat.BuffedItems.utility.set.BuffedSet;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BuffedItems extends JavaPlugin {

    private ItemManager itemManager;
    private EffectManager effectManager;
    private ActiveAttributeManager activeAttributeManager;
    private EffectApplicatorTask effectApplicatorTask;
    private static final ConcurrentHashMap<UUID, PlayerMenuUtility> playerMenuUtilityMap = new ConcurrentHashMap<>();
    private final Map<UUID, List<ItemStack>> deathKeptItems = new HashMap<>();
    private Metrics metrics;
    private InventoryListener inventoryListener;
    private UpdateHandler updateHandler;
    private CooldownManager cooldownManager;
    private CooldownVisualsTask cooldownVisualsTask;
    private CostManager costManager;
    private UpgradeManager upgradeManager;
    private HookManager hookManager;
    private ItemUpdater itemUpdater;
    private SetManager setManager;
    private CraftingManager craftingManager;

    @Override
    public void onEnable() {

        isCompatible();

        metrics = new Metrics(this, 27592);

        metrics.addCustomChart(new SingleLineChart("total_items", () -> itemManager != null ? itemManager.getLoadedItems().size() : 0));

        metrics.addCustomChart(new SingleLineChart("valid_items", () -> {
            if (itemManager == null) return 0;
            return (int) itemManager.getLoadedItems().values().stream()
                    .filter(BuffedItem::isValid)
                    .count();
        }));

        metrics.addCustomChart(new SingleLineChart("items_with_errors", () -> {
            if (itemManager == null) return 0;
            return (int) itemManager.getLoadedItems().values().stream()
                    .filter(item -> !item.isValid())
                    .count();
        }));

        ConfigManager.setup(this);
        saveDefaultConfig();
        ItemsConfig.setup(this);
        UpgradesConfig.setup(this);
        SetsConfig.setup(this);
        RecipesConfig.setup(this);
        initializeManagers();

        try {
            ConfigUpdater.update(this, "config.yml");
            ConfigUpdater.update(this, "items.yml");
            ConfigUpdater.update(this, "upgrades.yml");
            ConfigUpdater.update(this, "sets.yml");
            ConfigUpdater.update(this, "recipes.yml");
        } catch (Exception e) {
            getLogger().severe("WARNING: Failed to run file updater tasks.");
            e.printStackTrace();
        }

        ConfigManager.reloadConfig(true);

        registerListenersAndCommands();
        startEffectTask();

        final String GITHUB_REPO = "altkat/BuffedItems";

        updateHandler = new UpdateHandler(this, GITHUB_REPO);
        updateHandler.checkAsync();

        new BukkitRunnable() {
            @Override
            public void run() {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Startup] Delayed item loading task running...");
                itemManager.loadItems(false);
                upgradeManager.loadRecipes(false);
                setManager.loadSets(false);
                craftingManager.loadRecipes(false);
                printStartupSummary();
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Startup] Delayed item loading complete.");
            }
        }.runTaskLater(this, 20L);

        ConfigManager.logInfo("&aBuffedItems has been enabled!");
    }

    @Override
    public void onDisable() {
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Shutdown] Cleaning up all online players...");

        int playerCount = Bukkit.getOnlinePlayers().size();
        int successCount = 0;
        int failCount = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Shutdown] Clearing effects for: " + player.getName());
                effectManager.clearAllAttributes(player);

                effectApplicatorTask.getManagedEffects(player.getUniqueId())
                        .forEach(player::removePotionEffect);

                successCount++;
            } catch (Exception e) {
                failCount++;
                getLogger().warning("Failed to clear effects for player " + player.getName() + ": " + e.getMessage());
            }
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Shutdown] Saving final config...");
        ConfigManager.backupConfig();

        playerMenuUtilityMap.clear();

        if (cooldownVisualsTask != null) {
            cooldownVisualsTask.cleanup();
        }

        ConfigManager.logInfo("&aCleanup complete: &e" + successCount + "/" + playerCount + "&a players cleaned" + (failCount > 0 ? "&c (" + failCount + " failed)" : ""));
        ConfigManager.logInfo("&cBuffedItems has been disabled!");
    }

    private void initializeManagers() {
        ConfigManager.loadGlobalSettings();
        hookManager = new HookManager(this);
        costManager = new CostManager(this);
        itemManager = new ItemManager(this);
        effectManager = new EffectManager(this);
        activeAttributeManager = new ActiveAttributeManager();
        cooldownManager = new CooldownManager();
        upgradeManager = new UpgradeManager(this);
        itemUpdater = new ItemUpdater(this);
        setManager = new SetManager(this);
        craftingManager = new CraftingManager(this);
    }

    private void registerListenersAndCommands() {
        getCommand("buffeditems").setExecutor(new BuffedItemCommand(this));
        getCommand("buffeditems").setTabCompleter(new TabCompleteHandler(this));

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemConsumeListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemInteractListener(this), this);
        inventoryListener = new InventoryListener(this);
        getServer().getPluginManager().registerEvents(inventoryListener, this);
        getServer().getPluginManager().registerEvents(new CraftingListener(this), this);
    }

    private void startEffectTask() {
        effectApplicatorTask = new EffectApplicatorTask(this);
        effectApplicatorTask.runTaskTimer(this, 0L, 20L);

        cooldownVisualsTask = new CooldownVisualsTask(this);
        cooldownVisualsTask.runTaskTimer(this, 0L, 2L);
    }

    public static PlayerMenuUtility getPlayerMenuUtility(Player p) {
        PlayerMenuUtility playerMenuUtility;
        UUID playerUUID = p.getUniqueId();

        if (playerMenuUtilityMap.containsKey(playerUUID)) {
            return playerMenuUtilityMap.get(playerUUID);
        } else {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Util] Creating new PlayerMenuUtility for " + p.getName());
            playerMenuUtility = new PlayerMenuUtility(p);
            playerMenuUtilityMap.put(playerUUID, playerMenuUtility);
            return playerMenuUtility;
        }
    }

    private void printStartupSummary() {
        String separator = "&#FF6347==================================================";
        ConfigManager.logInfo(separator);
        ConfigManager.logInfo("&#FCD05CBuffedItems v" + getDescription().getVersion() + " - Startup Summary");
        ConfigManager.logInfo("");

        Map<String, BuffedItem> items = itemManager.getLoadedItems();
        long validItems = items.values().stream().filter(BuffedItem::isValid).count();
        long invalidItems = items.size() - validItems;

        ConfigManager.logInfo("&#5FE2C5  Items:");
        ConfigManager.logInfo("&#5FE2C5    • Total: &f" + items.size());
        ConfigManager.logInfo("&#5FE2C5    • Valid: &a" + validItems);
        if (invalidItems > 0) {
            ConfigManager.logInfo("&#5FE2C5    • Errors: &c" + invalidItems + " &7(Check console logs)");
        }

        ConfigManager.logInfo("");
        ConfigManager.logInfo("&#FF9F43  Sets:");
        if (!SetsConfig.get().getBoolean("settings.enabled", true)) {
            ConfigManager.logInfo("&#FF9F43    • Status: &cDisabled");
        } else {
            Map<String, BuffedSet> sets = setManager.getSets();
            if (sets.isEmpty()) {
                ConfigManager.logInfo("&#FF9F43    • Total: &f0");
            } else {
                long validSets = sets.values().stream().filter(BuffedSet::isValid).count();
                long invalidSets = sets.size() - validSets;
                ConfigManager.logInfo("&#FF9F43    • Total: &f" + sets.size());
                ConfigManager.logInfo("&#FF9F43    • Valid: &a" + validSets);
                if (invalidSets > 0) {
                    ConfigManager.logInfo("&#FF9F43    • Errors: &c" + invalidSets + " &7(Check console logs)");
                }
            }
        }

        ConfigManager.logInfo("");
        ConfigManager.logInfo("&#F5CD66  Upgrades:");
        if (!UpgradesConfig.get().getBoolean("settings.enabled", true)) {
            ConfigManager.logInfo("&#F5CD66    • Status: &cDisabled");
        } else {
            Map<String, UpgradeRecipe> recipes = upgradeManager.getRecipes();
            if (recipes.isEmpty()) {
                ConfigManager.logInfo("&#F5CD66    • Total: &f0");
            } else {
                long validRecipes = recipes.values().stream().filter(UpgradeRecipe::isValid).count();
                long invalidRecipes = recipes.size() - validRecipes;
                ConfigManager.logInfo("&#F5CD66    • Total: &f" + recipes.size());
                ConfigManager.logInfo("&#F5CD66    • Valid: &a" + validRecipes);
                if (invalidRecipes > 0) {
                    ConfigManager.logInfo("&#F5CD66    • Errors: &c" + invalidRecipes + " &7(Check console logs)");
                }
            }
        }

        ConfigManager.logInfo("");
        ConfigManager.logInfo("&#82E0AA  Crafting:");
        if (!RecipesConfig.get().getBoolean("settings.enabled", true)) {
            ConfigManager.logInfo("&#82E0AA    • Status: &cDisabled");
        } else {
            Map<String, CustomRecipe> craftingRecipes = craftingManager.getRecipes();
            if (craftingRecipes.isEmpty()) {
                ConfigManager.logInfo("&#82E0AA    • Total: &f0");
            } else {
                long validCrafting = craftingRecipes.values().stream().filter(CustomRecipe::isValid).count();
                long invalidCrafting = craftingRecipes.size() - validCrafting;
                ConfigManager.logInfo("&#82E0AA    • Total: &f" + craftingRecipes.size());
                ConfigManager.logInfo("&#82E0AA    • Valid: &a" + validCrafting);
                if (invalidCrafting > 0) {
                    ConfigManager.logInfo("&#82E0AA    • Errors: &c" + invalidCrafting + " &7(Check console logs)");
                }
            }
        }

        List<String> activeHooks = new ArrayList<>();
        if (hookManager.isPlaceholderAPILoaded()) activeHooks.add("PlaceholderAPI");
        if (hookManager.isVaultLoaded()) activeHooks.add("Vault");
        if (hookManager.isCoinsEngineLoaded()) activeHooks.add("CoinsEngine");
        if (hookManager.isItemsAdderLoaded()) activeHooks.add("ItemsAdder");
        if (hookManager.isNexoLoaded()) activeHooks.add("Nexo");

        ConfigManager.logInfo("");
        ConfigManager.logInfo("&#9B59B6  Hooks:");
        if (activeHooks.isEmpty()) {
            ConfigManager.logInfo("&#9B59B6    • &7None");
        } else {
            ConfigManager.logInfo("&#9B59B6    • Active: &f" + String.join(", ", activeHooks));
        }

        int currentDebugLevel = ConfigManager.getDebugLevel();
        String debugLevelInfo;
        if (currentDebugLevel <= ConfigManager.DEBUG_OFF) debugLevelInfo = "OFF";
        else if (currentDebugLevel == ConfigManager.DEBUG_INFO) debugLevelInfo = "INFO";
        else if (currentDebugLevel == ConfigManager.DEBUG_TASK) debugLevelInfo = "TASK";
        else if (currentDebugLevel == ConfigManager.DEBUG_DETAILED) debugLevelInfo = "DETAILED";
        else debugLevelInfo = "VERBOSE";

        String currentVersion = getDescription().getVersion();
        String latestVersion = (updateHandler != null) ? updateHandler.getLatestVersionCached() : null;
        String versionStatus;
        if (latestVersion == null) {
            versionStatus = "&7(Checking...)";
        } else if (UpdateHandler.isNewerVersion(currentVersion, latestVersion)) {
            versionStatus = "&c(Update Available: " + latestVersion + ")";
        } else {
            versionStatus = "&a(Latest)";
        }

        ConfigManager.logInfo("");
        ConfigManager.logInfo("&#5DADE2  System:");
        ConfigManager.logInfo("&#5DADE2    • Version: &f" + currentVersion + " " + versionStatus);
        ConfigManager.logInfo("&#5DADE2    • Debug Level: &f" + debugLevelInfo + " (" + currentDebugLevel + ")");
        ConfigManager.logInfo("&#5DADE2    • Server: &f" + Bukkit.getName() + " " + Bukkit.getMinecraftVersion());

        ConfigManager.logInfo(separator);
    }

    public ItemManager getItemManager() { return itemManager; }
    public EffectManager getEffectManager() { return effectManager; }
    public ActiveAttributeManager getActiveAttributeManager() { return activeAttributeManager; }
    public EffectApplicatorTask getEffectApplicatorTask() { return effectApplicatorTask; }
    public Map<UUID, List<ItemStack>> getDeathKeptItems() {
        return deathKeptItems;
    }
    public static void removePlayerMenuUtility(UUID uuid) {
        if(playerMenuUtilityMap.containsKey(uuid)) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Util] Removing PlayerMenuUtility for " + uuid);
        }
        playerMenuUtilityMap.remove(uuid);
    }
    public InventoryListener getInventoryChangeListener(){
        return inventoryListener;
    }
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    public CostManager getCostManager() {
        return costManager;
    }
    public UpgradeManager getUpgradeManager() { return upgradeManager; }
    public HookManager getHookManager() {
        return hookManager;
    }
    public UpdateHandler getUpdateHandler() {
        return updateHandler;
    }
    public ItemUpdater getItemUpdater() { return itemUpdater;}
    public SetManager getSetManager() {return setManager;}
    public CraftingManager getCraftingManager() {return craftingManager;}
    private void isCompatible() {
        try {
            Class.forName("com.destroystokyo.paper.event.player.PlayerArmorChangeEvent");
        }
        catch (ClassNotFoundException e) {
            getLogger().severe("==============================================================");
            getLogger().severe("! ! ! SEVERE WARNING ! ! !");
            getLogger().severe(" ");
            getLogger().severe("This plugin (BuffedItems) requires the Paper API to function properly.");
            getLogger().severe("It provides no guarantee to work on Spigot and will cause errors.");
            getLogger().severe("Please consider using a Paper-based server (Paper, Purpur, Pufferfish, etc.).");
            getLogger().severe("==============================================================");;
        }
    }
}