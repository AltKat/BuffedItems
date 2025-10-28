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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BuffedItems extends JavaPlugin {

    private ItemManager itemManager;
    private EffectManager effectManager;
    private ActiveAttributeManager activeAttributeManager;
    private EffectApplicatorTask effectApplicatorTask;
    private static final HashMap<UUID, PlayerMenuUtility> playerMenuUtilityMap = new HashMap<>();
    private final Map<UUID, List<ItemStack>> deathKeptItems = new HashMap<>();
    private BukkitTask autoSaveTask;
    private long autoSaveIntervalTicks = 6000L;

    @Override
    public void onEnable() {
        new Metrics(this, 27592);

        saveDefaultConfig();
        getConfig().options().copyHeader(true);
        ConfigManager.setup(this);

        try {
            ConfigUpdater.update(this);
        } catch (Exception e) {
            getLogger().severe("CRITICAL: Failed to update or load config.yml. Plugin may not function correctly.");
            e.printStackTrace();
        }
        reloadConfig();

        initializeManagers();
        registerListenersAndCommands();
        startEffectTask();
        startAutoSaveTask();

        final int SPIGOT_RESOURCE_ID = 129550;
        new UpdateChecker(this, SPIGOT_RESOURCE_ID).getVersion(newVersion -> {
            if (UpdateChecker.isNewerVersion(this.getDescription().getVersion(), newVersion)) {
                ConfigManager.logInfo("§eA new update is available! Version: &a" + newVersion);
                ConfigManager.logInfo("&eDownload it from: &ahttps://www.spigotmc.org/resources/buffeditems.129550/");
            } else {
                ConfigManager.logInfo("&aYou are using the latest version. (&e" + this.getDescription().getVersion() + "&a)");
            }
        });

        new BukkitRunnable() {
            @Override
            public void run() {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Startup] Delayed item loading task running...");
                itemManager.loadItems(false);
                printStartupSummary();
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Startup] Delayed item loading complete.");
            }
        }.runTaskLater(this, 20L);

        ConfigManager.logInfo("§aBuffedItems has been enabled!");
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

        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Shutdown] Saving final config...");
        ConfigManager.backupConfig();
        saveConfig();

        playerMenuUtilityMap.clear();

        ConfigManager.logInfo("&aCleanup complete: &e" + successCount + "/" + playerCount + "&a players cleaned" + (failCount > 0 ? "&c (" + failCount + " failed)" : ""));
        ConfigManager.logInfo("§cBuffedItems has been disabled!");
    }

    private void initializeManagers() {
        ConfigManager.loadGlobalSettings();
        updateAutoSaveIntervalVariable();
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
        getServer().getPluginManager().registerEvents(new InventoryChangeListener(this), this);
    }

    private void startEffectTask() {
        effectApplicatorTask = new EffectApplicatorTask(this);
        effectApplicatorTask.runTaskTimer(this, 0L, 20L);
    }

    private void startAutoSaveTask() {
        this.autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[AutoSave] Saving configuration to disk...");
                ConfigManager.backupConfig();
                saveConfig();
                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[AutoSave] Configuration saved.");
            }
        }.runTaskTimerAsynchronously(this, this.autoSaveIntervalTicks, this.autoSaveIntervalTicks);
    }

    private void updateAutoSaveIntervalVariable() {
        this.autoSaveIntervalTicks = getConfig().getLong("auto-save-interval-minutes", 5) * 20 * 60;
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Config] Auto-save interval set to " + (this.autoSaveIntervalTicks / 20 / 60) + " minutes (" + this.autoSaveIntervalTicks + " ticks).");
    }

    public void reloadConfigSettings() {
        updateAutoSaveIntervalVariable();
        restartAutoSaveTask();
    }

    public void restartAutoSaveTask() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[AutoSave] Auto-save timer reset by manual save or reload command.");
        }
        startAutoSaveTask();
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

        String separator = "&a==================================================";
        ConfigManager.logInfo(separator);
        ConfigManager.logInfo("&6BuffedItems v" + getDescription().getVersion() + " - Startup Summary");
        ConfigManager.logInfo("&a  Total Items: " + items.size());
        ConfigManager.logInfo("&a  Valid Items: " + validItems);

        if (invalidItems > 0) {
            ConfigManager.logInfo("&c  Items with Errors: " + invalidItems);
            ConfigManager.logInfo("&e  Run '/bi list' or '/bi menu' to view details.");
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

        ConfigManager.logInfo("&a  Debug Level: " + debugLevelInfo);
        ConfigManager.logInfo("&a  Auto-save: Every " + (autoSaveIntervalTicks / 20 / 60) + " minutes");
        ConfigManager.logInfo(separator);
    }

    public ItemManager getItemManager() { return itemManager; }
    public EffectManager getEffectManager() { return effectManager; }
    public ActiveAttributeManager getActiveAttributeManager() { return activeAttributeManager; }
    public EffectApplicatorTask getEffectApplicatorTask() { return effectApplicatorTask; }
    public Map<UUID, List<ItemStack>> getDeathKeptItems() {
        return deathKeptItems;
    }
    public long getAutoSaveIntervalTicks() {
        return this.autoSaveIntervalTicks;
    }
    public static void removePlayerMenuUtility(UUID uuid) {
        if(playerMenuUtilityMap.containsKey(uuid)) {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Util] Removing PlayerMenuUtility for " + uuid);
        }
        playerMenuUtilityMap.remove(uuid);
    }
}