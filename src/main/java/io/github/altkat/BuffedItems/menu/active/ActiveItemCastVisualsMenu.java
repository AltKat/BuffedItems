package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.passive.BossBarSettingsMenu;
import io.github.altkat.BuffedItems.menu.utility.ItemListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.menu.utility.SoundSettingsMenu;
import io.github.altkat.BuffedItems.menu.visual.ParticleListMenu;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.data.visual.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ActiveItemCastVisualsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public ActiveItemCastVisualsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Cast Visuals: " + itemId;
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new ActiveItemVisualsMenu(playerMenuUtility, plugin).open();
            return;
        }

        ClickType click = e.getClick();
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) {
            new ItemListMenu(playerMenuUtility, plugin).open();
            return;
        }

        switch (e.getSlot()) {
            case 20:
                if (click == ClickType.SHIFT_LEFT) { toggle("action-bar.enabled"); return; }
                if (click == ClickType.SHIFT_RIGHT) { resetSection("action-bar", p); return; }
                if (click.isLeftClick()) askForInput(p, "Action Bar Message", "active_ability.visuals.cast.action-bar.message");
                if (click.isRightClick()) askForInput(p, "Action Bar Duration (secs)", "active_ability.visuals.cast.action-bar.duration");
                break;
            case 22:
                if (click == ClickType.SHIFT_LEFT) { toggle("title.enabled"); return; }
                if (click == ClickType.SHIFT_RIGHT) { resetSection("title", p); return; }
                if (click.isLeftClick()) askForInput(p, "Title (Header|Subtitle)", "active_ability.visuals.cast.title.header");
                if (click.isRightClick()) askForInput(p, "Title Stay (seconds)", "active_ability.visuals.cast.title.stay");
                break;
            case 24:
                if (click == ClickType.SHIFT_LEFT) { toggle("sound.enabled"); return; }
                if (click == ClickType.SHIFT_RIGHT) { resetSection("sound", p); return; }
                if (click.isLeftClick()) new SoundSettingsMenu(playerMenuUtility, plugin, "cast").open();
                if (click.isRightClick()) askForInput(p, "Sound Delay (seconds)", "active_ability.visuals.cast.sound.delay");
                break;
            case 31:
                new ActiveCastBossBarSettingsMenu(playerMenuUtility, plugin).open();
                break;
            case 33:
                playerMenuUtility.setTargetSlot("CAST_VISUALS");
                new ParticleListMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    private void toggle(String path) {
        String fullPath = "active_ability.visuals.cast." + path;
        boolean currentState = false;
        CastVisuals visuals = plugin.getItemManager().getLoadedItems().get(itemId).getActiveAbility().getVisuals().getCast();
        
        if(path.contains("action-bar")) currentState = visuals.getActionBar().isEnabled();
        if(path.contains("title")) currentState = visuals.getTitle().isEnabled();
        if(path.contains("sound")) currentState = visuals.getSound().isEnabled();

        ConfigManager.setItemValue(itemId, fullPath, !currentState);
        this.open();
    }
    
    private void resetSection(String section, Player p) {
        ConfigManager.setItemValue(itemId, "active_ability.visuals.cast." + section, null);
        p.sendMessage(ConfigManager.fromSectionWithPrefix("§aReset " + section + " settings."));
        this.open();
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
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        setFillerGlass();
        inventory.setItem(44, makeItem(Material.BARRIER, "§cBack"));

        CastVisuals visuals = item.getActiveAbility().getVisuals().getCast();

        ActionBarSettings ab = visuals.getActionBar();
        inventory.setItem(20, createSmartIcon(
                Material.IRON_BARS, "Action Bar", ab.isEnabled(),
                new String[]{"Message: §f" + (ab.getMessage() != null ? ConfigManager.toSection(ConfigManager.fromLegacy(ab.getMessage())) : "None"),
                             "Duration: §f" + ab.getDuration() + "s"},
                "Edit Message", "Edit Duration"
        ));

        TitleSettings title = visuals.getTitle();
        inventory.setItem(22, createSmartIcon(
                Material.NAME_TAG, "Title Alert", title.isEnabled(),
                new String[]{"Header: §f" + (title.getHeader() != null ? ConfigManager.toSection(ConfigManager.fromLegacy(title.getHeader())) : "None"),
                             "Subtitle: §f" + (title.getSubtitle() != null ? ConfigManager.toSection(ConfigManager.fromLegacy(title.getSubtitle())) : "None"),
                             "Stay Time: §f" + (title.getStay() / 20.0) + "s"},
                "Edit Header/Sub", "Edit Stay Time"
        ));
        
        SoundSettings sound = visuals.getSound();
        inventory.setItem(24, createSmartIcon(
                Material.JUKEBOX, "Cast Sound", sound.isEnabled(),
                new String[]{"Sound: §f" + (sound.getSound() != null ? sound.getSound() : "None"),
                             "Delay: §f" + (sound.getDelay() / 20.0) + "s"},
                "Select Sound", "Edit Delay"
        ));

        inventory.setItem(31, makeItem(Material.BEACON, "§cBoss Bar Settings",
                "§7Configure the Boss Bar's title,",
                "§7color, style, and other options",
                "§7in a dedicated menu.", "",
                "§eClick to Open"));

        inventory.setItem(33, makeItem(Material.FIREWORK_STAR, "§dParticle Effects",
                "§7Configure particle effects for",
                "§7this ability.", "",
                "§eClick to Open"));
    }

    private ItemStack createSmartIcon(Material mat, String name, boolean enabled, String[] stats, String leftClick, String rightClick) {
        ItemStack item = makeItem(mat, "§6" + name + (enabled ? " §a(Enabled)" : " §c(Disabled)"));
        ItemMeta meta = item.getItemMeta();
        java.util.List<Component> lore = new java.util.ArrayList<>();
        
        for(String stat : stats) {
            lore.add(ConfigManager.fromSection("§7" + stat));
        }
        lore.add(ConfigManager.fromSection(""));
        lore.add(ConfigManager.fromSection("§eLeft-Click: §f" + leftClick));
        lore.add(ConfigManager.fromSection("§bRight-Click: §f" + rightClick));
        lore.add(ConfigManager.fromSection("§aShift-Left-Click: §fToggle Enabled"));
        lore.add(ConfigManager.fromSection("§cShift-Right-Click: §fReset All"));

        if (enabled) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
