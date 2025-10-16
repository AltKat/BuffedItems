package io.github.altkat.BuffedItems.Listeners;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.Managers.ConfigManager;
import io.github.altkat.BuffedItems.Menu.*;
import io.github.altkat.BuffedItems.utils.BuffedItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;

public class ChatListener implements Listener {

    private final BuffedItems plugin;
    private final String TARGET_SLOT = "INVENTORY";

    public ChatListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        PlayerMenuUtility pmu = BuffedItems.getPlayerMenuUtility(p);

        if (pmu.isWaitingForChatInput()) {
            e.setCancelled(true);
            String input = e.getMessage();
            String path = pmu.getChatInputPath();
            String itemId = pmu.getItemToEditId();

            Bukkit.getScheduler().runTask(plugin, () -> {


                if (path.equals("createnewitem")) {
                    String newItemId = input.toLowerCase().replaceAll("\\s+", "_");
                    if (ConfigManager.createNewItem(newItemId)) {
                        p.sendMessage("§aNew item '" + newItemId + "' created. Now editing...");
                        pmu.setItemToEditId(newItemId);
                        new ItemEditorMenu(pmu, plugin).open();
                    } else {
                        p.sendMessage("§cError: An item with the ID '" + newItemId + "' already exists.");
                        new MainMenu(pmu, plugin).open();
                    }

                } else if (path.startsWith("lore.")) {
                    BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
                    List<String> currentLore = new ArrayList<>(item.getLore());

                    if (path.equals("lore.add")) {
                        currentLore.add(input);
                    } else {
                        try {
                            int index = Integer.parseInt(path.substring(5));
                            if (index >= 0 && index < currentLore.size()) {
                                currentLore.set(index, input);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    ConfigManager.setItemValue(itemId, "lore", currentLore);
                    p.sendMessage("§aLore has been updated!");
                    new LoreEditorMenu(pmu, plugin).open();

                } else if (path.startsWith("attributes.add.")) {
                    String configPath = "items." + itemId + ".effects." + TARGET_SLOT + ".attributes";
                    List<String> attributes = plugin.getConfig().getStringList(configPath);
                    try {
                        String[] parts = path.substring(15).split("\\.");
                        String attributeName = parts[0];
                        String operationName = parts[1];
                        double amount = Double.parseDouble(input);

                        for (String existingAttr : attributes) {
                            if (existingAttr.startsWith(attributeName + ";")) {
                                p.sendMessage("§cError: This attribute already exists on the item.");
                                new AttributeListMenu(pmu, plugin).open();
                                return;
                            }
                        }

                        String newAttributeString = attributeName + ";" + operationName + ";" + amount;
                        attributes.add(newAttributeString);
                        ConfigManager.setItemValue(itemId, "effects." + TARGET_SLOT + ".attributes", attributes);
                        p.sendMessage("§aAttribute has been added!");
                    } catch (Exception ex) {
                        p.sendMessage("§cInvalid input. The original list was not modified.");
                    }
                    new AttributeListMenu(pmu, plugin).open();


                } else if (path.startsWith("potion_effects.add.")) {
                    String configPath = "items." + itemId + ".effects." + TARGET_SLOT + ".potion_effects";
                    List<String> effects = plugin.getConfig().getStringList(configPath);
                    try {
                        String effectName = path.substring(19);
                        int level = Integer.parseInt(input);

                        for (String existingEffect : effects) {
                            if (existingEffect.startsWith(effectName + ";")) {
                                p.sendMessage("§cError: This potion effect already exists on the item.");
                                new PotionEffectListMenu(pmu, plugin).open();
                                return;
                            }
                        }

                        String newEffectString = effectName + ";" + level;
                        effects.add(newEffectString);
                        ConfigManager.setItemValue(itemId, "effects." + TARGET_SLOT + ".potion_effects", effects);
                        p.sendMessage("§aEffect has been added!");
                    } catch (Exception ex) {
                        p.sendMessage("§cInvalid input. The original list was not modified.");
                    }
                    new PotionEffectListMenu(pmu, plugin).open();


                } else if ("permission".equals(path)) {
                    if ("none".equalsIgnoreCase(input) || "remove".equalsIgnoreCase(input)) {
                        ConfigManager.setItemValue(itemId, "permission", null);
                        p.sendMessage("§aPermission has been removed.");
                    } else {
                        ConfigManager.setItemValue(itemId, "permission", input);
                        p.sendMessage("§aPermission has been set!");
                    }
                    new ItemEditorMenu(pmu, plugin).open();
                } else {
                    ConfigManager.setItemValue(itemId, path, input);
                    p.sendMessage("§aValue has been updated!");
                    new ItemEditorMenu(pmu, plugin).open();
                }
            });
            pmu.setWaitingForChatInput(false);
        }
    }
}