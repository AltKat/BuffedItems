package io.github.altkat.BuffedItems.menu.passive;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
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

public class PassiveItemVisualsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public PassiveItemVisualsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Passive Visuals: " + itemId;
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
            new PassiveItemSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }
        
        ClickType click = e.getClick();

        switch (e.getSlot()) {
            case 20:
                if (click == ClickType.SHIFT_LEFT) { toggleMode("action-bar"); return; }
                if (click == ClickType.SHIFT_RIGHT) { toggle("action-bar.enabled"); return; }
                if (click == ClickType.DROP) { resetSection("action-bar", p); return; }
                if (click.isLeftClick()) askForInput(p, "Action Bar Message", "passive_effects.visuals.action-bar.message");
                if (click.isRightClick()) {
                    BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
                    if (item.getPassiveVisuals().getActionBar().getTriggerMode() == io.github.altkat.BuffedItems.utility.item.data.visual.VisualTriggerMode.CONTINUOUS) {
                         p.sendMessage(ConfigManager.fromSectionWithPrefix("§cDuration is only available in ON_EQUIP mode."));
                         return;
                    }
                    askForInput(p, "Action Bar Duration (secs)", "passive_effects.visuals.action-bar.duration");
                }
                break;
            case 22:
                if (click == ClickType.SHIFT_LEFT) { toggle("title.enabled"); return; }
                if (click == ClickType.SHIFT_RIGHT) { resetSection("title", p); return; }
                if (click.isLeftClick()) askForInput(p, "Title (Header|Subtitle)", "passive_effects.visuals.title.header");
                if (click.isRightClick()) askForInput(p, "Title Stay (seconds)", "passive_effects.visuals.title.stay");
                break;
            case 24:
                if (click == ClickType.SHIFT_LEFT) { toggle("sound.enabled"); return; }
                if (click == ClickType.SHIFT_RIGHT) { resetSection("sound", p); return; }
                if (click.isLeftClick()) new SoundSettingsMenu(playerMenuUtility, plugin, "passive").open();
                if (click.isRightClick()) askForInput(p, "Sound Delay (seconds)", "passive_effects.visuals.sound.delay");
                break;
            case 31:
                new BossBarSettingsMenu(playerMenuUtility, plugin).open();
                break;
            case 33:
                playerMenuUtility.setTargetSlot("PASSIVE_VISUALS");
                new ParticleListMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    private void toggle(String path) {
        String fullPath = "passive_effects.visuals." + path;
        boolean currentState = false; 
        if(path.contains("action-bar")) currentState = plugin.getItemManager().getLoadedItems().get(itemId).getPassiveVisuals().getActionBar().isEnabled();
        if(path.contains("title")) currentState = plugin.getItemManager().getLoadedItems().get(itemId).getPassiveVisuals().getTitle().isEnabled();
        if(path.contains("sound")) currentState = plugin.getItemManager().getLoadedItems().get(itemId).getPassiveVisuals().getSound().isEnabled();

        ConfigManager.setItemValue(itemId, fullPath, !currentState);
        this.open();
    }
    
    private void toggleMode(String section) {
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        VisualTriggerMode current = VisualTriggerMode.CONTINUOUS;
        
        if (section.equals("action-bar")) current = item.getPassiveVisuals().getActionBar().getTriggerMode();
        // Add others if needed later

        VisualTriggerMode next = current == VisualTriggerMode.CONTINUOUS ? VisualTriggerMode.ON_EQUIP : VisualTriggerMode.CONTINUOUS;
        ConfigManager.setItemValue(itemId, "passive_effects.visuals." + section + ".mode", next.name());
        this.open();
    }
    
    private void resetSection(String section, Player p) {
        ConfigManager.setItemValue(itemId, "passive_effects.visuals." + section, null);
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

        PassiveVisuals visuals = item.getPassiveVisuals();

        ActionBarSettings ab = visuals.getActionBar();
        String abDurationAction = ab.getTriggerMode() == io.github.altkat.BuffedItems.utility.item.data.visual.VisualTriggerMode.CONTINUOUS
                ? "§7Locked (Req. ON_EQUIP)" : "Edit Duration";

        inventory.setItem(20, createSmartIcon(
                Material.IRON_BARS, "Action Bar", ab.isEnabled(),
                new String[]{"Mode: §e" + ab.getTriggerMode().name(),
                             "Message: §f" + (ab.getMessage() != null ? ConfigManager.toSection(ConfigManager.fromLegacy(ab.getMessage())) : "None"),
                             "Duration (ON_EQUIP): §f" + ab.getDuration() + "s"},
                "Edit Message", abDurationAction
        ));

        TitleSettings title = visuals.getTitle();
        inventory.setItem(22, createSmartIcon2(
                Material.NAME_TAG, "Title Alert", title.isEnabled(),
                new String[]{"Header: §f" + (title.getHeader() != null ? ConfigManager.toSection(ConfigManager.fromLegacy(title.getHeader())) : "None"),
                             "Subtitle: §f" + (title.getSubtitle() != null ? ConfigManager.toSection(ConfigManager.fromLegacy(title.getSubtitle())) : "None"),
                             "Stay Time: §f" + (title.getStay() / 20.0) + "s"},
                "Edit Header/Sub", "Edit Stay Time"
        ));
        
        SoundSettings sound = visuals.getSound();
        inventory.setItem(24, createSmartIcon2(
                Material.JUKEBOX, "Equip Sound", sound.isEnabled(),
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
                "§7this item.", "",
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
        lore.add(ConfigManager.fromSection("§aShift-Left-Click: §fToggle Mode"));
        lore.add(ConfigManager.fromSection("§cShift-Right-Click: §fToggle Enabled"));
        lore.add(ConfigManager.fromSection("§4Drop (Q): §fReset Section"));

        if (enabled) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createSmartIcon2(Material mat, String name, boolean enabled, String[] stats, String leftClick, String rightClick) {
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
