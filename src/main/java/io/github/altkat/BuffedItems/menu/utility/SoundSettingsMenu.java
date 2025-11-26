package io.github.altkat.BuffedItems.menu.utility;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.active.ActiveItemSoundsMenu;
import io.github.altkat.BuffedItems.menu.base.Menu;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class SoundSettingsMenu extends Menu {

    private final BuffedItems plugin;
    private final String itemId;
    private final String soundType;

    public SoundSettingsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin, String soundType) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.itemId = playerMenuUtility.getItemToEditId();
        this.soundType = soundType;
    }

    @Override
    public String getMenuName() {
        String displayType = switch (soundType) {
            case "success" -> "Success";
            case "cooldown" -> "Cooldown";
            case "cost-fail" -> "Cost Fail";
            case "depletion" -> "Depletion";
            default -> soundType;
        };
        return "Set " + displayType + " Sound";
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        if (e.getCurrentItem().getType() == Material.BARRIER) {
            new ActiveItemSoundsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getCurrentItem().getType() == Material.JUKEBOX) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("active.sounds." + soundType);
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter the sound name in chat."));
            p.sendMessage(ConfigManager.fromSection("§7Format: SOUND_NAME;VOLUME;PITCH"));
            p.sendMessage(ConfigManager.fromSection("§7Examples:"));
            p.sendMessage(ConfigManager.fromSection("§7- ENTITY_PLAYER_LEVELUP;1.0;2.0"));
            p.sendMessage(ConfigManager.fromSection("§7- custom:my_sword_sound;1;1 (Resource Pack)"));
            return;
        }

        if (e.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
            ConfigManager.setItemValue(itemId, "sounds." + soundType, "NONE");
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            new ActiveItemSoundsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() >= 10 && e.getSlot() <= 34) {
            ItemStack clicked = e.getCurrentItem();
            if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

            String name = clicked.getItemMeta().getDisplayName();
            String soundData = null;

            if (name.contains("Level Up")) soundData = "ENTITY_PLAYER_LEVELUP;1.0;1.0";
            else if (name.contains("Exp Orb")) soundData = "ENTITY_EXPERIENCE_ORB_PICKUP;1.0;1.0";
            else if (name.contains("Villager No")) soundData = "ENTITY_VILLAGER_NO;1.0;1.0";
            else if (name.contains("Anvil Land")) soundData = "BLOCK_ANVIL_LAND;1.0;1.0";
            else if (name.contains("Explosion")) soundData = "ENTITY_GENERIC_EXPLODE;1.0;1.0";
            else if (name.contains("Click")) soundData = "UI_BUTTON_CLICK;1.0;1.0";
            else if (name.contains("Pling")) soundData = "BLOCK_NOTE_BLOCK_PLING;1.0;2.0";
            else if (name.contains("Break")) soundData = "ENTITY_ITEM_BREAK;1.0;1.0";
            else if (name.contains("Totem")) soundData = "ITEM_TOTEM_USE;1.0;1.0";
            else if (name.contains("Beacon")) soundData = "BLOCK_BEACON_ACTIVATE;1.0;1.0";

            if (soundData != null) {
                if (e.isLeftClick()) {
                    ConfigManager.setItemValue(itemId, "sounds." + soundType, soundData);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    new ActiveItemSoundsMenu(playerMenuUtility, plugin).open();
                } else if (e.isRightClick()) {
                    try {
                        p.playSound(p.getLocation(), org.bukkit.Sound.valueOf(soundData.split(";")[0]), 1f, 1f);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        inventory.setItem(4, makeItem(Material.BOOK, "§6Sound Info",
                "§7Select a common sound below",
                "§7or enter a custom one via chat.",
                "",
                "§7Format: §fNAME;VOL;PITCH",
                "§7Resource packs supported!"));

        inventory.setItem(40, makeItem(Material.JUKEBOX, "§bCustom Sound (Chat)",
                "§7Click to type a custom sound",
                "§7name and pitch/volume in chat."));

        inventory.setItem(36, makeItem(Material.REDSTONE_BLOCK, "§cDisable Sound",
                "§7Set to NONE."));

        inventory.setItem(44, makeItem(Material.BARRIER, "§cBack"));

        inventory.setItem(10, makeSoundItem(Material.EXPERIENCE_BOTTLE, "Level Up", "ENTITY_PLAYER_LEVELUP", "Classic RPG level up sound."));
        inventory.setItem(11, makeSoundItem(Material.GOLD_NUGGET, "Exp Orb", "ENTITY_EXPERIENCE_ORB_PICKUP", "Subtle 'ding' sound."));
        inventory.setItem(12, makeSoundItem(Material.NOTE_BLOCK, "Pling (High)", "BLOCK_NOTE_BLOCK_PLING", "High pitched note block."));
        inventory.setItem(13, makeSoundItem(Material.TOTEM_OF_UNDYING, "Totem", "ITEM_TOTEM_USE", "Loud totem activation."));
        inventory.setItem(14, makeSoundItem(Material.BEACON, "Beacon", "BLOCK_BEACON_ACTIVATE", "Powered up sound."));

        inventory.setItem(19, makeSoundItem(Material.EMERALD, "Villager No", "ENTITY_VILLAGER_NO", "Classic 'Hrmm' error sound."));
        inventory.setItem(20, makeSoundItem(Material.ANVIL, "Anvil Land", "BLOCK_ANVIL_LAND", "Heavy metallic clang."));
        inventory.setItem(21, makeSoundItem(Material.TNT, "Explosion", "ENTITY_GENERIC_EXPLODE", "Boom!"));
        inventory.setItem(22, makeSoundItem(Material.FLINT, "Item Break", "ENTITY_ITEM_BREAK", "Crunchy snapping sound."));
        inventory.setItem(23, makeSoundItem(Material.STONE_BUTTON, "Click", "UI_BUTTON_CLICK", "Simple UI click."));
    }

    private ItemStack makeSoundItem(Material mat, String displayName, String soundName, String desc) {
        return makeItem(mat, "§e" + displayName,
                "§7ID: " + soundName,
                "§7" + desc,
                "",
                "§aLeft-Click to Set",
                "§bRight-Click to Preview");
    }
}