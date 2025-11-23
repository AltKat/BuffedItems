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

            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter the command in chat."));

            p.sendMessage(ConfigManager.fromSection("§6Logic Prefixes (Any Order):"));
            p.sendMessage(ConfigManager.fromSection("§c• [else]      §7(Run if prev failed)."));
            p.sendMessage(ConfigManager.fromSection("§d• [delay:20]  §7(Wait ticks)."));
            p.sendMessage(ConfigManager.fromSection("§b• [chance:50] §7(Success chance)."));
            p.sendMessage(ConfigManager.fromSection("§e• [console]   §7(Run as admin)."));

            p.sendMessage(ConfigManager.fromSection("§6Message Actions:"));
            p.sendMessage(ConfigManager.fromSection("§a• [message] Hi! §7(Chat)"));
            p.sendMessage(ConfigManager.fromSection("§b• [actionbar] Hi! §7(Hotbar)"));
            p.sendMessage(ConfigManager.fromSection("§d• [title] Hi!|Sub §7(Title)"));

            p.sendMessage(ConfigManager.fromSection("§6Chaining:"));
            p.sendMessage(ConfigManager.fromSection("§fUse ';;' to separate commands."));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
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
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter the new command in chat."));
                p.sendMessage(ConfigManager.fromSection("§7Current: §f" + commands.get(commandIndex)));
            } else if (e.isRightClick()) {
                commands.remove(commandIndex);
                ConfigManager.setItemValue(itemId, "commands", commands);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cRemoved command."));
                this.open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuControls();

        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack to Settings"));
        inventory.setItem(51, makeItem(Material.ANVIL, "§aAdd New Command", "§7Click to add a command via chat."));

        inventory.setItem(4, makeItem(Material.BOOK, "§eCommand Info & Help",
                "§7Commands run when the item is right-clicked.",
                "",
                "§6Logic Flow:",
                "§f• Normal commands execute sequentially.",
                "§c• [else] §7commands ONLY execute if the",
                "§7  IMMEDIATELY PRECEDING command failed",
                "§7  (e.g., due to chance).",
                "",
                "§6Actions & Logic:",
                "§b• [chance:%]    §7(Success % 0-100)",
                "§d• [delay:ticks] §7(20 ticks = 1s)",
                "§e• [console]     §7(Run as Admin)",
                "§a• [message]     §7(Send chat msg)",
                "",
                "§6Chaining:",
                "§7Use ';;' to combine actions.",
                "§eSee wiki for detailed examples."));

        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        List<String> commands = item.getActiveCommands();

        if (!commands.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                int index = maxItemsPerPage * page + i;
                if (index >= commands.size()) break;

                String rawCmd = commands.get(index);
                String lowerCmd = rawCmd.toLowerCase().trim();

                boolean isElse = lowerCmd.startsWith("[else]");
                boolean isChain = rawCmd.contains(";;");

                Material icon;
                String title;

                int commandNumber = index + 1;

                if (isElse) {
                    int connectedTo = Math.max(1, commandNumber - 1);

                    icon = Material.CHAIN_COMMAND_BLOCK;
                    title = "§c⚡ Else Block of #" + connectedTo;
                } else {
                    if (isChain) {
                        icon = Material.REPEATING_COMMAND_BLOCK;
                        title = "§e▶ Command #" + commandNumber + " §7(Chain)";
                    } else {
                        icon = Material.COMMAND_BLOCK;
                        title = "§a▶ Command #" + commandNumber;
                    }
                }

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

            String displayPrefixes = "";
            String cleanCmd = step;

            boolean parsing = true;
            while(parsing) {
                parsing = false;
                String lower = cleanCmd.toLowerCase();

                if (lower.startsWith("[else]")) {
                    displayPrefixes += "§e(Else) ";
                    cleanCmd = cleanCmd.substring(6).trim();
                    parsing = true;
                }
                else if (lower.startsWith("[delay:")) {
                    int close = cleanCmd.indexOf("]");
                    if (close != -1) {
                        String val = cleanCmd.substring(7, close);
                        try {
                            double sec = Long.parseLong(val) / 20.0;
                            displayPrefixes += "§d⏳" + val + "t ";
                        } catch (Exception e) { displayPrefixes += "§d⏳" + val + " "; }
                        cleanCmd = cleanCmd.substring(close + 1).trim();
                        parsing = true;
                    }
                }
                else if (lower.startsWith("[chance:")) {
                    int close = cleanCmd.indexOf("]");
                    if (close != -1) {
                        String val = cleanCmd.substring(8, close);
                        displayPrefixes += "§b🎲" + val + "% ";
                        cleanCmd = cleanCmd.substring(close + 1).trim();
                        parsing = true;
                    }
                }
                else if (lower.startsWith("[console]")) {
                    displayPrefixes += "§c(Console) ";
                    cleanCmd = cleanCmd.substring(9).trim();
                    parsing = true;
                }

                if (lower.startsWith("[message]") || lower.startsWith("[msg]")) {
                    displayPrefixes += "§6(Chat) ";
                    cleanCmd = cleanCmd.substring(cleanCmd.indexOf("]") + 1).trim();
                    parsing = false;
                }
                else if (lower.startsWith("[actionbar]") || lower.startsWith("[ab]")) {
                    displayPrefixes += "§b(Action Bar) ";
                    cleanCmd = cleanCmd.substring(cleanCmd.indexOf("]") + 1).trim();
                    parsing = false;
                }
                else if (lower.startsWith("[title]")) {
                    displayPrefixes += "§d(Title) ";
                    cleanCmd = cleanCmd.substring(7).trim();
                    parsing = false;
                }
            }

            lore.add(stepLabel + displayPrefixes);

            String displayCmd = cleanCmd;
            if (displayCmd.length() > 40) {
                displayCmd = displayCmd.substring(0, 37) + "...";
            }

            lore.add(" §7➥ §f" + displayCmd);

            if (i < steps.length - 1) {
                lore.add("§8§m  ⬇  ");
            }
        }
        return lore;
    }
}