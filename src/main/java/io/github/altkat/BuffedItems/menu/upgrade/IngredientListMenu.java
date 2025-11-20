package io.github.altkat.BuffedItems.menu.upgrade;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IngredientListMenu extends Menu {

    private final BuffedItems plugin;
    private final String recipeId;

    public IngredientListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.recipeId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Ingredients: " + recipeId;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        if (e.getSlot() == 49) {
            new UpgradeRecipeEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() == 51) {
            new IngredientTypeSelectorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() < 45 && e.getCurrentItem().getType() != Material.BLACK_STAINED_GLASS_PANE) {
            List<Map<?, ?>> list = UpgradesConfig.get().getMapList("upgrades." + recipeId + ".ingredients");
            if (e.getSlot() >= list.size()) return;

            if (e.getClick() == ClickType.RIGHT) {
                list.remove(e.getSlot());
                ConfigManager.setUpgradeValue(recipeId, "ingredients", list);
                this.open();
            }
            else if (e.getClick() == ClickType.LEFT) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setEditIndex(e.getSlot());
                playerMenuUtility.setChatInputPath("upgrade.ingredients.edit.amount");
                e.getWhoClicked().closeInventory();
                e.getWhoClicked().sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter new amount in chat."));
            }
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack"));
        inventory.setItem(51, makeItem(Material.ANVIL, "§aAdd Ingredient", "§7Add a cost requirement."));

        List<Map<?, ?>> list = UpgradesConfig.get().getMapList("upgrades." + recipeId + ".ingredients");

        int index = 0;
        for (Map<?, ?> map : list) {
            if (index >= 45) break;

            ICost cost = plugin.getCostManager().parseCost(map);
            if (cost != null) {
                String type = (String) map.get("type");
                inventory.setItem(index, makeItem(Material.PAPER,
                        "§e" + type,
                        "§7" + cost.getDisplayString(),
                        "",
                        "§eLeft-Click to Edit Amount",
                        "§cRight-Click to Remove"));
            }
            index++;
        }
    }
}