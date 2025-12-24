package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CommandListMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final String itemId;
    private final CommandContext context;

    public enum CommandContext {
        ACTIVE(
                "Active Commands",
                "active_ability.commands.",
                "active_ability.actions.commands",
                "When Right-Clicked"
        ),
        DEPLETION(
                "Depletion Commands",
                "usage.commands.",
                "usage.commands",
                "When Item Depletes"
        );

        private final String title;
        private final String chatPrefix;
        private final String configKey;
        private final String description;

        CommandContext(String title, String chatPrefix, String configKey, String description) {
            this.title = title;
            this.chatPrefix = chatPrefix;
            this.configKey = configKey;
            this.description = description;
        }
    }

    public CommandListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin, CommandContext context) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
        this.context = context;
        this.maxItemsPerPage = 36;
    }

    @Override
    public String getMenuName() {
        return context.title + ": " + itemId;
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

        List<String> commands = (context == CommandContext.ACTIVE)
                ? new ArrayList<>(item.getActiveAbility().getCommands())
                : new ArrayList<>(item.getUsageDetails().getDepletionCommands());

        if (handlePageChange(e, commands.size())) return;

        if (e.getCurrentItem() == null) return;
        Material type = e.getCurrentItem().getType();

        if (type == Material.BARRIER && e.getSlot() == 53) {
            if (context == CommandContext.ACTIVE) {
                new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
            } else {
                new UsageLimitSettingsMenu(playerMenuUtility, plugin).open();
            }
            return;
        }

        if (type == Material.ANVIL && e.getSlot() == 49) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath(context.chatPrefix + "add");
            p.closeInventory();

            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter the command in chat."));
            p.sendMessage(ConfigManager.fromSection("§6Logic Prefixes:"));
            p.sendMessage(ConfigManager.fromSection("§f[console], [chance:50], [delay:20], [else]"));
            p.sendMessage(ConfigManager.fromSection("§6Actions:"));
            p.sendMessage(ConfigManager.fromSection("§f[message], [title], [actionbar], [sound]"));
            p.sendMessage(ConfigManager.fromSection("§6Variables & PAPI:"));
            p.sendMessage(ConfigManager.fromSection("§eInternals: §f%player%, %player_x%, %player_yaw%..."));
            p.sendMessage(ConfigManager.fromSection("§ePAPI Supported! §f(e.g %vault_eco_balance%)"));
            p.sendMessage(ConfigManager.fromSection("§6Chaining:"));
            p.sendMessage(ConfigManager.fromSection("§fUse '§e;;§f' to separate commands."));
            p.sendMessage(ConfigManager.fromSection(""));
            p.sendMessage(ConfigManager.fromSection("§eFor full guide use: §6/buffeditems wiki"));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            return;
        }

        if (e.getSlot() >= 9 && e.getSlot() < 45) {
            int slotIndex = e.getSlot() - 9;
            int commandIndex = maxItemsPerPage * page + slotIndex;

            if (commandIndex >= commands.size() || commandIndex < 0) return;

            if (e.isLeftClick()) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath(context.chatPrefix + "edit." + commandIndex);
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter the new command in chat."));
                p.sendMessage(ConfigManager.fromSection("§7Current: §f" + commands.get(commandIndex)));
            } else if (e.isRightClick()) {
                commands.remove(commandIndex);
                ConfigManager.setItemValue(itemId, context.configKey, commands);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cRemoved command."));
                this.open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);

        addMenuControls();

        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack to Settings"));
        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd New Command", "§7Click to add a command via chat."));

        inventory.setItem(4, makeItem(Material.BOOK, "§eCommand Info & Help",
                "§7Mode: §6" + context.description,
                "",
                "§6Logic Prefixes:",
                "§f• §e[console] §f-> §7Run as Console",
                "§f• §e[chance:%] §f-> §7Success Chance",
                "§f• §e[delay:ticks] §f-> §7Wait before run",
                "§f• §c[else] §f-> §7Run if prev [chance] failed",
                "",
                "§6Actions:",
                "§f• §e[message] §f-> §7Send Chat Msg",
                "§f• §e[title] §f-> §7Show Title",
                "§f• §e[actionbar] §f-> §7Show Actionbar",
                "§f• §e[sound] §f-> §7Play Sound",
                "",
                "§6Variables & PAPI:",
                "§f• §eInternals: §7%player%, %player_x%...",
                "§f• §ePAPI Supported!",
                "",
                "§6Chaining:",
                "§7Use '§e;;§7' to run multiple commands.",
                "",
                "§eFull Guide: §6/buffeditems wiki"));

        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        List<String> commands = (context == CommandContext.ACTIVE)
                ? item.getActiveAbility().getCommands()
                : item.getUsageDetails().getDepletionCommands();

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

                inventory.setItem(9 + i, makeItem(icon, title, formattedLore.toArray(new String[0])));
            }
        }
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