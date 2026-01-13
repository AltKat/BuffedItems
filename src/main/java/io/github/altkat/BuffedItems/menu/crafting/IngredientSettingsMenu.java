package io.github.altkat.BuffedItems.menu.crafting;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.manager.crafting.MatchType;
import io.github.altkat.BuffedItems.manager.crafting.RecipeIngredient;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.BuffedItemSelectorMenu;
import io.github.altkat.BuffedItems.menu.selector.MaterialSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.Serializer;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class IngredientSettingsMenu extends Menu {

    private final BuffedItems plugin;
    private final String recipeId;
    private final int slotIndex;
    private final boolean processSelection;

    public IngredientSettingsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        this(playerMenuUtility, plugin, false);
    }

    public IngredientSettingsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin, boolean processSelection) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.recipeId = playerMenuUtility.getRecipeToEditId();
        this.slotIndex = playerMenuUtility.getSelectedRecipeSlot();
        this.processSelection = processSelection;
    }

    @Override
    public String getMenuName() {
        return "Edit Slot: " + slotIndex;
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void open() {
        if (processSelection) {
            if (playerMenuUtility.getTempMaterial() != null) {
                saveIngredient(MatchType.MATERIAL, playerMenuUtility.getTempMaterial(), playerMenuUtility.getTempMaterial().name(), 1);
                playerMenuUtility.setTempMaterial(null);
            } else if (playerMenuUtility.getTempId() != null) {
                io.github.altkat.BuffedItems.utility.item.BuffedItem bi = plugin.getItemManager().getBuffedItem(playerMenuUtility.getTempId());
                Material mat = bi != null ? bi.getMaterial() : Material.BARRIER;
                saveIngredient(MatchType.BUFFED_ITEM, mat, playerMenuUtility.getTempId(), 1);
                playerMenuUtility.setTempId(null);
            }
        }
        super.open();
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        if (e.getSlot() == 10) {
            handleScanHand(p);
        } else if (e.getSlot() == 11) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cHand is empty!"));
            } else {
                String base64 = io.github.altkat.BuffedItems.utility.Serializer.toBase64(hand);
                saveIngredient(MatchType.EXACT, hand.getType(), base64, 1);
                p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
                new IngredientSettingsMenu(playerMenuUtility, plugin).open();
            }
        }

        else if (e.getSlot() == 12) {
            playerMenuUtility.setMaterialContext(PlayerMenuUtility.MaterialSelectionContext.CRAFTING_INGREDIENT);
            new MaterialSelectorMenu(playerMenuUtility, plugin).open();
        }
        else if (e.getSlot() == 13) {
            new BuffedItemSelectorMenu(playerMenuUtility, plugin, BuffedItemSelectorMenu.SelectionContext.CRAFTING_INGREDIENT).open();
        }
        else if (e.getSlot() == 15) {
            String recipeTypeStr = RecipesConfig.get().getString("recipes." + recipeId + ".type", "SHAPED");
            if (recipeTypeStr.equalsIgnoreCase("FURNACE") || recipeTypeStr.equalsIgnoreCase("BLAST_FURNACE") ||
                recipeTypeStr.equalsIgnoreCase("SMOKER") || recipeTypeStr.equalsIgnoreCase("CAMPFIRE")) {
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cCooking recipes only support an input amount of 1."));
                return;
            }
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("recipe_ingredient_amount");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter required amount in chat."));
        }
        else if (e.getSlot() == 16) {
            saveIngredient(null, null, null, 1);
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aSlot cleared."));
            new RecipeEditorMenu(playerMenuUtility, plugin).open();
        }
        else if (e.getSlot() == 26) {
            new RecipeEditorMenu(playerMenuUtility, plugin).open();
        }
    }

    private void handleScanHand(Player p) {
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§cHand is empty!"));
            return;
        }

        String detectedValue;
        MatchType detectedType;
        Material displayMat = item.getType();
        int amount = 1;

        NamespacedKey key = new NamespacedKey(plugin, "buffeditem_id");
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            detectedValue = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            detectedType = MatchType.BUFFED_ITEM;
        } else {
            detectedValue = item.getType().name();
            detectedType = MatchType.MATERIAL;
        }

        saveIngredient(detectedType, displayMat, detectedValue, amount);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
        new IngredientSettingsMenu(playerMenuUtility, plugin).open();
    }

    private void saveIngredient(MatchType type, Material mat, String value, int amount) {
        char key = (char) ('A' + slotIndex);
        String path = "recipes." + recipeId + ".ingredients." + key;
        String shapePath = "recipes." + recipeId + ".shape";
        ConfigurationSection config = RecipesConfig.get();

        String recipeTypeStr = config.getString("recipes." + recipeId + ".type", "SHAPED");
        boolean isCooking = recipeTypeStr.equalsIgnoreCase("FURNACE") || recipeTypeStr.equalsIgnoreCase("BLAST_FURNACE") ||
                recipeTypeStr.equalsIgnoreCase("SMOKER") || recipeTypeStr.equalsIgnoreCase("CAMPFIRE");
        
        if (isCooking) amount = 1;

        if (type == null) {
            config.set(path, null);
        } else {
            config.set(path + ".type", type.name());
            config.set(path + ".value", value);
            config.set(path + ".amount", amount);
        }

        List<String> rawShape = config.getStringList(shapePath);
        List<String> shape = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            String line = (i < rawShape.size()) ? rawShape.get(i) : "   ";
            if (line == null) line = "   ";
            StringBuilder sb = new StringBuilder(line);
            while (sb.length() < 3) sb.append(" ");
            if (sb.length() > 3) sb.setLength(3);
            shape.add(sb.toString());
        }

        int row = slotIndex / 3;
        int col = slotIndex % 3;
        StringBuilder targetLine = new StringBuilder(shape.get(row));
        targetLine.setCharAt(col, (type == null) ? ' ' : key);
        shape.set(row, targetLine.toString());
        config.set(shapePath, shape);
        playerMenuUtility.setUnsavedChanges(true);
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        char keyChar = (char) ('A' + slotIndex);
        String path = "recipes." + recipeId + ".ingredients." + keyChar;
        ConfigurationSection config = RecipesConfig.get();

        String typeStr = config.getString(path + ".type");
        String value = config.getString(path + ".value");
        int amount = config.getInt(path + ".amount", 1);

        RecipeIngredient ing = null;

        if (typeStr != null) {
            try {
                MatchType type = MatchType.valueOf(typeStr);
                Material mat = Material.STONE;
                ItemStack exactItem = null;

                if (type == MatchType.MATERIAL) {
                    mat = Material.matchMaterial(value != null ? value : "STONE");
                } else if (type == MatchType.BUFFED_ITEM) {
                    BuffedItem bi = plugin.getItemManager().getBuffedItem(value);
                    if (bi != null) mat = bi.getMaterial();
                } else if (type == MatchType.EXACT) {
                    exactItem = Serializer.fromBase64(value);
                    if (exactItem != null) mat = exactItem.getType();
                }

                ing = new RecipeIngredient(type, mat, value, amount);
                if (exactItem != null) ing.setExactReferenceItem(exactItem);

            }catch (Exception ignored) {}
        }

        ItemStack currentIcon;
        Component displayName;
        List<Component> lore = new ArrayList<>();

        if (ing != null) {
            displayName = ConfigManager.fromSection("§aCurrent Ingredient");
            Material mat = ing.getMaterial() != null ? ing.getMaterial() : Material.BARRIER;
            currentIcon = new ItemStack(mat);

            if (ing.getMatchType() == MatchType.BUFFED_ITEM) {
                io.github.altkat.BuffedItems.utility.item.BuffedItem bi = plugin.getItemManager().getBuffedItem(ing.getData());
                if (bi != null) currentIcon = new ItemBuilder(bi, plugin).build();
            }
            else if (ing.getMatchType() == MatchType.EXACT) {
                if (ing.getExactReferenceItem() != null) currentIcon = ing.getExactReferenceItem().clone();
            }

            lore.add(ConfigManager.fromSection("§7Type: " + ing.getMatchType().name()));

            if (ing.getMatchType() != MatchType.EXACT) {
                lore.add(ConfigManager.fromSection("§7Value: " + ing.getData()));
            } else {
                lore.add(ConfigManager.fromSection("§7Value: (Exact NBT Data)"));
            }

            lore.add(ConfigManager.fromSection("§7Amount: §e" + ing.getAmount()));
            currentIcon.setAmount(Math.max(1, ing.getAmount()));
        } else {
            displayName = ConfigManager.fromSection("§cEmpty Slot");
            currentIcon = new ItemStack(Material.BARRIER);
            lore.add(ConfigManager.fromSection("§7No item set."));
        }

        ItemMeta meta = currentIcon.getItemMeta();
        if (meta != null) {
            meta.displayName(displayName);
            meta.lore(lore);
            currentIcon.setItemMeta(meta);
        }

        inventory.setItem(4, currentIcon);

        inventory.setItem(10, makeItem(Material.HOPPER, "§bScan Hand", "§7Detect item from hand."));

        inventory.setItem(11, makeItem(Material.ENDER_EYE, "§5Scan as EXACT",
                "§7Saves the item in your hand",
                "§7with ALL metadata (Name, Lore, NBT).",
                "§7Useful for potions and special items."));

        inventory.setItem(12, makeItem(Material.GRASS_BLOCK, "§aVanilla Material", "§7Select from list."));
        inventory.setItem(13, makeItem(Material.NETHER_STAR, "§6Buffed Item", "§7Select custom item."));

        String recipeTypeStr = RecipesConfig.get().getString("recipes." + recipeId + ".type", "SHAPED");
        boolean isCooking = recipeTypeStr.equalsIgnoreCase("FURNACE") || recipeTypeStr.equalsIgnoreCase("BLAST_FURNACE") ||
                recipeTypeStr.equalsIgnoreCase("SMOKER") || recipeTypeStr.equalsIgnoreCase("CAMPFIRE");

        if (isCooking) {
            inventory.setItem(15, makeItem(Material.GOLD_NUGGET, "§eSet Amount",
                    "§7Current: §e1",
                    "",
                    "§cDisabled for cooking recipes."));
        } else {
            inventory.setItem(15, makeItem(Material.GOLD_NUGGET, "§eSet Amount", "§7Current: §e" + (ing != null ? ing.getAmount() : 1)));
        }

        inventory.setItem(16, makeItem(Material.RED_STAINED_GLASS_PANE, "§cClear Slot"));
        inventory.setItem(26, makeItem(Material.BARRIER, "§cBack"));
    }
}