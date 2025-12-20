package io.github.altkat.BuffedItems.menu.set;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.MainMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.set.BuffedSet;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SetListMenu extends PaginatedMenu {

    private final BuffedItems plugin;

    public SetListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.maxItemsPerPage = 36;
    }

    @Override
    public String getMenuName() {
        return "Item Sets (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        List<BuffedSet> sets = getSortedSets();

        if (e.getCurrentItem() == null) return;
        if (handlePageChange(e, sets.size())) return;

        if (e.getSlot() == 53) {
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() == 49) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("create_set");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter a unique ID for the new Item Set (e.g. 'paladin_set')."));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            return;
        }

        if (e.getSlot() >= 9 && e.getSlot() < 45) {
            int index = maxItemsPerPage * page + (e.getSlot() - 9);
            if (index >= sets.size()) return;

            BuffedSet set = sets.get(index);

            if (e.getClick() == ClickType.LEFT) {
                playerMenuUtility.setItemToEditId(set.getId());
                new SetEditorMenu(playerMenuUtility, plugin).open();
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
            }
            else if (e.getClick() == ClickType.RIGHT) {
                SetsConfig.get().set("sets." + set.getId(), null);
                SetsConfig.saveAsync();
                plugin.getSetManager().loadSets(true);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cSet '" + set.getId() + "' deleted."));
                p.playSound(p.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1, 1);
                this.open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);

        addMenuControls();

        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack to Main Menu"));
        inventory.setItem(49, makeItem(Material.ANVIL, "§aCreate New Set", "§7Click to create a new item set."));

        List<BuffedSet> sets = getSortedSets();

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= sets.size()) break;

            BuffedSet set = sets.get(index);

            Material iconMat = Material.ARMOR_STAND;
            if (!set.getItemIds().isEmpty()) {
                String firstItemId = set.getItemIds().get(0);
                if (plugin.getItemManager().getBuffedItem(firstItemId) != null) {
                    iconMat = plugin.getItemManager().getBuffedItem(firstItemId).getMaterial();
                }
            }

            List<String> lore = new ArrayList<>();
            lore.add("§7Display: " + ConfigManager.toSection(ConfigManager.fromLegacy(set.getDisplayName())));
            lore.add("");
            lore.add("§7Items: §f" + set.getItemIds().size());
            lore.add("§7Bonuses: §f" + set.getBonuses().size());

            if (!set.isValid()) {
                lore.add("");
                lore.add("§c⚠ CONFIG ERRORS:");
                for (String err : set.getErrorMessages()) {
                    lore.add("§c- " + err);
                }
            }

            lore.add("");
            lore.add("§eLeft-Click to Edit");
            lore.add("§cRight-Click to Delete");

            inventory.setItem(9 + i, makeItem(iconMat, "§6" + set.getId(), lore.toArray(new String[0])));
        }
    }

    private List<BuffedSet> getSortedSets() {
        List<BuffedSet> list = new ArrayList<>(plugin.getSetManager().getSets().values());
        list.sort(Comparator.comparing(BuffedSet::getId));
        return list;
    }
}