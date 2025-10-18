package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ItemProtectionListener implements Listener {

    private final BuffedItems plugin;
    private final NamespacedKey nbtKey;

    public ItemProtectionListener(BuffedItems plugin) {
        this.plugin = plugin;
        this.nbtKey = new NamespacedKey(plugin, "buffeditem_id");
    }


    private boolean itemHasFlag(ItemStack item, String flagName) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        String itemId = item.getItemMeta().getPersistentDataContainer().get(nbtKey, PersistentDataType.STRING);
        if (itemId == null) {
            return false;
        }
        BuffedItem buffedItem = plugin.getItemManager().getBuffedItem(itemId);
        if (buffedItem == null) {
            return false;
        }
        return buffedItem.getFlag(flagName);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        List<ItemStack> keptItems = new ArrayList<>();
        Iterator<ItemStack> iterator = e.getDrops().iterator();

        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (itemHasFlag(item, "PREVENT_DEATH_DROP")) {
                iterator.remove();
                keptItems.add(item);
            }
        }

        if (!keptItems.isEmpty()) {
            plugin.getDeathKeptItems().put(e.getEntity().getUniqueId(), keptItems);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        UUID playerUuid = e.getPlayer().getUniqueId();
        Map<UUID, List<ItemStack>> itemsMap = plugin.getDeathKeptItems();

        if (itemsMap.containsKey(playerUuid)) {
            List<ItemStack> keptItems = itemsMap.get(playerUuid);

            HashMap<Integer, ItemStack> didNotFit = e.getPlayer().getInventory().addItem(keptItems.toArray(new ItemStack[0]));

            if (!didNotFit.isEmpty()) {
                for (ItemStack item : didNotFit.values()) {
                    e.getPlayer().getWorld().dropItemNaturally(e.getRespawnLocation(), item);
                }
            }
            itemsMap.remove(playerUuid);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerUse(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null) return;

        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (itemHasFlag(item, "PREVENT_INTERACT")) {

            if (item.getType().isEdible()) return;
            if (item.getType().isBlock()) return;

            e.setCancelled(true);
            e.getPlayer().sendMessage("§cYou cannot use this special item.");
        }
    }

    @EventHandler
    public void onEntityPlace(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getItem() == null) {
            return;
        }

        Material itemType = e.getItem().getType();
        if (itemType == Material.ARMOR_STAND ||
                itemType.name().endsWith("_BOAT") ||
                itemType.name().endsWith("_MINECART")
        )
        {
            if (itemHasFlag(e.getItem(), "PREVENT_PLACEMENT")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (itemHasFlag(e.getItemInHand(), "PREVENT_PLACEMENT")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent e) {
        if (itemHasFlag(e.getPlayer().getInventory().getItemInMainHand(), "PREVENT_PLACEMENT")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cYou cannot place this special item.");
        }
    }


    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        if (itemHasFlag(e.getInventory().getItem(0), "PREVENT_ANVIL_USE") ||
                itemHasFlag(e.getInventory().getItem(1), "PREVENT_ANVIL_USE")) {
            e.setResult(null);
        }
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent e) {
        if (itemHasFlag(e.getItem(), "PREVENT_ENCHANT_TABLE")) {
            e.setCancelled(true);
            e.getEnchanter().sendMessage("§cYou cannot enchant this special item.");
        }
    }

    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent e) {
        if (itemHasFlag(e.getInventory().getItem(0), "PREVENT_SMITHING_USE") ||
                itemHasFlag(e.getInventory().getItem(1), "PREVENT_SMITHING_USE")) {
            e.setResult(null);
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent e) {
        for (ItemStack item : e.getInventory().getMatrix()) {
            if (itemHasFlag(item, "PREVENT_CRAFTING_USE")) {
                e.setCancelled(true);
                if (e.getWhoClicked() instanceof Player) {
                    ((Player) e.getWhoClicked()).sendMessage("§cYou cannot use this special item in crafting.");
                }
                return;
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        if (itemHasFlag(e.getItemDrop().getItemStack(), "PREVENT_DROP")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cYou cannot drop this item.");
        }
    }
}