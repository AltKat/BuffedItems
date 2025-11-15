package io.github.altkat.BuffedItems.menu.utility;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GeneralSettingsMenu extends Menu {

    private final BuffedItems plugin;

    public GeneralSettingsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "General Settings";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            case OBSERVER:
                int currentLevel = ConfigManager.getDebugLevel();
                int nextLevel = (currentLevel >= 4) ? 0 : currentLevel + 1;
                ConfigManager.setDebugLevel(nextLevel);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                this.open();
                break;

            case POTION:
                boolean currentShow = ConfigManager.shouldShowPotionIcons();
                ConfigManager.setShowPotionIcons(!currentShow);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                this.open();
                break;

            case BARRIER:
                new MainMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        int currentDebug = ConfigManager.getDebugLevel();
        String debugStatusColor = (currentDebug == 0) ? "§c" : (currentDebug < 3) ? "§e" : "§a";
        String debugName = getDebugLevelName(currentDebug);

        List<String> debugLore = new ArrayList<>();
        debugLore.add("§7Controls the verbosity of console logs.");
        debugLore.add("");
        debugLore.add("§7Current Level: " + debugStatusColor + currentDebug + " (" + debugName + ")");
        debugLore.add("");
        debugLore.add("§fLevels:");
        debugLore.add((currentDebug == 0 ? "§a" : "§7") + " 0: OFF §8(Errors only)");
        debugLore.add((currentDebug == 1 ? "§a" : "§7") + " 1: INFO §8(Startup/Shutdown/Reload)");
        debugLore.add((currentDebug == 2 ? "§a" : "§7") + " 2: TASK §8(Periodic tasks)");
        debugLore.add((currentDebug == 3 ? "§a" : "§7") + " 3: DETAILED §8(Per-player operations)");
        debugLore.add((currentDebug == 4 ? "§a" : "§7") + " 4: VERBOSE §8(Spammy - Dev only)");
        debugLore.add("");
        debugLore.add("§eClick to cycle levels.");

        inventory.setItem(11, makeItem(Material.OBSERVER, "§6Debug Level", debugLore.toArray(new String[0])));

        boolean showIcons = ConfigManager.shouldShowPotionIcons();
        String iconsStatus = showIcons ? "§aVisible" : "§cHidden";

        List<String> iconsLore = new ArrayList<>();
        iconsLore.add("§7Determines if custom potion effects");
        iconsLore.add("§7should appear in the top-right corner");
        iconsLore.add("§7of the player's screen.");
        iconsLore.add("");
        iconsLore.add("§7Current: " + iconsStatus);
        iconsLore.add("");
        iconsLore.add("§eClick to toggle.");

        ItemStack potionItem = makeItem(Material.POTION, "§bShow Potion Icons", iconsLore.toArray(new String[0]));
        ItemMeta meta = potionItem.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ATTRIBUTES);
        potionItem.setItemMeta(meta);

        inventory.setItem(15, potionItem);

        addBackButton(new MainMenu(playerMenuUtility, plugin));
    }

    private String getDebugLevelName(int level) {
        switch (level) {
            case 0: return "OFF";
            case 1: return "INFO";
            case 2: return "TASK";
            case 3: return "DETAILED";
            case 4: return "VERBOSE";
            default: return "UNKNOWN";
        }
    }
}