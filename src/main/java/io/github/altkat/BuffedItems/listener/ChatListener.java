package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.listener.handler.*;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final BuffedItems plugin;
    private final ChatInputRouter chatInputRouter;

    public ChatListener(BuffedItems plugin) {
        this.plugin = plugin;
        this.chatInputRouter = new ChatInputRouter();

        // Register handlers in order of specificity
        chatInputRouter.registerHandler(new EnchantmentSearchHandler(plugin));
        chatInputRouter.registerHandler(new IngredientInputHandler(plugin));
        chatInputRouter.registerHandler(new UpgradeInputHandler(plugin));
        chatInputRouter.registerHandler(new RecipeInputHandler(plugin));
        chatInputRouter.registerHandler(new CreationInputHandler(plugin));
        chatInputRouter.registerHandler(new SetInputHandler(plugin));
        chatInputRouter.registerHandler(new PassiveVisualsInputHandler(plugin));
        chatInputRouter.registerHandler(new CostInputHandler(plugin));
        chatInputRouter.registerHandler(new LoreInputHandler(plugin));
        chatInputRouter.registerHandler(new EffectInputHandler(plugin));
        chatInputRouter.registerHandler(new ParticleInputHandler(plugin));
        chatInputRouter.registerHandler(new BasicInputHandler(plugin));
        chatInputRouter.registerHandler(new ActiveSettingsInputHandler(plugin));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent e) {
        Player player = e.getPlayer();
        PlayerMenuUtility pmu = BuffedItems.getPlayerMenuUtility(player);

        if (!pmu.isWaitingForChatInput()) {
            return;
        }

        e.setCancelled(true);

        String input = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .build()
                .serialize(e.message());
        String path = pmu.getChatInputPath();
        String itemId = pmu.getItemToEditId();

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                () -> "[Chat] Processing input from " + player.getName() + ": path=" + path + ", input=" + input);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (path == null) {
                pmu.setWaitingForChatInput(false);
                return;
            }

            if (input.equalsIgnoreCase("cancel")) {
                player.sendMessage(ConfigManager.fromSectionWithPrefix("§cOperation cancelled."));
                chatInputRouter.handleCancel(player, pmu, path);
                pmu.setWaitingForChatInput(false);
                pmu.setChatInputPath(null);
            } else {
                chatInputRouter.handleInput(player, pmu, input, path, itemId);
            }
        });
    }
}