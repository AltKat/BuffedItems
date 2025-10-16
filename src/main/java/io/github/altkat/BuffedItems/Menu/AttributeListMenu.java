package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class AttributeListMenu extends Menu {
    private final BuffedItems plugin;
    private final String TARGET_SLOT = "INVENTORY";

    public AttributeListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Attributes for Slot: " + TARGET_SLOT;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        String itemId = playerMenuUtility.getItemToEditId();

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                new ItemEditorMenu(playerMenuUtility, plugin).open();
                break;
            case ANVIL:
                new AttributeSelectorMenu(playerMenuUtility, plugin).open();
                break;
            case IRON_SWORD:
                if (e.isRightClick()){
                    String attributeName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

                    List<String> attributes = plugin.getConfig().getStringList("items." + itemId + ".effects." + TARGET_SLOT + ".attributes");
                    attributes.removeIf(s -> s.startsWith(attributeName + ";"));

                    ConfigManager.setItemValue(itemId, "effects." + TARGET_SLOT + ".attributes", attributes);
                    e.getWhoClicked().sendMessage("§aAttribute '" + attributeName + "' has been removed.");
                    this.open();
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd New Attribute", "§7Adds an attribute to the §e" + TARGET_SLOT + " §7slot."));
        addBackButton(new ItemEditorMenu(playerMenuUtility, plugin));

        List<String> attributesConfig = plugin.getConfig().getStringList("items." + playerMenuUtility.getItemToEditId() + ".effects." + TARGET_SLOT + ".attributes");

        if (!attributesConfig.isEmpty()) {
            int slot = 0;
            for (String attrString : attributesConfig) {
                if (slot >= getSlots() - 9) break;
                String[] parts = attrString.split(";");
                inventory.setItem(slot, makeItem(Material.IRON_SWORD, "§b" + parts[0],
                        "§7Operation: §e" + parts[1], "§7Amount: §e" + parts[2], "", "§cRight-Click to Delete"));
                slot++;
            }
        }
        setFillerGlass();
    }
}