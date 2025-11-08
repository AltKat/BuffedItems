package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.github.altkat.BuffedItems.utils.BuffedItemEffect;
import io.github.altkat.BuffedItems.utils.ItemBuilder;
import io.github.altkat.BuffedItems.utils.ParsedAttribute;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

        if (e.getSlot() == 4) {
            if (e.isLeftClick()) {
                playerMenuUtility.toggleShowPreviewDetails();
                setMenuItems();
            }
            else if (e.isRightClick()) {
                BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
                if (item != null) {
                    List<Component> infoLore = generateItemInfoLore(item);
                    p.sendMessage(ConfigManager.fromSection("§8§m---[ §bItem Report: " + item.getId() + " §8§m]---"));

                    for (int i = 1; i < infoLore.size(); i++) {
                        p.sendMessage(infoLore.get(i));
                    }

                    p.sendMessage(ConfigManager.fromSection("§8§m---------------------------------"));
                }
            }
            return;
        }

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                new MainMenu(playerMenuUtility, plugin).open();
                break;
            case NAME_TAG:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("display_name");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromLegacy("§aPlease type the new display name in chat. Use '&' for color codes. &7[ &#E12B5DH&#E12B5De&#E12B5Dx &#E12B5Dc&#E12B5Do&#DD3266l&#D83870o&#D43F79r&#D04583s &#C75295s&#C3599Fu&#BF5FA8p&#BB66B2p&#B66CBBo&#B273C4r&#AE79CEt&#AA80D7e&#A586E1d&#A18DEA! &7]"));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                break;
            case BOOK:
                new LoreEditorMenu(playerMenuUtility, plugin).open();
                break;
            case PAPER:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("permission");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSection("§aPlease type the permission node in chat."));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'none' or 'remove' to clear the permission)"));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
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
                p.sendMessage(ConfigManager.fromSection("§aEnter Custom Model Data:"));
                p.sendMessage(ConfigManager.fromSection("§7Direct integer: §e100001"));
                p.sendMessage(ConfigManager.fromSection("§7ItemsAdder: §eitemsadder:fire_sword"));
                p.sendMessage(ConfigManager.fromSection("§7Nexo: §enexo:custom_helmet"));
                p.sendMessage(ConfigManager.fromSection("§7Type §6'none'§7 or §6'remove'§7 to clear."));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                break;
            case CLOCK:
                p.sendMessage(ConfigManager.fromSection("§6[BuffedItems] §eThis feature is currently under development. Stay tuned for upcoming updates!"));
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                break;
        }
    }

    @Override
    public void setMenuItems() {
        BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
        if (item == null) {
            playerMenuUtility.getOwner().sendMessage(ConfigManager.fromSection("§cError: Item could not be found. Returning to main menu."));
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;
            inventory.setItem(i, filler);
        }

        ItemStack previewItem = new ItemBuilder(item, plugin).build();
        ItemMeta previewMeta = previewItem.getItemMeta();

        if (previewMeta != null) {
            List<Component> originalLore = previewMeta.hasLore() ? new ArrayList<>(previewMeta.lore()) : new ArrayList<>();
            List<Component> combinedLore = new ArrayList<>(originalLore);

            if (!originalLore.isEmpty()) {
                combinedLore.add(Component.empty());
            }

            if (playerMenuUtility.isShowPreviewDetails()) {
                List<Component> infoLore = generateItemInfoLore(item);
                combinedLore.addAll(infoLore);
                combinedLore.add(Component.empty());
                combinedLore.add(ConfigManager.fromSection("§8§m------------------"));
                combinedLore.add(ConfigManager.fromSection("§7(Live Preview)"));
                combinedLore.add(ConfigManager.fromSection("§eLeft-click to hide details."));
                combinedLore.add(ConfigManager.fromSection("§eRight-click to print info to chat."));
            } else {
                combinedLore.add(ConfigManager.fromSection("§8§m------------------"));
                combinedLore.add(ConfigManager.fromSection("§7(Live Preview)"));
                combinedLore.add(ConfigManager.fromSection("§aLeft-click to see details."));
                combinedLore.add(ConfigManager.fromSection("§eRight-click to print info to chat."));
            }

            previewMeta.lore(combinedLore);
            previewMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            previewItem.setItemMeta(previewMeta);
        }

        inventory.setItem(4, previewItem);

        for (int i = 36; i < 44; i++) {
            inventory.setItem(i, filler);
        }

        inventory.setItem(9, filler);
        inventory.setItem(17, filler);
        inventory.setItem(18, filler);
        inventory.setItem(26, filler);
        inventory.setItem(27, filler);
        inventory.setItem(35, filler);

        String currentName = item.getDisplayName().replace('&', '§');

        inventory.setItem(11, makeItem(Material.NAME_TAG, "§aChange Display Name", "§7Current: " + currentName));
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
        inventory.setItem(31, makeItem(Material.CLOCK, "§6Active Item Settings §7(Coming Soon)",
                "§7Preview of the next planned update:",
                "§7• §fRight-click activation mode",
                "§7• §fCustom cooldowns & durations",
                "§7• §fVisual indicators",
                "",
                "§eComing soon!"));

        addBackButton(new MainMenu(playerMenuUtility, plugin));
    }

    private List<Component> generateItemInfoLore(BuffedItem item) {
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(ConfigManager.fromSection("§8§m------------------"));

        infoLore.add(ConfigManager.fromSection("§7Item ID: §b" + item.getId()));
        infoLore.add(ConfigManager.fromSection("§7Item Info:"));

        infoLore.add(ConfigManager.fromSection("§7- Material: §e" + item.getMaterial().name()));
        infoLore.add(ConfigManager.fromSection("§7- Glow: " + (item.hasGlow() ? "§aTrue" : "§cFalse")));
        infoLore.add(ConfigManager.fromSection("§7- Permission: §e" + item.getPermission().orElse("§cNone")));

        String cmdDisplayInfo;
        if (item.getCustomModelData().isPresent()) {
            cmdDisplayInfo = "§e" + item.getCustomModelData().get();
            if (item.getCustomModelDataRaw().isPresent()) {
                String raw = item.getCustomModelDataRaw().get();
                if (!raw.equals(String.valueOf(item.getCustomModelData().get()))) {
                    cmdDisplayInfo += " §7(" + raw + ")";
                }
            }
        } else {
            cmdDisplayInfo = "§cNone";
        }
        infoLore.add(ConfigManager.fromSection("§7- Model Data: " + cmdDisplayInfo));

        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (enchants.isEmpty()) {
            infoLore.add(ConfigManager.fromSection("§7- Enchants: §cNone"));
        } else {
            enchants.forEach((enchant, level) -> {
                String enchantName = enchant.getKey().getKey();
                enchantName = enchantName.replace("minecraft:", "").toUpperCase();
                infoLore.add(ConfigManager.fromSection("§7- Enchant: §d" + enchantName + " Lvl " + level));
            });
        }

        Map<String, BuffedItemEffect> effects = item.getEffects();
        if (effects.isEmpty()) {
            infoLore.add(ConfigManager.fromSection("§7- Effects: §cNone"));
        } else {
            infoLore.add(ConfigManager.fromSection("§7- Effects:"));
            effects.forEach((slot, effect) -> {
                infoLore.add(ConfigManager.fromSection("§b  [" + slot + "]"));

                Map<org.bukkit.potion.PotionEffectType, Integer> potions = effect.getPotionEffects();
                if (!potions.isEmpty()) {
                    potions.forEach((type, level) -> {
                        infoLore.add(ConfigManager.fromSection("§7    - Potion: §e" + type.getName() + " Lvl " + level));
                    });
                }

                List<ParsedAttribute> attributes = effect.getParsedAttributes();
                if (!attributes.isEmpty()) {
                    attributes.forEach(attr -> {
                        String attrName = attr.getAttribute().name().replace("GENERIC_", "");
                        String opName = attr.getOperation().name().replace("_NUMBER", "").replace("_SCALAR_1", "");
                        infoLore.add(ConfigManager.fromSection("§7    - Attr: §e" + attrName + " " + opName + " " + attr.getAmount()));
                    });
                }
            });
        }

        Map<String, Boolean> flags = item.getFlags();
        if (flags.isEmpty()) {
            infoLore.add(ConfigManager.fromSection("§7- Flags: §a(Using Defaults)"));
        } else {
            infoLore.add(ConfigManager.fromSection("§7- Flags (Overridden):"));
            flags.forEach((flag, value) -> {
                infoLore.add(ConfigManager.fromSection("§7  - §6" + flag + ": " + (value ? "§aTrue" : "§cFalse")));
            });
        }

        return infoLore;
    }
}