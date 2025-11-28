package io.github.altkat.BuffedItems.hooks;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.OfflinePlayer;

public class HookManager {

    private final BuffedItems plugin;

    private ItemsAdderHook itemsAdderHook;
    private NexoHook nexoHook;
    private PlaceholderAPIHook placeholderAPIHook;
    private VaultHook vaultHook;
    private CoinsEngineHook coinsEngineHook;

    public HookManager(BuffedItems plugin) {
        this.plugin = plugin;
        setupHooks();
    }

    private void setupHooks() {
        if (plugin.getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            this.itemsAdderHook = new ItemsAdderHook();
        }

        if (plugin.getServer().getPluginManager().getPlugin("Nexo") != null) {
            this.nexoHook = new NexoHook();
        }

        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderAPIHook = new PlaceholderAPIHook();
        }

        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            this.vaultHook = new VaultHook();
            if (!this.vaultHook.isHooked()) {
                this.vaultHook = null;
            }
        }

        if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") != null) {
            this.coinsEngineHook = new CoinsEngineHook();
        }

    }

    public ItemsAdderHook getItemsAdderHook() {
        return itemsAdderHook;
    }
    public NexoHook getNexoHook() {
        return nexoHook;
    }
    public PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }
    public VaultHook getVaultHook() { return vaultHook; }
    public CoinsEngineHook getCoinsEngineHook() { return coinsEngineHook; }

    public boolean isItemsAdderLoaded() {
        return itemsAdderHook != null;
    }
    public boolean isNexoLoaded() {
        return nexoHook != null;
    }
    public boolean isPlaceholderAPILoaded() {
        return placeholderAPIHook != null;
    }
    public boolean isVaultLoaded() { return vaultHook != null; }
    public boolean isCoinsEngineLoaded() { return coinsEngineHook != null; }

    public String processPlaceholders(OfflinePlayer p, String s) {
        if(s == null) return null;

        if (s.indexOf('%') == -1) {
            return s;
        }

        if (placeholderAPIHook == null) return s;
        return placeholderAPIHook.setPlaceholders(p, s);
    }
}