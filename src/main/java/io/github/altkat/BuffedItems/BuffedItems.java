package io.github.altkat.BuffedItems;


import io.github.altkat.BuffedItems.Handlers.UpdateChecker;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public final class BuffedItems extends JavaPlugin {

    @Override
    public void onEnable(){
        new Metrics(this, 27592);

        final int SPIGOT_RESOURCE_ID = 999999;
        new UpdateChecker(this, SPIGOT_RESOURCE_ID).getVersion(newVersion -> {
            if (UpdateChecker.isNewerVersion(this.getDescription().getVersion(), newVersion)) {
                getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §eA new update is available! Version: " + newVersion);
                getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §eDownload it from: https://www.spigotmc.org/resources/buffeditems." + SPIGOT_RESOURCE_ID + "/");
            }else {
                getServer().getConsoleSender().sendMessage("§f[BuffedItems] You are using the latest version. (" + this.getDescription().getVersion() +")");
            }
        });

        getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §aBuffedItems has been enabled!");
    }

    @Override
    public void onDisable(){
        getServer().getConsoleSender().sendMessage("§9[§6BuffedItems§9] §aBuffedItems has been enabled!");
    }


    private void initializeManagers() {

    }

    private void registerListenersAndCommands() {

    }


}
