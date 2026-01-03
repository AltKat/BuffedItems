package io.github.altkat.BuffedItems.menu.editor;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class PermissionSettingsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;

    public PermissionSettingsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
    }

    @Override
    public String getMenuName() {
        return "Permissions: " + itemId;
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        Material type = e.getCurrentItem().getType();

        if (type == Material.BARRIER) {
            new ItemEditorMenu(playerMenuUtility, plugin).open();
            return;
        }

        String inputPath = null;
        String title = "";

        switch (e.getSlot()) {
            case 11: // Main
                inputPath = "permission";
                title = "Main Permission";
                break;
            case 13: // Active
                inputPath = "active_ability.permission";
                title = "Active Permission (Override)";
                break;
            case 15: // Passive
                inputPath = "passive_effects.permission";
                title = "Passive Permission (Override)";
                break;
        }

        if (inputPath != null) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath(inputPath);
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter the " + title + " in chat."));
            p.sendMessage(ConfigManager.fromSection("§7Type 'none' to remove/inherit."));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit."));
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        addBackButton(new ItemEditorMenu(playerMenuUtility, plugin));

        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) return;

        String perm = item.getPermission();
        String mainPerm = (perm != null) ? perm : "NONE";
        String activePerm = item.getActiveAbility().getActivePermission();
        String passivePerm = item.getPassiveEffects().getPassivePermission();

        inventory.setItem(4, makeItem(Material.BOOK, "§bHow Permissions Work?",
                "§7Simple explanation:",
                "",
                "§e1. Main Permission:",
                "§7It controls both §fActive§7 and §fPassive§7.",
                "§7If you set this, player needs it for everything.",
                "",
                "§62. Specific Permissions:",
                "§7You can set separate permissions for",
                "§7§bActive§7 (Right-Click) and §dPassive§7 (Stats).",
                "§7If set, they replace the Main Permission."));

        // 1. MAIN PERMISSION
        inventory.setItem(11, makeItem(Material.PAPER, "§eMain Permission",
                "§7The base permission node.",
                "§7Current: §f" + mainPerm,
                "",
                "§aClick to Edit"));

        // 2. ACTIVE PERMISSION
        List<String> activeLore = new ArrayList<>();
        activeLore.add("§7Required for active(Right-Click) abilities.");
        if (activePerm == null) {
            activeLore.add("§7Current: §8(Inherits Main)");
            activeLore.add("§7Effective: §f" + mainPerm);
        } else {
            activeLore.add("§7Current: §b" + activePerm);
            activeLore.add("§7(Overrides Main)");
        }
        activeLore.add("");
        activeLore.add("§aClick to Edit");
        inventory.setItem(13, makeItem(Material.FISHING_ROD, "§bActive Permission (Override)", activeLore.toArray(new String[0])));

        // 3. PASSIVE PERMISSION
        List<String> passiveLore = new ArrayList<>();
        passiveLore.add("§7Required for passive attributes/effects.");
        if (passivePerm == null) {
            passiveLore.add("§7Current: §8(Inherits Main)");
            passiveLore.add("§7Effective: §f" + mainPerm);
        } else {
            passiveLore.add("§7Current: §d" + passivePerm);
            passiveLore.add("§7(Overrides Main)");
        }

        passiveLore.add("");
        passiveLore.add("§e⚠ Attribute Mode Warning:");
        passiveLore.add("§7Attribute restrictions ONLY");
        passiveLore.add("§7work in §bDYNAMIC §7mode.");
        passiveLore.add("§7In §fSTATIC §7mode, attributes");
        passiveLore.add("§7are always active (NBT).");
        passiveLore.add("");
        passiveLore.add("§7*Except §eINVENTORY §7slot:");
        passiveLore.add("§7 Attributes in inventory always");
        passiveLore.add("§7 §aRESPECT §7permission even in §fSTATIC §7mode.");

        passiveLore.add("");
        passiveLore.add("§aClick to Edit");
        inventory.setItem(15, makeItem(Material.SHIELD, "§dPassive Permission (Override)", passiveLore.toArray(new String[0])));
    }
}