package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.CostTypeSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CostListMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public CostListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Usage Costs: " + itemId;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem().getType() == Material.BARRIER && e.getSlot() == 49) {
            new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getCurrentItem().getType() == Material.ANVIL && e.getSlot() == 51) {
            new CostTypeSelectorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() < 45 && e.getCurrentItem().getType() != Material.BLACK_STAINED_GLASS_PANE) {
            List<Map<?, ?>> costList = ItemsConfig.get().getMapList("items." + itemId + ".active-mode.costs");
            if (e.getSlot() >= costList.size()) return;

            // 1. DELETE (Right Click)
            if (e.getClick() == ClickType.RIGHT) {
                costList.remove(e.getSlot());
                ConfigManager.setItemValue(itemId, "costs", costList);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cCost removed."));
                this.open();
            }
            // 2. EDIT AMOUNT (Left Click)
            else if (e.getClick() == ClickType.LEFT) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setEditIndex(e.getSlot());
                playerMenuUtility.setChatInputPath("active.costs.edit.amount");
                p.closeInventory();

                Map<?, ?> costData = costList.get(e.getSlot());
                String type = (String) costData.get("type");

                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEditing Amount for: §e" + type));
                if ("ITEM".equals(type)) {
                    p.sendMessage(ConfigManager.fromSection("§eCurrent: " + costData.get("amount")));
                    p.sendMessage(ConfigManager.fromSection("§aEnter new integer amount in chat."));
                } else {
                    p.sendMessage(ConfigManager.fromSection("§eCurrent: " + costData.get("amount")));
                    p.sendMessage(ConfigManager.fromSection("§aEnter new amount in chat."));
                }
            }
            // 3. EDIT MESSAGE (Shift + Left Click)
            else if (e.getClick() == ClickType.SHIFT_LEFT) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setEditIndex(e.getSlot());
                playerMenuUtility.setChatInputPath("active.costs.edit.message");
                p.closeInventory();

                Map<?, ?> costData = costList.get(e.getSlot());
                String type = (String) costData.get("type");
                String placeholders = "{amount}";

                if ("ITEM".equals(type)) {
                    placeholders = "{amount}, {material}";
                }
                else if ("BUFFED_ITEM".equals(type)) {
                    placeholders = "{amount}, {item_name}";
                }
                else if ("COINSENGINE".equals(type)) {
                    placeholders = "{amount}, {currency_name}";
                }

                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEditing Failure Message."));
                p.sendMessage(ConfigManager.fromSection("§7Placeholders: " + placeholders));
                p.sendMessage(ConfigManager.fromSection("§7Type 'default' to reset to config default, 'cancel' to exit."));
                p.sendMessage(ConfigManager.fromSection("§aEnter new message in chat."));
            }
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack"));
        inventory.setItem(51, makeItem(Material.ANVIL, "§aAdd New Cost", "§7Add a requirement to use this item."));

        List<Map<?, ?>> costList = ItemsConfig.get().getMapList("items." + itemId + ".active-mode.costs");
        int index = 0;

        for (Map<?, ?> costData : costList) {
            if (index >= 45) break;

            String type = (String) costData.get("type");
            Object amount = costData.get("amount");
            String msg = (String) costData.get("message");
            boolean isDefault = false;

            if (msg == null) {
                msg = ConfigManager.getDefaultCostMessage(type);
                isDefault = true;
            }

            Material icon = getIconForType(type);

            List<String> lore = new ArrayList<>();
            lore.add("§7Type: §f" + type);
            lore.add("§7Amount: §e" + amount);
            if (costData.containsKey("material")) {
                lore.add("§7Item: §b" + costData.get("material"));
            }

            if (costData.containsKey("item_id")) {
                String refId = (String) costData.get("item_id");
                BuffedItem bItem = plugin.getItemManager().getBuffedItem(refId);

                String displayName;
                if (bItem != null) {
                    displayName = ConfigManager.toSection(ConfigManager.fromLegacy(bItem.getDisplayName()));
                } else {
                    displayName = "§c" + refId + " (No BuffedItem found with this id!)";
                }

                lore.add("§7Item: §r" + displayName);
            }

            if (costData.containsKey("currency_id")) {
                lore.add("§7Currency: §e" + costData.get("currency_id"));
            }

            lore.add("");
            lore.add("§7Message:");
            String formattedMsg = ConfigManager.toSection(ConfigManager.fromLegacy(msg));
            if (isDefault) {
                lore.add("§r" + formattedMsg);
                lore.add("§8(Default Config)");
            } else {
                lore.add("§r" + formattedMsg);
            }
            lore.add("");
            lore.add("§eLeft-Click to Edit Amount");
            lore.add("§bShift+Left-Click to Edit Message");
            lore.add("§cRight-Click to Remove");

            inventory.setItem(index, makeItem(icon, "§6Cost #" + (index + 1), lore.toArray(new String[0])));
            index++;
        }
    }

    private Material getIconForType(String type) {
        if (type == null) return Material.PAPER;
        switch (type.toUpperCase()) {
            case "MONEY": return Material.GOLD_INGOT;
            case "EXPERIENCE": return Material.EXPERIENCE_BOTTLE;
            case "LEVEL": return Material.ENCHANTING_TABLE;
            case "HUNGER": return Material.COOKED_BEEF;
            case "HEALTH": return Material.RED_DYE;
            case "ITEM": return Material.CHEST;
            case "BUFFED_ITEM": return Material.NETHER_STAR;
            case "COINSENGINE": return Material.SUNFLOWER;
            default: return Material.PAPER;
        }
    }
}