package io.github.altkat.BuffedItems;

import io.github.altkat.BuffedItems.Commands.Commands;
import io.github.altkat.BuffedItems.Commands.TabCompleterHandler;
import io.github.altkat.BuffedItems.Handlers.UpdateChecker;
import io.github.altkat.BuffedItems.Listeners.*;
import io.github.altkat.BuffedItems.Managers.ActiveAttributeManager;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Managers.EffectManager;
import io.github.altkat.BuffedItems.Managers.ItemManager;
import io.github.altkat.BuffedItems.Menu.PlayerMenuUtility;
import io.github.altkat.BuffedItems.Tasks.EffectApplicatorTask;
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

        initializeManagers();
        registerListenersAndCommands();
        startEffectTask();
        startAutoSaveTask();

        final int SPIGOT_RESOURCE_ID = 129550;
        new UpdateChecker(this, SPIGOT_RESOURCE_ID).getVersion(newVersion -> {
            if (UpdateChecker.isNewerVersion(this.getDescription().getVersion(), newVersion)) {
                getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §eA new update is available! Version: " + newVersion);
                getLogger().warning("Download it from: https://www.spigotmc.org/resources/buffeditems.129550/");
            } else {
                getLogger().info("You are using the latest version. (" + this.getDescription().getVersion() + ")");
            }
        });

        getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §aBuffedItems has been enabled!");
    }

    @Override
    public void onDisable() {
        ConfigManager.sendDebugMessage(() -> "[Shutdown] Cleaning up all online players...");

        int playerCount = Bukkit.getOnlinePlayers().size();
        int successCount = 0;
        int failCount = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                ConfigManager.sendDebugMessage(() -> "[Shutdown] Clearing effects for: " + player.getName());
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

        ConfigManager.sendDebugMessage(() -> "[Shutdown] Saving final config...");
        saveConfig();

        getLogger().info("Cleanup complete: " + successCount + "/" + playerCount + " players cleaned" + (failCount > 0 ? " (" + failCount + " failed)" : ""));
        getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §cBuffedItems has been disabled!");
    }

    private void initializeManagers() {
        ConfigManager.setup(this);
        ConfigManager.updateDebugMode();
        this.autoSaveIntervalTicks = getConfig().getLong("auto-save-interval-minutes", 5) * 20 * 60;
        ConfigManager.sendDebugMessage(() -> "[Config] Auto-save interval set to " + (this.autoSaveIntervalTicks / 20 / 60) + " minutes (" + this.autoSaveIntervalTicks + " ticks).");
        itemManager = new ItemManager(this);
        effectManager = new EffectManager(this);
        activeAttributeManager = new ActiveAttributeManager();
        itemManager.loadItems(false);
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

    public static PlayerMenuUtility getPlayerMenuUtility(Player p) {
        PlayerMenuUtility playerMenuUtility;
        UUID playerUUID = p.getUniqueId();

        if (playerMenuUtilityMap.containsKey(playerUUID)) {
            return playerMenuUtilityMap.get(playerUUID);
        } else {
            playerMenuUtility = new PlayerMenuUtility(p);
            playerMenuUtilityMap.put(playerUUID, playerMenuUtility);
            return playerMenuUtility;
        }
    }

    private void startAutoSaveTask() {
        this.autoSaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                ConfigManager.sendDebugMessage(() -> "[AutoSave] Saving configuration to disk...");
                saveConfig();
                ConfigManager.sendDebugMessage(() -> "[AutoSave] Configuration saved.");
            }
        }.runTaskTimerAsynchronously(this, this.autoSaveIntervalTicks, this.autoSaveIntervalTicks);
    }

    private void updateAutoSaveIntervalVariable() {
        this.autoSaveIntervalTicks = getConfig().getLong("auto-save-interval-minutes", 5) * 20 * 60;
        ConfigManager.sendDebugMessage(() -> "[Config] Auto-save interval set to " + (this.autoSaveIntervalTicks / 20 / 60) + " minutes (" + this.autoSaveIntervalTicks + " ticks).");
    }

    public void reloadConfigSettings() {
        updateAutoSaveIntervalVariable();
        restartAutoSaveTask();
    }

    public void restartAutoSaveTask() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            ConfigManager.sendDebugMessage(() -> "[AutoSave] Auto-save timer reset by manual save or reload command.");
        }
        startAutoSaveTask();
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
        playerMenuUtilityMap.remove(uuid);
    }
}