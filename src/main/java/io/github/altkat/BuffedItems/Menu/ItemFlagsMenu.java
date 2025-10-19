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
            new FlagInfo("HIDE_ATTRIBUTES", Material.BOOK, "Hide Attributes", "Hides the green/blue attribute text in the lore."),
            new FlagInfo("HIDE_ENCHANTS", Material.ENCHANTING_TABLE, "Hide Enchants", "Hides the enchantment text in the lore."),
            new FlagInfo("HIDE_UNBREAKABLE", Material.BEDROCK, "Hide Unbreakable", "Hides the 'Unbreakable' text in the lore."),
            new FlagInfo("HIDE_POTION_EFFECTS", Material.POTION, "Hide Potion Effects", "Hides effect text on Potions/Tipped Arrows."),
            new FlagInfo("HIDE_DESTROYS", Material.IRON_PICKAXE, "Hide Destroys", "Hides the 'Can Destroy:' list for adventure mode."),
            new FlagInfo("HIDE_PLACED_ON", Material.STONE, "Hide Placed On", "Hides the 'Can Be Placed On:' list for adventure mode."),
            new FlagInfo("PREVENT_ANVIL_USE", Material.CHAINMAIL_CHESTPLATE, "Prevent Anvil Use", "Prevents renaming, repairing, or enchanting in an anvil."),
            new FlagInfo("PREVENT_ENCHANT_TABLE", Material.LAPIS_LAZULI, "Prevent Enchant Table", "Prevents the item from being put in an enchanting table."),
            new FlagInfo("PREVENT_SMITHING_USE", Material.SMITHING_TABLE, "Prevent Smithing Use", "Prevents the item from being used in a smithing table."),
            new FlagInfo("PREVENT_CRAFTING_USE", Material.CRAFTING_TABLE, "Prevent Crafting Use", "Prevents the item from being used as an ingredient in recipes."),
            new FlagInfo("PREVENT_DROP", Material.DROPPER, "Prevent Dropping", "Prevents the player from dropping the item (Q key)."),
            new FlagInfo("PREVENT_CONSUME", Material.APPLE, "Prevent Consumption", "Prevents the item from being eaten or drunk."),
            new FlagInfo("UNBREAKABLE", Material.ANVIL, "Unbreakable", "Prevents the item from losing durability."),
            new FlagInfo("PREVENT_PLACEMENT", Material.GRASS_BLOCK, "Prevent Placement", "Prevents placing blocks or items like boats, armor stands, etc."),
            new FlagInfo("PREVENT_DEATH_DROP", Material.TOTEM_OF_UNDYING, "Prevent Death Drop", "Keeps the item in the player's inventory upon death."),
            new FlagInfo("PREVENT_INTERACT", Material.STRUCTURE_VOID, "Prevent Use", "Prevents using the item (e.g., Flint & Steel, Hoe, Shield).")
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
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        int clickedSlot = e.getSlot();
        if (clickedSlot >= 0 && clickedSlot < flags.size()) {
            FlagInfo flag = flags.get(clickedSlot);
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

    @Override
    public void setMenuItems() {
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) {
            playerMenuUtility.getOwner().sendMessage("§cError: Item not found.");
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        for (int i = 0; i < flags.size(); i++) {
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

            inventory.setItem(i, itemStack);
        }

        addBackButton(new ItemEditorMenu(playerMenuUtility, plugin));
        setFillerGlass();
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