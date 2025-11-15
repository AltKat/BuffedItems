package io.github.altkat.BuffedItems.menu.passive;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
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
 * Replaces: PotionEffectListMenu, ActivePotionEffectListMenu,
 *           AttributeListMenu, ActiveAttributeListMenu
 *
 * Usage:
 *   - Passive Potion Effects: new EffectListMenu(pmu, plugin, EffectType.POTION, "MAIN_HAND").open();
 *   - Active Potion Effects:  new EffectListMenu(pmu, plugin, EffectType.POTION, "ACTIVE").open();
 *   - Passive Attributes:     new EffectListMenu(pmu, plugin, EffectType.ATTRIBUTE, "HELMET").open();
 *   - Active Attributes:      new EffectListMenu(pmu, plugin, EffectType.ATTRIBUTE, "ACTIVE").open();
 */
public class EffectListMenu extends Menu {

    private final BuffedItems plugin;
    private final EffectType effectType;
    private final String context;
    private final boolean isActive;

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
    }

    @Override
    public String getMenuName() {
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
            List<String> effects = ItemsConfig.get().getStringList(configPath);

            if (clickedSlot >= effects.size()) return;

            if (e.isRightClick()) {
                // Delete
                String removedInfo = effects.get(clickedSlot);
                effects.remove(clickedSlot);
                ConfigManager.setItemValue(itemId, extractConfigKey(configPath), effects);
                p.sendMessage(ConfigManager.fromSection("§aRemoved: §e" + removedInfo));
                this.open();
            }
            else if (e.isLeftClick() && clickedType == Material.POTION) {
                // Edit potion effect
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setEditIndex(clickedSlot);
                playerMenuUtility.setChatInputPath(buildChatPath("edit"));
                p.closeInventory();
                String effectName = effects.get(clickedSlot).split(";")[0];
                p.sendMessage(ConfigManager.fromSection("§aType new level for '" + effectName + "' in chat."));
            }
            else if (e.isLeftClick() && clickedType == Material.IRON_SWORD) {
                // Edit attribute
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setEditIndex(clickedSlot);
                playerMenuUtility.setChatInputPath(buildChatPath("edit"));
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSection("§aType new amount for the attribute in chat."));
            }
        }
    }

    @Override
    public void setMenuItems() {
        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd New " + capitalizeEffectType(),
                "§7Adds a " + effectType.name().toLowerCase().replace("_", " ") +
                        " to the " + (isActive ? "active" : context.toLowerCase()) + " slot."));

        addBackButton(this);

        String itemId = playerMenuUtility.getItemToEditId();
        String configPath = buildConfigPath(itemId);
        List<String> effectsConfig = ItemsConfig.get().getStringList(configPath);

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
                            "§7This line is malformed in config.yml:",
                            "§e" + effectString,
                            "",
                            "§cPossible Errors:",
                            "§7- Using ':' instead of ';'",
                            "§7- Missing a value",
                            "§7- Invalid " + effectType.name().toLowerCase() + " name",
                            "§7- Amount/level is not a number",
                            "§7- Accidental spaces",
                            "",
                            "§cRight-Click to Delete this entry."));
                }
            }
        }

        setFillerGlass();
    }

    /**
     * Opens the appropriate back menu based on context
     */
    private void openBackMenu() {
        if (isActive) {
            // Active items go back to settings menu
            new ActiveItemSettingsMenu(playerMenuUtility, plugin).open();
        } else {
            // Passive items go back to slot selection
            SlotSelectionMenu.MenuType menuType =
                    effectType == EffectType.POTION_EFFECT ?
                            SlotSelectionMenu.MenuType.POTION_EFFECT :
                            SlotSelectionMenu.MenuType.ATTRIBUTE;
            new SlotSelectionMenu(playerMenuUtility, plugin, menuType).open();
        }
    }

    /**
     * Opens the appropriate selector menu to add new effects
     */
    private void openSelectorMenu() {
        if (effectType == EffectType.POTION_EFFECT) {
            new PotionEffectSelectorMenu(playerMenuUtility, plugin).open();
        } else {
            new AttributeSelectorMenu(playerMenuUtility, plugin).open();
        }
    }

    /**
     * Builds the full config path for effects
     */
    private String buildConfigPath(String itemId) {
        if (isActive) {
            if (effectType == EffectType.POTION_EFFECT) {
                return "items." + itemId + ".active_effects.potion_effects";
            } else {
                return "items." + itemId + ".active_effects.attributes";
            }
        } else {
            if (effectType == EffectType.POTION_EFFECT) {
                return "items." + itemId + ".effects." + context + ".potion_effects";
            } else {
                return "items." + itemId + ".effects." + context + ".attributes";
            }
        }
    }

    /**
     * Extracts just the config key part (without "items.itemId." prefix)
     */
    private String extractConfigKey(String fullPath) {
        String itemId = playerMenuUtility.getItemToEditId();
        String prefix = "items." + itemId + ".";
        return fullPath.substring(prefix.length());
    }

    /**
     * Builds the chat input path for edit/add operations
     */
    private String buildChatPath(String operation) {
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

    /**
     * Validates if the effect entry is properly formatted
     */
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

    /**
     * Capitalizes effect type for display
     */
    private String capitalizeEffectType() {
        if (effectType == EffectType.POTION_EFFECT) {
            return "Potion Effect";
        } else {
            return "Attribute";
        }
    }
}