package io.github.altkat.BuffedItems.menu.set;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.selector.BuffedItemSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SetItemsMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final String setId;

    public SetItemsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.setId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Set Items: " + setId;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        List<String> items = SetsConfig.get().getStringList("sets." + setId + ".items");
        if (handlePageChange(e, items.size())) return;

        if (e.getSlot() == 53) {
            new SetEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() == 49) {
            new BuffedItemSelectorMenu(playerMenuUtility, plugin, BuffedItemSelectorMenu.SelectionContext.SET_MEMBER).open();
            return;
        }

        if (e.getSlot() < 45) {
            int index = maxItemsPerPage * page + e.getSlot();
            if (index >= items.size()) return;

            if (e.getClick() == ClickType.RIGHT) {
                String removedId = items.remove(index);
                SetsConfig.get().set("sets." + setId + ".items", items);
                SetsConfig.save();
                plugin.getSetManager().loadSets(true);
                e.getWhoClicked().sendMessage(ConfigManager.fromSectionWithPrefix("§cRemoved " + removedId + " from set."));
                this.open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuControls();
        setFillerGlass();

        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack"));
        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd Item", "§7Add a BuffedItem to this set."));

        List<String> items = SetsConfig.get().getStringList("sets." + setId + ".items");

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= items.size()) break;

            String itemId = items.get(index);
            BuffedItem bItem = plugin.getItemManager().getBuffedItem(itemId);

            ItemStack icon;
            if (bItem != null) {
                icon = new ItemBuilder(bItem, plugin).build();
            } else {
                icon = makeItem(Material.BEDROCK, "§c" + itemId, "§7Item not found!");
            }

            ItemMeta meta = icon.getItemMeta();
            List<net.kyori.adventure.text.Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(ConfigManager.fromSection("§7ID: §f" + itemId));
            lore.add(Component.empty());
            lore.add(ConfigManager.fromSection("§cRight-Click to Remove"));
            meta.lore(lore);
            icon.setItemMeta(meta);

            inventory.setItem(i, icon);
        }
    }
}