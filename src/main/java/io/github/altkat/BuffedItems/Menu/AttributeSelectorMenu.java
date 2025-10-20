package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AttributeSelectorMenu extends PaginatedMenu {
    private final BuffedItems plugin;
    private final List<Attribute> attributes;

    public AttributeSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.attributes = Arrays.stream(Attribute.values()).collect(Collectors.toList());
        this.maxItemsPerPage = 45;
    }

    @Override
    public String getMenuName() {
        return "Select an Attribute (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        int clickedSlot = e.getSlot();
        Material clickedType = e.getCurrentItem().getType();

        if (clickedSlot < this.maxItemsPerPage) {
            String attributeName = e.getCurrentItem().getItemMeta().getDisplayName().substring(2);
            playerMenuUtility.setAttributeToEdit(attributeName);

            new AttributeOperationSelectorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (handlePageChange(e, attributes.size())) {
            return;
        }

        if (clickedType == Material.BARRIER && clickedSlot == 49) {
            new AttributeListMenu(playerMenuUtility, plugin).open();
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(45, makeItem(Material.ARROW, "§aPrevious Page"));
        inventory.setItem(53, makeItem(Material.ARROW, "§aNext Page"));
        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack to Attribute List"));

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= attributes.size()) break;

            Attribute currentAttribute = attributes.get(index);
            if (currentAttribute == null) continue;

            ItemStack itemStack = makeItem(Material.IRON_CHESTPLATE, "§b" + currentAttribute.name(), "§7Click to select this attribute.");
            inventory.setItem(i, itemStack);
        }
    }
}