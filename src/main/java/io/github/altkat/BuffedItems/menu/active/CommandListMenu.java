package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

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

            p.sendMessage(ConfigManager.fromSection("§6Logic Prefixes (Any Order):"));
            p.sendMessage(ConfigManager.fromSection("§d• [delay:20] §7(Wait ticks)."));
            p.sendMessage(ConfigManager.fromSection("§b• [chance:50] §7(Success chance)."));
            p.sendMessage(ConfigManager.fromSection("§e• [console] §7(Run as admin)."));

            p.sendMessage(ConfigManager.fromSection("§6Message Actions:"));
            p.sendMessage(ConfigManager.fromSection("§a• [message] Hi! §7(Chat)"));
            p.sendMessage(ConfigManager.fromSection("§b• [actionbar] Hi! §7(Hotbar)"));
            p.sendMessage(ConfigManager.fromSection("§d• [title] Hi!|Sub §7(Title)"));

            p.sendMessage(ConfigManager.fromSection("§6Placeholders:"));
            p.sendMessage(ConfigManager.fromSection("§7• Built-in: %player%, %player_x%, %player_yaw%..."));
            p.sendMessage(ConfigManager.fromSection("§d• PlaceholderAPI: Fully Supported! (e.g. %player_ping%)"));

            p.sendMessage(ConfigManager.fromSection("§6Chaining:"));
            p.sendMessage(ConfigManager.fromSection("§fUse ';;' to separate commands."));
            return;
        }

        if (e.getSlot() < 45 && e.getSlot() >= 9) {
            int slotIndex = e.getSlot() - 9;
            int commandIndex = maxItemsPerPage * page + slotIndex;

            if (commandIndex >= commands.size() || commandIndex < 0) return;

            if (e.isLeftClick()) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("active.commands.edit." + commandIndex);
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSection("§aEnter the new command in chat."));
                p.sendMessage(ConfigManager.fromSection("§7Current: §f" + commands.get(commandIndex)));
            } else if (e.isRightClick()) {
                String removed = commands.remove(commandIndex);
                ConfigManager.setItemValue(itemId, "commands", commands);
                p.sendMessage(ConfigManager.fromSection("§cRemoved command."));
                this.open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuControls();

        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack to Settings"));
        inventory.setItem(51, makeItem(Material.ANVIL, "§aAdd New Command", "§7Click to add a command via chat."));

        inventory.setItem(45, makeItem(Material.BOOK, "§eCommand Info & Help",
                "§7Commands run when the item is right-clicked.",
                "",
                "§6Actions & Messages:",
                "§a• [message] Text   §7(Clean chat msg)",
                "§b• [actionbar] Text §7(Above hotbar)",
                "§d• [title] Main|Sub §7(Screen text)",
                "",
                "§6Logic Prefixes:",
                "§d• [delay:ticks] §7(20 ticks = 1s)",
                "§b• [chance:%]    §7(Success % 0-100)",
                "§e• [console]     §7(Run as Admin)",
                "",
                "§6Variables & PAPI:",
                "§7• %player%, %player_x%, %player_yaw%...",
                "§d• PlaceholderAPI Supported!",
                "§7  (e.g. %vault_eco_balance%)",
                "",
                "§6Chaining:",
                "§7Use ';;' to combine them."));

        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        List<String> commands = item.getActiveCommands();

        if (!commands.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                int index = maxItemsPerPage * page + i;
                if (index >= commands.size()) break;

                String rawCmd = commands.get(index);

                boolean isChain = rawCmd.contains(";;");
                boolean hasConsole = rawCmd.toLowerCase().contains("[console]");

                Material icon;
                if (isChain) icon = Material.CHAIN_COMMAND_BLOCK;
                else if (hasConsole) icon = Material.COMMAND_BLOCK;
                else icon = Material.PAPER;

                String title = isChain ? "§6⚡ Chain Sequence" : "§fCommand #" + (index + 1);

                List<String> formattedLore = formatCommandForDisplay(rawCmd);

                formattedLore.add("");
                formattedLore.add("§eLeft-Click to Edit");
                formattedLore.add("§cRight-Click to Remove");

                inventory.setItem(i + 9, makeItem(icon, title, formattedLore.toArray(new String[0])));
            }
        }
        setFillerGlass();
    }

    private List<String> formatCommandForDisplay(String rawCmd) {
        List<String> lore = new ArrayList<>();
        String[] steps = rawCmd.split(";;");

        for (int i = 0; i < steps.length; i++) {
            String step = steps[i].trim();
            String stepLabel = (steps.length > 1) ? "§7Step " + (i + 1) + ": " : "";

            String displayDelay = "";
            String displayChance = "";
            String displayType = "§a(Player)";
            String cleanCmd = step;

            boolean parsing = true;
            while(parsing) {
                parsing = false;
                String lower = cleanCmd.toLowerCase();

                if (lower.startsWith("[delay:")) {
                    int close = cleanCmd.indexOf("]");
                    if (close != -1) {
                        String val = cleanCmd.substring(7, close);
                        try {
                            double sec = Long.parseLong(val) / 20.0;
                            displayDelay = "§d⏳" + val + "t §8(" + sec + "s) ";
                        } catch (Exception e) { displayDelay = "§d⏳" + val + " "; }
                        cleanCmd = cleanCmd.substring(close + 1).trim();
                        parsing = true;
                    }
                } else if (lower.startsWith("[chance:")) {
                    int close = cleanCmd.indexOf("]");
                    if (close != -1) {
                        String val = cleanCmd.substring(8, close);
                        displayChance = "§b🎲" + val + "% ";
                        cleanCmd = cleanCmd.substring(close + 1).trim();
                        parsing = true;
                    }
                } else if (lower.startsWith("[console]")) {
                    displayType = "§c(Console)";
                    cleanCmd = cleanCmd.substring(9).trim();
                    parsing = true;
                } else if (lower.startsWith("[console]")) {
                    displayType = "§c(Console)";
                    cleanCmd = cleanCmd.substring(9).trim();
                    parsing = true;
                }
                else if (lower.startsWith("[message]") || lower.startsWith("[msg]")) {
                    displayType = "§6(Chat)";
                    cleanCmd = cleanCmd.substring(cleanCmd.indexOf("]") + 1).trim();
                    parsing = true;
                }
                else if (lower.startsWith("[actionbar]") || lower.startsWith("[ab]")) {
                    displayType = "§b(Action Bar)";
                    cleanCmd = cleanCmd.substring(cleanCmd.indexOf("]") + 1).trim();
                    parsing = true;
                }
                else if (lower.startsWith("[title]")) {
                    displayType = "§d(Title)";
                    cleanCmd = cleanCmd.substring(7).trim();
                    parsing = true;
                }
            }

            lore.add(stepLabel + displayDelay + displayChance + displayType);
            lore.add(" §7➥ §f/" + (cleanCmd.length() > 35 ? cleanCmd.substring(0, 32) + "..." : cleanCmd));

            if (i < steps.length - 1) {
                lore.add("§8§m  ⬇  ");
            }
        }
        return lore;
    }
}