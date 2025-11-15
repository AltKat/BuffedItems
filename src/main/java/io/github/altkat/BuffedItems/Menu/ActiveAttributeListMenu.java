package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Managers.ItemsConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class ActiveAttributeListMenu extends Menu {
    private final BuffedItems plugin;

    public ActiveAttributeListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Active Attributes";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        String itemId = playerMenuUtility.getItemToEditId();

        if (e.getCurrentItem() == null) return;
        Material clickedType = e.getCurrentItem().getType();
        int clickedSlot = e.getSlot();

        if (clickedType == Material.BARRIER && clickedSlot == 53) {
            new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (clickedType == Material.ANVIL && clickedSlot == 49) {
            playerMenuUtility.setTargetSlot("ACTIVE");
            new AttributeSelectorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (clickedSlot < 45) {
            String configPath = "items." + itemId + ".active_effects.attributes";
            List<String> attributes = ItemsConfig.get().getStringList(configPath);

            if (clickedSlot >= attributes.size()) return;

            if (e.isRightClick()) {
                String removedInfo = attributes.get(clickedSlot);
                attributes.remove(clickedSlot);
                ConfigManager.setItemValue(itemId, "active_effects.attributes", attributes);
                p.sendMessage(ConfigManager.fromSection("§aRemoved: §e" + removedInfo));
                this.open();
            }
            else if (e.isLeftClick() && clickedType == Material.IRON_SWORD) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setEditIndex(clickedSlot);
                playerMenuUtility.setChatInputPath("active.attributes.edit");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSection("§aType new amount for active attribute."));
            }
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd Active Attribute", "§7Adds an attribute to the active list."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack"));

        String configPath = "items." + playerMenuUtility.getItemToEditId() + ".active_effects.attributes";
        List<String> attributesConfig = ItemsConfig.get().getStringList(configPath);

        if (!attributesConfig.isEmpty()) {
            for (int i = 0; i < attributesConfig.size(); i++) {
                if (i >= 45) break;
                String attrString = attributesConfig.get(i);
                String[] parts = attrString.split(";");

                if (parts.length == 3) {
                    inventory.setItem(i, makeItem(Material.IRON_SWORD, "§b" + parts[0],
                            "§7Op: §e" + parts[1], "§7Amount: §e" + parts[2], "", "§aLeft-Click to Edit", "§cRight-Click to Delete"));
                } else {
                    inventory.setItem(i, makeItem(Material.BARRIER, "§cCorrupt Entry", "§7" + attrString));
                }
            }
        }
    }
}