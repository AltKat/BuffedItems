package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.github.altkat.BuffedItems.utils.ItemBuilder;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ItemEditorMenu extends Menu {
    private final BuffedItems plugin;

    public ItemEditorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Editing: " + playerMenuUtility.getItemToEditId();
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                new MainMenu(playerMenuUtility, plugin).open();
                break;
            case CHEST_MINECART:
                BuffedItem itemToClone = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
                if (itemToClone != null) {
                    ItemStack clone = new ItemBuilder(itemToClone, plugin).build();
                    ItemMeta meta = clone.getItemMeta();

                    if (meta != null) {
                        String finalDisplayName = meta.hasDisplayName() ? meta.getDisplayName() : "";
                        List<String> finalLore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

                        if (plugin.isPlaceholderApiEnabled()) {

                            finalDisplayName = PlaceholderAPI.setPlaceholders(p, finalDisplayName);

                            finalLore = finalLore.stream()
                                    .map(line -> PlaceholderAPI.setPlaceholders(p, line))
                                    .collect(Collectors.toList());
                        }

                        finalDisplayName = ChatColor.translateAlternateColorCodes('&', finalDisplayName);
                        finalLore = finalLore.stream()
                                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                                .collect(Collectors.toList());

                        String suffix = ChatColor.translateAlternateColorCodes('&', " &7(TEST COPY)");
                        finalDisplayName += suffix;

                        finalLore.add("");
                        finalLore.add(ChatColor.YELLOW + "Remember: This is a test copy.");
                        finalLore.add(ChatColor.YELLOW + "It may not reflect saved changes yet.");

                        meta.setDisplayName(finalDisplayName);
                        meta.setLore(finalLore);
                        clone.setItemMeta(meta);
                    }

                    p.getInventory().addItem(clone);
                    p.sendMessage("§bTest copy of '" + itemToClone.getId() + "' has been added to your inventory.");

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        plugin.getEffectApplicatorTask().markPlayerForUpdate(p.getUniqueId());
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_TASK, () -> "[Menu] Marked " + p.getName() + " for update after receiving item via Test Copy button.");
                    }, 1L);

                } else {
                    p.sendMessage("§cCould not generate test copy. Item not found in memory.");
                }
                break;
            case NAME_TAG:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("display_name");
                p.closeInventory();
                p.sendMessage("§aPlease type the new display name in chat. Use '&' for color codes.");
                break;
            case BOOK:
                new LoreEditorMenu(playerMenuUtility, plugin).open();
                break;
            case PAPER:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("permission");
                p.closeInventory();
                p.sendMessage("§aPlease type the permission node in chat.");
                p.sendMessage("§7(Type 'none' or 'remove' to clear the permission)");
                break;
            case BEACON:
                BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
                if (item != null) {
                    ConfigManager.setItemValue(item.getId(), "glow", !item.hasGlow());
                    this.open();
                }
                break;
            case GRASS_BLOCK:
                new MaterialSelectorMenu(playerMenuUtility, plugin).open();
                break;
            case POTION:
                new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.POTION_EFFECT).open();
                break;
            case IRON_SWORD:
                new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.ATTRIBUTE).open();
                break;
            case REDSTONE_TORCH:
                new ItemFlagsMenu(playerMenuUtility, plugin).open();
                break;
            case ENCHANTED_BOOK:
                new EnchantmentListMenu(playerMenuUtility, plugin).open();
                break;
            case ARMOR_STAND:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("custom_model_data");
                p.closeInventory();
                p.sendMessage("§aEnter Custom Model Data:");
                p.sendMessage("§7Direct integer: §e100001");
                p.sendMessage("§7ItemsAdder: §eitemsadder:fire_sword");
                p.sendMessage("§7Nexo: §enexo:custom_helmet");
                p.sendMessage("§7Type §6'none'§7 or §6'remove'§7 to clear.");
                break;
        }
    }

    @Override
    public void setMenuItems() {
        BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
        if (item == null) {
            playerMenuUtility.getOwner().sendMessage("§cError: Item could not be found. Returning to main menu.");
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
        }
        for (int i = 36; i < 44; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(9, filler);
        inventory.setItem(17, filler);
        inventory.setItem(18, filler);
        inventory.setItem(26, filler);
        inventory.setItem(27, filler);
        inventory.setItem(35, filler);

        inventory.setItem(11, makeItem(Material.NAME_TAG, "§aChange Display Name", "§7Current: " + item.getDisplayName()));
        inventory.setItem(12, makeItem(Material.GRASS_BLOCK, "§aChange Material", "§7Current: §e" + item.getMaterial().name()));
        inventory.setItem(13, makeItem(Material.BOOK, "§aEdit Lore", "§7Click to modify the item's lore."));
        inventory.setItem(14, makeItem(Material.PAPER, "§aSet Permission", "§7Current: §e" + item.getPermission().orElse("§cNone")));

        String cmdDisplay;
        if (item.getCustomModelData().isPresent()) {
            cmdDisplay = "§e" + item.getCustomModelData().get();
            if (item.getCustomModelDataRaw().isPresent()) {
                String raw = item.getCustomModelDataRaw().get();
                if (!raw.equals(String.valueOf(item.getCustomModelData().get()))) {
                    cmdDisplay += " §7(" + raw + ")";
                }
            }
        } else {
            cmdDisplay = "§cNone";
        }
        inventory.setItem(15, makeItem(Material.ARMOR_STAND, "§dSet Custom Model Data",
                "§7Current: " + cmdDisplay,
                "§7Used for custom resource pack models.",
                "§7Supports: Direct, ItemsAdder, Nexo"));

        inventory.setItem(20, makeItem(Material.ENCHANTED_BOOK, "§dEdit Enchantments", "§7Click to add, remove, or modify", "§7the item's enchantments."));
        inventory.setItem(21, makeItem(Material.BEACON, "§aToggle Glow", "§7Current: " + (item.hasGlow() ? "§aEnabled" : "§cDisabled")));
        inventory.setItem(22, makeItem(Material.POTION, "§aEdit Potion Effects", "§7Click to manage potion effects."));
        inventory.setItem(23, makeItem(Material.IRON_SWORD, "§aEdit Attributes", "§7Click to manage attributes."));
        inventory.setItem(24, makeItem(Material.REDSTONE_TORCH, "§6Edit Item Flags", "§7Control item behaviors like 'Unbreakable',", "§7'Prevent Anvil Use', 'Hide Attributes', etc."));

        inventory.setItem(31, makeItem(Material.CHEST_MINECART, "§bGet Test Copy", "§7Gives you a copy of this item", "§7with all current changes."));

        addBackButton(new MainMenu(playerMenuUtility, plugin));
    }
}