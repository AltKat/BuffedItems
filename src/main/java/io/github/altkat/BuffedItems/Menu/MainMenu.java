package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.github.altkat.BuffedItems.utils.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private int page = 0;

    private int maxItemsPerPage = 28;

    public MainMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.maxItemsPerPage = 28;
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
                p.sendMessage("§aPlease type the unique ID for the new item in chat (e.g., 'fire_sword').");
                break;
            case ARROW:
                if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Next")) {

                    if (!((page + 1) * maxItemsPerPage >= items.size())) {
                        page++;
                        super.open();
                    }
                } else if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Back")) {

                    if (page > 0) {
                        page--;
                        super.open();
                    }
                }
                break;
            case LAVA_BUCKET:

                p.sendMessage("§cEşya silme menüsü yakında eklenecek!");
                break;
            default:
                if (e.getCurrentItem().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "buffeditem_id"), PersistentDataType.STRING)) {
                    String itemId = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "buffeditem_id"), PersistentDataType.STRING);
                    playerMenuUtility.setItemToEditId(itemId);

                    if (e.isLeftClick()) {
                        new ItemEditorMenu(playerMenuUtility, plugin).open();
                    } else if (e.isRightClick()) {
                        new ConfirmationMenu(playerMenuUtility, plugin, itemId).open();
                    }
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {

        addMenuControls();
        inventory.setItem(49, makeItem(Material.ANVIL, "§bCreate New Item", "§7Click to create a brand new item."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cClose Menu"));

        List<BuffedItem> items = new ArrayList<>(plugin.getItemManager().getLoadedItems().values());


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
                inventory.addItem(itemStack);
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