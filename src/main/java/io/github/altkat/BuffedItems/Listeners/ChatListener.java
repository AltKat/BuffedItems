package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Menu.*;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import io.papermc.paper.event.player.AsyncChatEvent; // <-- Değişti
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer; // <-- Eklendi
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.yaml.snakeyaml.serializer.Serializer;
// import org.bukkit.event.player.AsyncPlayerChatEvent; // <-- Kaldırıldı

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final BuffedItems plugin;

    private static final Pattern PERMISSION_NODE_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]+$");

    public ChatListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent e) { // <-- Değişti (AsyncPlayerChatEvent -> AsyncChatEvent)
        Player p = e.getPlayer();
        PlayerMenuUtility pmu = BuffedItems.getPlayerMenuUtility(p);

        if (pmu.isWaitingForChatInput()) {
            e.setCancelled(true);

            // Mesajı Adventure Component'ten düz metne çevir
            String input = PlainTextComponentSerializer.plainText().serialize(e.message()); // <-- Değişti

            String path = pmu.getChatInputPath();
            String itemId = pmu.getItemToEditId();
            String targetSlot = pmu.getTargetSlot();

            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Processing input from " + p.getName() + ": path=" + path + ", input=" + input);

            Bukkit.getScheduler().runTask(plugin, () -> {

                if ("createnewitem".equals(path)) {
                    String newItemId = input.toLowerCase().replaceAll("\\s+", "_");
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Creating new item: " + newItemId);

                    if (ConfigManager.createNewItem(newItemId)) {
                        p.sendMessage("§aNew item '" + newItemId + "' created. Now editing...");
                        pmu.setItemToEditId(newItemId);
                        new ItemEditorMenu(pmu, plugin).open();
                    } else {
                        p.sendMessage("§cError: An item with the ID '" + newItemId + "' already exists.");
                        new MainMenu(pmu, plugin).open();
                    }
                } else if (path.startsWith("lore.")) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Updating lore for item: " + itemId);
                    BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
                    if (item == null) {
                        p.sendMessage("§cError: Item '" + itemId + "' not found in memory.");
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
                            }else {
                                p.sendMessage("§cError: Invalid lore index.");
                                new LoreEditorMenu(pmu, plugin).open();
                                return;
                            }
                        } catch (NumberFormatException ex) {
                            p.sendMessage("§cError: Could not parse lore index from path: " + path);
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Could not parse lore index from path: " + path);
                            new LoreEditorMenu(pmu, plugin).open();
                            return;
                        }
                    }
                    ConfigManager.setItemValue(itemId, "lore", currentLore);
                    p.sendMessage("§aLore has been updated!");
                    new LoreEditorMenu(pmu, plugin).open();

                } else if ("attributes.edit".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Editing attribute amount for item: " + itemId + ", slot: " + targetSlot);
                    String configPath = "items." + itemId + ".effects." + targetSlot + ".attributes";
                    List<String> attributes = plugin.getConfig().getStringList(configPath);
                    int index = pmu.getEditIndex();

                    if (index != -1 && index < attributes.size()) {
                        try {
                            double newAmount = Double.parseDouble(input);
                            String[] parts = attributes.get(index).split(";");
                            if (parts.length == 3) {
                                String newAttributeString = parts[0] + ";" + parts[1] + ";" + newAmount;
                                attributes.set(index, newAttributeString);
                                ConfigManager.setItemValue(itemId, "effects." + targetSlot + ".attributes", attributes);
                                p.sendMessage("§aAttribute amount has been updated!");
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Updated attribute: " + newAttributeString + " for " + itemId);
                            } else {
                                p.sendMessage("§cError: Corrupted attribute data found in config for index " + index);
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Corrupted attribute data in config for item " + itemId + " at index " + index);
                            }
                        } catch (NumberFormatException ex) {
                            p.sendMessage("§cInvalid amount. Please enter a number (e.g., 2.0, -1.5).");
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            p.sendMessage("§cError: Could not parse existing attribute data at index " + index);
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Could not parse attribute data in config for item " + itemId + " at index " + index);
                        }
                    }else {
                        p.sendMessage("§cError: Invalid edit index for attribute.");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid edit index " + index + " for attributes on item " + itemId);
                    }
                    pmu.setEditIndex(-1);
                    new AttributeListMenu(pmu, plugin).open();
                } else if (path.startsWith("attributes.add.")) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Adding new attribute for item: " + itemId + ", slot: " + targetSlot);
                    String configPath = "items." + itemId + ".effects." + targetSlot + ".attributes";
                    List<String> attributes = plugin.getConfig().getStringList(configPath);
                    try {
                        String[] parts = path.substring(15).split("\\.");
                        String attributeName = parts[0];
                        String operationName = parts[1];
                        org.bukkit.attribute.Attribute.valueOf(attributeName.toUpperCase());
                        org.bukkit.attribute.AttributeModifier.Operation.valueOf(operationName.toUpperCase());

                        double amount = Double.parseDouble(input);

                        boolean exists = attributes.stream().anyMatch(s -> s.toUpperCase().startsWith(attributeName.toUpperCase() + ";"));
                        if (exists) {
                            p.sendMessage("§cError: This attribute already exists on the item in this slot.");
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Attribute " + attributeName + " already exists for " + itemId + " in slot " + targetSlot);
                            new AttributeListMenu(pmu, plugin).open();
                            return;
                        }

                        String newAttributeString = attributeName + ";" + operationName + ";" + amount;
                        attributes.add(newAttributeString);
                        ConfigManager.setItemValue(itemId, "effects." + targetSlot + ".attributes", attributes);
                        p.sendMessage("§aAttribute has been added!");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Added attribute: " + newAttributeString + " for " + itemId);
                    } catch (NumberFormatException ex) {
                        p.sendMessage("§cInvalid amount. Please enter a number (e.g., 2.0, -1.5).");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                    } catch (IllegalArgumentException ex) {
                        p.sendMessage("§cError: Invalid attribute or operation name in path: " + path + ". " + ex.getMessage());
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Failed to add attribute due to invalid path/enum: " + path + " - " + ex.getMessage());
                        plugin.getLogger().warning("[Chat] Failed to add attribute due to invalid path/enum: " + path);
                    } catch (Exception ex) {
                        p.sendMessage("§cAn unexpected error occurred. The original list was not modified.");
                        plugin.getLogger().warning("[Chat] Failed to add attribute: " + ex.getMessage());
                    }
                    new AttributeListMenu(pmu, plugin).open();
                } else if ("potion_effects.edit".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Editing potion effect level for item: " + itemId + ", slot: " + targetSlot);
                    String configPath = "items." + itemId + ".effects." + targetSlot + ".potion_effects";
                    List<String> effects = plugin.getConfig().getStringList(configPath);
                    int index = pmu.getEditIndex();

                    if (index != -1 && index < effects.size()) {
                        try {
                            int newLevel = Integer.parseInt(input);
                            if (newLevel <= 0) {
                                p.sendMessage("§cInvalid level. Please enter a positive whole number (e.g., 1, 2, 5).");
                                new PotionEffectListMenu(pmu, plugin).open();
                                return;
                            }
                            String[] parts = effects.get(index).split(";");
                            if (parts.length == 2) {
                                String effectName = parts[0];
                                String newEffectString = effectName + ";" + newLevel;
                                effects.set(index, newEffectString);
                                ConfigManager.setItemValue(itemId, "effects." + targetSlot + ".potion_effects", effects);
                                p.sendMessage("§aPotion effect level has been updated!");
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Updated effect: " + newEffectString + " for " + itemId);
                            } else {
                                p.sendMessage("§cError: Corrupted potion effect data found in config for index " + index);
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Corrupted potion effect data in config for item " + itemId + " at index " + index);
                            }
                        } catch (NumberFormatException ex) {
                            p.sendMessage("§cInvalid level. Please enter a whole number (e.g., 1, 2, 5).");
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            p.sendMessage("§cError: Could not parse existing potion effect data at index " + index);
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Could not parse potion effect data in config for item " + itemId + " at index " + index);
                        }
                    } else {
                        p.sendMessage("§cError: Invalid edit index for potion effect.");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid edit index " + index + " for potion effects on item " + itemId);
                    }
                    pmu.setEditIndex(-1);
                    new PotionEffectListMenu(pmu, plugin).open();
                } else if (path.startsWith("potion_effects.add.")) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Adding new potion effect for item: " + itemId + ", slot: " + targetSlot);
                    String configPath = "items." + itemId + ".effects." + targetSlot + ".potion_effects";
                    List<String> effects = new ArrayList<>(plugin.getConfig().getStringList(configPath));
                    try {
                        String effectName = path.substring(19);
                        if (org.bukkit.potion.PotionEffectType.getByName(effectName.toUpperCase()) == null) {
                            throw new IllegalArgumentException("Invalid PotionEffectType name");
                        }

                        int level = Integer.parseInt(input);
                        if (level <= 0) {
                            p.sendMessage("§cInvalid level. Please enter a positive whole number (e.g., 1, 2, 5).");
                            new PotionEffectListMenu(pmu, plugin).open();
                            return;
                        }

                        boolean exists = effects.stream().anyMatch(s -> s.toUpperCase().startsWith(effectName.toUpperCase() + ";"));
                        if (exists) {
                            p.sendMessage("§cError: This potion effect already exists on the item in this slot.");
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Effect " + effectName + " already exists for " + itemId + " in slot " + targetSlot);
                            new PotionEffectListMenu(pmu, plugin).open();
                            return;
                        }

                        String newEffectString = effectName + ";" + level;
                        effects.add(newEffectString);
                        ConfigManager.setItemValue(itemId, "effects." + targetSlot + ".potion_effects", effects);
                        p.sendMessage("§aEffect has been added!");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Added effect: " + newEffectString + " for " + itemId);
                    } catch (NumberFormatException ex) {
                        p.sendMessage("§cInvalid level. Please enter a whole number (e.g., 1, 2, 5).");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                    } catch (IllegalArgumentException ex) {
                        p.sendMessage("§cError: Invalid effect name in path: " + path + ". " + ex.getMessage());
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Failed to add effect due to invalid path/enum: " + path + " - " + ex.getMessage());
                        plugin.getLogger().warning("[Chat] Failed to add effect due to invalid path/enum: " + path);
                    } catch (Exception ex) {
                        p.sendMessage("§cAn unexpected error occurred. The original list was not modified.");
                        plugin.getLogger().warning("[Chat] Failed to add effect: " + ex.getMessage());
                    }
                    new PotionEffectListMenu(pmu, plugin).open();

                }else if ("enchantments.edit".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Editing enchantment level for item: " + itemId);
                    String configPath = "items." + itemId + ".enchantments";
                    List<String> enchantments = new ArrayList<>(plugin.getConfig().getStringList(configPath));
                    int index = pmu.getEditIndex();

                    if (index != -1 && index < enchantments.size()) {
                        try {
                            int newLevel = Integer.parseInt(input);
                            if (newLevel <= 0) {
                                p.sendMessage("§cInvalid level. Please enter a positive whole number (e.g., 1, 5, 10).");
                                new EnchantmentListMenu(pmu, plugin).open();
                                return;
                            }
                            String[] parts = enchantments.get(index).split(";");
                            if (parts.length == 2) {
                                String enchantName = parts[0];
                                if (Enchantment.getByName(enchantName.toUpperCase()) == null) {
                                    p.sendMessage("§cError: Corrupted enchantment name '" + enchantName + "' found in config.");
                                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Corrupted enchantment name in config for item " + itemId + " at index " + index + ": " + enchantName);
                                    new EnchantmentListMenu(pmu, plugin).open();
                                    return;
                                }
                                String newEnchantString = enchantName + ";" + newLevel;
                                enchantments.set(index, newEnchantString);
                                ConfigManager.setItemValue(itemId, "enchantments", enchantments);
                                p.sendMessage("§aEnchantment level has been updated!");
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Updated enchantment: " + newEnchantString + " for " + itemId);
                            } else {
                                p.sendMessage("§cError: Corrupted enchantment data found in config for index " + index);
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Corrupted enchantment data in config for item " + itemId + " at index " + index);
                            }
                        } catch (NumberFormatException ex) {
                            p.sendMessage("§cInvalid level. Please enter a whole number (e.g., 1, 5, 10).");
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                        } catch (ArrayIndexOutOfBoundsException ex) {
                            p.sendMessage("§cError: Could not parse existing enchantment data at index " + index);
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Could not parse enchantment data in config for item " + itemId + " at index " + index);
                        }
                    } else {
                        p.sendMessage("§cError: Invalid edit index for enchantment.");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid edit index " + index + " for enchantments on item " + itemId);
                    }
                    pmu.setEditIndex(-1);
                    new EnchantmentListMenu(pmu, plugin).open();

                } else if (path.startsWith("enchantments.add.")) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Adding new enchantment for item: " + itemId);
                    String configPath = "items." + itemId + ".enchantments";
                    List<String> enchantments = new ArrayList<>(plugin.getConfig().getStringList(configPath));
                    try {
                        String enchantName = path.substring(17);
                        if (Enchantment.getByName(enchantName.toUpperCase()) == null) {
                            throw new IllegalArgumentException("Invalid Enchantment name");
                        }

                        int level = Integer.parseInt(input);
                        if (level <= 0) {
                            p.sendMessage("§cInvalid level. Please enter a positive whole number (e.g., 1, 5, 10).");
                            new EnchantmentListMenu(pmu, plugin).open();
                            return;
                        }


                        boolean exists = enchantments.stream().anyMatch(s -> s.toUpperCase().startsWith(enchantName.toUpperCase() + ";"));
                        if (exists) {
                            p.sendMessage("§cError: This enchantment already exists on the item.");
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Enchantment " + enchantName + " already exists for " + itemId);
                            new EnchantmentListMenu(pmu, plugin).open();
                            return;
                        }

                        String newEnchantString = enchantName + ";" + level;
                        enchantments.add(newEnchantString);
                        ConfigManager.setItemValue(itemId, "enchantments", enchantments);
                        p.sendMessage("§aEnchantment has been added!");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Added enchantment: " + newEnchantString + " for " + itemId);
                    } catch (NumberFormatException ex) {
                        p.sendMessage("§cInvalid level. Please enter a whole number (e.g., 1, 5, 10).");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid number format from user " + p.getName() + ": " + input);
                    } catch (IllegalArgumentException ex) {
                        p.sendMessage("§cError: Invalid enchantment name in path: " + path + ". " + ex.getMessage());
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Failed to add enchantment due to invalid path/enum: " + path + " - " + ex.getMessage());
                        plugin.getLogger().warning("[Chat] Failed to add enchantment due to invalid path/enum: " + path);
                    } catch (Exception ex) {
                        p.sendMessage("§cAn unexpected error occurred. The original list was not modified.");
                        plugin.getLogger().warning("[Chat] Failed to add enchantment: " + ex.getMessage());
                    }
                    new EnchantmentListMenu(pmu, plugin).open();

                } else if ("permission".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Setting permission for item: " + itemId);

                    if ("none".equalsIgnoreCase(input) || "remove".equalsIgnoreCase(input)) {
                        ConfigManager.setItemValue(itemId, "permission", null);
                        p.sendMessage("§aPermission has been removed.");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Removed permission for " + itemId);
                    } else {
                        Matcher matcher = PERMISSION_NODE_PATTERN.matcher(input);
                        if (matcher.matches()) {
                            ConfigManager.setItemValue(itemId, "permission", input);
                            p.sendMessage("§aPermission has been set!");
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Set permission for " + itemId + " to: " + input);
                        } else {
                            p.sendMessage("§cInvalid permission node! Permissions can only contain letters, numbers, dots (.), hyphens (-), and underscores (_).");
                            p.sendMessage("§cYour input was: §e" + input);
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED, () -> "[Chat] Invalid permission node from " + p.getName() + ": " + input);
                        }
                    }
                    new ItemEditorMenu(pmu, plugin).open();

                } else if ("custom_model_data".equals(path)) {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                            () -> "[Chat] Setting custom-model-data for item: " + itemId);

                    if ("none".equalsIgnoreCase(input) || "remove".equalsIgnoreCase(input)) {
                        ConfigManager.setItemValue(itemId, "custom-model-data", null);
                        p.sendMessage("§aCustom Model Data has been removed.");
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                                () -> "[Chat] Removed custom-model-data for " + itemId);
                    } else {
                        try {
                            int directValue = Integer.parseInt(input);
                            if (directValue < 0) {
                                p.sendMessage("§cCustom Model Data must be positive!");
                                new ItemEditorMenu(pmu, plugin).open();
                                return;
                            }
                            ConfigManager.setItemValue(itemId, "custom-model-data", directValue);
                            p.sendMessage("§aCustom Model Data set to: §e" + directValue);
                            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                                    () -> "[Chat] Set direct custom-model-data for " + itemId + ": " + directValue);
                        } catch (NumberFormatException ex) {
                            if (input.contains(":")) {
                                ConfigManager.setItemValue(itemId, "custom-model-data", input);
                                p.sendMessage("§aCustom Model Data set to: §e" + input);
                                p.sendMessage("§7It will be resolved on next reload/save.");
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                                        () -> "[Chat] Set external custom-model-data for " + itemId + ": " + input);
                            } else {
                                p.sendMessage("§cInvalid format! Use:");
                                p.sendMessage("§e100001 §7(direct integer)");
                                p.sendMessage("§eitemsadder:item_id");
                                p.sendMessage("§enexo:item_id");
                                ConfigManager.sendDebugMessage(ConfigManager.DEBUG_DETAILED,
                                        () -> "[Chat] Invalid custom-model-data format from " + p.getName() + ": " + input);
                            }
                        }
                    }
                    new ItemEditorMenu(pmu, plugin).open();
                }
                else {
                    ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE, () -> "[Chat] Setting generic value: " + path + " = " + input + " for " + itemId);
                    if ("display_name".equals(path) || "material".equals(path)) {
                        ConfigManager.setItemValue(itemId, path, input);
                        p.sendMessage("§aValue for '" + path + "' has been updated!");
                    } else {
                        p.sendMessage("§cError: Unknown or unsupported direct chat edit path: " + path);
                        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO, () -> "[Chat] Attempted to set unknown path via chat: " + path + " for item " + itemId);
                        plugin.getLogger().warning("[Chat] Attempted to set unknown path via chat: " + path);
                        new MainMenu(pmu, plugin).open();
                        return;
                    }
                    new ItemEditorMenu(pmu, plugin).open();
                }
                pmu.setWaitingForChatInput(false);
                pmu.setChatInputPath(null);
            });
        }
    }
}