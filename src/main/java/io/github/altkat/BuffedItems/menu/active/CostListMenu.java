package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.TypeSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import su.nightexpress.coinsengine.api.currency.Currency;

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
            new TypeSelectorMenu(playerMenuUtility, plugin, PlayerMenuUtility.MaterialSelectionContext.COST).open();
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
                p.sendMessage(ConfigManager.fromSection("§eCurrent: " + costData.get("amount")));
                p.sendMessage(ConfigManager.fromSection("§aEnter new amount in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
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
                p.sendMessage(ConfigManager.fromSection("§7Type 'default' to reset to config default."));
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
            inventory.setItem(index, createVisualItem(costData, index));
            index++;
        }
    }

    private ItemStack createVisualItem(Map<?, ?> map, int index) {
        String type = (String) map.get("type");
        Object amountObj = map.get("amount");
        String amountStr = String.valueOf(amountObj);
        String message = (String) map.get("message");

        boolean isError = false;
        String errorDetail = "";

        ItemStack displayItem;
        Component title = null;
        List<Component> lore = new ArrayList<>();

        // --- 1. VANILLA ITEM ---
        if ("ITEM".equals(type)) {
            Object matObj = map.get("material");
            String matName = (matObj != null) ? matObj.toString() : "NULL";
            Material mat = Material.matchMaterial(matName);

            if (mat == null) {
                isError = true;
                displayItem = new ItemStack(Material.BARRIER);
                title = ConfigManager.fromSection("§cInvalid Item Cost");
                errorDetail = "Unknown Material: " + matName;
            } else {
                displayItem = new ItemStack(mat);
                title = ConfigManager.fromSection("§f" + amountStr + "x " + formatMaterialName(mat));
                lore.add(ConfigManager.fromSection("§8Type: §7Vanilla Item"));
                lore.add(ConfigManager.fromSection("§8Material: §b" + mat.name()));
            }

            // --- 2. CUSTOM BUFFED ITEM ---
        } else if ("BUFFED_ITEM".equals(type)) {
            String bItemId = (String) map.get("item_id");
            BuffedItem bItem = plugin.getItemManager().getBuffedItem(bItemId);

            if (bItem != null) {
                displayItem = new ItemBuilder(bItem, plugin).build();
                title = ConfigManager.fromSection("§f" + amountStr + "x ")
                        .append(ConfigManager.fromLegacy(bItem.getDisplayName()));
                lore.add(ConfigManager.fromSection("§8Type: §#FF6347Buffed Item§#FFD700"));
                lore.add(ConfigManager.fromSection("§8ID: §7" + bItemId));
            } else {
                isError = true;
                displayItem = new ItemStack(Material.BARRIER);
                title = ConfigManager.fromSection("§cUnknown Buffed Item");
                errorDetail = "ID not found: " + bItemId;
            }

            // --- 3. COINS ENGINE ---
        } else if ("COINSENGINE".equals(type)) {
            Object currObj = map.get("currency_id");
            String currencyId = (currObj != null) ? currObj.toString() : "coins";

            if (!plugin.getHookManager().isCoinsEngineLoaded()) {
                isError = true;
                displayItem = new ItemStack(Material.BARRIER);
                title = ConfigManager.fromSection("§cCoinsEngine Missing");
                errorDetail = "Plugin not hooked!";
            } else {
                Currency currency = plugin.getHookManager().getCoinsEngineHook().getCurrency(currencyId);
                if (currency == null) {
                    isError = true;
                    displayItem = new ItemStack(Material.BARRIER);
                    title = ConfigManager.fromSection("§cInvalid Currency");
                    errorDetail = "Currency ID: " + currencyId;
                } else {
                    displayItem = new ItemStack(Material.SUNFLOWER);
                    title = ConfigManager.fromSection("§a" + currency.getName());
                    lore.add(ConfigManager.fromSection("§8Type: §7CoinsEngine"));
                    lore.add(ConfigManager.fromSection("§8Currency ID: §e" + currencyId));
                    lore.add(ConfigManager.fromSection("§8Amount: §e" + amountStr));
                }
            }

            // --- 4. MONEY / VAULT ---
        } else if ("MONEY".equals(type)) {
            if (plugin.getHookManager().getVaultHook() == null) {
                isError = true;
                displayItem = new ItemStack(Material.BARRIER);
                title = ConfigManager.fromSection("§cVault Error");
                errorDetail = "Vault/Economy missing!";
            } else {
                displayItem = new ItemStack(Material.GOLD_INGOT);
                title = ConfigManager.fromSection("§aVault Currency");
                lore.add(ConfigManager.fromSection("§8Type: §7MONEY"));
                lore.add(ConfigManager.fromSection("§8Amount: §e" + amountStr));
            }

            // --- 5. GENERIC TYPES ---
        } else {
            Material iconMat = Material.PAPER;
            String name = type;
            boolean knownType = true;

            if ("EXPERIENCE".equals(type)) { iconMat = Material.EXPERIENCE_BOTTLE; name = "XP Points"; }
            else if ("LEVEL".equals(type)) { iconMat = Material.ENCHANTING_TABLE; name = "XP Levels"; }
            else if ("HUNGER".equals(type)) { iconMat = Material.COOKED_BEEF; name = "Food Level"; }
            else if ("HEALTH".equals(type)) { iconMat = Material.RED_DYE; name = "Health (Hearts)"; }
            else {
                knownType = false;
                isError = true;
                iconMat = Material.BARRIER;
                title = ConfigManager.fromSection("§cInvalid Cost Type");
                errorDetail = "Unknown Type: " + type;
            }

            displayItem = new ItemStack(iconMat);
            if (!isError) {
                title = ConfigManager.fromSection("§a" + name);
                lore.add(ConfigManager.fromSection("§8Type: §7" + type));
                lore.add(ConfigManager.fromSection("§8Amount: §e" + amountStr));
            }
        }

        if (isError) {
            lore.add(ConfigManager.fromSection("§c⚠ CONFIGURATION ERROR"));
            lore.add(ConfigManager.fromSection("§7" + errorDetail));
            lore.add(ConfigManager.fromSection(""));
            lore.add(ConfigManager.fromSection("§eRight-Click to Remove"));
        } else {
            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(ConfigManager.fromSection("§7Failure Message:"));
            if (message == null) {
                String defaultMsg = ConfigManager.getDefaultCostMessage(type);
                lore.add(ConfigManager.fromSection("§r").append(ConfigManager.fromLegacy(defaultMsg)));
                lore.add(ConfigManager.fromSection("§8(Default Config)"));
            } else {
                lore.add(ConfigManager.fromSection("§r").append(ConfigManager.fromLegacy(message)));
            }

            lore.add(net.kyori.adventure.text.Component.empty());
            lore.add(ConfigManager.fromSection("§eLeft-Click to Edit Amount"));
            lore.add(ConfigManager.fromSection("§bShift+Left-Click to Edit Message"));
            lore.add(ConfigManager.fromSection("§cRight-Click to Remove"));
        }

        lore.add(ConfigManager.fromSection("§8(#" + (index + 1) + ")"));

        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            meta.displayName(title);
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            displayItem.setItemMeta(meta);
        }
        return displayItem;
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}