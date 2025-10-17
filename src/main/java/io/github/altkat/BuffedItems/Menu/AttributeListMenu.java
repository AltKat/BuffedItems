package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
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

        Player p = (Player) e.getWhoClicked();
        String itemId = playerMenuUtility.getItemToEditId();
        String targetSlot = playerMenuUtility.getTargetSlot();
        Material clickedType = e.getCurrentItem().getType();
        int clickedSlot = e.getSlot();

        if (clickedType == Material.ANVIL && clickedSlot == 49) {
            new AttributeSelectorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (clickedType == Material.BARRIER && e.getSlot() == getSlots() - 1) {
            new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.ATTRIBUTE).open();
            return;
        }

        if (clickedSlot < 45) {
            String configPath = "items." + itemId + ".effects." + targetSlot + ".attributes";
            List<String> attributes = plugin.getConfig().getStringList(configPath);

            if (clickedSlot >= attributes.size()) return;

            if (e.isRightClick()) {
                String removedInfo = attributes.get(clickedSlot);
                attributes.remove(clickedSlot);
                ConfigManager.setItemValue(itemId, "effects." + targetSlot + ".attributes", attributes);
                p.sendMessage("§aEntry removed: §e" + removedInfo);
                this.open();
            }
            else if (e.isLeftClick() && clickedType == Material.IRON_SWORD) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setEditIndex(clickedSlot);
                playerMenuUtility.setChatInputPath("attributes.edit");
                p.closeInventory();
                p.sendMessage("§aPlease type the new amount for the attribute in chat.");
            }
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

                if (parts.length == 3) {
                    inventory.setItem(i, makeItem(Material.IRON_SWORD, "§b" + parts[0],
                            "§7Operation: §e" + parts[1], "§7Amount: §e" + parts[2], "", "§aLeft-Click to Edit Amount", "§cRight-Click to Delete"));
                } else {
                    inventory.setItem(i, makeItem(Material.BARRIER, "§c§lCORRUPT ENTRY",
                            "§7This line is malformed in config.yml:", "§e" + attrString, "", "§cRight-Click to Delete this entry."));
                }
            }
        }
        setFillerGlass();
    }
}