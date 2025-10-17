package io.github.altkat.BuffedItems;

import io.github.altkat.BuffedItems.Commands.Commands;
import io.github.altkat.BuffedItems.Commands.TabCompleterHandler;
import io.github.altkat.BuffedItems.Handlers.UpdateChecker;
import io.github.altkat.BuffedItems.Listeners.*;
import io.github.altkat.BuffedItems.Managers.*;
import io.github.altkat.BuffedItems.Menu.PlayerMenuUtility;
import io.github.altkat.BuffedItems.Tasks.EffectApplicatorTask;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class BuffedItems extends JavaPlugin {

    private ItemManager itemManager;
    private EffectManager effectManager;
    private ActiveAttributeManager activeAttributeManager;
    private EffectApplicatorTask effectApplicatorTask;
    private static final HashMap<Player, PlayerMenuUtility> playerMenuUtilityMap = new HashMap<>();

    @Override
    public void onEnable() {
        new Metrics(this, 27592);

        saveDefaultConfig();
        getConfig().options().copyHeader(true);

        initializeManagers();
        registerListenersAndCommands();
        startEffectTask();

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
        getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §cBuffedItems has been disabled!");
    }

    private void initializeManagers() {
        ConfigManager.setup(this);
        itemManager = new ItemManager(this);
        effectManager = new EffectManager(this);
        activeAttributeManager = new ActiveAttributeManager();
        itemManager.loadItems(false);
    }

    private void registerListenersAndCommands() {
        getCommand("buffeditems").setExecutor(new Commands(this));
        getCommand("buffeditems").setTabCompleter(new TabCompleterHandler(this));

        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemConsumeListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryCloseListener(this), this);
    }

    private void startEffectTask() {
        effectApplicatorTask = new EffectApplicatorTask(this);
        effectApplicatorTask.runTaskTimer(this, 0L, 20L);
    }

    public static PlayerMenuUtility getPlayerMenuUtility(Player p) {
        return playerMenuUtilityMap.computeIfAbsent(p, PlayerMenuUtility::new);
    }

    public ItemManager getItemManager() { return itemManager; }
    public EffectManager getEffectManager() { return effectManager; }
    public ActiveAttributeManager getActiveAttributeManager() { return activeAttributeManager; }
    public EffectApplicatorTask getEffectApplicatorTask() { return effectApplicatorTask; }
}