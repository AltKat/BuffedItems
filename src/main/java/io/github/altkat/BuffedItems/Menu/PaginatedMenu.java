package io.github.altkat.BuffedItems.Menu;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

public abstract class PaginatedMenu extends Menu {

    protected int page = 0;
    protected int maxItemsPerPage = 28;
    protected int index = 0;

    public PaginatedMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    public void addMenuControls() {
        inventory.setItem(48, makeItem(Material.ARROW, "§aPrevious Page"));
        inventory.setItem(50, makeItem(Material.ARROW, "§aNext Page"));
    }

    public boolean handlePageChange(InventoryClickEvent e, int listSize) {
        if (e.getCurrentItem().getType() == Material.ARROW) {

            if (e.getSlot() < this.maxItemsPerPage) {
                return false;
            }

            if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Next")) {
                if (!((page + 1) * maxItemsPerPage >= listSize)) {
                    page++;
                    this.open();
                }
            } else if (e.getCurrentItem().getItemMeta().getDisplayName().contains("Previous")) {
                if (page > 0) {
                    page--;
                    this.open();
                }
            }
            return true;
        }
        return false;
    }
}