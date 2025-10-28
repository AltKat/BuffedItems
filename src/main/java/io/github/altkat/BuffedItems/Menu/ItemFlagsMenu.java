package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemFlagsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    private final List<FlagInfo> flags = Arrays.asList(
            new FlagInfo("HIDE_ATTRIBUTES", Material.BOOK, "Hide Attributes", "Hides the default attribute modifiers text (e.g., '+5 Attack Damage')."),
            new FlagInfo("HIDE_ENCHANTS", Material.ENCHANTING_TABLE, "Hide Enchants", "Hides the enchantment list text."),
            new FlagInfo("HIDE_UNBREAKABLE", Material.BEDROCK, "Hide Unbreakable", "Hides the 'Unbreakable' text."),
            new FlagInfo("HIDE_POTION_EFFECTS", Material.POTION, "Hide Potion Effects", "Hides effect text on Potions, Tipped Arrows, Suspicious Stew."),
            new FlagInfo("HIDE_DESTROYS", Material.IRON_PICKAXE, "Hide Destroys", "Hides the 'Can Destroy:' block list (Adventure mode)."),
            new FlagInfo("HIDE_PLACED_ON", Material.STONE, "Hide Placed On", "Hides the 'Can Be Placed On:' block list (Adventure mode)."),
            new FlagInfo("PREVENT_ANVIL_USE", Material.CHAINMAIL_CHESTPLATE, "Prevent Anvil Use", "Prevents renaming, repairing, or combining enchantments in an anvil."),
            new FlagInfo("PREVENT_ENCHANT_TABLE", Material.LAPIS_LAZULI, "Prevent Enchant Table", "Prevents the item from being placed in an enchanting table."),
            new FlagInfo("PREVENT_SMITHING_USE", Material.SMITHING_TABLE, "Prevent Smithing Use", "Prevents the item from being used in a smithing table (base, addition, or template)."),
            new FlagInfo("PREVENT_CRAFTING_USE", Material.CRAFTING_TABLE, "Prevent Crafting Use", "Prevents the item from being used as an ingredient in crafting recipes."),
            new FlagInfo("PREVENT_DROP", Material.DROPPER, "Prevent Dropping", "Prevents the player from dropping the item (Q key or inventory drag)."),
            new FlagInfo("PREVENT_CONSUME", Material.APPLE, "Prevent Consumption", "Prevents the item from being eaten, drunk (potions), or consumed."),
            new FlagInfo("UNBREAKABLE", Material.ANVIL, "Unbreakable", "Prevents the item from losing durability."),
            new FlagInfo("PREVENT_PLACEMENT", Material.GRASS_BLOCK, "Prevent Placement", "Prevents placing blocks, boats, minecarts, armor stands, item frames, paintings, etc."),
            new FlagInfo("PREVENT_DEATH_DROP", Material.TOTEM_OF_UNDYING, "Prevent Death Drop", "Keeps the item in the player's inventory upon death."),
            new FlagInfo("PREVENT_INTERACT", Material.STRUCTURE_VOID, "Prevent Use (Right-Click)", "Prevents most right-click actions (axe strip/wax, hoe till, shovel path, flint&steel, shield block, shear, spyglass, fishing rod, etc.). Does NOT prevent eating/drinking or block placement.")
    );

    public ItemFlagsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Editing Flags for: " + itemId;
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        Material clickedType = e.getCurrentItem().getType();

        if (clickedType == Material.BARRIER && e.getSlot() == getSlots() - 1) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (clickedType == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        List<Integer> itemSlots = Arrays.asList(
                11, 12, 13, 14, 15,
                20, 21, 22, 23, 24,
                29, 30, 31, 32, 33, 34
        );

        int clickedSlot = e.getSlot();

        if (itemSlots.contains(clickedSlot)) {
            int flagIndex = itemSlots.indexOf(clickedSlot);

            if (flagIndex >= 0 && flagIndex < flags.size()) {
                FlagInfo flag = flags.get(flagIndex);
                String clickedFlagName = flag.id;

                BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
                boolean currentValue = item.getFlag(clickedFlagName);
                boolean newValue = !currentValue;

                String configPath = "flags." + clickedFlagName.toUpperCase();
                ConfigManager.setItemValue(itemId, configPath, newValue);

                p.sendMessage("§aFlag '" + clickedFlagName + "' set to " + (newValue ? "§aEnabled" : "§cDisabled"));

                this.open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) {
            playerMenuUtility.getOwner().sendMessage("§cError: Item not found.");
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
        }
        for (int i = 36; i < 44; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(9, filler);
        inventory.setItem(17, filler);
        inventory.setItem(18, filler);
        inventory.setItem(26, filler);
        inventory.setItem(27, filler);
        inventory.setItem(35, filler);

        List<Integer> itemSlots = Arrays.asList(
                11, 12, 13, 14, 15,
                20, 21, 22, 23, 24,
                29, 30, 31, 32, 33, 34
        );

        for (int i = 0; i < flags.size(); i++) {
            if (i >= itemSlots.size()) break;
            FlagInfo flag = flags.get(i);
            boolean isEnabled = item.getFlag(flag.id);
            String status = isEnabled ? "§aEnabled" : "§cDisabled";

            List<String> lore = new ArrayList<>();
            lore.add("§7" + flag.description);
            lore.add("");
            lore.add("§7Status: " + status);
            lore.add("§7Internal ID: " + flag.id);
            lore.add("");
            lore.add("§aClick to " + (isEnabled ? "Disable" : "Enable"));

            ItemStack itemStack = makeItem(flag.icon, "§e" + flag.displayName, lore.toArray(new String[0]));

            if (isEnabled) {
                ItemMeta meta = itemStack.getItemMeta();
                meta.addEnchant(Enchantment.LUCK, 1, false);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                itemStack.setItemMeta(meta);
            }

            inventory.setItem(itemSlots.get(i), itemStack);
        }
        addBackButton(new ItemEditorMenu(playerMenuUtility, plugin));
    }

    private static class FlagInfo {
        final String id;
        final Material icon;
        final String displayName;
        final String description;

        FlagInfo(String id, Material icon, String displayName, String description) {
            this.id = id;
            this.icon = icon;
            this.displayName = displayName;
            this.description = description;
        }
    }
}