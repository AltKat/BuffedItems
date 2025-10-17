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

        playerMenuUtility.setNavigating(true);

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
                return;
            } else {
                playerMenuUtility.setNavigating(false);
            }
        } else {
            playerMenuUtility.setNavigating(false);
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

                boolean isValid = false;
                if (parts.length == 3) {
                    try {
                        org.bukkit.attribute.Attribute.valueOf(parts[0].toUpperCase());
                        org.bukkit.attribute.AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                        Double.parseDouble(parts[2]);
                        isValid = true;
                    } catch (IllegalArgumentException | NullPointerException e) {
                        isValid = false;
                    }
                }

                if (isValid) {
                    inventory.setItem(i, makeItem(Material.IRON_SWORD, "§b" + parts[0],
                            "§7Operation: §e" + parts[1], "§7Amount: §e" + parts[2], "", "§aLeft-Click to Edit Amount", "§cRight-Click to Delete"));
                } else {
                    inventory.setItem(i, makeItem(Material.BARRIER, "§c§lCORRUPT ENTRY",
                            "§7This line is malformed in config.yml:",
                            "§e" + attrString,
                            "",
                            "§cPossible Errors:",
                            "§7- Using ':' instead of the correct ';'.",
                            "§7- Missing a value (e.g., 'ATTRIBUTE;OPERATION').",
                            "§7- A typo in an Attribute or Operation name.",
                            "§7- Amount is not a number (use '.' for decimals, not ',').",
                            "§7- Accidental spaces before/after values.",
                            "",
                            "§aCorrect Format: §eGENERIC_MAX_HEALTH;ADD_NUMBER;4.0",
                            "",
                            "§cRight-Click to Delete this entry."));
                }
            }
        }
        setFillerGlass();
    }
}