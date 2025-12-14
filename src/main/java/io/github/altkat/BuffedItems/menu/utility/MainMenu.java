package io.github.altkat.BuffedItems.menu.utility;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.crafting.RecipeListMenu;
import io.github.altkat.BuffedItems.menu.set.SetListMenu;
import io.github.altkat.BuffedItems.menu.upgrade.UpgradeRecipeListMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MainMenu extends Menu {

    private final BuffedItems plugin;

    public MainMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        playerMenuUtility.flushData();
    }

    @Override
    public String getMenuName() {
        return "BuffedItems > Dashboard";
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

        if (type == Material.GRAY_DYE) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§cThis feature is currently disabled in config."));
            return;
        }

        switch (e.getSlot()) {
            case 11:
                new ItemListMenu(playerMenuUtility, plugin).open();
                break;

            case 13:
                new SetListMenu(playerMenuUtility, plugin).open();
                break;

            case 15:
                new UpgradeRecipeListMenu(playerMenuUtility, plugin).open();
                break;

            case 21:
                new RecipeListMenu(playerMenuUtility, plugin).open();
                break;

            case 23:
                new GeneralSettingsMenu(playerMenuUtility, plugin).open();
                break;

            case 31:
                handlePluginInfo(p);
                break;

            case 44:
                p.closeInventory();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        inventory.setItem(11, makeItem(Material.DIAMOND_SWORD, "§bItem Editor",
                "§7Create, edit and manage",
                "§7your custom items.",
                "",
                "§eClick to Open"));

        if (SetsConfig.get().getBoolean("settings.enabled", true)) {
            inventory.setItem(13, makeItem(Material.GOLDEN_CHESTPLATE, "§6Item Sets",
                    "§7Create armor sets with",
                    "§7tiered bonuses.",
                    "",
                    "§eClick to Manage"));
        } else {
            inventory.setItem(13, makeDisabledItem("Item Sets"));
        }

        if (UpgradesConfig.get().getBoolean("settings.enabled", true)) {
            inventory.setItem(15, makeItem(Material.SMITHING_TABLE, "§eUpgrades",
                    "§7Create upgrade recipes and",
                    "§7evolution paths.",
                    "",
                    "§eClick to Manage"));
        } else {
            inventory.setItem(15, makeDisabledItem("Upgrades"));
        }

        if (RecipesConfig.get().getBoolean("settings.enabled", true)) {
            inventory.setItem(21, makeItem(Material.CRAFTING_TABLE, "§aCustom Crafting",
                    "§7Create shaped recipes for",
                    "§7your custom items.",
                    "",
                    "§eClick to Manage"));
        } else {
            inventory.setItem(21, makeDisabledItem("Custom Crafting"));
        }

        inventory.setItem(23, makeItem(Material.COMPARATOR, "§7General Settings",
                "§7Configure global settings",
                "§7(Debug, Visuals, etc.)",
                "",
                "§eClick to Open"));

        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Version: §f" + plugin.getDescription().getVersion());
        infoLore.add("");
        infoLore.add("§bLinks:");
        infoLore.add("§f• Wiki / Docs");
        infoLore.add("§f• Discord Support");
        infoLore.add("");
        infoLore.add("§eClick to print links in chat.");
        inventory.setItem(31, makeItem(Material.BOOK, "§dPlugin Information", infoLore.toArray(new String[0])));

        inventory.setItem(44, makeItem(Material.BARRIER, "§cClose Menu"));
    }

    private ItemStack makeDisabledItem(String name) {
        ItemStack item = makeItem(Material.GRAY_DYE, "§7" + name,
                "",
                "§c[DISABLED]",
                "§7Enable in config to use.");
        return item;
    }

    private void handlePluginInfo(Player p) {
        p.closeInventory();
        p.sendMessage(ConfigManager.fromSection("§8§m-------------------------------------------"));
        p.sendMessage(ConfigManager.fromSection("§6§lBuffedItems Information"));
        p.sendMessage(Component.empty());

        Component wikiLink = Component.text("Click Here", NamedTextColor.YELLOW)
                .decoration(TextDecoration.UNDERLINED, true)
                .clickEvent(ClickEvent.openUrl("https://github.com/AltKat/BuffedItems/wiki"))
                .hoverEvent(HoverEvent.showText(ConfigManager.fromSection("§7Click to open the Wiki page.")));

        p.sendMessage(ConfigManager.fromSection("§bWiki & Docs: ").append(wikiLink));
        p.sendMessage(Component.empty());

        Component discordLink = Component.text("Click Here", NamedTextColor.YELLOW)
                .decoration(TextDecoration.UNDERLINED, true)
                .clickEvent(ClickEvent.openUrl("https://discord.gg/nxY3fc7xz9"))
                .hoverEvent(HoverEvent.showText(ConfigManager.fromSection("§7Click to join our Discord server.")));

        p.sendMessage(ConfigManager.fromSection("§9Discord Support: ").append(discordLink));
        p.sendMessage(ConfigManager.fromSection("§8§m-------------------------------------------"));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }
}