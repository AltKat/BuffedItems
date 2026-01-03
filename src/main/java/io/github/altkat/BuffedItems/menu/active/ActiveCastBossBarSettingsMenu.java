package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.BossBarColorMenu;
import io.github.altkat.BuffedItems.menu.selector.BossBarStyleMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.data.visual.BossBarSettings;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ActiveCastBossBarSettingsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public ActiveCastBossBarSettingsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Cast Boss Bar: " + itemId;
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;
        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new ActiveItemCastVisualsMenu(playerMenuUtility, plugin).open();
            return;
        }

        String path = "";
        switch (e.getSlot()) {
            case 10:
                path = "active_ability.visuals.cast.boss-bar.enabled";
                boolean currentState = ItemsConfig.get().getBoolean("items." + itemId + "." + path, false);
                ConfigManager.setItemValue(itemId, path, !currentState);
                this.open();
                return;
            case 12:
                askForInput(p, "Boss Bar Title", "active_ability.visuals.cast.boss-bar.title");
                return;
            case 13:
                askForInput(p, "Boss Bar Duration (seconds)", "active_ability.visuals.cast.boss-bar.duration");
                return;
            case 14:
                askForInput(p, "Boss Bar Delay (seconds)", "active_ability.visuals.cast.boss-bar.delay");
                return;
            case 16:
                playerMenuUtility.setChatInputPath("active_cast_visuals");
                if (e.isLeftClick()) new BossBarColorMenu(playerMenuUtility, plugin).open();
                else new BossBarStyleMenu(playerMenuUtility, plugin).open();
                return;
        }
    }

    private void askForInput(Player p, String title, String path) {
        playerMenuUtility.setWaitingForChatInput(true);
        playerMenuUtility.setChatInputPath(path);
        p.closeInventory();
        p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter new " + title + " in chat."));
        p.sendMessage(ConfigManager.fromSection("§7Type 'none' to remove, 'cancel' to exit."));
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        inventory.setItem(26, makeItem(Material.BARRIER, "§cBack"));

        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;
        // Access cast visuals boss bar settings
        BossBarSettings settings = item.getActiveAbility().getVisuals().getCast().getBossBar();

        inventory.setItem(10, createToggle(settings.isEnabled()));

        inventory.setItem(12, makeItem(Material.WRITABLE_BOOK, "§eEdit Title",
                "§7Current: §f" + (settings.getTitle() != null ? ConfigManager.toSection(ConfigManager.fromLegacy(settings.getTitle())) : "(Item Name)")));

        inventory.setItem(13, makeItem(Material.CLOCK, "§eEdit Duration",
                "§7In seconds.", "§7Current: §f" + settings.getDuration() + "s"));

        inventory.setItem(14, makeItem(Material.ENDER_PEARL, "§eEdit Delay",
                "§7In seconds.", "§7Current: §f" + (settings.getDelay() / 20.0) + "s"));

        inventory.setItem(16, makeItem(Material.BEACON, "§6Edit Look",
                "§7Color: §f" + settings.getColor().name(),
                "§7Style: §f" + settings.getStyle().name(), "",
                "§eLeft-Click for Color", "§eRight-Click for Style"));
    }

    private ItemStack createToggle(boolean state) {
        Material mat = state ? Material.LIME_DYE : Material.GRAY_DYE;
        String status = state ? "§aEnabled" : "§cDisabled";
        ItemStack item = makeItem(mat, "§6" + "Boss Bar", "§7Status: " + status, "", "§eClick to Toggle");
        if (state) {
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}
