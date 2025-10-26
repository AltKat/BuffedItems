package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EnchantmentListMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public EnchantmentListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Enchantments for: " + itemId;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        Player p = (Player) e.getWhoClicked();
        Material clickedType = e.getCurrentItem().getType();
        int clickedSlot = e.getSlot();

        if (clickedType == Material.BARRIER && clickedSlot == getSlots() - 1) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (clickedType == Material.ANVIL && clickedSlot == 49) {
            new EnchantmentSelectorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (clickedSlot < 45 && clickedType == Material.ENCHANTED_BOOK) {
            String configPath = "items." + itemId + ".enchantments";
            List<String> enchantmentsConfig = plugin.getConfig().getStringList(configPath);

            if (clickedSlot >= enchantmentsConfig.size()) {
                return;
            }

            String clickedEnchantString = enchantmentsConfig.get(clickedSlot);

            if (e.isRightClick()) {
                List<String> updatedList = new ArrayList<>(enchantmentsConfig);
                updatedList.remove(clickedSlot);
                ConfigManager.setItemValue(itemId, "enchantments", updatedList);
                p.sendMessage("§aEnchantment '" + clickedEnchantString.split(";")[0] + "' removed.");
                this.open();
            }
            else if (e.isLeftClick()) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setEditIndex(clickedSlot);
                playerMenuUtility.setChatInputPath("enchantments.edit");
                String enchantName = clickedEnchantString.split(";")[0];
                p.closeInventory();
                p.sendMessage("§aPlease type the new Level for '" + enchantName + "' in chat (e.g., 1, 5, 10).");
            }
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd New Enchantment", "§7Adds an enchantment to this item."));
        addBackButton(new ItemEditorMenu(playerMenuUtility, plugin));

        String configPath = "items." + playerMenuUtility.getItemToEditId() + ".enchantments";
        List<String> enchantmentsConfig = plugin.getConfig().getStringList(configPath);

        if (!enchantmentsConfig.isEmpty()) {
            for (int i = 0; i < enchantmentsConfig.size(); i++) {
                if (i >= getSlots() - 9) break;
                String enchString = enchantmentsConfig.get(i);
                String[] parts = enchString.split(";");

                boolean isValid = false;
                Enchantment enchantment = null;
                int level = 0;

                if (parts.length == 2) {
                    try {
                        enchantment = Enchantment.getByName(parts[0].toUpperCase());
                        level = Integer.parseInt(parts[1]);
                        if (enchantment != null && level > 0) {
                            isValid = true;
                        }
                    } catch (IllegalArgumentException | NullPointerException |ArrayIndexOutOfBoundsException ignored) {
                    }
                }

                if (isValid) {
                    ItemStack book = makeItem(Material.ENCHANTED_BOOK, "§b" + parts[0],
                            "§7Level: §e" + level,
                            "",
                            "§aLeft-Click to Edit Level",
                            "§cRight-Click to Delete");
                    ItemMeta meta = book.getItemMeta();
                    meta.addEnchant(enchantment, level, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    book.setItemMeta(meta);
                    inventory.setItem(i, book);
                } else {
                    inventory.setItem(i, makeItem(Material.BARRIER, "§c§lCORRUPT ENTRY",
                            "§7This line is malformed in config.yml:",
                            "§e" + enchString,
                            "",
                            "§cPossible Errors:",
                            "§7- Using ':' instead of the correct ';'.",
                            "§7- Missing the level (e.g., 'DAMAGE_ALL;').",
                            "§7- A typo in the Enchantment name (e.g., 'SHARPNES').",
                            "§7- Level is not a whole number or is zero/negative.",
                            "§7- Accidental spaces.",
                            "",
                            "§aCorrect Format: §eDAMAGE_ALL;5",
                            "",
                            "§cRight-Click to Delete this entry."));
                }
            }
        }
        setFillerGlass();
    }
}