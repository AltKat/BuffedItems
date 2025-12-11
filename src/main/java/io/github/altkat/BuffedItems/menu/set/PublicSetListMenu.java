package io.github.altkat.BuffedItems.menu.set;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.attribute.ParsedAttribute;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.BuffedItemEffect;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import io.github.altkat.BuffedItems.utility.set.BuffedSet;
import io.github.altkat.BuffedItems.utility.set.SetBonus;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PublicSetListMenu extends PaginatedMenu {

    private final BuffedItems plugin;

    public PublicSetListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.maxItemsPerPage = 36;
    }

    @Override
    public String getMenuName() {
        return "Item Sets (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        List<BuffedSet> sets = getSortedSets();
        if (handlePageChange(e, sets.size())) return;

        if (e.getSlot() == 49) {
            e.getWhoClicked().closeInventory();
        }
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);

        addMenuControls();

        inventory.setItem(49, makeItem(Material.BARRIER, "§cClose Menu"));

        List<BuffedSet> sets = getSortedSets();

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= sets.size()) break;

            BuffedSet set = sets.get(index);
            inventory.setItem(9 + i, createSetIcon(set));
        }
    }

    private ItemStack createSetIcon(BuffedSet set) {
        ItemStack iconStack;
        if (!set.getItemIds().isEmpty()) {
            String firstId = set.getItemIds().get(0);
            BuffedItem bItem = plugin.getItemManager().getBuffedItem(firstId);
            if (bItem != null) {
                iconStack = new ItemBuilder(bItem, plugin).build();
            } else {
                iconStack = new ItemStack(Material.ARMOR_STAND);
            }
        } else {
            iconStack = new ItemStack(Material.ARMOR_STAND);
        }

        ItemMeta meta = iconStack.getItemMeta();
        if (meta != null) {
            meta.displayName(ConfigManager.fromSection(ConfigManager.toSection(ConfigManager.fromLegacy(set.getDisplayName()))));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());

            lore.add(ConfigManager.fromSection("§7Set Pieces (" + set.getItemIds().size() + "):"));
            for (String itemId : set.getItemIds()) {
                BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
                String itemName = (item != null) ? ConfigManager.toSection(ConfigManager.fromLegacy(item.getDisplayName())) : itemId;
                lore.add(ConfigManager.fromSection(" §8• §f" + itemName));
            }
            lore.add(Component.empty());

            lore.add(ConfigManager.fromSection("§6Set Bonuses:"));

            Map<Integer, SetBonus> sortedBonuses = new TreeMap<>(set.getBonuses());

            if (sortedBonuses.isEmpty()) {
                lore.add(ConfigManager.fromSection(" §7None"));
            } else {
                for (Map.Entry<Integer, SetBonus> entry : sortedBonuses.entrySet()) {
                    int count = entry.getKey();
                    SetBonus bonus = entry.getValue();
                    BuffedItemEffect effects = bonus.getEffects();

                    lore.add(ConfigManager.fromSection(" §e[" + count + "] Pieces:"));

                    boolean hasEffect = false;

                    if (effects.getPotionEffects() != null) {
                        for (Map.Entry<PotionEffectType, Integer> potion : effects.getPotionEffects().entrySet()) {
                            String pName = formatEnumName(potion.getKey().getName());
                            lore.add(ConfigManager.fromSection("   §7• §b" + pName + " " + potion.getValue()));
                            hasEffect = true;
                        }
                    }

                    if (effects.getParsedAttributes() != null) {
                        for (ParsedAttribute attr : effects.getParsedAttributes()) {
                            String attrName = formatEnumName(attr.getAttribute().name().replace("GENERIC_", ""));
                            String op = (attr.getOperation() == AttributeModifier.Operation.ADD_NUMBER) ? "" : "%";
                            String amountStr = (attr.getAmount() > 0 ? "+" : "") + attr.getAmount();

                            if (attr.getOperation() != AttributeModifier.Operation.ADD_NUMBER) {
                                amountStr = (attr.getAmount() > 0 ? "+" : "") + (int)(attr.getAmount() * 100);
                            }

                            lore.add(ConfigManager.fromSection("   §7• §a" + attrName + " " + amountStr + op));
                            hasEffect = true;
                        }
                    }

                    if (!hasEffect) {
                        lore.add(ConfigManager.fromSection("   §7• No Effects"));
                    }
                }
            }

            meta.lore(lore);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES, org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS, org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            iconStack.setItemMeta(meta);
        }

        iconStack.setAmount(1);
        return iconStack;
    }

    private List<BuffedSet> getSortedSets() {
        List<BuffedSet> list = new ArrayList<>(plugin.getSetManager().getSets().values());
        list.sort(Comparator.comparing(BuffedSet::getId));
        return list;
    }

    private String formatEnumName(String name) {
        String[] words = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}