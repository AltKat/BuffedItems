package io.github.altkat.BuffedItems;

import io.github.altkat.BuffedItems.Commands.Commands;
import io.github.altkat.BuffedItems.Commands.TabCompleterHandler;
import io.github.altkat.BuffedItems.Handlers.UpdateChecker;
import io.github.altkat.BuffedItems.Listeners.*;
import io.github.altkat.BuffedItems.Managers.*;
import io.github.altkat.BuffedItems.Menu.PlayerMenuUtility;
import io.github.altkat.BuffedItems.Tasks.EffectApplicatorTask;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BuffedItems extends JavaPlugin {

    private ItemManager itemManager;
    private EffectManager effectManager;
    private ActiveAttributeManager activeAttributeManager;
    private EffectApplicatorTask effectApplicatorTask;
    private static final ConcurrentHashMap<UUID, PlayerMenuUtility> playerMenuUtilityMap = new ConcurrentHashMap<>();
    private final Map<UUID, List<ItemStack>> deathKeptItems = new HashMap<>();
    private Metrics metrics;
    private boolean placeholderApiEnabled = false;
    private InventoryChangeListener inventoryChangeListener;
    private UpdateChecker updateChecker;

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
        ItemsConfig.setup(this);

        initializeManagers();

        try {
            ConfigUpdater.update(this, "config.yml");
            ConfigUpdater.update(this, "items.yml");
        } catch (Exception e) {
            getLogger().severe("WARNING: Failed to run file updater tasks.");
            e.printStackTrace();
        }

        ConfigManager.reloadConfig(true);

        PluginManager pm = getServer().getPluginManager();
        if (pm.getPlugin("PlaceholderAPI") != null) {
            placeholderApiEnabled = true;
            ConfigManager.logInfo("&aPlaceholderAPI found! Enabling placeholder support.");
        } else {
            placeholderApiEnabled = false;
        }

        registerListenersAndCommands();
        startEffectTask();

        final String GITHUB_REPO = "altkat/BuffedItems";

        updateChecker = new UpdateChecker(this, GITHUB_REPO);
        updateChecker.checkAsync();

        new BukkitRunnable() {
            @Override
            public void run() {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Startup] Delayed item loading task running...");
                itemManager.loadItems(false);
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

        ConfigManager.logInfo("&aCleanup complete: &e" + successCount + "/" + playerCount + "&a players cleaned" + (failCount > 0 ? "&c (" + failCount + " failed)" : ""));
        ConfigManager.logInfo("&cBuffedItems has been disabled!");
    }

    private void initializeManagers() {
        ConfigManager.loadGlobalSettings();
        itemManager = new ItemManager(this);
        effectManager = new EffectManager(this);
        activeAttributeManager = new ActiveAttributeManager();
    }

    private void registerListenersAndCommands() {
        getCommand("buffeditems").setExecutor(new Commands(this));
        getCommand("buffeditems").setTabCompleter(new TabCompleterHandler(this));

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemConsumeListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemProtectionListener(this), this);
        inventoryChangeListener = new InventoryChangeListener(this);
        getServer().getPluginManager().registerEvents(inventoryChangeListener, this);
    }

    private void startEffectTask() {
        effectApplicatorTask = new EffectApplicatorTask(this);
        effectApplicatorTask.runTaskTimer(this, 0L, 20L);
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
        Map<String, BuffedItem> items = itemManager.getLoadedItems();
        long validItems = items.values().stream().filter(BuffedItem::isValid).count();
        long invalidItems = items.size() - validItems;

        String separator = "&#FF6347==================================================";
        ConfigManager.logInfo(separator);
        ConfigManager.logInfo("&#FCD05CBuffedItems v" + getDescription().getVersion() + " - Startup Summary");
        ConfigManager.logInfo("&#5FE2C5  Total Items: " + items.size());
        ConfigManager.logInfo("&#5FE2C5  Valid Items: " + validItems);

        if (invalidItems > 0) {
            ConfigManager.logInfo("&#E25F5F  Items with Errors: " + invalidItems);
            ConfigManager.logInfo("&#F5CD66  Run '/bi list' or '/bi menu' to view details.");
        }

        int currentDebugLevel = ConfigManager.getDebugLevel();
        String debugLevelInfo;

        if (currentDebugLevel <= ConfigManager.DEBUG_OFF) { // 0
            debugLevelInfo = "0 (OFF)";
        } else if (currentDebugLevel == ConfigManager.DEBUG_INFO) { // 1
            debugLevelInfo = "1 (INFO)";
        } else if (currentDebugLevel == ConfigManager.DEBUG_TASK) { // 2
            debugLevelInfo = "2 (TASK)";
        } else if (currentDebugLevel == ConfigManager.DEBUG_DETAILED) { // 3
            debugLevelInfo = "3 (DETAILED)";
        } else { // 4+ (VERBOSE)
            debugLevelInfo = currentDebugLevel + " (VERBOSE)";
        }

        ConfigManager.logInfo("&#5FE2C5  Debug Level: " + debugLevelInfo);
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
    public boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }
    public InventoryChangeListener getInventoryChangeListener(){
        return inventoryChangeListener;
    }
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