package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

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

        if (type == Material.BARRIER && e.getSlot() == 53) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        switch (type) {
            case LEVER:
                boolean newMode = !item.isActiveMode();
                ConfigManager.setItemValue(itemId, "active_mode", newMode);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                this.open();
                break;

            case CLOCK:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("active.cooldown");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSection("§aEnter the Cooldown (in seconds) in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(Current: " + item.getCooldown() + "s)"));
                break;

            case COMPASS:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("active.duration");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSection("§aEnter the Effect Duration (in seconds) in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(Current: " + item.getActiveDuration() + "s)"));
                break;

            case COMMAND_BLOCK:
                new CommandListMenu(playerMenuUtility, plugin).open();
                break;

            case LINGERING_POTION:
                playerMenuUtility.setTargetSlot("ACTIVE");
                new PotionEffectListMenu(playerMenuUtility, plugin).open();
                break;

            case NETHER_STAR:
                playerMenuUtility.setTargetSlot("ACTIVE");
                new ActiveAttributeListMenu(playerMenuUtility, plugin).open();
                break;

            case GOAT_HORN:
                if (e.getSlot() == 19) {
                    new SoundSettingsMenu(playerMenuUtility, plugin, "success").open();
                } else if (e.getSlot() == 25) {
                    new SoundSettingsMenu(playerMenuUtility, plugin, "cooldown").open();
                }
                break;

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

                if (slot == 37) {
                    path = "active.msg.chat";
                    title = "Chat Message";
                } else if (slot == 39) {
                    path = "active.msg.title";
                    title = "Title Message (Split with '|' for subtitle)";
                } else if (slot == 41) {
                    path = "active.msg.actionbar";
                    title = "Action Bar Message";
                } else if (slot == 43) {
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

        boolean isActive = item.isActiveMode();
        inventory.setItem(10, makeItem(Material.LEVER,
                isActive ? "§aActive Mode: ON" : "§cActive Mode: OFF",
                "§7Enable/Disable right-click functionality.", "", "§eClick to Toggle"));

        inventory.setItem(12, makeItem(Material.CLOCK, "§bSet Cooldown",
                "§7Current: §e" + item.getCooldown() + "s", "§eClick to Edit"));

        inventory.setItem(14, makeItem(Material.COMPASS, "§bSet Effect Duration",
                "§7Current: §e" + item.getActiveDuration() + "s", "§eClick to Edit"));

        inventory.setItem(16, makeItem(Material.COMMAND_BLOCK, "§6Manage Commands",
                "§7Current: §e" + item.getActiveCommands().size() + " commands", "§eClick to Edit List"));

        String currSuccess = item.getCustomSuccessSound();
        if (currSuccess == null) currSuccess = "§8(Default Config)";
        else currSuccess = "§a" + currSuccess;
        inventory.setItem(19, makeItem(Material.GOAT_HORN, "§6Success Sound",
                "§7Current: " + currSuccess, "§eClick to Change"));

        inventory.setItem(21, makeItem(Material.LINGERING_POTION, "§dActive Potion Effects",
                "§7Manage potion effects applied", "§7when used.", "", "§eClick to Edit"));

        inventory.setItem(23, makeItem(Material.NETHER_STAR, "§bActive Attributes",
                "§7Manage temporary attributes", "§7given when used.", "", "§eClick to Edit"));

        String currCool = item.getCustomCooldownSound();
        if (currCool == null) currCool = "§8(Default Config)";
        else currCool = "§c" + currCool;
        inventory.setItem(25, makeItem(Material.GOAT_HORN, "§6Cooldown Sound",
                "§7Current: " + currCool, "§eClick to Change"));

        inventory.setItem(28, createVisualToggle(Material.PAPER, "Chat Message", item.isVisualChat()));

        inventory.setItem(30, createVisualToggle(Material.NAME_TAG, "Title Alert", item.isVisualTitle()));

        inventory.setItem(32, createVisualToggle(Material.IRON_BARS, "Action Bar", item.isVisualActionBar()));

        inventory.setItem(34, createVisualToggle(Material.WITHER_SKELETON_SKULL, "Boss Bar", item.isVisualBossBar()));

        inventory.setItem(37, makeItem(Material.WRITABLE_BOOK, "§eEdit Chat Msg",
                formatMessageLore(item.getCustomChatMsg())));

        inventory.setItem(39, makeItem(Material.WRITABLE_BOOK, "§eEdit Title Msg",
                formatTitleLore(item.getCustomTitleMsg(), item.getCustomSubtitleMsg())));

        inventory.setItem(41, makeItem(Material.WRITABLE_BOOK, "§eEdit Action Bar",
                formatMessageLore(item.getCustomActionBarMsg())));

        inventory.setItem(43, makeItem(Material.WRITABLE_BOOK, "§eEdit Boss Bar",
                formatMessageLore(item.getCustomBossBarMsg())));

        inventory.setItem(48, makeItem(Material.GLOW_INK_SAC, "§dBossBar Color",
                "§7Current: §e" + item.getBossBarColor(), "§eClick to Change"));

        inventory.setItem(50, makeItem(Material.PAINTING, "§dBossBar Style",
                "§7Current: §e" + item.getBossBarStyle(), "§eClick to Change"));

        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack"));
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

    private String[] formatMessageLore(String msg) {
        List<String> lore = new ArrayList<>();
        if (msg == null) {
            lore.add("§7Current: §8(Default Config)");
        } else {
            lore.add("§7Current:");
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
        if (title == null) {
            lore.add("§7Title: §8(Default Config)");
        } else {
            lore.add("§7Title:");
            lore.add("§r" + ConfigManager.toSection(ConfigManager.fromLegacy(title)));
        }
        lore.add("");
        if (subtitle == null) {
            lore.add("§7Subtitle: §8(Default/None)");
        } else {
            lore.add("§7Subtitle:");
            lore.add("§r" + ConfigManager.toSection(ConfigManager.fromLegacy(subtitle)));
        }
        lore.add("");
        lore.add("§eLeft-Click to Edit");
        lore.add("§cRight-Click to Reset");
        return lore.toArray(new String[0]);
    }
}