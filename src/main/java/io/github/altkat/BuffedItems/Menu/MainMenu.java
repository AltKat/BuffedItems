package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.github.altkat.BuffedItems.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainMenu extends PaginatedMenu {

    private final BuffedItems plugin;

    private int maxItemsPerPage = 27;

    public MainMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.maxItemsPerPage = 27;
    }

    @Override
    public String getMenuName() {
        return "BuffedItems > Main Menu";
    }

    @Override
    public int getSlots() {

        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        List<BuffedItem> items = new ArrayList<>(plugin.getItemManager().getLoadedItems().values());
        ItemStack clickedItem = e.getCurrentItem();

        if (clickedItem == null) return;

        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "buffeditem_id"), PersistentDataType.STRING)) {
            String itemId = clickedItem.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "buffeditem_id"), PersistentDataType.STRING);
            playerMenuUtility.setItemToEditId(itemId);

            if (e.isLeftClick()) {
                new ItemEditorMenu(playerMenuUtility, plugin).open();
            } else if (e.isRightClick()) {
                new ConfirmationMenu(playerMenuUtility, plugin, itemId).open();
            }
            return;
        }

        if (handlePageChange(e, items.size())) return;

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                p.closeInventory();
                break;
            case ANVIL:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("createnewitem");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSection( "§aPlease type the unique ID for the new item in chat (e.g., 'fire_sword')."));
                break;
            case EMERALD:
                if (e.getSlot() == 52) {
                    try {
                        ConfigManager.backupConfig();
                        plugin.saveConfig();
                        plugin.restartAutoSaveTask();
                        p.sendMessage( ConfigManager.fromSection("§aBuffedItems configuration has been saved successfully!"));
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                    } catch (Exception ex) {
                        p.sendMessage( ConfigManager.fromSection("§cAn error occurred while saving the config. Check the console."));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        plugin.getLogger().severe("Failed to manually save config: " + ex.getMessage());
                    }
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
        }

        for (int i = 36; i < 45; i++) {
            inventory.setItem(i, filler);
        }

        addMenuControls();
        inventory.setItem(49, makeItem(Material.ANVIL, "§bCreate New Item", "§7Click to create a brand new item."));
        long autoSaveMinutes = plugin.getAutoSaveIntervalTicks() / 20 / 60;

        inventory.setItem(52, makeItem(Material.EMERALD, "§aManual Save",
                "§7The plugin auto-saves every " + autoSaveMinutes + " minutes.",
                "§7Click here to save changes to config.yml",
                "§7immediately and reset the auto-save timer."));

        inventory.setItem(53, makeItem(Material.BARRIER, "§cClose Menu"));

        inventory.setItem(45, filler);
        inventory.setItem(46, filler);
        inventory.setItem(47, filler);
        inventory.setItem(51, filler);

        List<BuffedItem> items = new ArrayList<>(plugin.getItemManager().getLoadedItems().values());

        items.sort(Comparator.comparing(BuffedItem::getId));


        if (!items.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                int index = maxItemsPerPage * page + i;
                if (index >= items.size()) break;

                BuffedItem currentItem = items.get(index);
                ItemStack itemStack;

                if (currentItem.isValid()) {
                    itemStack = new ItemBuilder(currentItem, plugin).build();
                    ItemMeta meta = itemStack.getItemMeta();
                    List<String> newLore = (meta.getLore() != null) ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    newLore.add("");
                    newLore.add("§eLeft-Click to Edit");
                    newLore.add("§cRight-Click to Delete");
                    meta.setLore(newLore);
                    itemStack.setItemMeta(meta);
                } else {
                    itemStack = new ItemStack(Material.BARRIER);
                    ItemMeta meta = itemStack.getItemMeta();
                    meta.setDisplayName("§c§lERROR: " + currentItem.getId());
                    List<String> errorLore = new ArrayList<>();
                    errorLore.add("§7This item has configuration errors.");
                    errorLore.add("");
                    errorLore.addAll(currentItem.getErrorMessages());
                    errorLore.add("");
                    errorLore.add("§eLeft-Click to Edit and fix the errors.");
                    meta.setLore(errorLore);

                    NamespacedKey key = new NamespacedKey(plugin, "buffeditem_id");
                    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, currentItem.getId());
                    itemStack.setItemMeta(meta);
                }
                inventory.setItem(i + 9, itemStack);
            }
        }
    }

    @Override
    public void addMenuControls() {
        super.addMenuControls();
        inventory.setItem(49, makeItem(Material.ANVIL, "§bCreate New Item", "§7Click to create a brand new item."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cClose Menu"));
    }
}