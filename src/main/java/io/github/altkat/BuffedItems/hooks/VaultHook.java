package io.github.altkat.BuffedItems.hooks;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private Economy economy;

    public VaultHook() {
        if (setupEconomy()) {
            ConfigManager.logInfo("&aVault hooked successfully");
        } else {
            ConfigManager.logInfo("&cVault found but failed to hook Economy provider.");
        }
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean has(Player player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    public void withdraw(Player player, double amount) {
        if (economy == null) return;
        economy.withdrawPlayer(player, amount);
    }

    public boolean isHooked() {
        return economy != null;
    }
}