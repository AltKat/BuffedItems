package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.BuffedItemSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.DepletionAction;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class UsageLimitSettingsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public UsageLimitSettingsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Usage Limit: " + itemId;
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;
        Material type = e.getCurrentItem().getType();

        if (type == Material.BARRIER && e.getSlot() == 44) {
            new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }

        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        if (e.getSlot() == 10) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("usage-limit.max-usage");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Max Usage amount in chat (e.g. 50)."));
            p.sendMessage(ConfigManager.fromSection("§7Current: " + (item.getMaxUses() > 0 ? item.getMaxUses() : "Unlimited (-1)")));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit."));
            return;
        }

        if (e.getSlot() == 12) {
            DepletionAction current = item.getDepletionAction();
            DepletionAction next;
            if (current == DepletionAction.DISABLE) next = DepletionAction.DESTROY;
            else if (current == DepletionAction.DESTROY) next = DepletionAction.TRANSFORM;
            else next = DepletionAction.DISABLE;

            ConfigManager.setItemValue(itemId, "usage-limit.action", next.name());
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            this.open();
            return;
        }

        if (e.getSlot() == 14 && type == Material.HOPPER) {
            new BuffedItemSelectorMenu(
                    playerMenuUtility,
                    plugin,
                    BuffedItemSelectorMenu.SelectionContext.USAGE_LIMIT
            ).open();
            return;
        }

        else if (e.getSlot() == 16) {
            new CommandListMenu(playerMenuUtility, plugin, CommandListMenu.CommandContext.DEPLETION).open();
            return;
        }

        String configKey = null;
        String inputPath = null;
        String title = null;

        if (e.getSlot() == 28) {
            configKey = "usage-limit.lore";
            inputPath = "usage-limit.lore";
            title = "Usage Lore Line";
        }
        else if (e.getSlot() == 30) {
            configKey = "usage-limit.depleted-lore";
            inputPath = "usage-limit.depleted-lore";
            title = "Depleted Lore Line";
        }
        else if (e.getSlot() == 32) {
            configKey = "usage-limit.depleted-message";
            inputPath = "usage-limit.depleted-message";
            title = "Depleted Denial Message";
        }
        else if (e.getSlot() == 34) {
            configKey = "usage-limit.depletion-notification";
            inputPath = "usage-limit.depletion-notification";
            title = "Break Notification";
        }

        if (configKey != null) {
            if (e.getClick() == ClickType.RIGHT) {
                ConfigManager.setItemValue(itemId, configKey, null);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aReset " + title + " to default."));
                this.open();
            } else {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath(inputPath);
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter new " + title + " in chat."));
                p.sendMessage(ConfigManager.fromSection("§7Placeholders: {remaining_uses}, {total_uses}"));
                p.sendMessage(ConfigManager.fromSection("§7Type 'cancel' to exit."));
            }
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        int maxUses = item.getMaxUses();
        String usageStatus = (maxUses > 0) ? "§e" + maxUses : "§aUnlimited (-1)";
        inventory.setItem(10, makeItem(Material.REDSTONE, "§cMax Usage Limit",
                "§7Current: " + usageStatus, "", "§eClick to Edit"));

        DepletionAction action = item.getDepletionAction();
        Material actionIcon = Material.BARRIER;
        String actionDesc = "§7Item becomes unusable.";
        if (action == DepletionAction.DESTROY) {
            actionIcon = Material.TNT;
            actionDesc = "§cItem is destroyed.";
        } else if (action == DepletionAction.TRANSFORM) {
            actionIcon = Material.CHEST;
            actionDesc = "§bItem transforms.";
        }
        inventory.setItem(12, makeItem(actionIcon, "§6Depletion Action",
                "§7Current: §f" + action.name(), actionDesc, "", "§eClick to Switch"));

        if (action == DepletionAction.TRANSFORM) {
            String targetId = item.getDepletionTransformId();

            String displayName = "§cNone";
            String idLine = "";

            if (targetId != null) {
                BuffedItem targetItem = plugin.getItemManager().getBuffedItem(targetId);
                if (targetItem != null) {
                    displayName = ConfigManager.toSection(ConfigManager.fromLegacy(targetItem.getDisplayName()));
                } else {
                    displayName = "§cUnknown Item";
                }
                idLine = "§8(ID: " + targetId + ")";
            }

            inventory.setItem(14, makeItem(Material.HOPPER, "§dTransform Target",
                    "§7The item to give upon depletion.",
                    "",
                    "§7Current: " + displayName,
                    idLine,
                    "",
                    "§eClick to Select"));
        } else {
            inventory.setItem(14, makeItem(Material.MINECART, "§8Transform Target", "§7(Requires TRANSFORM action)"));
        }

        inventory.setItem(16, makeItem(Material.COMMAND_BLOCK, "§6Depletion Commands",
                "§7Manage commands executed when",
                "§7the item usage reaches 0.",
                "",
                "§7Current: §f" + item.getDepletionCommands().size() + " commands",
                "",
                "§eClick to Edit"));

        inventory.setItem(28, makeMessageItem(Material.PAPER, "Usage Lore", item.getUsageLore(maxUses > 0 ? maxUses : 1), "usage-limit-lore"));
        inventory.setItem(30, makeMessageItem(Material.BOOK, "Depleted Lore", item.getDepletedLore(), "usage-limit-broken-lore"));
        inventory.setItem(32, makeMessageItem(Material.WRITABLE_BOOK, "Denial Message", item.getDepletedMessage(), "usage-limit-depleted-message"));
        inventory.setItem(34, makeMessageItem(Material.BELL, "Break Notification", item.getDepletionNotification(), "usage-limit-break-notification"));

        inventory.setItem(44, makeItem(Material.BARRIER, "§cBack"));
    }

    private org.bukkit.inventory.ItemStack makeMessageItem(Material mat, String title, String currentVal, String defaultKey) {
        return makeItem(mat, "§e" + title,
                "§7Current:", "§r" + ConfigManager.toSection(ConfigManager.fromLegacy(currentVal)),
                "",
                "§eLeft-Click to Edit",
                "§cRight-Click to Reset");
    }
}