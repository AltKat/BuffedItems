package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CommandListMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final String itemId;

    public CommandListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Commands: " + itemId;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        List<String> commands = new ArrayList<>(item.getActiveCommands());

        if (handlePageChange(e, commands.size())) return;

        if (e.getCurrentItem() == null) return;
        Material type = e.getCurrentItem().getType();

        if (type == Material.BARRIER && e.getSlot() == 49) {
            new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (type == Material.ANVIL && e.getSlot() == 51) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("active.commands.add");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSection("§aEnter the command in chat."));
            p.sendMessage(ConfigManager.fromSection("§eStart with '[console] ' to run as console."));
            p.sendMessage(ConfigManager.fromSection("§7Placeholders: %player%, %player_name%, %player_x%..."));
            return;
        }

        if (type == Material.PAPER || type == Material.COMMAND_BLOCK) {
            int slotIndex = e.getSlot();
            int commandIndex = maxItemsPerPage * page + slotIndex;

            if (commandIndex >= commands.size()) return;

            if (e.isLeftClick()) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("active.commands.edit." + commandIndex);
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSection("§aEnter the new command in chat."));
                p.sendMessage(ConfigManager.fromSection("§7Current: §f" + commands.get(commandIndex)));
            } else if (e.isRightClick()) {
                String removed = commands.remove(commandIndex);
                ConfigManager.setItemValue(itemId, "commands", commands);
                p.sendMessage(ConfigManager.fromSection("§cRemoved command: §7" + removed));
                this.open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuControls();

        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack to Settings"));
        inventory.setItem(51, makeItem(Material.ANVIL, "§aAdd New Command", "§7Click to add a command via chat."));

        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        List<String> commands = item.getActiveCommands();

        if (!commands.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                int index = maxItemsPerPage * page + i;
                if (index >= commands.size()) break;

                String cmd = commands.get(index);
                Material icon = cmd.toLowerCase().startsWith("[console]") ? Material.COMMAND_BLOCK : Material.PAPER;
                String type = cmd.toLowerCase().startsWith("[console]") ? "§c[Console]" : "§a[Player]";

                String displayCmd = cmd.replace("[console]", "").replace("[CONSOLE]", "").trim();
                if (displayCmd.length() > 30) displayCmd = displayCmd.substring(0, 30) + "...";

                List<String> lore = new ArrayList<>();
                lore.add("§7Type: " + type);
                lore.add("§7Full: §f" + cmd);
                lore.add("");
                lore.add("§eLeft-Click to Edit");
                lore.add("§cRight-Click to Remove");

                inventory.setItem(i, makeItem(icon, "§f" + displayCmd, lore.toArray(new String[0])));
            }
        }
        setFillerGlass();
    }
}