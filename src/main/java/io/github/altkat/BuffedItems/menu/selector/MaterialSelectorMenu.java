package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.editor.ItemEditorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
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
        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();
        int clickedSlot = e.getSlot();
        Material clickedType = e.getCurrentItem().getType();

        if (clickedSlot < this.maxItemsPerPage) {
            String itemId = playerMenuUtility.getItemToEditId();

            ConfigManager.setItemValue(itemId, "material", clickedType.name());


            p.sendMessage(ConfigManager.fromSection("§aMaterial has been updated to " + clickedType.name()));

            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (handlePageChange(e, materials.size())) {
            return;
        }

        if (clickedType == Material.BARRIER && clickedSlot == 49) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
        }

        if (clickedType == Material.ANVIL && clickedSlot == 48) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("material.manual");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSection("§aPlease type the Material name in chat (e.g., 'STONE', 'diamond_pickaxe')."));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(45, makeItem(Material.ARROW, "§aPrevious Page"));
        inventory.setItem(53, makeItem(Material.ARROW, "§aNext Page"));
        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack to Editor"));
        inventory.setItem(48, makeItem(Material.ANVIL, "§bEnter Manually", "§7Click to type the material name in chat."));

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= materials.size()) break;

            Material currentMaterial = materials.get(index);
            ItemStack itemStack = new ItemStack(currentMaterial);
            inventory.setItem(i, itemStack);
        }
    }
}