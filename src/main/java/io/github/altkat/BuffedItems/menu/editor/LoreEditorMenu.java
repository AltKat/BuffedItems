package io.github.altkat.BuffedItems.menu.editor;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.ItemListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LoreEditorMenu extends PaginatedMenu {
    private final BuffedItems plugin;
    private final int MAX_TOTAL_LORE_LINES = 100;

    public LoreEditorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.maxItemsPerPage = 36;
    }

    @Override
    public String getMenuName() {
        return "Editing Lore (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());

        if (item == null) {
            new ItemListMenu(playerMenuUtility, plugin).open();
            return;
        }

        List<String> lore = new ArrayList<>(item.getLore());

        if (handlePageChange(e, lore.size())) return;

        if (e.getCurrentItem() == null) return;

        if (e.getSlot() == 53) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() == 49) {
            if (lore.size() >= MAX_TOTAL_LORE_LINES) {
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: You cannot add more than " + MAX_TOTAL_LORE_LINES + " lines of lore."));
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("lore.add");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromLegacyWithPrefix("§aPlease type the new lore line in chat. Use '&' for color codes."));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            return;
        }

        if (e.getSlot() == 51) {
            if (lore.size() >= MAX_TOTAL_LORE_LINES) {
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: You cannot add more than " + MAX_TOTAL_LORE_LINES + " lines of lore."));
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            lore.add("");
            ConfigManager.setItemValue(item.getId(), "lore", lore);
            this.open();
            return;
        }

        if (e.getSlot() >= 9 && e.getSlot() < 45) {
            int slotIndex = e.getSlot() - 9;
            int loreIndex = maxItemsPerPage * page + slotIndex;

            if (loreIndex >= lore.size()) return;

            if (e.isLeftClick()) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("lore." + loreIndex);
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aPlease type the edited lore line in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            } else if (e.isRightClick()) {
                lore.remove(loreIndex);
                ConfigManager.setItemValue(item.getId(), "lore", lore);
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

        inventory.setItem(49, makeItem(Material.ANVIL, "§bAdd New Line (Chat)", "§7Click to add a new line of text via chat."));
        inventory.setItem(51, makeItem(Material.PAPER, "§eAdd Blank Line", "§7Click to insert an empty line."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack"));

        BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
        if (item == null) {
            new ItemListMenu(playerMenuUtility, plugin).open();
            return;
        }

        List<String> lore = item.getLore();

        if (!lore.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                int index = maxItemsPerPage * page + i;
                if (index >= lore.size()) break;

                String line = lore.get(index);
                String formattedLine = ConfigManager.toSection(ConfigManager.fromLegacy(line));
                String displayName = line.isEmpty() ? "§7(Empty Line)" : formattedLine;

                List<String> itemLore = new ArrayList<>();
                if (line.isEmpty()) {
                    itemLore.add("§8(This is a blank line)");
                }
                itemLore.add(" ");
                itemLore.add("§aLeft-Click to Edit");
                itemLore.add("§cRight-Click to Delete");
                itemLore.add("§8(Line " + (index + 1) + ")");

                inventory.setItem(9 + i, makeItem(Material.BOOK, displayName, itemLore.toArray(new String[0])));
            }
        }
    }
}