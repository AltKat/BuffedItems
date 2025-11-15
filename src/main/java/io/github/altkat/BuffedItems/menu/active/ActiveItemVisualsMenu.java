package io.github.altkat.BuffedItems.menu.active;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.BossBarColorMenu;
import io.github.altkat.BuffedItems.menu.selector.BossBarStyleMenu;
import io.github.altkat.BuffedItems.menu.utility.MainMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ActiveItemVisualsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public ActiveItemVisualsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Visual Settings: " + itemId;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        Material type = e.getCurrentItem().getType();
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);

        if (item == null) {
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (type == Material.BARRIER && e.getSlot() == 49) {
            new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }

        switch (type) {
            case PAPER:
                ConfigManager.setItemValue(itemId, "visuals.chat", !item.isVisualChat());
                this.open();
                break;
            case NAME_TAG:
                ConfigManager.setItemValue(itemId, "visuals.title", !item.isVisualTitle());
                this.open();
                break;
            case IRON_BARS:
                ConfigManager.setItemValue(itemId, "visuals.action-bar", !item.isVisualActionBar());
                this.open();
                break;
            case WITHER_SKELETON_SKULL:
                ConfigManager.setItemValue(itemId, "visuals.boss-bar", !item.isVisualBossBar());
                this.open();
                break;

            case GLOW_INK_SAC:
                new BossBarColorMenu(playerMenuUtility, plugin).open();
                break;
            case PAINTING:
                new BossBarStyleMenu(playerMenuUtility, plugin).open();
                break;

            case WRITABLE_BOOK:
                int slot = e.getSlot();
                String path = "";
                String title = "";

                if (slot == 19) {
                    path = "active.msg.chat";
                    title = "Chat Message";
                } else if (slot == 21) {
                    path = "active.msg.title";
                    title = "Title Message (Split with '|' for subtitle)";
                } else if (slot == 23) {
                    path = "active.msg.actionbar";
                    title = "Action Bar Message";
                } else if (slot == 25) {
                    path = "active.msg.bossbar";
                    title = "Boss Bar Message";
                }

                if (!path.isEmpty()) {
                    if (e.isRightClick()) {
                        if (path.contains("title")) ConfigManager.setItemValue(itemId, "visuals.messages.subtitle", null);

                        String configPath = path.replace("active.msg.", "visuals.messages.")
                                .replace("actionbar", "action-bar")
                                .replace("bossbar", "boss-bar");
                        ConfigManager.setItemValue(itemId, configPath, null);
                        p.sendMessage(ConfigManager.fromSection("§aReset to default config message."));
                        this.open();
                    } else {
                        playerMenuUtility.setWaitingForChatInput(true);
                        playerMenuUtility.setChatInputPath(path);
                        p.closeInventory();
                        p.sendMessage(ConfigManager.fromSection("§aEnter new " + title + " in chat."));
                        p.sendMessage(ConfigManager.fromSection("§7Use {time} for remaining seconds."));
                        p.sendMessage(ConfigManager.fromSection("§7Type 'default' to reset."));
                    }
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        setFillerGlass();

        inventory.setItem(10, createVisualToggle(Material.PAPER, "Chat Message", item.isVisualChat()));
        inventory.setItem(12, createVisualToggle(Material.NAME_TAG, "Title Alert", item.isVisualTitle()));
        inventory.setItem(14, createVisualToggle(Material.IRON_BARS, "Action Bar", item.isVisualActionBar()));
        inventory.setItem(16, createVisualToggle(Material.WITHER_SKELETON_SKULL, "Boss Bar", item.isVisualBossBar()));

        inventory.setItem(19, makeItem(Material.WRITABLE_BOOK, "§eEdit Chat Msg",
                formatMessageLore(item.getCustomChatMsg(), "active-items.messages.cooldown-chat")));

        inventory.setItem(21, makeItem(Material.WRITABLE_BOOK, "§eEdit Title Msg",
                formatTitleLore(item.getCustomTitleMsg(), item.getCustomSubtitleMsg())));

        inventory.setItem(23, makeItem(Material.WRITABLE_BOOK, "§eEdit Action Bar",
                formatMessageLore(item.getCustomActionBarMsg(), "active-items.messages.cooldown-action-bar")));

        inventory.setItem(25, makeItem(Material.WRITABLE_BOOK, "§eEdit Boss Bar",
                formatMessageLore(item.getCustomBossBarMsg(), "active-items.messages.cooldown-boss-bar")));

        inventory.setItem(34, makeItem(Material.PAINTING, "§dBossBar Style", "§7Current: §e" + item.getBossBarStyle(), "§eClick to Change"));
        inventory.setItem(43, makeItem(Material.GLOW_INK_SAC, "§dBossBar Color", "§7Current: §e" + item.getBossBarColor(), "§eClick to Change"));

        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack"));
    }

    private ItemStack createVisualToggle(Material mat, String name, boolean state) {
        String status = state ? "§aEnabled" : "§cDisabled";
        ItemStack item = makeItem(mat, "§6Visual: " + name, "§7Status: " + status, "", "§eClick to Toggle");
        if (state) {
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String[] formatMessageLore(String msg, String configPath) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Current:");

        if (msg == null) {
            String defMsg = plugin.getConfig().getString(configPath, "§cMissing Config");
            String formatted = ConfigManager.toSection(ConfigManager.fromLegacy(defMsg));
            lore.add("§r" + formatted);
            lore.add("§8(Default Value)");
        } else {
            String formatted = ConfigManager.toSection(ConfigManager.fromLegacy(msg));
            lore.add("§r" + formatted);
        }

        lore.add("");
        lore.add("§eLeft-Click to Edit");
        lore.add("§cRight-Click to Reset");
        return lore.toArray(new String[0]);
    }

    private String[] formatTitleLore(String title, String subtitle) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Includes Subtitle (Split with '|')");
        lore.add("");

        lore.add("§7Title:");
        if (title == null) {
            String defTitle = plugin.getConfig().getString("active-items.messages.cooldown-title", "");
            lore.add("§r" + ConfigManager.toSection(ConfigManager.fromLegacy(defTitle)));
            lore.add("§8(Default Value)");
        } else {
            lore.add("§r" + ConfigManager.toSection(ConfigManager.fromLegacy(title)));
        }

        lore.add("");

        lore.add("§7Subtitle:");
        if (subtitle == null) {
            String defSub = plugin.getConfig().getString("active-items.messages.cooldown-subtitle", "");
            lore.add("§r" + ConfigManager.toSection(ConfigManager.fromLegacy(defSub)));
            lore.add("§8(Default Value)");
        } else {
            lore.add("§r" + ConfigManager.toSection(ConfigManager.fromLegacy(subtitle)));
        }

        lore.add("");
        lore.add("§eLeft-Click to Edit");
        lore.add("§cRight-Click to Reset");
        return lore.toArray(new String[0]);
    }
}