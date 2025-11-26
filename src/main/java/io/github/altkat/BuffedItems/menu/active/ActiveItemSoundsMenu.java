package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.menu.utility.SoundSettingsMenu;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ActiveItemSoundsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public ActiveItemSoundsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Sound Settings: " + itemId;
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        Material type = e.getCurrentItem().getType();

        if (type == Material.BARRIER && e.getSlot() == 26) {
            new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() == 10) {
            new SoundSettingsMenu(playerMenuUtility, plugin, "success").open();
        }

        else if (e.getSlot() == 12) {
            new SoundSettingsMenu(playerMenuUtility, plugin, "cost-fail").open();
        }

        else if (e.getSlot() == 14) {
            new SoundSettingsMenu(playerMenuUtility, plugin, "cooldown").open();
        }

        else if (e.getSlot() == 16) {
            new SoundSettingsMenu(playerMenuUtility, plugin, "depletion").open();
        }
    }

    @Override
    public void setMenuItems() {
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        setFillerGlass();

        String currSuccess = item.getCustomSuccessSound();
        if (currSuccess == null) {
            currSuccess = "§a" + ConfigManager.getGlobalSuccessSound() + " §8(Default)";
        } else {
            currSuccess = "§a" + currSuccess;
        }

        inventory.setItem(10, makeItem(Material.EXPERIENCE_BOTTLE, "§aSuccess Sound",
                "§7Sound played on successful use.",
                "§7Current: " + currSuccess,
                "",
                "§eClick to Change"));


        String currCostFail = item.getCustomCostFailSound();
        if (currCostFail == null) {
            currCostFail = "§6" + ConfigManager.getGlobalCostFailSound() + " §8(Default)";
        } else {
            currCostFail = "§6" + currCostFail;
        }

        inventory.setItem(12, makeItem(Material.REDSTONE, "§6Cost Fail Sound",
                "§7Sound played when a cost is not met.",
                "§7Current: " + currCostFail,
                "",
                "§eClick to Change"));



        String currCool = item.getCustomCooldownSound();
        if (currCool == null) {
            currCool = "§c" + ConfigManager.getGlobalCooldownSound() + " §8(Default)";
        } else {
            currCool = "§c" + currCool;
        }

        inventory.setItem(14, makeItem(Material.ANVIL, "§cCooldown Sound",
                "§7Sound played when on cooldown.",
                "§7Current: " + currCool,
                "",
                "§eClick to Change"));

        String currDepletion = item.getCustomDepletionSound();
        if (currDepletion == null) {
            currDepletion = "§5" + ConfigManager.getGlobalDepletionSound() + " §8(Default)";
        } else {
            currDepletion = "§5" + currDepletion;
        }

        inventory.setItem(16, makeItem(Material.JUKEBOX, "§5Depletion Sound",
                "§7Sound played when item breaks/depletes.",
                "§7Current: " + currDepletion,
                "",
                "§eClick to Change"));

        inventory.setItem(26, makeItem(Material.BARRIER, "§cBack"));
    }
}