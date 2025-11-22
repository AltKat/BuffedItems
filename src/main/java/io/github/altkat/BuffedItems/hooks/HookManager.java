package io.github.altkat.BuffedItems.hooks;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.OfflinePlayer;

public class HookManager {

    private final BuffedItems plugin;

    private ItemsAdderHook itemsAdderHook;
    private NexoHook nexoHook;
    private PlaceholderAPIHook placeholderAPIHook;

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

    public boolean isItemsAdderLoaded() {
        return itemsAdderHook != null;
    }

    public boolean isNexoLoaded() {
        return nexoHook != null;
    }

    public boolean isPlaceholderAPILoaded() {
        return placeholderAPIHook != null;
    }

    public String processPlaceholders(OfflinePlayer p, String s) {
        if (placeholderAPIHook == null) return s;
        return placeholderAPIHook.setPlaceholders(p, s);
    }
}