package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Menu.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InventoryCloseListener implements Listener {

    private final BuffedItems plugin;

    public InventoryCloseListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu)) {
            return;
        }

        Player p = (Player) e.getPlayer();
        PlayerMenuUtility pmu = BuffedItems.getPlayerMenuUtility(p);

        if (pmu.isNavigating()) {
            pmu.setNavigating(false);
            return;
        }

        if (e.getInventory().getHolder() instanceof ItemEditorMenu ||
                e.getInventory().getHolder() instanceof LoreEditorMenu ||
                e.getInventory().getHolder() instanceof AttributeListMenu ||
                e.getInventory().getHolder() instanceof PotionEffectListMenu ||
                e.getInventory().getHolder() instanceof SlotSelectionMenu ||
                e.getInventory().getHolder() instanceof ItemFlagsMenu)
        {
            if (ConfigManager.isDirty()) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    new SaveChangesConfirmationMenu(pmu, plugin).open();
                }, 1L);
            }
        }
    }
}