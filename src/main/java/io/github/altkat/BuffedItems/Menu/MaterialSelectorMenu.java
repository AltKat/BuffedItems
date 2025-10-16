package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MaterialSelectorMenu extends PaginatedMenu {
    private final BuffedItems plugin;
    private final List<Material> materials;

    public MaterialSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.materials = Arrays.stream(Material.values())
                .filter(Material::isItem)
                .collect(Collectors.toList());
        this.maxItemsPerPage = 45;
    }

    @Override
    public String getMenuName() {
        return "Select a Material (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (handlePageChange(e, materials.size())) {
            return;
        }

        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        Material selectedMaterial = e.getCurrentItem().getType();
        String itemId = playerMenuUtility.getItemToEditId();

        ConfigManager.setItemValue(itemId, "material", selectedMaterial.name());

        Player p = (Player) e.getWhoClicked();
        p.sendMessage("§aMaterial has been updated to " + selectedMaterial.name());

        new ItemEditorMenu(playerMenuUtility, plugin).open();
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(45, makeItem(Material.ARROW, "§aPrevious Page"));
        inventory.setItem(53, makeItem(Material.ARROW, "§aNext Page"));
        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack to Editor"));

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= materials.size()) break;

            Material currentMaterial = materials.get(index);
            ItemStack itemStack = new ItemStack(currentMaterial);
            inventory.setItem(i, itemStack);
        }
    }
}