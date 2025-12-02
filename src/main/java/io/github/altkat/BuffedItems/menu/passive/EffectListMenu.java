package io.github.altkat.BuffedItems.menu.passive;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.menu.active.ActiveItemSettingsMenu;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.AttributeSelectorMenu;
import io.github.altkat.BuffedItems.menu.selector.PotionEffectSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Generic menu for displaying and managing effects (Potion, Attribute)
 */
public class EffectListMenu extends Menu {

    private final BuffedItems plugin;
    private final EffectType effectType;
    private final String context;
    private final boolean isActive;
    private final boolean isSetBonus;

    public enum EffectType {
        POTION_EFFECT(Material.POTION),
        ATTRIBUTE(Material.IRON_SWORD);

        private final Material icon;

        EffectType(Material icon) {
            this.icon = icon;
        }

        public Material getIcon() {
            return icon;
        }
    }

    public EffectListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin,
                          EffectType effectType, String context) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.effectType = effectType;
        this.context = context;
        this.isActive = "ACTIVE".equals(context);
        this.isSetBonus = "SET_BONUS".equals(context);
        playerMenuUtility.setTargetSlot(context);
    }

    @Override
    public String getMenuName() {
        if (isSetBonus) {
            String setName = playerMenuUtility.getTempSetId();
            int count = playerMenuUtility.getTempBonusCount();
            return (effectType == EffectType.POTION_EFFECT ? "Potions: " : "Attrs: ") + setName + " (" + count + ")";
        }
        if (isActive) {
            return effectType == EffectType.POTION_EFFECT ?
                    "Active Potion Effects" : "Active Attributes";
        } else {
            return "Effects for Slot: " + context;
        }
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

        // Back button
        if (clickedType == Material.BARRIER && clickedSlot == 53) {
            openBackMenu();
            return;
        }

        // Add button
        if (clickedType == Material.ANVIL && clickedSlot == 49) {
            openSelectorMenu();
            return;
        }

        // Effect slots (click to edit/delete)
        if (clickedSlot < 45) {
            String configPath = buildConfigPath(itemId);
            List<String> effects;

            if (isSetBonus) {
                effects = SetsConfig.get().getStringList(configPath);
            } else {
                effects = ItemsConfig.get().getStringList(configPath);
            }

            if (clickedSlot >= effects.size()) return;

            if (e.isRightClick()) {
                // Delete
                String removedInfo = effects.get(clickedSlot);
                effects.remove(clickedSlot);

                if (isSetBonus) {
                    SetsConfig.get().set(configPath, effects);
                    SetsConfig.save();
                    plugin.getSetManager().loadSets(true);
                } else {
                    ConfigManager.setItemValue(itemId, extractConfigKey(configPath), effects);
                }

                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aRemoved: §e" + removedInfo));
                this.open();
            }
            else if (e.isLeftClick()) {
                // Edit Logic
                String effectName = effects.get(clickedSlot).split(";")[0];
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setEditIndex(clickedSlot);
                playerMenuUtility.setChatInputPath(buildChatPath("edit"));

                p.closeInventory();

                if (effectType == EffectType.POTION_EFFECT) {
                    p.sendMessage(ConfigManager.fromSectionWithPrefix("§aType new level for '" + effectName + "' in chat."));
                } else {
                    p.sendMessage(ConfigManager.fromSectionWithPrefix("§aType new amount for the attribute in chat."));
                }
                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            }
        }
    }

    @Override
    public void setMenuItems() {
        String desc;
        if (isSetBonus) {
            desc = "to the " + playerMenuUtility.getTempBonusCount() + "-piece bonus of " + playerMenuUtility.getTempSetId();
        } else {
            desc = "to the " + (isActive ? "active" : context.toLowerCase()) + " slot.";
        }

        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd New " + capitalizeEffectType(),
                "§7Adds a " + effectType.name().toLowerCase().replace("_", " ") + " " + desc));

        addBackButton(this);

        String itemId = playerMenuUtility.getItemToEditId();
        String configPath = buildConfigPath(itemId);

        List<String> effectsConfig;
        if (isSetBonus) {
            effectsConfig = SetsConfig.get().getStringList(configPath);
        } else {
            effectsConfig = ItemsConfig.get().getStringList(configPath);
        }

        if (!effectsConfig.isEmpty()) {
            for (int i = 0; i < effectsConfig.size(); i++) {
                if (i >= getSlots() - 9) break;

                String effectString = effectsConfig.get(i);
                String[] parts = effectString.split(";");

                boolean isValid = validateEffect(effectType, parts);

                if (isValid) {
                    String displayName = parts[0];
                    String level = parts.length > 1 ? parts[1] : "?";
                    String amount = parts.length > 2 ? parts[2] : "?";

                    String displayValue = effectType == EffectType.POTION_EFFECT ?
                            ("§7Level: §e" + level) :
                            ("§7Amount: §e" + amount);

                    inventory.setItem(i, makeItem(effectType.getIcon(), "§b" + displayName,
                            displayValue, "", "§aLeft-Click to Edit", "§cRight-Click to Delete"));
                } else {
                    inventory.setItem(i, makeItem(Material.BARRIER, "§c§lCORRUPT ENTRY",
                            "§7Malformed: " + effectString,
                            "§cRight-Click to Delete"));
                }
            }
        }

        setFillerGlass();
    }

    private void openBackMenu() {
        if (isSetBonus) {
            new io.github.altkat.BuffedItems.menu.set.SetBonusEffectSelectorMenu(playerMenuUtility, plugin).open();
        } else if (isActive) {
            new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
        } else {
            io.github.altkat.BuffedItems.menu.passive.SlotSelectionMenu.MenuType menuType =
                    effectType == EffectType.POTION_EFFECT ?
                            io.github.altkat.BuffedItems.menu.passive.SlotSelectionMenu.MenuType.POTION_EFFECT :
                            io.github.altkat.BuffedItems.menu.passive.SlotSelectionMenu.MenuType.ATTRIBUTE;
            new io.github.altkat.BuffedItems.menu.passive.SlotSelectionMenu(playerMenuUtility, plugin, menuType).open();
        }
    }

    private void openSelectorMenu() {
        playerMenuUtility.setTargetSlot(context);

        if (effectType == EffectType.POTION_EFFECT) {
            new PotionEffectSelectorMenu(playerMenuUtility, plugin).open();
        } else {
            new AttributeSelectorMenu(playerMenuUtility, plugin).open();
        }
    }

    private String buildConfigPath(String itemId) {
        if (isSetBonus) {
            String setId = playerMenuUtility.getTempSetId();
            int count = playerMenuUtility.getTempBonusCount();
            if (effectType == EffectType.POTION_EFFECT) {
                return "sets." + setId + ".bonuses." + count + ".potion_effects";
            } else {
                return "sets." + setId + ".bonuses." + count + ".attributes";
            }
        }

        if (isActive) {
            if (effectType == EffectType.POTION_EFFECT) {
                return "items." + itemId + ".active-mode.effects.potion_effects";
            } else {
                return "items." + itemId + ".active-mode.effects.attributes";
            }
        } else {
            if (effectType == EffectType.POTION_EFFECT) {
                return "items." + itemId + ".effects." + context + ".potion_effects";
            } else {
                return "items." + itemId + ".effects." + context + ".attributes";
            }
        }
    }

    private String extractConfigKey(String fullPath) {
        if (isSetBonus) {
            return fullPath;
        }
        String itemId = playerMenuUtility.getItemToEditId();
        String prefix = "items." + itemId + ".";
        if (fullPath.startsWith(prefix)) {
            return fullPath.substring(prefix.length());
        }
        return fullPath;
    }

    private String buildChatPath(String operation) {
        if (isSetBonus) {
            if (effectType == EffectType.POTION_EFFECT) {
                return "set.potion." + operation;
            } else {
                return "set.attribute." + operation;
            }
        }
        if (isActive) {
            if (effectType == EffectType.POTION_EFFECT) {
                return "active.potion_effects." + operation;
            } else {
                return "active.attributes." + operation;
            }
        } else {
            if (effectType == EffectType.POTION_EFFECT) {
                return "potion_effects." + operation;
            } else {
                return "attributes." + operation;
            }
        }
    }

    private boolean validateEffect(EffectType type, String[] parts) {
        if (parts.length < 2) return false;
        try {
            if (type == EffectType.POTION_EFFECT) {
                PotionEffectType.getByName(parts[0].toUpperCase());
                Integer.parseInt(parts[1]);
                return true;
            } else {
                org.bukkit.attribute.Attribute.valueOf(parts[0].toUpperCase());
                if (parts.length < 3) return false;
                org.bukkit.attribute.AttributeModifier.Operation.valueOf(parts[1].toUpperCase());
                Double.parseDouble(parts[2]);
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private String capitalizeEffectType() {
        return effectType == EffectType.POTION_EFFECT ? "Potion Effect" : "Attribute";
    }
}