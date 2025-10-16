package io.github.altkat.BuffedItems;

import io.github.altkat.BuffedItems.Commands.Commands;
import io.github.altkat.BuffedItems.Commands.TabCompleterHandler;
import io.github.altkat.BuffedItems.Handlers.ConfigUpdater;
import io.github.altkat.BuffedItems.Handlers.UpdateChecker;
import io.github.altkat.BuffedItems.Listeners.MenuListener;
import io.github.altkat.BuffedItems.Listeners.PlayerQuitListener;
import io.github.altkat.BuffedItems.Managers.ActiveAttributeManager;
import io.github.altkat.BuffedItems.Managers.EffectManager;
import io.github.altkat.BuffedItems.Managers.ItemManager;
import io.github.altkat.BuffedItems.Menu.PlayerMenuUtility;
import io.github.altkat.BuffedItems.Tasks.EffectApplicatorTask;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;

public final class BuffedItems extends JavaPlugin {

    private ItemManager itemManager;
    private EffectManager effectManager;
    private EffectApplicatorTask effectApplicatorTask;
    private ActiveAttributeManager activeAttributeManager;
    private static final HashMap<Player, PlayerMenuUtility> playerMenuUtilityMap = new HashMap<>();

    @Override
    public void onEnable() {
        new Metrics(this, 27592);

        saveDefaultConfig();
        try {
            ConfigUpdater.update(this);
        } catch (IOException e) {
            getLogger().severe("Could not update config.yml!");
            e.printStackTrace();
        }
        reloadConfig();

        initializeManagers();
        registerListenersAndCommands();

        startEffectTask();

        final int SPIGOT_RESOURCE_ID = 999999;
        new UpdateChecker(this, SPIGOT_RESOURCE_ID).getVersion(newVersion -> {
            if (UpdateChecker.isNewerVersion(this.getDescription().getVersion(), newVersion)) {
                getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §eA new update is available! Version: " + newVersion);
                getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §eDownload it from: https://www.spigotmc.org/resources/buffeditems." + SPIGOT_RESOURCE_ID + "/");
            } else {
                getServer().getConsoleSender().sendMessage("§f[BuffedItems] You are using the latest version. (" + this.getDescription().getVersion() + ")");
            }
        });

        getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §aBuffedItems has been enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §cBuffedItems has been disabled!");
    }

    private void initializeManagers() {
        itemManager = new ItemManager(this);
        effectManager = new EffectManager(this);
        itemManager.loadItems();
        activeAttributeManager = new ActiveAttributeManager();

    }

    private void registerListenersAndCommands() {
        this.getCommand("buffeditems").setExecutor(new Commands(this));
        this.getCommand("buffeditems").setTabCompleter(new TabCompleterHandler(this));
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
    }

    private void startEffectTask() {
        effectApplicatorTask = new EffectApplicatorTask(this);
        effectApplicatorTask.runTaskTimer(this, 0L, 20L);
        getLogger().info("Effect applicator task has been started.");
    }

    public static PlayerMenuUtility getPlayerMenuUtility(Player p) {
        PlayerMenuUtility playerMenuUtility;
        if (!(playerMenuUtilityMap.containsKey(p))) {
            playerMenuUtility = new PlayerMenuUtility(p);
            playerMenuUtilityMap.put(p, playerMenuUtility);
            return playerMenuUtility;
        } else {
            return playerMenuUtilityMap.get(p);
        }
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public EffectApplicatorTask getEffectApplicatorTask() {
        return effectApplicatorTask;
    }

    public ActiveAttributeManager getActiveAttributeManager() {
        return activeAttributeManager;
    }


}