package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.selector.EnchantmentSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.entity.Player;

public class EnchantmentSearchHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public EnchantmentSearchHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean shouldHandle(String path) {
        return path.equals("enchantment_search");
    }

    @Override
    public void onCancel(Player player, PlayerMenuUtility pmu, String path) {
        new EnchantmentSelectorMenu(pmu, plugin).open();
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        closeChatInput(pmu);
        EnchantmentSelectorMenu menu = new EnchantmentSelectorMenu(pmu, plugin);
        menu.searchEnchantments(input);
        menu.open();

        if (input.equalsIgnoreCase("clear")) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSearch cleared. Showing all enchantments."));
        } else {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSearching for: §e" + input));
        }
    }
    
    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }
}
