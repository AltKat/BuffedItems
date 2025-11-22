package io.github.altkat.BuffedItems.menu.upgrade;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.TypeSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
            new TypeSelectorMenu(playerMenuUtility, plugin, PlayerMenuUtility.MaterialSelectionContext.INGREDIENT).open();
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
                e.getWhoClicked().sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
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
            inventory.setItem(index, createVisualItem(map, index));
            index++;
        }
    }

    private ItemStack createVisualItem(Map<?, ?> map, int index) {
        String type = (String) map.get("type");
        Object amountObj = map.get("amount");
        String amountStr = String.valueOf(amountObj);

        ItemStack displayItem;
        net.kyori.adventure.text.Component title;
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();

        // --- 1. VANILLA ITEM ---
        if ("ITEM".equals(type)) {
            Object matObj = map.get("material");
            String matName = (matObj != null) ? matObj.toString() : "STONE";

            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.STONE;

            displayItem = new ItemStack(mat);
            title = ConfigManager.fromSection("§f" + amountStr + "x " + formatMaterialName(mat));

            lore.add(ConfigManager.fromSection("§8Type: §7Vanilla Item"));
            lore.add(ConfigManager.fromSection("§8Material: §b" + mat.name()));

            // --- 2. CUSTOM BUFFED ITEM ---
        } else if ("BUFFED_ITEM".equals(type)) {
            String itemId = (String) map.get("item_id");
            BuffedItem bItem = plugin.getItemManager().getBuffedItem(itemId);

            if (bItem != null) {
                displayItem = new ItemBuilder(bItem, plugin).build();
                title = ConfigManager.fromSection("§f" + amountStr + "x ")
                        .append(ConfigManager.fromLegacy(bItem.getDisplayName()));
            } else {
                displayItem = new ItemStack(Material.BEDROCK);
                title = ConfigManager.fromSection("§cUnknown Buffed Item: " + itemId);
            }

            lore.add(ConfigManager.fromSection("§8Type: §#FF6347Buffed Item§#FFD700"));
            lore.add(ConfigManager.fromSection("§8ID: §7" + itemId));

            // --- 3. COINS ENGINE (NEW) ---
        } else if ("COINSENGINE".equals(type)) {
            Object currObj = map.get("currency_id");
            String currencyId = (currObj != null) ? currObj.toString() : "coins";

            if (plugin.getServer().getPluginManager().getPlugin("CoinsEngine") == null) {
                displayItem = new ItemStack(Material.BEDROCK);
                title = ConfigManager.fromSection("§cCoinsEngine Not Found");
                lore.add(ConfigManager.fromSection("§cPlugin is missing!"));
            } else {
                su.nightexpress.coinsengine.api.currency.Currency currency = su.nightexpress.coinsengine.api.CoinsEngineAPI.getCurrency(currencyId);
                if (currency == null) {
                    displayItem = new ItemStack(Material.BEDROCK);
                    title = ConfigManager.fromSection("§cUnknown Currency: " + currencyId);
                } else {
                    displayItem = new ItemStack(Material.SUNFLOWER);
                    title = ConfigManager.fromSection("§a" + currency.getName());
                }
            }
            lore.add(ConfigManager.fromSection("§8Type: §7CoinsEngine"));
            lore.add(ConfigManager.fromSection("§8Currency ID: §e" + currencyId));
            lore.add(ConfigManager.fromSection("§8Amount: §e" + amountStr));

            // --- 4. MONEY / VAULT (NEW) ---
        } else if ("MONEY".equals(type)) {
            if (plugin.getCostManager().getEconomy() == null) {
                displayItem = new ItemStack(Material.BEDROCK);
                title = ConfigManager.fromSection("§cVault/Economy Not Found");
                lore.add(ConfigManager.fromSection("§cVault is missing or not hooked!"));
            } else {
                displayItem = new ItemStack(Material.GOLD_INGOT);
                title = ConfigManager.fromSection("§aVault Currency");
            }
            lore.add(ConfigManager.fromSection("§8Type: §7MONEY"));
            lore.add(ConfigManager.fromSection("§8Amount: §e" + amountStr));

            // --- 5. GENERIC TYPES (XP, LEVEL, HUNGER, HEALTH) ---
        } else {
            Material iconMat;
            String name;

            switch (type) {
                case "EXPERIENCE": iconMat = Material.EXPERIENCE_BOTTLE; name = "XP Points"; break;
                case "LEVEL": iconMat = Material.ENCHANTING_TABLE; name = "XP Levels"; break;
                case "HUNGER": iconMat = Material.COOKED_BEEF; name = "Food Level"; break;
                case "HEALTH": iconMat = Material.RED_DYE; name = "Health (Hearts)"; break;
                default: iconMat = Material.PAPER; name = type; break;
            }

            displayItem = new ItemStack(iconMat);
            title = ConfigManager.fromSection("§a" + name);

            lore.add(ConfigManager.fromSection("§8Type: §7" + type));
            lore.add(ConfigManager.fromSection("§8Amount: §e" + amountStr));
        }

        // --- COMMON FOOTER ---
        lore.add(net.kyori.adventure.text.Component.empty());
        lore.add(ConfigManager.fromSection("§eLeft-Click to Edit Amount"));
        lore.add(ConfigManager.fromSection("§cRight-Click to Remove"));
        lore.add(ConfigManager.fromSection("§8(#" + (index + 1) + ")"));

        // Apply Meta
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