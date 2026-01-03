package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.editor.ItemEditorMenu;
import io.github.altkat.BuffedItems.menu.passive.EffectListMenu;
import io.github.altkat.BuffedItems.menu.utility.ItemListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ActiveItemSettingsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public ActiveItemSettingsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Active Settings: " + itemId;
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
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);

        if (item == null) {
            new ItemListMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (type == Material.BARRIER && e.getSlot() == 44) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        switch (type) {
            case LEVER:
                boolean newMode = !item.getActiveAbility().isEnabled();
                ConfigManager.setItemValue(itemId, "active_ability.enabled", newMode);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                this.open();
                break;

            case CLOCK:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("active_ability.cooldown");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter the Cooldown (in seconds) in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(Current: " + item.getActiveAbility().getCooldown() + "s)"));
                break;

            case COMPASS:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("active_ability.duration");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter the Effect Duration (in seconds) in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(Current: " + item.getActiveAbility().getDuration() + "s)"));
                break;

            case COMMAND_BLOCK:
                new CommandListMenu(playerMenuUtility, plugin, CommandListMenu.CommandContext.ACTIVE).open();
                break;

            case LINGERING_POTION:
                playerMenuUtility.setTargetSlot("ACTIVE");
                new EffectListMenu(playerMenuUtility, plugin,
                        EffectListMenu.EffectType.POTION_EFFECT, "ACTIVE").open();
                break;

            case NETHER_STAR:
                playerMenuUtility.setTargetSlot("ACTIVE");
                new EffectListMenu(playerMenuUtility, plugin,
                        EffectListMenu.EffectType.ATTRIBUTE, "ACTIVE").open();
                break;

            case JUKEBOX:
                new ActiveItemSoundsMenu(playerMenuUtility, plugin).open();
                break;

            case PAINTING:
                new ActiveItemVisualsMenu(playerMenuUtility, plugin).open();
                break;

            case EMERALD:
                new CostListMenu(playerMenuUtility, plugin).open();
                break;

            case DIAMOND:
                new UsageLimitSettingsMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        boolean isActive = item.getActiveAbility().isEnabled();
        inventory.setItem(10, makeItem(Material.LEVER,
                isActive ? "§aActive Mode: ON" : "§cActive Mode: OFF",
                "§7Enable/Disable right-click functionality.",
                "",
                "§eClick to Toggle"));

        inventory.setItem(12, makeItem(Material.CLOCK, "§bSet Cooldown",
                "§7Current: §e" + item.getActiveAbility().getCooldown() + "s",
                "",
                "§eClick to Edit"));

        inventory.setItem(14, makeItem(Material.COMPASS, "§bSet Effect Duration",
                "§7Current: §e" + item.getActiveAbility().getDuration() + "s",
                "",
                "§eClick to Edit"));

        inventory.setItem(16, makeItem(Material.COMMAND_BLOCK, "§6Manage Commands",
                "§7Current: §e" + item.getActiveAbility().getCommands().size() + " commands",
                "",
                "§eClick to Edit List"));

        inventory.setItem(19, makeItem(Material.LINGERING_POTION, "§dActive Potion Effects",
                "§7Manage potion effects applied", "§7when used.",
                "",
                "§eClick to Edit"));

        inventory.setItem(21, makeItem(Material.NETHER_STAR, "§bActive Attributes",
                "§7Manage temporary attributes", "§7given when used.",
                "",
                "§eClick to Edit"));

        inventory.setItem(23, makeItem(Material.PAINTING, "§eVisual Settings",
                "§7Configure visual indicators:",
                "§f• Cast Visuals",
                "§f• Cooldown Visuals",
                "",
                "§eClick to Edit"));

        inventory.setItem(25, makeItem(Material.JUKEBOX, "§6Sound Settings",
                "§7Configure item sounds:",
                "§f• Success Sound",
                "§f• Cooldown Sound",
                "§f• Cost Fail Sound",
                "",
                "§eClick to Edit"));

        inventory.setItem(28, makeItem(Material.EMERALD, "§aUsage Costs",
                "§7Configure requirements to use this item.",
                "§f• Money, XP, Items, Health...",
                "",
                "§eClick to Manage"));

        inventory.setItem(30, makeItem(Material.DIAMOND, "§aUsage Limits",
                "§7Limit how many times this item's",
                "§7ability can be used.",
                "",
                "§f• Max Usage & Actions",
                "§f• Usage Lore",
                "§f• Depletion Messages",
                "§f• Depletion Sounds",
                "§f• Transformation Logic",
                "",
                "§eClick to Manage"));

        inventory.setItem(44, makeItem(Material.BARRIER, "§cBack"));

        setFillerGlass();
    }
}