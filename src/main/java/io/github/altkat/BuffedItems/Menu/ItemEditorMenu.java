package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

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
        BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());

        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                new MainMenu(playerMenuUtility, plugin).open();
                break;
            case NAME_TAG:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("display_name");
                p.closeInventory();
                p.sendMessage("§aPlease type the new display name in chat. Use '&' for color codes.");
                break;
            case BOOK:
                new LoreEditorMenu(playerMenuUtility, plugin).open();
                break;
            case PAPER:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("permission");
                p.closeInventory();
                p.sendMessage("§aPlease type the permission node in chat.");
                p.sendMessage("§7(Type 'none' or 'remove' to clear the permission)");
                break;
            case BEACON:
                ConfigManager.setItemValue(item.getId(), "glow", !item.hasGlow());
                this.open();
                break;
            case GRASS_BLOCK:
                new MaterialSelectorMenu(playerMenuUtility, plugin).open();
                break;
            case POTION:
                new PotionEffectListMenu(playerMenuUtility, plugin).open();
                break;
            case IRON_SWORD:
                new AttributeListMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
        if (item == null) {
            playerMenuUtility.getOwner().sendMessage("§cError: Item could not be found. Returning to main menu.");
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        inventory.setItem(10, makeItem(Material.NAME_TAG, "§aChange Display Name",
                "§7Current: " + item.getDisplayName()));

        inventory.setItem(12, makeItem(Material.GRASS_BLOCK, "§aChange Material",
                "§7Current: §e" + item.getMaterial().name()));

        String loreString = item.getLore().isEmpty() ? "§cNone" : item.getLore().stream().map(s -> "§f- " + s).collect(Collectors.joining("\n"));
        inventory.setItem(14, makeItem(Material.BOOK, "§aEdit Lore",
                "§7Click to modify the item's lore."));

        inventory.setItem(16, makeItem(Material.PAPER, "§aSet Permission",
                "§7Current: §e" + item.getPermission().orElse("§cNone")));

        inventory.setItem(28, makeItem(Material.BEACON, "§aToggle Glow",
                "§7Current: " + (item.hasGlow() ? "§aEnabled" : "§cDisabled")));

        inventory.setItem(30, makeItem(Material.POTION, "§aEdit Potion Effects",
                "§7Click to manage potion effects."));

        inventory.setItem(32, makeItem(Material.IRON_SWORD, "§aEdit Attributes",
                "§7Click to manage attributes."));


        addBackButton(new MainMenu(playerMenuUtility, plugin));
        setFillerGlass();
    }
}