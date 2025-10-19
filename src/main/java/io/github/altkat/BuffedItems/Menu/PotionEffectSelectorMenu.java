package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PotionEffectSelectorMenu extends PaginatedMenu {
    private final BuffedItems plugin;
    private final List<PotionEffectType> effectTypes;

    public PotionEffectSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.effectTypes = Arrays.stream(PotionEffectType.values()).collect(Collectors.toList());
        this.maxItemsPerPage = 45;
    }

    @Override
    public String getMenuName() {
        return "Select an Effect (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;

        if (handlePageChange(e, effectTypes.size())) {
            return;
        }

        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new PotionEffectListMenu(playerMenuUtility, plugin).open();
            return;
        }


        Player p = (Player) e.getWhoClicked();
        String effectName = e.getCurrentItem().getItemMeta().getDisplayName().substring(2);


        playerMenuUtility.setWaitingForChatInput(true);

        playerMenuUtility.setChatInputPath("potion_effects.add." + effectName);
        p.closeInventory();
        p.sendMessage("§aPlease type the Potion Level (e.g., 1, 2, 5) in chat.");
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(45, makeItem(Material.ARROW, "§aPrevious Page"));
        inventory.setItem(53, makeItem(Material.ARROW, "§aNext Page"));
        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack to Effect List"));

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= effectTypes.size()) break;

            PotionEffectType currentType = effectTypes.get(index);
            if (currentType == null) continue;

            ItemStack itemStack = makeItem(Material.POTION, "§b" + currentType.getName(), "§7Click to select this effect.");
            inventory.setItem(i, itemStack);
        }
    }
}