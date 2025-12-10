package io.github.altkat.BuffedItems.menu.editor;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.menu.active.ActiveItemSettingsMenu;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.passive.PassiveItemSettingsMenu;
import io.github.altkat.BuffedItems.menu.selector.MaterialSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.ItemListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;
import io.github.altkat.BuffedItems.utility.item.DepletionAction;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
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
        return 36;
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
                new ItemListMenu(playerMenuUtility, plugin).open();
                break;
            case NAME_TAG:
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("display_name");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromLegacyWithPrefix("§aPlease type the new display name in chat. Use '&' for color codes. &7[ &#E12B5DH&#E12B5De&#E12B5Dx &#E12B5Dc&#E12B5Do&#DD3266l&#D83870o&#D43F79r&#D04583s &#C75295s&#C3599Fu&#BF5FA8p&#BB66B2p&#B66CBBo&#B273C4r&#AE79CEt&#AA80D7e&#A586E1d&#A18DEA! &7]"));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                break;
            case BOOK:
                new LoreEditorMenu(playerMenuUtility, plugin).open();
                break;
            case PAPER:
                new PermissionSettingsMenu(playerMenuUtility, plugin).open();
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
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Custom Model Data:"));
                p.sendMessage(ConfigManager.fromSection("§7Direct integer: §e100001"));
                p.sendMessage(ConfigManager.fromSection("§7ItemsAdder: §eitemsadder:fire_sword"));
                p.sendMessage(ConfigManager.fromSection("§7Nexo: §enexo:custom_helmet"));
                p.sendMessage(ConfigManager.fromSection("§7Type §6'none'§7 or §6'remove'§7 to clear."));
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                break;
            case CHEST_MINECART:
                new PassiveItemSettingsMenu(playerMenuUtility, plugin).open();
                break;

            case CLOCK:
                new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
                break;
        }
    }

    @Override
    public void setMenuItems() {
        BuffedItem item = plugin.getItemManager().getBuffedItem(playerMenuUtility.getItemToEditId());
        if (item == null) {
            playerMenuUtility.getOwner().sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Item could not be found. Returning to main menu."));
            new ItemListMenu(playerMenuUtility, plugin).open();
            return;
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

        String currentName = item.getDisplayName().replace('&', '§');

        inventory.setItem(11, makeItem(Material.NAME_TAG, "§aChange Display Name", "§7Current: " + currentName));
        inventory.setItem(12, makeItem(Material.GRASS_BLOCK, "§aChange Material", "§7Current: §e" + item.getMaterial().name()));
        inventory.setItem(13, makeItem(Material.BOOK, "§aEdit Lore", "§7Click to modify the item's lore."));
        String permDisplay = (item.getPermission() != null) ? item.getPermission() : "§cNone";
        inventory.setItem(14, makeItem(Material.PAPER, "§aPermission Settings", "§7Click to modify permission settings"));

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

        inventory.setItem(20, makeItem(Material.BEACON, "§aToggle Glow", "§7Current: " + (item.hasGlow() ? "§aEnabled" : "§cDisabled")));
        inventory.setItem(21, makeItem(Material.ENCHANTED_BOOK, "§dEdit Enchantments", "§7Click to add, remove, or modify", "§7the item's enchantments."));
        inventory.setItem(22, makeItem(Material.REDSTONE_TORCH, "§6Edit Item Flags", "§7Control item behaviors like 'Unbreakable',", "§7'Prevent Anvil Use', 'Hide Attributes', etc."));
        inventory.setItem(23, makeItem(Material.CHEST_MINECART, "§b§lPassive Effects",
                "§7Effects that apply when holding",
                "§7or wearing the item.",
                "",
                "§f• Potion Effects",
                "§f• Attribute Modifiers",
                "",
                "§eClick to Manage"));

        inventory.setItem(24, makeItem(Material.CLOCK, "§6§lActive Abilities",
                "§7Features triggered by",
                "§7right-clicking the item.",
                "",
                "§f• Cooldowns & Visuals",
                "§f• Commands & Sounds",
                "§f• Temporary Effects",
                "",
                "§eClick to Manage"));

        addBackButton(new ItemListMenu(playerMenuUtility, plugin));
        setFillerGlass();
    }

    private List<Component> generateItemInfoLore(BuffedItem item) {
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(ConfigManager.fromSection("§8§m------------------"));

        infoLore.add(ConfigManager.fromSection("§7Item ID: §b" + item.getId()));
        infoLore.add(ConfigManager.fromSection("§7Material: §f" + item.getMaterial().name()));

        String glowStatus = item.hasGlow() ? "§aYes" : "§cNo";
        infoLore.add(ConfigManager.fromSection("§7Glow: " + glowStatus));

        String mainPerm = (item.getPermission() != null) ? item.getPermission() : "§7None";
        infoLore.add(ConfigManager.fromSection("§7Permission: §f" + mainPerm));

        if (item.getActivePermissionRaw() != null) {
            infoLore.add(ConfigManager.fromSection("§7- Active Perm: §b" + item.getActivePermissionRaw()));
        }
        if (item.getPassivePermissionRaw() != null) {
            infoLore.add(ConfigManager.fromSection("§7- Passive Perm: §d" + item.getPassivePermissionRaw()));
        }

        if (item.getCustomModelData().isPresent()) {
            String cmdDisplay = "§e" + item.getCustomModelData().get();
            if (item.getCustomModelDataRaw().isPresent()) {
                String raw = item.getCustomModelDataRaw().get();
                if (!raw.equals(String.valueOf(item.getCustomModelData().get()))) {
                    cmdDisplay += " §7(" + raw + ")";
                }
            }
            infoLore.add(ConfigManager.fromSection("§7Model Data: " + cmdDisplay));
        }

        infoLore.add(Component.empty());
        if (item.isActiveMode()) {
            infoLore.add(ConfigManager.fromSection("§6§l[Active Ability: ON]"));
            infoLore.add(ConfigManager.fromSection("§7Cooldown: §f" + item.getCooldown() + "s §7| Duration: §f" + item.getActiveDuration() + "s"));

            int maxUses = item.getMaxUses();
            if (maxUses > 0) {
                infoLore.add(ConfigManager.fromSection("§7Usage Limit: §e" + maxUses));
                infoLore.add(ConfigManager.fromSection("§7Depletion: §f" + item.getDepletionAction().name()));
                if (item.getDepletionAction() == DepletionAction.TRANSFORM) {
                    String target = item.getDepletionTransformId();
                    infoLore.add(ConfigManager.fromSection("§7Transform: §b" + (target != null ? target : "None")));
                }
            } else {
                infoLore.add(ConfigManager.fromSection("§7Usage Limit: §aUnlimited"));
            }

            StringBuilder visuals = new StringBuilder();
            visuals.append(item.isVisualChat() ? "§a[Chat] " : "§8[Chat] ");
            visuals.append(item.isVisualTitle() ? "§a[Title] " : "§8[Title] ");
            visuals.append(item.isVisualActionBar() ? "§a[Action] " : "§8[Action] ");
            visuals.append(item.isVisualBossBar() ? "§a[BossBar]" : "§8[BossBar]");
            infoLore.add(ConfigManager.fromSection("§7Visuals: " + visuals.toString()));

            List<ICost> costs = item.getCosts();
            if (!costs.isEmpty()) {
                infoLore.add(ConfigManager.fromSection("§7Costs:"));
                for (ICost cost : costs) {
                    infoLore.add(ConfigManager.fromSection("  §c- " + cost.getDisplayString()));
                }
            } else {
                infoLore.add(ConfigManager.fromSection("§7Costs: §aFree"));
            }

            if (!item.getActiveCommands().isEmpty()) {
                infoLore.add(ConfigManager.fromSection("§7Commands: §f" + item.getActiveCommands().size() + " active"));
            }

            BuffedItemEffect activeEffects = item.getActiveEffects();
            if (activeEffects != null) {
                if (!activeEffects.getPotionEffects().isEmpty() || !activeEffects.getParsedAttributes().isEmpty()) {
                    infoLore.add(ConfigManager.fromSection("§7On-Click Effects:"));
                    activeEffects.getPotionEffects().forEach((type, level) ->
                            infoLore.add(ConfigManager.fromSection("  §7+ Potion: §d" + type.getName() + " " + level)));

                    activeEffects.getParsedAttributes().forEach(attr -> {
                        String attrName = attr.getAttribute().name().replace("GENERIC_", "");
                        infoLore.add(ConfigManager.fromSection("  §7+ Attr: §d" + attrName + " " + attr.getAmount()));
                    });
                }
            }

        } else {
            infoLore.add(ConfigManager.fromSection("§7Active Ability: §cOFF"));
        }

        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (!enchants.isEmpty()) {
            infoLore.add(Component.empty());
            infoLore.add(ConfigManager.fromSection("§b§l[Enchantments]"));
            enchants.forEach((enchant, level) -> {
                String enchantName = enchant.getKey().getKey().replace("minecraft:", "").toUpperCase();
                infoLore.add(ConfigManager.fromSection("§7- " + enchantName + " " + level));
            });
        }

        Map<String, BuffedItemEffect> effects = item.getEffects();
        if (!effects.isEmpty()) {
            infoLore.add(Component.empty());
            infoLore.add(ConfigManager.fromSection("§d§l[Passive Effects]"));
            effects.forEach((slot, effect) -> {
                boolean hasContent = !effect.getPotionEffects().isEmpty() || !effect.getParsedAttributes().isEmpty();
                if (hasContent) {
                    infoLore.add(ConfigManager.fromSection("§eSlot: " + slot));

                    effect.getPotionEffects().forEach((type, level) ->
                            infoLore.add(ConfigManager.fromSection("  §7• Potion: §f" + type.getName() + " " + level)));

                    effect.getParsedAttributes().forEach(attr -> {
                        String attrName = attr.getAttribute().name().replace("GENERIC_", "");
                        String opSymbol = attr.getOperation() == org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER ? "+" : "%";
                        infoLore.add(ConfigManager.fromSection("  §7• Attr: §f" + attrName + " " + opSymbol + attr.getAmount()));
                    });
                }
            });
        }

        Map<String, Boolean> flags = item.getFlags();
        List<String> activeFlags = new ArrayList<>();
        flags.forEach((flag, value) -> {
            if(value) activeFlags.add(flag);
        });

        if (!activeFlags.isEmpty()) {
            infoLore.add(Component.empty());
            infoLore.add(ConfigManager.fromSection("§c§l[Flags]"));
            for (String flag : activeFlags) {
                infoLore.add(ConfigManager.fromSection("§7- " + flag));
            }
        }

        return infoLore;
    }
}