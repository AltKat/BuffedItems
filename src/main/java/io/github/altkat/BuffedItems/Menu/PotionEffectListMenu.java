package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class PotionEffectListMenu extends Menu {
    private final BuffedItems plugin;

    public PotionEffectListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Effects for Slot: " + playerMenuUtility.getTargetSlot();
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        String itemId = playerMenuUtility.getItemToEditId();
        String targetSlot = playerMenuUtility.getTargetSlot();

        if (e.getCurrentItem() == null) return;

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.POTION_EFFECT).open();
                break;
            case ANVIL:
                new PotionEffectSelectorMenu(playerMenuUtility, plugin).open();
                break;
            case POTION:
                String configPath = "items." + itemId + ".effects." + targetSlot + ".potion_effects";
                List<String> effects = plugin.getConfig().getStringList(configPath);
                int clickedSlot = e.getSlot();

                if (clickedSlot >= effects.size()) return;

                String effectString = effects.get(clickedSlot);
                String effectName = effectString.split(";")[0];

                if (e.isRightClick()) {
                    effects.remove(clickedSlot);
                    ConfigManager.setItemValue(itemId, "effects." + targetSlot + ".potion_effects", effects);
                    p.sendMessage("§aEffect '" + effectName + "' has been removed from slot " + targetSlot + ".");
                    this.open();
                } else if (e.isLeftClick()) {
                    playerMenuUtility.setWaitingForChatInput(true);
                    playerMenuUtility.setEditIndex(clickedSlot);
                    playerMenuUtility.setChatInputPath("potion_effects.edit");
                    p.closeInventory();
                    p.sendMessage("§aPlease type the new level for '" + effectName + "' in chat.");
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd New Potion Effect", "§7Adds an effect to the §e" + playerMenuUtility.getTargetSlot() + " §7slot."));
        addBackButton(new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.POTION_EFFECT));

        String configPath = "items." + playerMenuUtility.getItemToEditId() + ".effects." + playerMenuUtility.getTargetSlot() + ".potion_effects";
        List<String> potionEffectsConfig = plugin.getConfig().getStringList(configPath);

        if (!potionEffectsConfig.isEmpty()) {
            for (int i = 0; i < potionEffectsConfig.size(); i++) {
                if (i >= getSlots() - 9) break;
                String effectString = potionEffectsConfig.get(i);
                String[] parts = effectString.split(";");
                inventory.setItem(i, makeItem(Material.POTION, "§b" + parts[0],
                        "§7Level: §e" + parts[1], "", "§aLeft-Click to Edit Level", "§cRight-Click to Delete"));
            }
        }
        setFillerGlass();
    }
}