package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Managers.ItemsConfig;
import io.github.altkat.BuffedItems.Menu.*;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final BuffedItems plugin;

    private static final Pattern PERMISSION_NODE_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9._-]*[a-z0-9])?$", Pattern.CASE_INSENSITIVE);

    public ChatListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        PlayerMenuUtility pmu = BuffedItems.getPlayerMenuUtility(p);

        if (pmu.isWaitingForChatInput()) {
            e.setCancelled(true);

            String input = PlainTextComponentSerializer.plainText().serialize(e.message());

            String path = pmu.getChatInputPath();
            String itemId = pmu.getItemToEditId();
            String targetSlot = pmu.getTargetSlot();

            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Processing input from " + p.getName() + ": path=" + path + ", input=" + input);

            Bukkit.getScheduler().runTask(plugin, () -> {

                if ("createnewitem".equals(path)) {
                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cItem creation cancelled."));
                        new MainMenu(pmu, plugin).open();
                    } else {
                        String newItemId = input.toLowerCase().replaceAll("\\s+", "_");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Creating new item: " + newItemId);

                        if (ConfigManager.createNewItem(newItemId)) {
                            p.sendMessage(ConfigManager.fromSection("§aNew item '" + newItemId + "' created. Now editing..."));
                            pmu.setItemToEditId(newItemId);
                            new ItemEditorMenu(pmu, plugin).open();
                        } else {
                            p.sendMessage(ConfigManager.fromSection("§cError: An item with the ID '" + newItemId + "' already exists."));
                            new MainMenu(pmu, plugin).open();
                        }
                    }
                } else if ("duplicateitem".equals(path)) {
                    String sourceItemId = pmu.getItemToEditId();

                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cDuplicate item cancelled."));
                        new MainMenu(pmu, plugin).open();
                    } else {
                        String newItemId = input.toLowerCase().replaceAll("\\s+", "_");

                        if (newItemId.equals(sourceItemId)) {
                            p.sendMessage(ConfigManager.fromSection("§cError: New ID cannot be the same as the source ID."));
                            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                            return;
                        }

                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Attempting to duplicate '" + sourceItemId + "' as '" + newItemId + "'");

                        String createdId = ConfigManager.duplicateItem(sourceItemId, newItemId);

                        if (createdId != null) {
                            p.sendMessage(ConfigManager.fromSection("§aItem '§e" + sourceItemId + "§a' successfully duplicated as '§e" + createdId + "§a'."));
                            new MainMenu(pmu, plugin).open();
                        } else {
                            p.sendMessage(ConfigManager.fromSection("§cError: An item with the ID '§e" + newItemId + "§c' already exists (or source item was invalid)."));
                            p.sendMessage(ConfigManager.fromSection("§aPlease try a different ID."));
                            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                            return;
                        }
                    }
                } else if (path.startsWith("lore.")) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Updating lore for item: " + itemId);

                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cLore editing cancelled."));
                        new LoreEditorMenu(pmu, plugin).open();
                    } else {
                        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
                        if (item == null) {
                            p.sendMessage(ConfigManager.fromSection("§cError: Item '" + itemId + "' not found in memory."));
                            new MainMenu(pmu, plugin).open();
                            return;
                        }
                        List<String> currentLore = new ArrayList<>(item.getLore());

                        if (path.equals("lore.add")) {
                            currentLore.add(input);
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Added new lore line to " + itemId);
                        } else {
                            try {
                                int index = Integer.parseInt(path.substring(5));
                                if (index >= 0 && index < currentLore.size()) {
                                    currentLore.set(index, input);
                                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Updated lore line " + index + " for " + itemId);
                                } else {
                                    p.sendMessage(ConfigManager.fromSection("§cError: Invalid lore index."));
                                    new LoreEditorMenu(pmu, plugin).open();
                                    return;
                                }
                            } catch (NumberFormatException ex) {
                                p.sendMessage(ConfigManager.fromSection("§cError: Could not parse lore index from path: " + path));
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Could not parse lore index from path: " + path);
                                new LoreEditorMenu(pmu, plugin).open();
                                return;
                            }
                        }
                        ConfigManager.setItemValue(itemId, "lore", currentLore);
                        p.sendMessage(ConfigManager.fromSection("§aLore has been updated!"));
                        new LoreEditorMenu(pmu, plugin).open();
                    }
                } else if ("active.attributes.edit".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Editing active attribute for item: " + itemId);
                    String configPath = "items." + itemId + ".active_effects.attributes";
                    List<String> attributes = ItemsConfig.get().getStringList(configPath);
                    int index = pmu.getEditIndex();

                    if (index != -1 && index < attributes.size()) {
                        try {
                            double newAmount = Double.parseDouble(input);
                            String[] parts = attributes.get(index).split(";");
                            if (parts.length == 3) {
                                String newAttributeString = parts[0] + ";" + parts[1] + ";" + newAmount;
                                attributes.set(index, newAttributeString);
                                ConfigManager.setItemValue(itemId, "active_effects.attributes", attributes);
                                p.sendMessage(ConfigManager.fromSection("§aActive attribute updated!"));
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Updated active attribute: " + newAttributeString);
                            } else {
                                p.sendMessage(ConfigManager.fromSection("§cError: Corrupted attribute data found."));
                            }
                        } catch (NumberFormatException ex) {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid amount. Please enter a number (e.g., 2.0)."));
                        }
                    } else {
                        p.sendMessage(ConfigManager.fromSection("§cError: Invalid attribute index."));
                    }
                    new ActiveAttributeListMenu(pmu, plugin).open();

                } else if ("attributes.edit".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Editing attribute amount for item: " + itemId + ", slot: " + targetSlot);
                    String configPath = "items." + itemId + ".effects." + targetSlot + ".attributes";
                    List<String> attributes = ItemsConfig.get().getStringList(configPath);
                    int index = pmu.getEditIndex();

                    if (index != -1 && index < attributes.size()) {
                        try {
                            double newAmount = Double.parseDouble(input);
                            String[] parts = attributes.get(index).split(";");
                            if (parts.length == 3) {
                                String newAttributeString = parts[0] + ";" + parts[1] + ";" + newAmount;
                                attributes.set(index, newAttributeString);
                                ConfigManager.setItemValue(itemId, "effects." + targetSlot + ".attributes", attributes);
                                p.sendMessage(ConfigManager.fromSection("§aAttribute amount has been updated!"));
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Updated attribute: " + newAttributeString + " for " + itemId);
                            } else {
                                p.sendMessage(ConfigManager.fromSection("§cError: Corrupted attribute data found in config for index " + index));
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Corrupted attribute data in config for item " + itemId + " at index " + index);
                            }
                        } catch (NumberFormatException ex) {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid amount. Please enter a number (e.g., 2.0, -1.5)."));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            p.sendMessage(ConfigManager.fromSection("§cError: Could not parse existing attribute data at index " + index));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Could not parse attribute data in config for item " + itemId + " at index " + index);
                        }
                    }else {
                        p.sendMessage(ConfigManager.fromSection("§cError: Invalid edit index for attribute."));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid edit index " + index + " for attributes on item " + itemId);
                    }
                    pmu.setEditIndex(-1);
                    new AttributeListMenu(pmu, plugin).open();

                } else if (path.startsWith("attributes.add.")) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Adding new attribute for item: " + itemId + ", slot: " + targetSlot);

                    String relativePath = "ACTIVE".equals(targetSlot)
                            ? "active_effects.attributes"
                            : "effects." + targetSlot + ".attributes";
                    String fullPath = "items." + itemId + "." + relativePath;

                    List<String> attributes = ItemsConfig.get().getStringList(fullPath);
                    try {
                        String[] parts = path.substring(15).split("\\.");
                        String attributeName = parts[0];
                        String operationName = parts[1];
                        org.bukkit.attribute.Attribute.valueOf(attributeName.toUpperCase());
                        org.bukkit.attribute.AttributeModifier.Operation.valueOf(operationName.toUpperCase());

                        double amount = Double.parseDouble(input);

                        boolean exists = attributes.stream().anyMatch(s -> s.toUpperCase().startsWith(attributeName.toUpperCase() + ";"));
                        if (exists) {
                            p.sendMessage(ConfigManager.fromSection("§cError: This attribute already exists on the item in this slot."));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Attribute " + attributeName + " already exists for " + itemId + " in slot " + targetSlot);
                        } else {
                            String newAttributeString = attributeName + ";" + operationName + ";" + amount;
                            attributes.add(newAttributeString);
                            ConfigManager.setItemValue(itemId, relativePath, attributes);
                            p.sendMessage(ConfigManager.fromSection("§aAttribute has been added!"));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Added attribute: " + newAttributeString + " for " + itemId);
                        }
                    } catch (NumberFormatException ex) {
                        p.sendMessage(ConfigManager.fromSection("§cInvalid amount. Please enter a number (e.g., 2.0, -1.5)."));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                    } catch (IllegalArgumentException ex) {
                        p.sendMessage(ConfigManager.fromSection("§cError: Invalid attribute or operation name in path: " + path + ". " + ex.getMessage()));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Failed to add attribute due to invalid path/enum: " + path + " - " + ex.getMessage());
                        plugin.getLogger().warning("[Chat] Failed to add attribute due to invalid path/enum: " + path);
                    } catch (Exception ex) {
                        p.sendMessage(ConfigManager.fromSection("§cAn unexpected error occurred. The original list was not modified."));
                        plugin.getLogger().warning("[Chat] Failed to add attribute: " + ex.getMessage());
                    }

                    if ("ACTIVE".equals(targetSlot)) {
                        new ActiveAttributeListMenu(pmu, plugin).open();
                    } else {
                        new AttributeListMenu(pmu, plugin).open();
                    }

                } else if ("active.potion_effects.edit".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Editing active potion effect for item: " + itemId);
                    String configPath = "items." + itemId + ".active_effects.potion_effects";
                    List<String> effects = ItemsConfig.get().getStringList(configPath);
                    int index = pmu.getEditIndex();

                    if (index != -1 && index < effects.size()) {
                        try {
                            int newLevel = Integer.parseInt(input);
                            if (newLevel <= 0) {
                                p.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a positive whole number."));
                                new ActivePotionEffectListMenu(pmu, plugin).open();
                                return;
                            }
                            String[] parts = effects.get(index).split(";");
                            if (parts.length == 2) {
                                String newEffectString = parts[0] + ";" + newLevel;
                                effects.set(index, newEffectString);
                                ConfigManager.setItemValue(itemId, "active_effects.potion_effects", effects);
                                p.sendMessage(ConfigManager.fromSection("§aActive potion effect updated!"));
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Updated active effect: " + newEffectString);
                            }
                        } catch (Exception ex) {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a whole number."));
                        }
                    } else {
                        p.sendMessage(ConfigManager.fromSection("§cError: Invalid edit index for potion effect."));
                    }
                    new ActivePotionEffectListMenu(pmu, plugin).open();

                } else if ("potion_effects.edit".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Editing potion effect level for item: " + itemId + ", slot: " + targetSlot);
                    String configPath = "items." + itemId + ".effects." + targetSlot + ".potion_effects";
                    List<String> effects = ItemsConfig.get().getStringList(configPath);
                    int index = pmu.getEditIndex();

                    if (index != -1 && index < effects.size()) {
                        try {
                            int newLevel = Integer.parseInt(input);
                            if (newLevel <= 0) {
                                p.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a positive whole number (e.g., 1, 2, 5)."));
                                new PotionEffectListMenu(pmu, plugin).open();
                                return;
                            }
                            String[] parts = effects.get(index).split(";");
                            if (parts.length == 2) {
                                String effectName = parts[0];
                                String newEffectString = effectName + ";" + newLevel;
                                effects.set(index, newEffectString);
                                ConfigManager.setItemValue(itemId, "effects." + targetSlot + ".potion_effects", effects);
                                p.sendMessage(ConfigManager.fromSection("§aPotion effect level has been updated!"));
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Updated effect: " + newEffectString + " for " + itemId);
                            } else {
                                p.sendMessage(ConfigManager.fromSection("§cError: Corrupted potion effect data found in config for index " + index));
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Corrupted potion effect data in config for item " + itemId + " at index " + index);
                            }
                        } catch (NumberFormatException ex) {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a whole number (e.g., 1, 2, 5)."));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            p.sendMessage(ConfigManager.fromSection("§cError: Could not parse existing potion effect data at index " + index));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Could not parse potion effect data in config for item " + itemId + " at index " + index);
                        }
                    } else {
                        p.sendMessage(ConfigManager.fromSection("§cError: Invalid edit index for potion effect."));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid edit index " + index + " for potion effects on item " + itemId);
                    }
                    pmu.setEditIndex(-1);
                    new PotionEffectListMenu(pmu, plugin).open();

                } else if (path.startsWith("potion_effects.add.")) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Adding new potion effect for item: " + itemId + ", slot: " + targetSlot);

                    String relativePath = "ACTIVE".equals(targetSlot)
                            ? "active_effects.potion_effects"
                            : "effects." + targetSlot + ".potion_effects";
                    String fullPath = "items." + itemId + "." + relativePath;

                    List<String> effects = new ArrayList<>(ItemsConfig.get().getStringList(fullPath));
                    try {
                        String effectName = path.substring(19);
                        if (org.bukkit.potion.PotionEffectType.getByName(effectName.toUpperCase()) == null) {
                            throw new IllegalArgumentException("Invalid PotionEffectType name");
                        }

                        int level = Integer.parseInt(input);
                        if (level <= 0) {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a positive whole number (e.g., 1, 2, 5)."));
                            if ("ACTIVE".equals(targetSlot)) new ActivePotionEffectListMenu(pmu, plugin).open();
                            else new PotionEffectListMenu(pmu, plugin).open();
                            return;
                        }

                        boolean exists = effects.stream().anyMatch(s -> s.toUpperCase().startsWith(effectName.toUpperCase() + ";"));
                        if (exists) {
                            p.sendMessage(ConfigManager.fromSection("§cError: This potion effect already exists on the item in this slot."));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Effect " + effectName + " already exists for " + itemId + " in slot " + targetSlot);
                        } else {
                            String newEffectString = effectName + ";" + level;
                            effects.add(newEffectString);
                            ConfigManager.setItemValue(itemId, relativePath, effects);
                            p.sendMessage(ConfigManager.fromSection("§aEffect has been added!"));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Added effect: " + newEffectString + " for " + itemId);
                        }
                    } catch (NumberFormatException ex) {
                        p.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a whole number (e.g., 1, 2, 5)."));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                    } catch (IllegalArgumentException ex) {
                        p.sendMessage(ConfigManager.fromSection("§cError: Invalid effect name in path: " + path + ". " + ex.getMessage()));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Failed to add effect due to invalid path/enum: " + path + " - " + ex.getMessage());
                        plugin.getLogger().warning("[Chat] Failed to add effect due to invalid path/enum: " + path);
                    } catch (Exception ex) {
                        p.sendMessage(ConfigManager.fromSection("§cAn unexpected error occurred. The original list was not modified."));
                        plugin.getLogger().warning("[Chat] Failed to add effect: " + ex.getMessage());
                    }

                    if ("ACTIVE".equals(targetSlot)) {
                        new ActivePotionEffectListMenu(pmu, plugin).open();
                    } else {
                        new PotionEffectListMenu(pmu, plugin).open();
                    }

                }else if ("enchantments.edit".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Editing enchantment level for item: " + itemId);
                    String configPath = "items." + itemId + ".enchantments";
                    List<String> enchantments = new ArrayList<>(ItemsConfig.get().getStringList(configPath));
                    int index = pmu.getEditIndex();

                    if (index != -1 && index < enchantments.size()) {
                        try {
                            int newLevel = Integer.parseInt(input);
                            if (newLevel <= 0) {
                                p.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a positive whole number (e.g., 1, 5, 10)."));
                                new EnchantmentListMenu(pmu, plugin).open();
                                return;
                            }
                            String[] parts = enchantments.get(index).split(";");
                            if (parts.length == 2) {
                                String enchantName = parts[0];
                                if (Enchantment.getByName(enchantName.toUpperCase()) == null) {
                                    p.sendMessage(ConfigManager.fromSection("§cError: Corrupted enchantment name '" + enchantName + "' found in config."));
                                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Corrupted enchantment name in config for item " + itemId + " at index " + index + ": " + enchantName);
                                    new EnchantmentListMenu(pmu, plugin).open();
                                    return;
                                }
                                String newEnchantString = enchantName + ";" + newLevel;
                                enchantments.set(index, newEnchantString);
                                ConfigManager.setItemValue(itemId, "enchantments", enchantments);
                                p.sendMessage(ConfigManager.fromSection("§aEnchantment level has been updated!"));
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Updated enchantment: " + newEnchantString + " for " + itemId);
                            } else {
                                p.sendMessage(ConfigManager.fromSection("§cError: Corrupted enchantment data found in config for index " + index));
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Corrupted enchantment data in config for item " + itemId + " at index " + index);
                            }
                        } catch (NumberFormatException ex) {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a whole number (e.g., 1, 5, 10)."));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            p.sendMessage(ConfigManager.fromSection("§cError: Could not parse existing enchantment data at index " + index));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Could not parse enchantment data in config for item " + itemId + " at index " + index);
                        }
                    } else {
                        p.sendMessage(ConfigManager.fromSection("§cError: Invalid edit index for enchantment."));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid edit index " + index + " for enchantments on item " + itemId);
                    }
                    pmu.setEditIndex(-1);
                    new EnchantmentListMenu(pmu, plugin).open();

                } else if (path.startsWith("enchantments.add.")) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Adding new enchantment for item: " + itemId);
                    String configPath = "items." + itemId + ".enchantments";
                    List<String> enchantments = new ArrayList<>(ItemsConfig.get().getStringList(configPath));
                    try {
                        String enchantName = path.substring(17);
                        if (Enchantment.getByName(enchantName.toUpperCase()) == null) {
                            throw new IllegalArgumentException("Invalid Enchantment name");
                        }

                        int level = Integer.parseInt(input);
                        if (level <= 0) {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a positive whole number (e.g., 1, 5, 10)."));
                            new EnchantmentListMenu(pmu, plugin).open();
                            return;
                        }


                        boolean exists = enchantments.stream().anyMatch(s -> s.toUpperCase().startsWith(enchantName.toUpperCase() + ";"));
                        if (exists) {
                            p.sendMessage(ConfigManager.fromSection("§cError: This enchantment already exists on the item."));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Enchantment " + enchantName + " already exists for " + itemId);
                            new EnchantmentListMenu(pmu, plugin).open();
                            return;
                        }

                        String newEnchantString = enchantName + ";" + level;
                        enchantments.add(newEnchantString);
                        ConfigManager.setItemValue(itemId, "enchantments", enchantments);
                        p.sendMessage(ConfigManager.fromSection("§aEnchantment has been added!"));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Added enchantment: " + newEnchantString + " for " + itemId);
                    } catch (NumberFormatException ex) {
                        p.sendMessage(ConfigManager.fromSection("§cInvalid level. Please enter a whole number (e.g., 1, 5, 10)."));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                    } catch (IllegalArgumentException ex) {
                        p.sendMessage(ConfigManager.fromSection("§cError: Invalid enchantment name in path: " + path + ". " + ex.getMessage()));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Failed to add enchantment due to invalid path/enum: " + path + " - " + ex.getMessage());
                        plugin.getLogger().warning("[Chat] Failed to add enchantment due to invalid path/enum: " + path);
                    } catch (Exception ex) {
                        p.sendMessage(ConfigManager.fromSection("§cAn unexpected error occurred. The original list was not modified."));
                        plugin.getLogger().warning("[Chat] Failed to add enchantment: " + ex.getMessage());
                    }
                    new EnchantmentListMenu(pmu, plugin).open();

                } else if ("permission".equals(path)) {

                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Setting permission for item: " + itemId);

                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cPermission setting cancelled."));
                        new ItemEditorMenu(pmu, plugin).open();

                    } else if ("none".equalsIgnoreCase(input) || "remove".equalsIgnoreCase(input)) {
                        ConfigManager.setItemValue(itemId, "permission", null);
                        p.sendMessage(ConfigManager.fromSection("§aPermission has been removed."));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Removed permission for " + itemId);
                        new ItemEditorMenu(pmu, plugin).open();

                    } else {
                        if (input.contains("..") || input.startsWith(".") || input.endsWith(".")) {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid permission node!"));
                            p.sendMessage(ConfigManager.fromSection("§cDo not use: double dots (..), leading/trailing dots"));
                            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                            return;
                        }

                        Matcher matcher = PERMISSION_NODE_PATTERN.matcher(input);
                        if (matcher.matches()) {
                            ConfigManager.setItemValue(itemId, "permission", input);
                            p.sendMessage(ConfigManager.fromSection("§aPermission has been set!"));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Set permission for " + itemId + " to: " + input);
                            new ItemEditorMenu(pmu, plugin).open();
                        } else {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid permission node! Permissions can only contain letters, numbers, dots (.), hyphens (-), and underscores (_)."));
                            p.sendMessage(ConfigManager.fromSection("§cYour input was: §e" + input));
                            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                            return;
                        }
                    }
                } else if ("material.manual".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Setting material manually for item: " + itemId);

                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cMaterial selection cancelled."));
                        new MaterialSelectorMenu(pmu, plugin).open();
                    } else {
                        String materialName = input.toUpperCase().replaceAll("\\s+", "_");
                        Material selectedMaterial = Material.matchMaterial(materialName);

                        if (selectedMaterial != null && selectedMaterial.isItem()) {
                            ConfigManager.setItemValue(itemId, "material", selectedMaterial.name());
                            p.sendMessage(ConfigManager.fromSection("§aMaterial has been updated to " + selectedMaterial.name()));
                            new ItemEditorMenu(pmu, plugin).open();
                        } else {
                            p.sendMessage(ConfigManager.fromSection("§cError: Invalid material name: '" + input + "'"));
                            p.sendMessage(ConfigManager.fromSection("§cMaterial not found or is not an item. Please try again."));
                            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                            return;
                        }
                    }

                } else if ("custom_model_data".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                            () -> "[Chat] Setting custom-model-data for item: " + itemId);

                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cCustom Model Data setting cancelled."));
                        new ItemEditorMenu(pmu, plugin).open();

                    } else if ("none".equalsIgnoreCase(input) || "remove".equalsIgnoreCase(input)) {
                        ConfigManager.setItemValue(itemId, "custom-model-data", null);
                        p.sendMessage(ConfigManager.fromSection("§aCustom Model Data has been removed."));
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                                () -> "[Chat] Removed custom-model-data for " + itemId);
                        new ItemEditorMenu(pmu, plugin).open();

                    } else {
                        try {
                            int directValue = Integer.parseInt(input);
                            if (directValue < 0) {
                                p.sendMessage(ConfigManager.fromSection("§cCustom Model Data must be positive!"));
                                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                                return;
                            }
                            ConfigManager.setItemValue(itemId, "custom-model-data", directValue);
                            p.sendMessage(ConfigManager.fromSection("§aCustom Model Data set to: §e" + directValue));
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                                    () -> "[Chat] Set direct custom-model-data for " + itemId + ": " + directValue);
                            new ItemEditorMenu(pmu, plugin).open();

                        } catch (NumberFormatException ex) {
                            if (input.contains(":")) {
                                ConfigManager.setItemValue(itemId, "custom-model-data", input);
                                p.sendMessage(ConfigManager.fromSection("§aCustom Model Data set to: §e" + input));
                                p.sendMessage(ConfigManager.fromSection("§7It will be resolved on next reload/save."));
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                                        () -> "[Chat] Set external custom-model-data for " + itemId + ": " + input);
                                new ItemEditorMenu(pmu, plugin).open();
                            } else {
                                p.sendMessage(ConfigManager.fromSection("§cInvalid format! Use:"));
                                p.sendMessage(ConfigManager.fromSection("§e100001 §7(direct integer)"));
                                p.sendMessage(ConfigManager.fromSection("§eitemsadder:item_id"));
                                p.sendMessage(ConfigManager.fromSection("§enexo:item_id"));
                                p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                                        () -> "[Chat] Invalid custom-model-data format from " + p.getName() + ": " + input);
                                return;
                            }
                        }
                    }
                }else if ("active.cooldown".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Setting cooldown for item: " + itemId);
                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cCancelled."));
                    } else {
                        try {
                            int val = Integer.parseInt(input);
                            if (val < 0) throw new NumberFormatException();

                            ConfigManager.setItemValue(itemId, "cooldown", val);
                            p.sendMessage(ConfigManager.fromSection("§aCooldown updated to: §e" + val + "s"));
                        } catch (NumberFormatException error) {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid number! Please enter a positive integer (e.g. 10)."));
                            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                            return;
                        }
                    }
                    new ActiveItemSettingsMenu(pmu, plugin).open();

                } else if ("active.duration".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Setting duration for item: " + itemId);
                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cCancelled."));
                    } else {
                        try {
                            int val = Integer.parseInt(input);
                            if (val < 0) throw new NumberFormatException();

                            ConfigManager.setItemValue(itemId, "effect_duration", val);
                            p.sendMessage(ConfigManager.fromSection("§aEffect Duration updated to: §e" + val + "s"));
                        } catch (NumberFormatException error) {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid number! Please enter a positive integer (e.g. 5)."));
                            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                            return;
                        }
                    }
                    new ActiveItemSettingsMenu(pmu, plugin).open();

                }else if ("active.commands.add".equals(path)) {
                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cCancelled."));
                    } else {
                        List<String> commands = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + ".commands"));
                        commands.add(input);
                        ConfigManager.setItemValue(itemId, "commands", commands);
                        p.sendMessage(ConfigManager.fromSection("§aCommand added!"));
                    }
                    new CommandListMenu(pmu, plugin).open();

                } else if (path.startsWith("active.commands.edit.")) {
                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cCancelled."));
                    } else {
                        try {
                            int index = Integer.parseInt(path.substring(21));
                            List<String> commands = new ArrayList<>(ItemsConfig.get().getStringList("items." + itemId + ".commands"));

                            if (index >= 0 && index < commands.size()) {
                                commands.set(index, input);
                                ConfigManager.setItemValue(itemId, "commands", commands);
                                p.sendMessage(ConfigManager.fromSection("§aCommand updated!"));
                            } else {
                                p.sendMessage(ConfigManager.fromSection("§cError: Command index out of bounds."));
                            }
                        } catch (Exception error) {
                            p.sendMessage(ConfigManager.fromSection("§cError processing edit."));
                        }
                    }
                    new CommandListMenu(pmu, plugin).open();

                }else if (path.startsWith("active.msg.")) {
                    String configPath = path.replace("active.msg.", "visuals.messages.").replace("actionbar", "action-bar").replace("bossbar", "boss-bar");

                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cCancelled."));
                    } else if (input.equalsIgnoreCase("default")) {
                        ConfigManager.setItemValue(itemId, configPath, null);

                        if(path.endsWith("title")) ConfigManager.setItemValue(itemId, "visuals.messages.subtitle", null);
                        p.sendMessage(ConfigManager.fromSection("§aReset to default."));
                    } else {
                        if (path.endsWith("title")) {

                            if (input.contains("|")) {
                                String[] parts = input.split("\\|", 2);
                                ConfigManager.setItemValue(itemId, configPath, parts[0].trim());
                                ConfigManager.setItemValue(itemId, "visuals.messages.subtitle", parts[1].trim());
                                p.sendMessage(ConfigManager.fromSection("§aTitle and Subtitle updated!"));
                            } else {
                                ConfigManager.setItemValue(itemId, configPath, input);
                                p.sendMessage(ConfigManager.fromSection("§aTitle updated (Subtitle unchanged)."));
                            }
                        } else {
                            ConfigManager.setItemValue(itemId, configPath, input);
                            p.sendMessage(ConfigManager.fromSection("§aMessage updated!"));
                        }
                    }
                    new ActiveItemVisualsMenu(pmu, plugin).open();

                }else if (path.startsWith("active.sounds.")) {
                    String type = path.substring(14);

                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cCancelled."));
                    } else {

                        boolean valid = true;
                        if (input.contains(";")) {
                            String[] parts = input.split(";");
                            try {
                                if (parts.length > 1) Float.parseFloat(parts[1]);
                                if (parts.length > 2) Float.parseFloat(parts[2]);
                            } catch (NumberFormatException error) {
                                valid = false;
                            }
                        }

                        if (valid) {
                            ConfigManager.setItemValue(itemId, "sounds." + type, input);
                            p.sendMessage(ConfigManager.fromSection("§aSound updated!"));

                            try {
                                String soundName = input.split(";")[0];
                                try {
                                    org.bukkit.Sound s = org.bukkit.Sound.valueOf(soundName.toUpperCase());
                                    p.playSound(p.getLocation(), s, 1f, 1f);
                                } catch (IllegalArgumentException ignored) {}
                            } catch (Exception ignored) {}

                        } else {
                            p.sendMessage(ConfigManager.fromSection("§cInvalid format! Use: NAME;VOLUME;PITCH"));
                            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
                            return;
                        }
                    }
                    new SoundSettingsMenu(pmu, plugin, type).open();

                } else if ("display_name".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Setting display_name for item: " + itemId);
                    if (input.equalsIgnoreCase("cancel")) {
                        p.sendMessage(ConfigManager.fromSection("§cDisplay name change cancelled."));
                    } else {
                        ConfigManager.setItemValue(itemId, "display_name", input);
                        p.sendMessage(ConfigManager.fromSection("§aDisplay name has been updated!"));
                    }
                    new ItemEditorMenu(pmu, plugin).open();

                } else if ("material".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Setting generic value: " + path + " = " + input + " for " + itemId);
                    ConfigManager.setItemValue(itemId, path, input);
                    p.sendMessage(ConfigManager.fromSection("§aValue for '" + path + "' has been updated!"));
                    new ItemEditorMenu(pmu, plugin).open();

                } else {
                    p.sendMessage(ConfigManager.fromSection("§cError: Unknown or unsupported direct chat edit path: " + path));
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Attempted to set unknown path via chat: " + path + " for item " + itemId);
                    plugin.getLogger().warning("[Chat] Attempted to set unknown path via chat: " + path);
                    new MainMenu(pmu, plugin).open();
                    return;
                }

                pmu.setWaitingForChatInput(false);
                pmu.setChatInputPath(null);
            });
        }
    }
}