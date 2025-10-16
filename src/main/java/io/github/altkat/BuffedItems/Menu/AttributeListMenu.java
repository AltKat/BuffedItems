package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class AttributeListMenu extends Menu {
    private final BuffedItems plugin;

    public AttributeListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Attributes for Slot: " + playerMenuUtility.getTargetSlot();
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        String itemId = playerMenuUtility.getItemToEditId();
        String targetSlot = playerMenuUtility.getTargetSlot();
        Player p = (Player) e.getWhoClicked();

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.ATTRIBUTE).open();
                break;
            case ANVIL:
                new AttributeSelectorMenu(playerMenuUtility, plugin).open();
                break;
            case IRON_SWORD:
                String configPath = "items." + itemId + ".effects." + targetSlot + ".attributes";
                List<String> attributes = plugin.getConfig().getStringList(configPath);
                int clickedSlot = e.getSlot();

                if (clickedSlot >= attributes.size()) return;

                String attributeString = attributes.get(clickedSlot);
                String attributeName = attributeString.split(";")[0];

                if (e.isRightClick()) {
                    attributes.remove(clickedSlot);
                    ConfigManager.setItemValue(itemId, "effects." + targetSlot + ".attributes", attributes);
                    p.sendMessage("§aAttribute '" + attributeName + "' has been removed from slot " + targetSlot + ".");
                    this.open();
                } else if (e.isLeftClick()) {
                    playerMenuUtility.setWaitingForChatInput(true);
                    playerMenuUtility.setEditIndex(clickedSlot);
                    playerMenuUtility.setChatInputPath("attributes.edit");
                    p.closeInventory();
                    p.sendMessage("§aPlease type the new amount for '" + attributeName + "' in chat (e.g., 2.0, -1.5, 0.1).");
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd New Attribute", "§7Adds an attribute to the §e" + playerMenuUtility.getTargetSlot() + " §7slot."));
        addBackButton(new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.ATTRIBUTE));

        String configPath = "items." + playerMenuUtility.getItemToEditId() + ".effects." + playerMenuUtility.getTargetSlot() + ".attributes";
        List<String> attributesConfig = plugin.getConfig().getStringList(configPath);

        if (!attributesConfig.isEmpty()) {
            for (int i = 0; i < attributesConfig.size(); i++) {
                if (i >= getSlots() - 9) break;
                String attrString = attributesConfig.get(i);
                String[] parts = attrString.split(";");
                inventory.setItem(i, makeItem(Material.IRON_SWORD, "§b" + parts[0],
                        "§7Operation: §e" + parts[1], "§7Amount: §e" + parts[2], "", "§aLeft-Click to Edit Amount", "§cRight-Click to Delete"));
            }
        }
        setFillerGlass();
    }
}