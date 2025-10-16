package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PotionEffectListMenu extends Menu {
    private final BuffedItems plugin;
    private final String TARGET_SLOT = "INVENTORY";

    public PotionEffectListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Effects for Slot: " + TARGET_SLOT;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        String itemId = playerMenuUtility.getItemToEditId();

        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                new ItemEditorMenu(playerMenuUtility, plugin).open();
                break;
            case ANVIL:
                new PotionEffectSelectorMenu(playerMenuUtility, plugin).open();
                break;
            case POTION:
                if (e.isRightClick()) {
                    String effectName = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

                    List<String> effects = plugin.getConfig().getStringList("items." + itemId + ".effects." + TARGET_SLOT + ".potion_effects");

                    effects.removeIf(s -> s.startsWith(effectName + ";"));

                    ConfigManager.setItemValue(itemId, "effects." + TARGET_SLOT + ".potion_effects", effects);
                    p.sendMessage("§aEffect '" + effectName + "' has been removed.");
                    this.open();
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd New Potion Effect", "§7Adds an effect to the §e" + TARGET_SLOT + " §7slot."));
        addBackButton(new ItemEditorMenu(playerMenuUtility, plugin));

        List<String> potionEffectsConfig = plugin.getConfig().getStringList("items." + playerMenuUtility.getItemToEditId() + ".effects." + TARGET_SLOT + ".potion_effects");

        if (!potionEffectsConfig.isEmpty()) {
            int slot = 0;
            for (String effectString : potionEffectsConfig) {
                if (slot >= getSlots() - 9) break;
                String[] parts = effectString.split(";");
                inventory.setItem(slot, makeItem(Material.POTION, "§b" + parts[0],
                        "§7Level: §e" + parts[1], "", "§cRight-Click to Delete"));
                slot++;
            }
        }
        setFillerGlass();
    }
}