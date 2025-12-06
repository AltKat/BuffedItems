package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.crafting.IngredientSettingsMenu;
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

        if (handlePageChange(e, materials.size())) {
            return;
        }

        if (clickedType == Material.BARRIER && clickedSlot == 49) {
            handleBack();
            return;
        }

        if (clickedType == Material.ANVIL && clickedSlot == 48) {
            handleManualInput(p);
            return;
        }

        if (clickedSlot < this.maxItemsPerPage) {
            handleMaterialSelection(p, clickedType);
        }
    }

    private void handleMaterialSelection(Player p, Material material) {
        PlayerMenuUtility.MaterialSelectionContext context = playerMenuUtility.getMaterialContext();

        if (context == PlayerMenuUtility.MaterialSelectionContext.ICON) {
            String itemId = playerMenuUtility.getItemToEditId();
            ConfigManager.setItemValue(itemId, "material", material.name());
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aMaterial has been updated to " + material.name()));
            new ItemEditorMenu(playerMenuUtility, plugin).open();
        }
        else if (context == PlayerMenuUtility.MaterialSelectionContext.COST) {
            playerMenuUtility.setTempMaterial(material);
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("active.costs.add.ITEM_QUANTITY");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aSelected: §e" + material.name()));
            p.sendMessage(ConfigManager.fromSection("§aPlease enter the Amount in chat."));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
        }
        else if (context == PlayerMenuUtility.MaterialSelectionContext.INGREDIENT) {
            playerMenuUtility.setTempMaterial(material);
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("upgrade.ingredients.add.ITEM_QUANTITY");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aSelected: §e" + material.name()));
            p.sendMessage(ConfigManager.fromSection("§aPlease enter the Amount in chat."));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
        }
        else if (context == PlayerMenuUtility.MaterialSelectionContext.CRAFTING_INGREDIENT) {
            playerMenuUtility.setTempMaterial(material);
            new IngredientSettingsMenu(playerMenuUtility, plugin, true).open();
        }
    }

    private void handleManualInput(Player p) {
        playerMenuUtility.setWaitingForChatInput(true);
        p.closeInventory();

        PlayerMenuUtility.MaterialSelectionContext context = playerMenuUtility.getMaterialContext();

        if (context == PlayerMenuUtility.MaterialSelectionContext.ICON) {
            playerMenuUtility.setChatInputPath("material.manual");
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aPlease type the Material name in chat (e.g., 'STONE')."));
        }
        else if (context == PlayerMenuUtility.MaterialSelectionContext.COST) {
            playerMenuUtility.setChatInputPath("active.costs.add.ITEM");
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§eFormat: AMOUNT;MATERIAL (e.g. 10;DIAMOND)"));
        }
        else if (context == PlayerMenuUtility.MaterialSelectionContext.INGREDIENT) {
            playerMenuUtility.setChatInputPath("upgrade.ingredients.add.ITEM");
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§eFormat: AMOUNT;MATERIAL (e.g. 10;DIAMOND)"));
        }
        else if (context == PlayerMenuUtility.MaterialSelectionContext.CRAFTING_INGREDIENT) {
            playerMenuUtility.setChatInputPath("recipe_ingredient_material_manual");
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aPlease type the Material name in chat (e.g., 'IRON_INGOT')."));
        }

        p.sendMessage(ConfigManager.fromSectionWithPrefix("§7(Type 'cancel' to exit)"));
    }

    private void handleBack() {
        PlayerMenuUtility.MaterialSelectionContext context = playerMenuUtility.getMaterialContext();
        if (context == PlayerMenuUtility.MaterialSelectionContext.COST) {
            new TypeSelectorMenu(playerMenuUtility, plugin, context).open();
        }
        else if (context == PlayerMenuUtility.MaterialSelectionContext.INGREDIENT) {
            new TypeSelectorMenu(playerMenuUtility, plugin, context).open();
        }
        else if (context == PlayerMenuUtility.MaterialSelectionContext.CRAFTING_INGREDIENT) {
            new IngredientSettingsMenu(playerMenuUtility, plugin, false).open();
        }
        else {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
        }

        if (context != PlayerMenuUtility.MaterialSelectionContext.ICON) {
            playerMenuUtility.setMaterialContext(PlayerMenuUtility.MaterialSelectionContext.ICON);
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