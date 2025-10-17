package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
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

        playerMenuUtility.setNavigating(true);

        if (e.getCurrentItem().getType() == Material.BARRIER || e.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) {
            if (e.getCurrentItem().getType() == Material.BARRIER) {
                new AttributeSelectorMenu(playerMenuUtility, plugin).open();
            }
            return;
        }

        Player p = (Player) e.getWhoClicked();
        String operationName = e.getCurrentItem().getItemMeta().getDisplayName().substring(2);
        String attributeName = playerMenuUtility.getAttributeToEdit();


        playerMenuUtility.setWaitingForChatInput(true);

        playerMenuUtility.setChatInputPath("attributes.add." + attributeName + "." + operationName);
        p.closeInventory();
        p.sendMessage("§aPlease type the Amount (e.g., 2.0, -1.5, 0.1) in chat.");
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