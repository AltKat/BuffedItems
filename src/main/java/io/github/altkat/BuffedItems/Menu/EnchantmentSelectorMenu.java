package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EnchantmentSelectorMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final List<Enchantment> enchantments;
    private final String itemId;

    public EnchantmentSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();


        this.enchantments = Arrays.stream(Enchantment.values())
                .filter(e -> e != null)
                .sorted(Comparator.comparing(e -> e.getKey().getKey()))
                .collect(Collectors.toList());
        this.maxItemsPerPage = 45;
    }

    @Override
    public String getMenuName() {
        return "Select Enchantment (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        Player p = (Player) e.getWhoClicked();
        int clickedSlot = e.getSlot();
        Material clickedType = e.getCurrentItem().getType();

        if (clickedSlot < this.maxItemsPerPage && clickedType == Material.ENCHANTED_BOOK) {
            ItemMeta meta = e.getCurrentItem().getItemMeta();
            if (meta == null || meta.getLore() == null || meta.getLore().isEmpty()) return;

            Component loreLine = meta.lore().get(0);
            String rawEnchantName = ConfigManager.toPlainText(loreLine);

            if (!rawEnchantName.startsWith("ID: ")) return;

            String enchantKey = rawEnchantName.substring(4);
            Enchantment selectedEnchant = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey.replace("minecraft:", "")));

            if (selectedEnchant == null) {
                p.sendMessage(ConfigManager.fromSection("§cError: Could not identify selected enchantment '" + enchantKey + "'."));
                return;
            }

            String configPath = "items." + itemId + ".enchantments";
            List<String> currentEnchants = plugin.getConfig().getStringList(configPath);
            boolean alreadyExists = currentEnchants.stream()
                    .anyMatch(s -> s.toUpperCase().startsWith(selectedEnchant.getName() + ";"));

            if (alreadyExists) {
                p.sendMessage(ConfigManager.fromSection("§cThis item already has the enchantment: " + selectedEnchant.getName()));
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }


            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("enchantments.add." + selectedEnchant.getName());
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSection("§aPlease type the Level for '" + selectedEnchant.getName() + "' in chat (e.g., 1, 5, 10)."));
            return;
        }

        if (handlePageChange(e, enchantments.size())) {
            return;
        }

        if (clickedType == Material.BARRIER && clickedSlot == 49) {
            new EnchantmentListMenu(playerMenuUtility, plugin).open();
            return;
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(45, makeItem(Material.ARROW, "§aPrevious Page"));
        inventory.setItem(53, makeItem(Material.ARROW, "§aNext Page"));
        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack to Enchantment List"));

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= enchantments.size()) break;

            Enchantment currentEnchant = enchantments.get(index);
            if (currentEnchant == null) continue;

            List<String> lore = new ArrayList<>();
            lore.add("§8ID: " + currentEnchant.getKey().getKey());
            lore.add("§7Click to select this enchantment.");

            ItemStack itemStack = makeItem(Material.ENCHANTED_BOOK, "§b" + currentEnchant.getName(),
                    lore.toArray(new String[0]));
            inventory.setItem(i, itemStack);
        }
        setFillerGlass();
    }
}