package io.github.altkat.BuffedItems.menu.set;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.SetsConfig;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class SetBonusesMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final String setId;

    public SetBonusesMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.setId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Set Bonuses: " + setId;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        List<String> bonusKeys = getBonusKeys();
        if (handlePageChange(e, bonusKeys.size())) return;

        if (e.getSlot() == 49) {
            new SetEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() == 51) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("create_bonus_tier");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter piece count for new bonus (e.g. 3)."));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            return;
        }

        if (e.getSlot() < 45) {
            int index = maxItemsPerPage * page + e.getSlot();
            if (index >= bonusKeys.size()) return;

            String countKey = bonusKeys.get(index);

            if (e.getClick() == ClickType.RIGHT) {
                SetsConfig.get().set("sets." + setId + ".bonuses." + countKey, null);
                SetsConfig.save();
                plugin.getSetManager().loadSets(true);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cBonus tier " + countKey + " removed."));
                this.open();
            }
            else if (e.getClick() == ClickType.LEFT) {
                playerMenuUtility.setTempSetId(setId);
                playerMenuUtility.setTempBonusCount(Integer.parseInt(countKey));
                new SetBonusEffectSelectorMenu(playerMenuUtility, plugin).open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuControls();
        setFillerGlass();

        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack"));
        inventory.setItem(51, makeItem(Material.ANVIL, "§aAdd Bonus Tier", "§7Create a new bonus level (e.g. 3 pieces)."));

        List<String> bonusKeys = getBonusKeys();

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= bonusKeys.size()) break;

            String count = bonusKeys.get(index);
            inventory.setItem(i, makeItem(Material.EXPERIENCE_BOTTLE,
                    "§b" + count + " Pieces Bonus",
                    "",
                    "§7Click to Edit Effects",
                    "§cRight-Click to Remove"));
        }
    }

    private List<String> getBonusKeys() {
        ConfigurationSection sec = SetsConfig.get().getConfigurationSection("sets." + setId + ".bonuses");
        if (sec == null) return new ArrayList<>();
        List<String> keys = new ArrayList<>(sec.getKeys(false));
        keys.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
            } catch (Exception e) { return a.compareTo(b); }
        });
        return keys;
    }
}