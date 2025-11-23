package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingTrimRecipe;
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

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[DeathListener] Player " + player.getName() + " died. Cleaning up effects and cache.");

        plugin.getEffectManager().clearAllAttributes(player);
        plugin.getEffectApplicatorTask().playerQuit(player);
        plugin.getEffectApplicatorTask().getManagedEffects(player.getUniqueId())
                .forEach(player::removePotionEffect);

        List<ItemStack> keptItems = new ArrayList<>();
        Iterator<ItemStack> iterator = e.getDrops().iterator();

        while (iterator.hasNext()) {
            ItemStack item = iterator.next();

            if (itemHasFlag(item, "LOST_ON_DEATH")) {
                iterator.remove();
                sendProtectionMessage(player, "protection-lost-on-death");
                continue;
            }

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
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[RespawnListener] Player " + e.getPlayer().getName() + " respawned. Scheduling inventory update (1-tick delay).");
            plugin.getEffectApplicatorTask().markPlayerForUpdate(playerUuid);
        }, 1L);
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
            sendProtectionMessage(e.getPlayer(),"protection-prevent-interact");
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
                sendProtectionMessage(e.getPlayer(),"protection-prevent-placement-entity");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (itemHasFlag(e.getItemInHand(), "PREVENT_PLACEMENT")) {
            e.setCancelled(true);
            sendProtectionMessage(e.getPlayer(),"protection-prevent-placement-block");
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent e) {
        if (itemHasFlag(e.getPlayer().getInventory().getItemInMainHand(), "PREVENT_PLACEMENT")) {
            e.setCancelled(true);
            sendProtectionMessage(e.getPlayer(),"protection-prevent-placement");
        }
    }


    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        boolean prevent = itemHasFlag(e.getInventory().getItem(0), "PREVENT_ANVIL_USE") ||
                itemHasFlag(e.getInventory().getItem(1), "PREVENT_ANVIL_USE");

        if (prevent) {
            e.setResult(null);
            for (HumanEntity viewer : e.getViewers()) {
                if (viewer instanceof Player) {
                   sendProtectionMessage(((Player) viewer),"protection-prevent-anvil-use");
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent e) {
        if (itemHasFlag(e.getItem(), "PREVENT_ENCHANT_TABLE")) {
            e.setCancelled(true);
            sendProtectionMessage(e.getEnchanter(),"protection-prevent-enchant-table");
        }
    }

    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent e) {
        ItemStack base = e.getInventory().getItem(0);
        ItemStack addition = e.getInventory().getItem(1);
        ItemStack material = e.getInventory().getItem(2);

        HumanEntity viewer = e.getView().getPlayer();
        if (!(viewer instanceof Player player)) return;

        boolean prevent = itemHasFlag(base, "PREVENT_SMITHING_USE")
                || itemHasFlag(addition, "PREVENT_SMITHING_USE")
                || itemHasFlag(material, "PREVENT_SMITHING_USE");

        boolean isTrimRecipe = (e.getInventory().getRecipe() instanceof SmithingTrimRecipe);

        if (prevent || isTrimRecipe) {
            e.setResult(null);
            sendProtectionMessage(player,"protection-prevent-smithing-use");
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent e) {
        for (ItemStack item : e.getInventory().getMatrix()) {
            if (itemHasFlag(item, "PREVENT_CRAFTING_USE")) {
                e.setCancelled(true);
                if (e.getWhoClicked() instanceof Player) {
                    sendProtectionMessage(((Player) e.getWhoClicked()),"protection-prevent-crafting-use");
                }
                return;
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        if (itemHasFlag(e.getItemDrop().getItemStack(), "PREVENT_DROP")) {
            e.setCancelled(true);
            sendProtectionMessage(e.getPlayer(),"protection-prevent-drop");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClickPreventDrop(InventoryClickEvent e) {
        Inventory clickedInv = e.getClickedInventory();
        ItemStack cursorItem = e.getCursor();
        ItemStack currentItem = e.getCurrentItem();
        InventoryAction action = e.getAction();

        Inventory targetInventory = null;
        ItemStack itemToCheck = null;

        if (clickedInv != null && !(clickedInv.getHolder() instanceof Player) && cursorItem != null && cursorItem.getType() != Material.AIR) {

            if (isContainerInventory(clickedInv)) {
                targetInventory = clickedInv;
                itemToCheck = cursorItem;
            }
        }
        else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY && currentItem != null && currentItem.getType() != Material.AIR) {
            Inventory topInv = e.getView().getTopInventory();
            Inventory bottomInv = e.getView().getBottomInventory();
            Inventory sourceInv = e.getClickedInventory();

            if (sourceInv != null && sourceInv.equals(bottomInv) && isContainerInventory(topInv)) {
                targetInventory = topInv;
                itemToCheck = currentItem;
            }
        }

        if (targetInventory != null && itemToCheck != null && itemHasFlag(itemToCheck, "PREVENT_DROP")) {
            e.setCancelled(true);
            if (e.getWhoClicked() instanceof Player) {
                sendProtectionMessage(((Player) e.getWhoClicked()),"protection-prevent-container-store");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractItemFramePreventDrop(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof ItemFrame) {
            Player player = e.getPlayer();
            ItemStack itemInHand = player.getInventory().getItem(e.getHand());

            if (itemHasFlag(itemInHand, "PREVENT_DROP")) {
                ItemFrame frame = (ItemFrame) e.getRightClicked();
                if (frame.getItem() == null || frame.getItem().getType() == Material.AIR) {
                    e.setCancelled(true);
                    sendProtectionMessage(player,"protection-prevent-itemframe-store");
                }
            }
        }
    }

    private boolean isContainerInventory(Inventory inv) {
        if (inv == null) return false;
        InventoryType type = inv.getType();
        return inv.getHolder() instanceof BlockInventoryHolder && type != InventoryType.ENDER_CHEST;
    }

    private void sendProtectionMessage(Player p, String key) {
        String rawMsg = plugin.getConfig().getString("messages." + key);
        if (rawMsg == null) rawMsg = "&cAction blocked.";
        String parsedMsg = plugin.getHookManager().processPlaceholders(p, rawMsg);
        p.sendMessage(ConfigManager.fromLegacyWithPrefix(parsedMsg));
    }
}