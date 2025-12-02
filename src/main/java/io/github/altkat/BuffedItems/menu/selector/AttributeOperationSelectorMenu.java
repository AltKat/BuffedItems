package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class AttributeOperationSelectorMenu extends Menu {
    private final BuffedItems plugin;

    public AttributeOperationSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Select an Operation";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        if (e.getCurrentItem().getType() == Material.BARRIER || e.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) {
            if (e.getCurrentItem().getType() == Material.BARRIER) {
                new AttributeSelectorMenu(playerMenuUtility, plugin).open();
            }
            return;
        }

        Player p = (Player) e.getWhoClicked();
        String operationName = e.getCurrentItem().getItemMeta().getDisplayName().substring(2);
        String attributeName = playerMenuUtility.getAttributeToEdit();

        playerMenuUtility.setWaitingForChatInput(true);

        String targetSlot = playerMenuUtility.getTargetSlot();
        String prefix;

        if ("SET_BONUS".equals(targetSlot)) {
            prefix = "set.attribute.";
        } else if ("ACTIVE".equals(targetSlot)) {
            prefix = "active.attributes.";
        } else {
            prefix = "attributes.";
        }

        playerMenuUtility.setChatInputPath(prefix + "add." + attributeName + "." + operationName);

        p.closeInventory();
        p.sendMessage(ConfigManager.fromSectionWithPrefix("§aPlease type the Amount (e.g., 2.0, -1.5, 0.1) in chat."));
        p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(11, makeItem(Material.GREEN_WOOL, "§aADD_NUMBER", "§7Adds a flat value."));
        inventory.setItem(13, makeItem(Material.YELLOW_WOOL, "§eADD_SCALAR", "§7Adds a percentage of the base value."));
        inventory.setItem(15, makeItem(Material.RED_WOOL, "§cMULTIPLY_SCALAR_1", "§7Multiplies the final value."));

        addBackButton(new AttributeSelectorMenu(playerMenuUtility, plugin));
        setFillerGlass();
    }
}