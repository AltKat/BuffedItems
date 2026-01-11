package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.BuffedItemSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.menu.utility.SoundSettingsMenu;
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
            playerMenuUtility.setChatInputPath("usage.limit");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Max Usage amount in chat (e.g. 50)."));
            p.sendMessage(ConfigManager.fromSection("§7Current: " + (item.getUsageDetails().getMaxUses() > 0 ? item.getUsageDetails().getMaxUses() : "Unlimited (-1)")));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit."));
            return;
        }

        if (e.getSlot() == 12) {
            DepletionAction current = item.getUsageDetails().getDepletionAction();
            DepletionAction next;
            if (current == DepletionAction.DISABLE) next = DepletionAction.DESTROY;
            else if (current == DepletionAction.DESTROY) next = DepletionAction.TRANSFORM;
            else next = DepletionAction.DISABLE;

            ConfigManager.setItemValue(itemId, "usage.action", next.name());
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
        } else if (e.getSlot() == 16) {
            new CommandListMenu(playerMenuUtility, plugin, CommandListMenu.CommandContext.DEPLETION).open();
            return;
        } else if (e.getSlot() == 32) {
            new SoundSettingsMenu(playerMenuUtility, plugin, "depletion").open();
            return;
        } else if (e.getSlot() == 34) {
            new SoundSettingsMenu(playerMenuUtility, plugin, "depleted-try").open();
            return;
        }


        String configKey = null;
        String inputPath = null;
        String title = null;

        if (e.getSlot() == 20) {
            configKey = "usage.lore_format";
            inputPath = "usage.lore_format";
            title = "Usage Lore Line";
        } else if (e.getSlot() == 22) {
            configKey = "usage.depleted_lore";
            inputPath = "usage.depleted_lore";
            title = "Depleted Lore Line";
        } else if (e.getSlot() == 24) {
            configKey = "usage.depleted_message";
            inputPath = "usage.depleted_message";
            title = "Depleted Denial Message";
        } else if (e.getSlot() == 28) {
            configKey = "usage.depletion_notification";
            inputPath = "usage.depletion_notification";
            title = "Break Notification";
        } else if (e.getSlot() == 30) {
            configKey = "usage.transform_message";
            inputPath = "usage.transform_message";
            title = "Transform Message";
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
                if (configKey.equals("usage.lore_format")) {
                    p.sendMessage(ConfigManager.fromSection("§7Placeholders: {remaining_uses}, {total_uses}"));
                }
                p.sendMessage(ConfigManager.fromSection("§7(Type 'NONE' to disable this message)"));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit."));
            }
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        int maxUses = item.getUsageDetails().getMaxUses();
        String usageStatus = (maxUses > 0) ? "§e" + maxUses : "§aUnlimited (-1)";
        inventory.setItem(10, makeItem(Material.REDSTONE, "§cMax Usage Limit",
                "§7Current: " + usageStatus, "", "§eClick to Edit"));

        DepletionAction action = item.getUsageDetails().getDepletionAction();
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
            String targetId = item.getUsageDetails().getTransformId();
            String displayName = "§cNone";
            String idLine = "";

            if (targetId != null && !targetId.isEmpty()) {
                BuffedItem targetItem = plugin.getItemManager().getBuffedItem(targetId);
                displayName = (targetItem != null)
                        ? ConfigManager.toSection(ConfigManager.fromLegacy(targetItem.getItemDisplay().getDisplayName()))
                        : "§cUnknown Item";
                idLine = "§8(ID: " + targetId + ")";
            }

            inventory.setItem(14, makeItem(Material.HOPPER, "§dTransform Target",
                    "§7The item to give upon depletion.", "", "§7Current: " + displayName, idLine, "", "§eClick to Select"));
        } else {
            inventory.setItem(14, makeItem(Material.MINECART, "§8Transform Target", "§7(Requires TRANSFORM action)"));
        }

        inventory.setItem(16, makeItem(Material.COMMAND_BLOCK, "§6Depletion Commands",
                "§7Manage commands executed when", "§7the item usage reaches 0.", "",
                "§7Current: §f" + item.getUsageDetails().getDepletionCommands().size() + " commands", "", "§eClick to Edit"));

        // Messages with default fallbacks
        String localUsageLore = item.getUsageDetails().getUsageLore();
        String displayUsageLore;
        if (localUsageLore == null || localUsageLore.isEmpty() || localUsageLore.equalsIgnoreCase("NONE")) {
            if (localUsageLore != null && localUsageLore.equalsIgnoreCase("NONE")) displayUsageLore = "§cDISABLED";
            else {
                String global = ConfigManager.getGlobalUsageLore();
                if (global == null) displayUsageLore = "§cDISABLED";
                else {
                    displayUsageLore = global.replace("{remaining_uses}", String.valueOf(maxUses > 0 ? maxUses : 1))
                            .replace("{total_uses}", String.valueOf(maxUses > 0 ? maxUses : 1)) + " &8(Default)";
                }
            }
        } else {
            displayUsageLore = item.getUsageLore(maxUses > 0 ? maxUses : 1);
        }
        inventory.setItem(20, makeMessageItem(Material.PAPER, "Usage Lore", displayUsageLore, "usage.lore_format"));

        String depletedLore = item.getUsageDetails().getDepletedLore();
        String displayDepletedLore;
        if (depletedLore == null || depletedLore.isEmpty() || depletedLore.equalsIgnoreCase("NONE")) {
            if (depletedLore != null && depletedLore.equalsIgnoreCase("NONE")) displayDepletedLore = "§cDISABLED";
            else {
                String global = ConfigManager.getGlobalDepletedLore();
                displayDepletedLore = (global != null) ? global + " &8(Default)" : "§cDISABLED";
            }
        } else {
            displayDepletedLore = depletedLore;
        }
        inventory.setItem(22, makeMessageItem(Material.BOOK, "Depleted Lore", displayDepletedLore, "usage.depleted_lore"));

        String depletedMsg = item.getUsageDetails().getDepletedMessage();
        String displayDepletedMsg;
        if (depletedMsg == null || depletedMsg.isEmpty() || depletedMsg.equalsIgnoreCase("NONE")) {
            if (depletedMsg != null && depletedMsg.equalsIgnoreCase("NONE")) displayDepletedMsg = "§cDISABLED";
            else {
                String global = ConfigManager.getGlobalDepletedMessage();
                displayDepletedMsg = (global != null) ? global + " &8(Default)" : "§cDISABLED";
            }
        } else {
            displayDepletedMsg = depletedMsg;
        }
        inventory.setItem(24, makeMessageItem(Material.WRITABLE_BOOK, "Denial Message", displayDepletedMsg, "usage.depleted_message"));

        String depletionNotif = item.getUsageDetails().getDepletionNotification();
        String displayDepletionNotif;
        if (depletionNotif == null || depletionNotif.isEmpty() || depletionNotif.equalsIgnoreCase("NONE")) {
            if (depletionNotif != null && depletionNotif.equalsIgnoreCase("NONE")) displayDepletionNotif = "§cDISABLED";
            else {
                String global = ConfigManager.getGlobalDepletionNotification();
                displayDepletionNotif = (global != null) ? global + " &8(Default)" : "§cDISABLED";
            }
        } else {
            displayDepletionNotif = depletionNotif;
        }
        inventory.setItem(28, makeMessageItem(Material.BELL, "Break Notification", displayDepletionNotif, "usage.depletion_notification"));

        String transformMsg = item.getUsageDetails().getDepletionTransformMessage();
        String displayTransformMsg;
        if (transformMsg == null || transformMsg.isEmpty() || transformMsg.equalsIgnoreCase("NONE")) {
            if (transformMsg != null && transformMsg.equalsIgnoreCase("NONE")) displayTransformMsg = "§cDISABLED";
            else {
                String global = ConfigManager.getGlobalDepletionTransformMessage();
                displayTransformMsg = (global != null) ? global + " &8(Default)" : "§cDISABLED";
            }
        } else {
            displayTransformMsg = transformMsg;
        }
        inventory.setItem(30, makeMessageItem(Material.CHERRY_SIGN, "Transform Message", displayTransformMsg, "usage.transform_message"));


        // Sounds with default fallbacks
        String currDepletion = item.getUsageDetails().getDepletionSound();
        if (currDepletion == null) currDepletion = "§5" + ConfigManager.getGlobalDepletionSound() + " §8(Default)";
        else currDepletion = "§5" + currDepletion;
        inventory.setItem(32, makeItem(Material.JUKEBOX, "§5Depletion Sound", "§7Current: " + currDepletion, "", "§eClick to Change"));

        String currTry = item.getUsageDetails().getDepletedTrySound();
        if (currTry == null) currTry = "§7" + ConfigManager.getGlobalDepletedTrySound() + " §8(Default)";
        else currTry = "§7" + currTry;
        inventory.setItem(34, makeItem(Material.DISPENSER, "§8Depleted Try Sound", "§7Current: " + currTry, "", "§eClick to Change"));


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
