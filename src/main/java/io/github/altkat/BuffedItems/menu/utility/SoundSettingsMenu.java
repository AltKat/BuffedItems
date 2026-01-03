package io.github.altkat.BuffedItems.menu.utility;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.active.ActiveItemCastVisualsMenu;
import io.github.altkat.BuffedItems.menu.active.ActiveItemSoundsMenu;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.passive.PassiveItemVisualsMenu;
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
            case "depleted-try" -> "Depleted Try";
            case "passive" -> "Equip";
            case "cast" -> "Cast Visual";
            default -> soundType;
        };
        return "Set " + displayType + " Sound";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        if (e.getCurrentItem().getType() == Material.BARRIER) {
            openPreviousMenu();
            return;
        }

        String basePath;
        switch (soundType) {
            case "passive":
                basePath = "passive_effects.visuals.sound.";
                break;
            case "cast":
                basePath = "active_ability.visuals.cast.sound.";
                break;
            case "depletion":
            case "depleted-try":
                basePath = "usage.";
                break;
            default:
                basePath = "active_ability.sounds.";
                break;
        }
        
        String soundKey = ("depletion".equals(soundType) || "depleted-try".equals(soundType))
                ? (soundType.equals("depletion") ? "depletion_sound" : "depleted_try_sound")
                : ((soundType.equals("passive") || soundType.equals("cast")) ? "sound" : soundType);


        if (e.getCurrentItem().getType() == Material.JUKEBOX) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath(basePath + soundKey);
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter the sound name in chat."));
            p.sendMessage(ConfigManager.fromSection("§7Format: SOUND_NAME;VOLUME;PITCH"));
            p.sendMessage(ConfigManager.fromSection("§7Examples:"));
            p.sendMessage(ConfigManager.fromSection("§7- ENTITY_PLAYER_LEVELUP;1.0;2.0"));
            p.sendMessage(ConfigManager.fromSection("§7- custom:my_sword_sound;1;1 (Resource Pack)"));
            return;
        }

        if (e.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
            ConfigManager.setItemValue(itemId, basePath + soundKey, "NONE");
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            openPreviousMenu();
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
            String idLine = ConfigManager.toPlainText(clicked.getItemMeta().lore().get(0));
            if (idLine.startsWith("ID: ")) {
                String soundData = idLine.substring(4) + ";1.0;1.0";
                if (soundData.contains("NOTE_BLOCK_PLING")) soundData = soundData.replace("1.0", "2.0");

                if (e.isLeftClick()) {
                    ConfigManager.setItemValue(itemId, basePath + soundKey, soundData);
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    openPreviousMenu();
                } else if (e.isRightClick()) {
                    try {
                        float pitch = soundData.contains("2.0") ? 2.0f : 1.0f;
                        p.playSound(p.getLocation(), Sound.valueOf(soundData.split(";")[0]), 1f, pitch);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private void openPreviousMenu() {
        if ("cast".equals(soundType)) {
            new ActiveItemCastVisualsMenu(playerMenuUtility, plugin).open();
        } else if ("passive".equals(soundType)) {
            new PassiveItemVisualsMenu(playerMenuUtility, plugin).open();
        } else if ("depletion".equals(soundType) || "depleted-try".equals(soundType)) {
            new io.github.altkat.BuffedItems.menu.active.UsageLimitSettingsMenu(playerMenuUtility, plugin).open();
        } else {
            new ActiveItemSoundsMenu(playerMenuUtility, plugin).open();
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        inventory.setItem(4, makeItem(Material.BOOK, "§6Sound Library",
                "§7Select a preset sound below",
                "§7or use Jukebox for custom input.",
                "",
                "§aLeft-Click to Set",
                "§eRight-Click to Preview"));

        inventory.setItem(49, makeItem(Material.BARRIER, "§cBack"));

        inventory.setItem(45, makeItem(Material.REDSTONE_BLOCK, "§cDisable Sound", "§7Set to NONE."));
        inventory.setItem(53, makeItem(Material.JUKEBOX, "§bCustom Sound (Chat)", "§7Type sound ID manually."));

        int i = 10;
        inventory.setItem(i++, makeSoundItem(Material.EXPERIENCE_BOTTLE, "Level Up", "ENTITY_PLAYER_LEVELUP", "Classic RPG level up sound."));
        inventory.setItem(i++, makeSoundItem(Material.GOLD_NUGGET, "Exp Orb", "ENTITY_EXPERIENCE_ORB_PICKUP", "Subtle 'ding' sound."));
        inventory.setItem(i++, makeSoundItem(Material.NOTE_BLOCK, "Pling (High)", "BLOCK_NOTE_BLOCK_PLING", "High pitched note block."));
        inventory.setItem(i++, makeSoundItem(Material.EMERALD, "Success", "ENTITY_VILLAGER_YES", "Villager agreement sound."));
        inventory.setItem(i++, makeSoundItem(Material.BEACON, "Beacon Power", "BLOCK_BEACON_POWER_SELECT", "Magical power up."));
        inventory.setItem(i++, makeSoundItem(Material.TOTEM_OF_UNDYING, "Totem Use", "ITEM_TOTEM_USE", "Divine activation sound."));
        inventory.setItem(i++, makeSoundItem(Material.ENCHANTING_TABLE, "Enchant", "BLOCK_ENCHANTMENT_TABLE_USE", "Magical hum."));

        i = 19;
        inventory.setItem(i++, makeSoundItem(Material.REDSTONE, "Villager No", "ENTITY_VILLAGER_NO", "Classic error sound."));
        inventory.setItem(i++, makeSoundItem(Material.NOTE_BLOCK, "Bass (Fail)", "BLOCK_NOTE_BLOCK_BASS", "Low pitch error sound."));
        inventory.setItem(i++, makeSoundItem(Material.IRON_TRAPDOOR, "Click/Trapdoor", "BLOCK_IRON_TRAPDOOR_OPEN", "Mechanical click."));
        inventory.setItem(i++, makeSoundItem(Material.FLINT_AND_STEEL, "Extinguish", "ENTITY_GENERIC_EXTINGUISH_FIRE", "Fizzle sound."));
        inventory.setItem(i++, makeSoundItem(Material.DISPENSER, "Dispenser Fail", "BLOCK_DISPENSER_FAIL", "Empty click sound."));
        inventory.setItem(i++, makeSoundItem(Material.LAVA_BUCKET, "Lava Pop", "BLOCK_LAVA_POP", "Bubble pop sound."));
        inventory.setItem(i++, makeSoundItem(Material.CHAIN, "Chain Break", "BLOCK_CHAIN_BREAK", "Metallic snap."));

        i = 28;
        inventory.setItem(i++, makeSoundItem(Material.FLINT, "Item Break", "ENTITY_ITEM_BREAK", "Crunchy snapping sound."));
        inventory.setItem(i++, makeSoundItem(Material.ANVIL, "Anvil Land", "BLOCK_ANVIL_LAND", "Heavy metallic clang."));
        inventory.setItem(i++, makeSoundItem(Material.TNT, "Explosion", "ENTITY_GENERIC_EXPLODE", "Boom!"));
        inventory.setItem(i++, makeSoundItem(Material.IRON_DOOR, "Zombie Door", "ENTITY_ZOMBIE_ATTACK_IRON_DOOR", "Loud banging sound."));
        inventory.setItem(i++, makeSoundItem(Material.GHAST_TEAR, "Ghast Shoot", "ENTITY_GHAST_SHOOT", "Retro arcade shot."));
        inventory.setItem(i++, makeSoundItem(Material.BLAZE_ROD, "Blaze Shoot", "ENTITY_BLAZE_SHOOT", "Fire shot sound."));
        inventory.setItem(i++, makeSoundItem(Material.DRAGON_HEAD, "Dragon Growl", "ENTITY_ENDER_DRAGON_GROWL", "Epic boss roar."));

        i = 37;
        inventory.setItem(i++, makeSoundItem(Material.WITHER_SKELETON_SKULL, "Wither Spawn", "ENTITY_WITHER_SPAWN", "Dark summon sound."));
        inventory.setItem(i++, makeSoundItem(Material.BELL, "Bell Ring", "BLOCK_BELL_USE", "Clear ding sound."));
        inventory.setItem(i++, makeSoundItem(Material.CHEST, "Chest Open", "BLOCK_CHEST_OPEN", "Wooden creak."));
        inventory.setItem(i++, makeSoundItem(Material.ENDER_CHEST, "Ender Chest", "BLOCK_ENDER_CHEST_OPEN", "Mystical open sound."));
        inventory.setItem(i++, makeSoundItem(Material.SHIELD, "Shield Block", "ITEM_SHIELD_BLOCK", "Thud sound."));
        inventory.setItem(i++, makeSoundItem(Material.TRIDENT, "Trident Throw", "ITEM_TRIDENT_THROW", "Whoosh sound."));
        inventory.setItem(i++, makeSoundItem(Material.CROSSBOW, "Crossbow Shoot", "ITEM_CROSSBOW_SHOOT", "Sharp release sound."));
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