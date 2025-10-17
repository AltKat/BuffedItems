package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.github.altkat.BuffedItems.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.stream.Collectors;

public class ItemEditorMenu extends Menu {
    private final BuffedItems plugin;

    public ItemEditorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Editing: " + playerMenuUtility.getItemToEditId();
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                playerMenuUtility.setNavigating(true);
                if (ConfigManager.isDirty()) {
                    new SaveChangesConfirmationMenu(playerMenuUtility, plugin).open();
                } else {
                    new MainMenu(playerMenuUtility, plugin).open();
                }
                break;
            case EMERALD:
                playerMenuUtility.setNavigating(true);
                ConfigManager.saveConfigIfDirty();
                p.sendMessage("§aChanges have been saved successfully!");
                this.open();
                break;
            case CHEST_MINECART:
                BuffedItem itemToClone = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
                if (itemToClone != null) {
                    ItemStack clone = new ItemBuilder(itemToClone, plugin).build();
                    p.getInventory().addItem(clone);
                    p.sendMessage("§bTest copy of '" + itemToClone.getId() + "' has been added to your inventory.");
                    playerMenuUtility.setNavigating(true);
                    p.closeInventory();
                } else {
                    p.sendMessage("§cCould not generate test copy. Item not found in memory.");
                }
                break;
            case NAME_TAG:
                playerMenuUtility.setNavigating(true);
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("display_name");
                p.closeInventory();
                p.sendMessage("§aPlease type the new display name in chat. Use '&' for color codes.");
                break;
            case BOOK:
                playerMenuUtility.setNavigating(true);
                new LoreEditorMenu(playerMenuUtility, plugin).open();
                break;
            case PAPER:
                playerMenuUtility.setNavigating(true);
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("permission");
                p.closeInventory();
                p.sendMessage("§aPlease type the permission node in chat.");
                p.sendMessage("§7(Type 'none' or 'remove' to clear the permission)");
                break;
            case BEACON:
                playerMenuUtility.setNavigating(true);
                BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
                ConfigManager.setItemValue(item.getId(), "glow", !item.hasGlow());
                this.open();
                break;
            case GRASS_BLOCK:
                playerMenuUtility.setNavigating(true);
                new MaterialSelectorMenu(playerMenuUtility, plugin).open();
                break;
            case POTION:
                playerMenuUtility.setNavigating(true);
                new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.POTION_EFFECT).open();
                break;
            case IRON_SWORD:
                playerMenuUtility.setNavigating(true);
                new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.ATTRIBUTE).open();
                break;
            default:
                playerMenuUtility.setNavigating(false);
                break;
        }
    }

    @Override
    public void setMenuItems() {
        BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
        if (item == null) {
            playerMenuUtility.getOwner().sendMessage("§cError: Item could not be found. Returning to main menu.");
            playerMenuUtility.setNavigating(true);
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        inventory.setItem(10, makeItem(Material.NAME_TAG, "§aChange Display Name", "§7Current: " + item.getDisplayName()));
        inventory.setItem(12, makeItem(Material.GRASS_BLOCK, "§aChange Material", "§7Current: §e" + item.getMaterial().name()));
        inventory.setItem(14, makeItem(Material.BOOK, "§aEdit Lore", "§7Click to modify the item's lore."));
        inventory.setItem(16, makeItem(Material.PAPER, "§aSet Permission", "§7Current: §e" + item.getPermission().orElse("§cNone")));
        inventory.setItem(28, makeItem(Material.BEACON, "§aToggle Glow", "§7Current: " + (item.hasGlow() ? "§aEnabled" : "§cDisabled")));
        inventory.setItem(30, makeItem(Material.POTION, "§aEdit Potion Effects", "§7Click to manage potion effects."));
        inventory.setItem(32, makeItem(Material.IRON_SWORD, "§aEdit Attributes", "§7Click to manage attributes."));
        inventory.setItem(42, makeItem(Material.CHEST_MINECART, "§bGet Test Copy", "§7Gives you a copy of this item", "§7with all current (even unsaved) changes.", "§cThis does not save the item."));
        if (ConfigManager.isDirty()) {
            inventory.setItem(43, makeItem(Material.EMERALD, "§a§lSave Changes", "§7Click to write all pending changes", "§7to the config.yml file."));
        } else {
            inventory.setItem(43, makeItem(Material.GRAY_DYE, "§7No Changes to Save", "§8Make an edit to enable saving."));
        }

        addBackButton(new MainMenu(playerMenuUtility, plugin));
        setFillerGlass();
    }
}