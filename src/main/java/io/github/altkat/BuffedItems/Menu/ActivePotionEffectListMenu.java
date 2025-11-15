package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Managers.ItemsConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class ActivePotionEffectListMenu extends Menu {
    private final BuffedItems plugin;

    public ActivePotionEffectListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Active Potion Effects";
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
        Material clickedType = e.getCurrentItem().getType();
        int clickedSlot = e.getSlot();

        if (clickedType == Material.BARRIER && clickedSlot == 53) {
            new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (clickedType == Material.ANVIL && clickedSlot == 49) {
            playerMenuUtility.setTargetSlot("ACTIVE");
            new PotionEffectSelectorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (clickedSlot < 45) {
            String configPath = "items." + itemId + ".active_effects.potion_effects";
            List<String> effects = ItemsConfig.get().getStringList(configPath);

            if (clickedSlot >= effects.size()) return;

            if (e.isRightClick()) {
                effects.remove(clickedSlot);
                ConfigManager.setItemValue(itemId, "active_effects.potion_effects", effects);
                p.sendMessage(ConfigManager.fromSection("§aActive effect removed."));
                this.open();
            }
            else if (e.isLeftClick() && clickedType == Material.POTION) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setEditIndex(clickedSlot);
                playerMenuUtility.setChatInputPath("active.potion_effects.edit");

                String effectName = effects.get(clickedSlot).split(";")[0];
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSection("§aType new level for '" + effectName + "' in chat."));
            }
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd Active Effect", "§7Adds an effect to the active list."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack"));

        String configPath = "items." + playerMenuUtility.getItemToEditId() + ".active_effects.potion_effects";
        List<String> potionEffectsConfig = ItemsConfig.get().getStringList(configPath);

        if (!potionEffectsConfig.isEmpty()) {
            for (int i = 0; i < potionEffectsConfig.size(); i++) {
                if (i >= 45) break;
                String effectString = potionEffectsConfig.get(i);
                String[] parts = effectString.split(";");

                boolean valid = false;
                if (parts.length == 2) {
                    try {
                        PotionEffectType.getByName(parts[0]);
                        Integer.parseInt(parts[1]);
                        valid = true;
                    } catch (Exception ignored) {}
                }

                if (valid) {
                    inventory.setItem(i, makeItem(Material.POTION, "§b" + parts[0],
                            "§7Level: §e" + parts[1], "", "§aLeft-Click to Edit", "§cRight-Click to Delete"));
                } else {
                    inventory.setItem(i, makeItem(Material.BARRIER, "§cCorrupt Entry", "§7" + effectString));
                }
            }
        }
    }
}