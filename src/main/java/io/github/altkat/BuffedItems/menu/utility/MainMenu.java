package io.github.altkat.BuffedItems.menu.utility;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.hooks.HookManager;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.editor.ItemEditorMenu;
import io.github.altkat.BuffedItems.menu.set.SetListMenu;
import io.github.altkat.BuffedItems.menu.upgrade.UpgradeRecipeListMenu;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MainMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final HookManager hooks;

    private int maxItemsPerPage = 27;

    public MainMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.hooks = plugin.getHookManager();
        this.maxItemsPerPage = 27;
    }

    @Override
    public String getMenuName() {
        return "BuffedItems > Main Menu";
    }

    @Override
    public int getSlots() {

        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        List<BuffedItem> items = new ArrayList<>(plugin.getItemManager().getLoadedItems().values());
        ItemStack clickedItem = e.getCurrentItem();

        if (clickedItem == null) return;

        if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "buffeditem_id"), PersistentDataType.STRING)) {
            String itemId = clickedItem.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "buffeditem_id"), PersistentDataType.STRING);
            playerMenuUtility.setItemToEditId(itemId);

            if (e.getClick() == ClickType.SHIFT_LEFT) {
                BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
                if (item != null) {
                    ItemStack stack = new ItemBuilder(item, plugin).build();


                    ItemMeta meta = stack.getItemMeta();
                    if (meta != null) {
                        if (meta.hasDisplayName()) {
                            Component originalName = meta.displayName();
                            if (originalName != null) {
                                String legacyNameWithSection = ConfigManager.toSection(originalName);
                                String parsedName = hooks.processPlaceholders(p, legacyNameWithSection);
                                meta.displayName(ConfigManager.fromSection(parsedName));
                            }
                        }

                        if (meta.hasLore()) {
                            List<Component> originalLore = meta.lore();
                            if (originalLore != null) {
                                List<Component> parsedLore = originalLore.stream()
                                        .map(ConfigManager::toSection)
                                        .map(line -> hooks.processPlaceholders(p, line))
                                        .map(ConfigManager::fromSection)
                                        .collect(Collectors.toList());
                                meta.lore(parsedLore);
                            }
                        }
                        stack.setItemMeta(meta);
                    }


                    p.getInventory().addItem(stack);

                    p.sendMessage(ConfigManager.fromSectionWithPrefix("§aItem received: §f" + itemId));
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);

                    plugin.getEffectApplicatorTask().markPlayerForUpdate(p.getUniqueId());
                }
            }
            else if (e.isLeftClick()) {
                new ItemEditorMenu(playerMenuUtility, plugin).open();
            }
            else if (e.getClick() == ClickType.SHIFT_RIGHT) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("duplicateitem");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aDuplicating '§e" + itemId + "§a'."));
                p.sendMessage(ConfigManager.fromSection("§aPlease type the NEW unique ID for the copy in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(e.g., 'new_fire_sword'). (Type 'cancel' to exit)"));

            }
            else if (e.isRightClick()) {
                new ConfirmationMenu(playerMenuUtility, plugin, itemId).open();
            }
            return;
        }

        if (handlePageChange(e, items.size())) return;

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                p.closeInventory();
                break;
            case ANVIL:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("createnewitem");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix( "§aPlease type the unique ID for the new item in chat (e.g., 'fire_sword')."));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                break;
            case COMPARATOR:
                new GeneralSettingsMenu(playerMenuUtility, plugin).open();
                break;
            case SMITHING_TABLE:
                new UpgradeRecipeListMenu(playerMenuUtility, plugin).open();
                break;
            case GOLDEN_CHESTPLATE:
                new SetListMenu(playerMenuUtility, plugin).open();
                break;
            case BOOK:
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
                break;
        }


    }

    @Override
    public void setMenuItems() {

        addMenuControls();
        inventory.setItem(49, makeItem(Material.ANVIL, "§bCreate New Item", "§7Click to create a brand new item."));

        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("§7Version: §f" + plugin.getDescription().getVersion());
        infoLore.add("");
        infoLore.add("§6Did you know?");
        infoLore.add("§7You can use §dPlaceholderAPI");
        infoLore.add("§dplaceholders§7, and §#00FFE0H§#1AE3E3E§#34C6E7X §#688EEEC§#8371F1o§#9D55F5l§#B739F8o§#D11CFCr§#EB00FFs");
        infoLore.add("§7in every message, §7item name,");
        infoLore.add("§7and lore!");
        infoLore.add("");
        infoLore.add("§bNeed Help?");
        infoLore.add("§7Click to get links for:");
        infoLore.add("§f• Wiki Page");
        infoLore.add("§f• Discord Support");
        infoLore.add("");
        infoLore.add("§eClick to print links in chat.");
        inventory.setItem(52, makeItem(Material.BOOK, "§aPlugin Information", infoLore.toArray(new String[0])));

        inventory.setItem(53, makeItem(Material.BARRIER, "§cClose Menu"));
        inventory.setItem(45, makeItem(Material.COMPARATOR, "§6General Settings",
                "§7Configure global plugin settings.",
                "§7(Debug level, Potion icons, etc.)"));
        inventory.setItem(46, makeItem(Material.GOLDEN_CHESTPLATE, "§6Item Sets",
                "§7Create and manage armor sets.",
                "§7(Bonuses for wearing multiple items)",
                "",
                "§eClick to Manage"));
        inventory.setItem(47, makeItem(Material.SMITHING_TABLE, "§6Configure Upgrades", "§7Create and edit upgrade recipes."));


        List<BuffedItem> items = new ArrayList<>(plugin.getItemManager().getLoadedItems().values());

        items.sort(Comparator.comparing(BuffedItem::getId));


        if (!items.isEmpty()) {
            for (int i = 0; i < maxItemsPerPage; i++) {
                int index = maxItemsPerPage * page + i;
                if (index >= items.size()) break;

                BuffedItem currentItem = items.get(index);
                ItemStack itemStack;

                if (currentItem.isValid()) {
                    itemStack = new ItemBuilder(currentItem, plugin).build();
                    ItemMeta meta = itemStack.getItemMeta();
                    List<String> newLore = (meta.getLore() != null) ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    newLore.add("");
                    newLore.add("§8§m------------------");
                    newLore.add("§eLeft-Click to Edit");
                    newLore.add("§aShift + Left-Click to Get");
                    newLore.add("§cRight-Click to Delete");
                    newLore.add("§bShift + Right Click to Duplicate");
                    meta.setLore(newLore);
                    itemStack.setItemMeta(meta);
                } else {
                    itemStack = new ItemStack(Material.BARRIER);
                    ItemMeta meta = itemStack.getItemMeta();
                    meta.setDisplayName("§c§lERROR: " + currentItem.getId());
                    List<String> errorLore = new ArrayList<>();
                    errorLore.add("§7This item has configuration errors.");
                    errorLore.add("");
                    errorLore.addAll(currentItem.getErrorMessages());
                    errorLore.add("");
                    errorLore.add("§8§m------------------");
                    errorLore.add("§eLeft-Click to Edit and fix the errors.");
                    errorLore.add("§aShift + Left-Click to Get (as-is)");
                    errorLore.add("§cRight-Click to Delete");
                    errorLore.add("§bShift + Right Click to Duplicate (as-is)");
                    meta.setLore(errorLore);

                    NamespacedKey key = new NamespacedKey(plugin, "buffeditem_id");
                    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, currentItem.getId());
                    itemStack.setItemMeta(meta);
                }
                inventory.setItem(i + 9, itemStack);
            }
        }
        setFillerGlass();
    }

    @Override
    public void addMenuControls() {
        super.addMenuControls();
        inventory.setItem(49, makeItem(Material.ANVIL, "§bCreate New Item", "§7Click to create a brand new item."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cClose Menu"));
    }
}