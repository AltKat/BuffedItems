package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.active.CostListMenu;
import io.github.altkat.BuffedItems.menu.active.UsageLimitSettingsMenu;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.upgrade.IngredientListMenu;
import io.github.altkat.BuffedItems.menu.upgrade.UpgradeRecipeEditorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuffedItemSelectorMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final SelectionContext context;
    private final List<BuffedItem> items;

    public enum SelectionContext {
        COST,
        INGREDIENT,
        BASE,
        RESULT,
        USAGE_LIMIT
    }

    public BuffedItemSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin, SelectionContext context) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.context = context;
        this.items = new ArrayList<>(plugin.getItemManager().getLoadedItems().values());
    }

    @Override
    public String getMenuName() {
        return "Select Item (" + context.name() + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null) return;
        if (handlePageChange(e, items.size())) return;

        if (e.getSlot() == 49 && e.getCurrentItem().getType() == Material.PAPER) {
            handleManualInput(p);
            return;
        }

        if (e.getSlot() == 53 && e.getCurrentItem().getType() == Material.BARRIER) {
            handleBack();
            return;
        }

        if (e.getSlot() < 45) {
            int index = maxItemsPerPage * page + e.getSlot();
            if (index >= items.size()) return;

            BuffedItem selectedItem = items.get(index);
            handleSelection(p, selectedItem.getId());
        }
    }

    private void handleSelection(Player p, String itemId) {
        switch (context) {
            case BASE:
                ConfigManager.setUpgradeValue(playerMenuUtility.getItemToEditId(), "base", itemId);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aBase item updated to: §e" + itemId));
                new UpgradeRecipeEditorMenu(playerMenuUtility, plugin).open();
                break;

            case RESULT:
                ConfigManager.setUpgradeValue(playerMenuUtility.getItemToEditId(), "result.item", itemId);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aResult item updated to: §e" + itemId));
                new UpgradeRecipeEditorMenu(playerMenuUtility, plugin).open();
                break;

            case INGREDIENT:
                playerMenuUtility.setTempId(itemId);
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("upgrade.ingredients.add.BUFFED_ITEM_QUANTITY");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aSelected Item: §e" + itemId));
                p.sendMessage(ConfigManager.fromSection("§aPlease enter the Amount (Quantity) in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                break;

            case COST:
                playerMenuUtility.setTempId(itemId);
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("active.costs.add.BUFFED_ITEM_QUANTITY");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aSelected Item: §e" + itemId));
                p.sendMessage(ConfigManager.fromSection("§aPlease enter the Amount (Quantity) in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                break;

            case USAGE_LIMIT:
                ConfigManager.setItemValue(playerMenuUtility.getItemToEditId(), "usage-limit.transform-item", itemId);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aTransform Target updated to: §e" + itemId));
                new UsageLimitSettingsMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    private void handleManualInput(Player p) {
        playerMenuUtility.setWaitingForChatInput(true);
        p.closeInventory();

        switch (context) {
            case BASE:
                playerMenuUtility.setChatInputPath("upgrade.base.set_id");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Base Item ID manually."));
                break;

            case RESULT:
                playerMenuUtility.setChatInputPath("upgrade.result.item");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Result Item ID manually."));
                break;

            case INGREDIENT:
                playerMenuUtility.setChatInputPath("upgrade.ingredients.add.BUFFED_ITEM");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Buffed Item ID manually."));
                p.sendMessage(ConfigManager.fromSection("§eFormat: AMOUNT;ITEM_ID"));
                break;

            case COST:
                playerMenuUtility.setChatInputPath("active.costs.add.BUFFED_ITEM");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Buffed Item ID manually."));
                p.sendMessage(ConfigManager.fromSection("§eFormat: AMOUNT;ITEM_ID"));
                break;

            case USAGE_LIMIT:
                playerMenuUtility.setChatInputPath("usage-limit.transform-item");
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Target Buffed Item ID manually."));
                break;
        }
        p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
    }

    private void handleBack() {
        switch (context) {
            case BASE:
            case RESULT:
                new UpgradeRecipeEditorMenu(playerMenuUtility, plugin).open();
                break;

            case INGREDIENT:
                new IngredientListMenu(playerMenuUtility, plugin).open();
                break;

            case COST:
                new CostListMenu(playerMenuUtility, plugin).open();
                break;

            case USAGE_LIMIT:
                new UsageLimitSettingsMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        addMenuControls();

        inventory.setItem(49, makeItem(Material.PAPER, "§eManual Input", "§7Click to type ID in chat manually."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack"));

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= items.size()) break;

            BuffedItem item = items.get(index);
            ItemStack stack = new ItemBuilder(item, plugin).build();

            inventory.setItem(i, stack);
        }
    }
}