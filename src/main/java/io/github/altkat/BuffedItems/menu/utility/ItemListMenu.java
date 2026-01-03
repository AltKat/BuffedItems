package io.github.altkat.BuffedItems.menu.utility;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.hooks.HookManager;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.editor.ItemEditorMenu;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
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

public class ItemListMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final HookManager hooks;

    public ItemListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.hooks = plugin.getHookManager();
        this.maxItemsPerPage = 36;
    }

    @Override
    public String getMenuName() {
        return "Item Editor List";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        List<BuffedItem> items = new ArrayList<>(plugin.getItemManager().getLoadedItems().values());
        items.sort(Comparator.comparing(BuffedItem::getId));

        ItemStack clickedItem = e.getCurrentItem();

        if (clickedItem == null) return;

        if (handlePageChange(e, items.size())) return;
        if (e.getSlot() >= 9 && e.getSlot() < 45) {
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
                } else if (e.isLeftClick()) {
                    new ItemEditorMenu(playerMenuUtility, plugin).open();
                } else if (e.getClick() == ClickType.SHIFT_RIGHT) {
                    playerMenuUtility.setWaitingForChatInput(true);
                    playerMenuUtility.setChatInputPath("duplicateitem");
                    p.closeInventory();
                    p.sendMessage(ConfigManager.fromSectionWithPrefix("§aDuplicating '§e" + itemId + "§a'."));
                    p.sendMessage(ConfigManager.fromSection("§aPlease type the NEW unique ID for the copy in chat."));
                    p.sendMessage(ConfigManager.fromSection("§7(e.g., 'new_fire_sword'). (Type 'cancel' to exit)"));
                } else if (e.isRightClick()) {
                    new ConfirmationMenu(playerMenuUtility, plugin, itemId).open();
                }
                return;
            }
        }

        if (e.getSlot() == 49) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("createnewitem");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix( "§aPlease type the unique ID for the new item in chat (e.g., 'fire_sword')."));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
        }
        else if (e.getSlot() == 53) {
            new MainMenu(playerMenuUtility, plugin).open();
        }
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);

        addMenuControls();


        inventory.setItem(49, makeItem(Material.ANVIL, "§bCreate New Item", "§7Click to create a brand new item."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack to Dashboard"));

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
                    newLore.add("§7ID: §f" + currentItem.getId());
                    newLore.add("§8§m------------------");
                    newLore.add("§eLeft-Click to Edit");
                    newLore.add("§aShift + Left-Click to Get");
                    newLore.add("§cRight-Click to Delete");
                    newLore.add("§bShift + Right Click to Duplicate");
                    newLore.add("§8§m------------------");
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
                    errorLore.add("§8§m------------------");
                    meta.setLore(errorLore);

                    NamespacedKey key = new NamespacedKey(plugin, "buffeditem_id");
                    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, currentItem.getId());
                    itemStack.setItemMeta(meta);
                }
                inventory.setItem(9 + i, itemStack);
            }
        }
    }
}