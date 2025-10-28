package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class LoreEditorMenu extends PaginatedMenu {
    private final BuffedItems plugin;
    private final int maxLinesPerPage = 36;
    private final int MAX_TOTAL_LORE_LINES = 100;

    public LoreEditorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
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
        List<String> lore = new ArrayList<>(item.getLore());

        if (handlePageChange(e, lore.size())) return;

        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                new ItemEditorMenu(playerMenuUtility, plugin).open();
                break;
            case ANVIL:
                if (lore.size() >= MAX_TOTAL_LORE_LINES) {
                    p.sendMessage("§cError: You cannot add more than " + MAX_TOTAL_LORE_LINES + " lines of lore.");
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("lore.add");
                p.closeInventory();
                p.sendMessage("§aPlease type the new lore line in chat. Use '&' for color codes.");
                break;
            case PAPER:
                if (lore.size() >= MAX_TOTAL_LORE_LINES) {
                    p.sendMessage("§cError: You cannot add more than " + MAX_TOTAL_LORE_LINES + " lines of lore.");
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                lore.add("");
                ConfigManager.setItemValue(item.getId(), "lore", lore);
                this.open();
                break;
            case BOOK:
                int slotIndex = e.getSlot();
                int loreIndex = maxLinesPerPage * page + slotIndex;

                if (loreIndex >= lore.size()) return;

                if (e.isLeftClick()) {
                    playerMenuUtility.setWaitingForChatInput(true);
                    playerMenuUtility.setChatInputPath("lore." + loreIndex);
                    p.closeInventory();
                    p.sendMessage("§aPlease type the edited lore line in chat.");
                } else if (e.isRightClick()) {
                    lore.remove(loreIndex);
                    ConfigManager.setItemValue(item.getId(), "lore", lore);
                    this.open();
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {
        addMenuControls();

        BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
        List<String> lore = item.getLore();

        if (item == null) {
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }
        addMenuControls();

        if (!lore.isEmpty()) {
            for (int i = 0; i < maxLinesPerPage; i++) {
                int index = maxLinesPerPage * page + i;
                if (index >= lore.size()) break;

                String line = lore.get(index);
                String displayName = line.isEmpty() ? "§7(Empty Line)" : "§e" + line;
                inventory.setItem(i, makeItem(Material.BOOK, displayName,
                        "§aLeft-Click to Edit", "§cRight-Click to Delete", "§8(Line " + (index + 1) + ")"));
            }
        }
    }
    @Override
    public void addMenuControls() {

        inventory.setItem(45, makeItem(Material.ARROW, "§aPrevious Page"));
        inventory.setItem(53, makeItem(Material.ARROW, "§aNext Page"));

        inventory.setItem(48, makeItem(Material.PAPER, "§bAdd Blank Line", "§7Click to add an empty line."));
        inventory.setItem(49, makeItem(Material.ANVIL, "§bAdd New Line (Chat)", "§7Click to add a new line of text."));


        inventory.setItem(52, makeItem(Material.BARRIER, "§cBack"));
    }
}