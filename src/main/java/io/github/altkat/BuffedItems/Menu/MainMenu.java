package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
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

public class MainMenu extends Menu {

    private final BuffedItems plugin;
    private int page = 0;

    private final int maxItemsPerPage = 28;

    public MainMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
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


        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                p.closeInventory();
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
            case ANVIL:

                p.sendMessage("§aYeni eşya oluşturma menüsü yakında eklenecek!");
                break;
            case LAVA_BUCKET:

                p.sendMessage("§cEşya silme menüsü yakında eklenecek!");
                break;
            default:

                if (e.getCurrentItem().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "buffeditem_id"), PersistentDataType.STRING)) {
                    String itemId = e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "buffeditem_id"), PersistentDataType.STRING);
                    playerMenuUtility.setItemToEditId(itemId);

                    p.sendMessage("§eDüzenleme menüsü yakında eklenecek! Düzenlenecek Eşya: " + itemId);
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {

        addMenuControls();

        List<BuffedItem> items = new ArrayList<>(plugin.getItemManager().getLoadedItems().values());


        if (!items.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                int index = maxItemsPerPage * page + i;
                if (index >= items.size()) break;

                BuffedItem currentItem = items.get(index);
                ItemStack itemStack = new ItemBuilder(currentItem, plugin).build();


                ItemMeta meta = itemStack.getItemMeta();
                List<String> newLore = new ArrayList<>(meta.getLore());
                newLore.add("");
                newLore.add("§eLeft-Click to Edit");
                newLore.add("§cRight-Click to Delete");
                meta.setLore(newLore);
                itemStack.setItemMeta(meta);

                inventory.addItem(itemStack);
            }
        }
    }

    private void addMenuControls() {

        ItemStack left = makeItem(Material.ARROW, "§aPrevious Page");
        ItemStack right = makeItem(Material.ARROW, "§aNext Page");
        inventory.setItem(48, left);
        inventory.setItem(50, right);


        ItemStack createItem = makeItem(Material.ANVIL, "§bCreate New Item", "§7Click to create a brand new", "§7item from scratch.");
        inventory.setItem(49, createItem);


        inventory.setItem(53, makeItem(Material.BARRIER, "§cClose Menu"));
    }
}