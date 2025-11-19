package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.listener.handler.*;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.active.ActiveItemSettingsMenu;
import io.github.altkat.BuffedItems.menu.active.ActiveItemVisualsMenu;
import io.github.altkat.BuffedItems.menu.editor.EnchantmentListMenu;
import io.github.altkat.BuffedItems.menu.editor.ItemEditorMenu;
import io.github.altkat.BuffedItems.menu.editor.LoreEditorMenu;
import io.github.altkat.BuffedItems.menu.passive.EffectListMenu;
import io.github.altkat.BuffedItems.menu.utility.MainMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Handles all chat-based input for the BuffedItems in-game editor.
 * Optimized with DRY principles to minimize code duplication.
 */
public class ChatListener implements Listener {

    private final BuffedItems plugin;
    private final BasicInputHandler basicInputHandler;
    private final LoreInputHandler loreInputHandler;
    private final CostInputHandler costInputHandler;
    private final ActiveSettingsInputHandler activeSettingsInputHandler;
    private final EffectInputHandler effectInputHandler;
    private final CreationInputHandler creationInputHandler;

    public ChatListener(BuffedItems plugin) {
        this.plugin = plugin;
        this.basicInputHandler = new BasicInputHandler(plugin);
        this.loreInputHandler = new LoreInputHandler(plugin);
        this.costInputHandler = new CostInputHandler(plugin);
        this.activeSettingsInputHandler = new ActiveSettingsInputHandler(plugin);
        this.effectInputHandler = new EffectInputHandler(plugin);
        this.creationInputHandler = new CreationInputHandler(plugin);
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent e) {
        Player player = e.getPlayer();
        PlayerMenuUtility pmu = BuffedItems.getPlayerMenuUtility(player);

        if (!pmu.isWaitingForChatInput()) {
            return;
        }

        e.setCancelled(true);

        String input = PlainTextComponentSerializer.plainText().serialize(e.message());
        String path = pmu.getChatInputPath();
        String itemId = pmu.getItemToEditId();

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                () -> "[Chat] Processing input from " + player.getName() + ": path=" + path + ", input=" + input);

        Bukkit.getScheduler().runTask(plugin, () -> {
            handleChatInput(player, pmu, input, path, itemId);
        });
    }

    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }

    /**
     * Main chat input handler - routes all paths to appropriate methods
     */
    private void handleChatInput(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path == null) {
            closeChatInput(pmu);
            return;
        }

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ConfigManager.fromSection("§cOperation cancelled."));
            handleCancelAction(player, pmu, path);
            closeChatInput(pmu);
            return;
        }

        if (path.equals("createnewitem") || path.equals("duplicateitem")) {
            creationInputHandler.handle(player, pmu, input, path, itemId);
        } else if (path.startsWith("lore.")) {
            loreInputHandler.handle(player, pmu, input, path, itemId);
        }else if (path.equals("display_name") || path.equals("permission") || path.equals("material.manual") || path.equals("custom_model_data")) {
            basicInputHandler.handle(player, pmu, input, path, itemId);
        } else if (path.startsWith("active.cooldown") || path.startsWith("active.duration") ||
                path.startsWith("active.commands.") || path.startsWith("active.msg.") ||
                path.startsWith("active.sounds.")) {
            activeSettingsInputHandler.handle(player, pmu, input, path, itemId);
        }else if (path.contains("potion_effects") || path.contains("attributes") || path.contains("enchantments")) {
            effectInputHandler.handle(player, pmu, input, path, itemId);
        } else if (path.startsWith("active.costs.")) {
            costInputHandler.handle(player, pmu, input, path, itemId);
        } else {
            player.sendMessage(ConfigManager.fromSection("§cError: Unknown input path: " + path));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[Chat] Attempted to set unknown path via chat: " + path);
            closeChatInput(pmu);
        }
    }

    /**
     * Handles cancel action - returns to appropriate menu based on path type
     */
    private void handleCancelAction(Player player, PlayerMenuUtility pmu, String path) {
        if (path.equals("createnewitem") || path.equals("duplicateitem")) {
            new MainMenu(pmu, plugin).open();
        } else if (path.startsWith("lore.")) {
            new LoreEditorMenu(pmu, plugin).open();
        } else if (path.equals("display_name") || path.equals("permission") ||
                path.equals("material.manual") || path.equals("custom_model_data")) {
            new ItemEditorMenu(pmu, plugin).open();
        } else if (path.startsWith("active.")) {
            if (path.startsWith("active.msg.") || path.startsWith("active.sounds.")) {
                new ActiveItemVisualsMenu(pmu, plugin).open();
            } else if (path.equals("active.cooldown") || path.equals("active.duration") ||
                    path.equals("active.commands.add") || path.startsWith("active.commands.edit.")) {
                new ActiveItemSettingsMenu(pmu, plugin).open();
            } else if (path.startsWith("active.potion_effects")) {
                new EffectListMenu(pmu, plugin,
                        EffectListMenu.EffectType.POTION_EFFECT, "ACTIVE").open();
            } else if (path.startsWith("active.attributes")) {
                new EffectListMenu(pmu, plugin,
                        EffectListMenu.EffectType.ATTRIBUTE, "ACTIVE").open();
            }
        } else if (path.startsWith("potion_effects")) {
            String slot = pmu.getTargetSlot();
            new EffectListMenu(pmu, plugin,
                    EffectListMenu.EffectType.POTION_EFFECT, slot).open();
        } else if (path.startsWith("attributes")) {
            String slot = pmu.getTargetSlot();
            new EffectListMenu(pmu, plugin,
                    EffectListMenu.EffectType.ATTRIBUTE, slot).open();
        } else if (path.startsWith("enchantments")) {
            new EnchantmentListMenu(pmu, plugin).open();
        }
    }
}