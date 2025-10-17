package io.github.altkat.BuffedItems.Menu;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.potion.PotionEffectType;

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

        playerMenuUtility.setNavigating(true);

        Material clickedType = e.getCurrentItem().getType();
        int clickedSlot = e.getSlot();

        if (clickedType == Material.BARRIER && clickedSlot == getSlots() - 1) {
            new SlotSelectionMenu(playerMenuUtility, plugin, SlotSelectionMenu.MenuType.POTION_EFFECT).open();
            return;
        }

        if (clickedType == Material.ANVIL && clickedSlot == 49) {
            new PotionEffectSelectorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (clickedSlot < 45) {
            String configPath = "items." + itemId + ".effects." + targetSlot + ".potion_effects";
            List<String> effects = plugin.getConfig().getStringList(configPath);

            if (clickedSlot >= effects.size()) {
                playerMenuUtility.setNavigating(false);
                return;
            }

            if (e.isRightClick()) {
                String removedInfo = effects.get(clickedSlot);
                effects.remove(clickedSlot);
                ConfigManager.setItemValue(itemId, "effects." + targetSlot + ".potion_effects", effects);
                p.sendMessage("§aEffect '" + removedInfo.split(";")[0] + "' has been removed from slot " + targetSlot + ".");
                this.open();
            }
            else if (e.isLeftClick() && clickedType == Material.POTION) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setEditIndex(clickedSlot);
                playerMenuUtility.setChatInputPath("potion_effects.edit");
                String effectName = effects.get(clickedSlot).split(";")[0];
                p.closeInventory();
                p.sendMessage("§aPlease type the new level for '" + effectName + "' in chat.");
            } else {
                playerMenuUtility.setNavigating(false);
            }
        } else {
            playerMenuUtility.setNavigating(false);
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

                boolean isValid = false;
                if (parts.length == 2) {
                    try {
                        Integer.parseInt(parts[1]);
                        if (PotionEffectType.getByName(parts[0]) != null) {
                            isValid = true;
                        }
                    } catch (NumberFormatException e) {
                        isValid = false;
                    }
                }

                if (isValid) {
                    inventory.setItem(i, makeItem(Material.POTION, "§b" + parts[0],
                            "§7Level: §e" + parts[1], "", "§aLeft-Click to Edit Level", "§cRight-Click to Delete"));
                } else {
                    inventory.setItem(i, makeItem(Material.BARRIER, "§c§lCORRUPT ENTRY",
                            "§7This line is malformed in config.yml:",
                            "§e" + effectString,
                            "",
                            "§cPossible Errors:",
                            "§7- Using ':' instead of the correct ';'.",
                            "§7- Missing the level (e.g., 'SPEED;').",
                            "§7- A typo in the Effect name (e.g., 'SPED').",
                            "§7- Level is not a whole number (e.g., '1.5').",
                            "§7- Accidental spaces before/after values.",
                            "",
                            "§aCorrect Format: §eFAST_DIGGING;2",
                            "",
                            "§cRight-Click to Delete this entry."));
                }
            }
        }
        setFillerGlass();
    }
}