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
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
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
        return 36;
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
        }
        else if (e.getSlot() == 12) {
            playerMenuUtility.setMaterialContext(PlayerMenuUtility.MaterialSelectionContext.CRAFTING_INGREDIENT);
            new MaterialSelectorMenu(playerMenuUtility, plugin).open();
        }
        else if (e.getSlot() == 13) {
            new BuffedItemSelectorMenu(playerMenuUtility, plugin, BuffedItemSelectorMenu.SelectionContext.CRAFTING_INGREDIENT).open();
        }
        else if (e.getSlot() == 14) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("recipe_ingredient_external");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter the External ID (e.g. itemsadder:ruby)."));
        }
        else if (e.getSlot() == 16) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("recipe_ingredient_amount");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter required amount in chat."));
        }
        else if (e.getSlot() == 31) {
            saveIngredient(null, null, null, 1);
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aSlot cleared."));
            new RecipeEditorMenu(playerMenuUtility, plugin).open();
        }
        else if (e.getSlot() == 35) {
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
        int amount = item.getAmount();

        NamespacedKey key = new NamespacedKey(plugin, "buffeditem_id");
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            detectedValue = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            detectedType = MatchType.BUFFED_ITEM;
        } else if (plugin.getHookManager().isItemsAdderLoaded() && dev.lone.itemsadder.api.CustomStack.byItemStack(item) != null) {
            detectedValue = dev.lone.itemsadder.api.CustomStack.byItemStack(item).getNamespacedID();
            detectedType = MatchType.EXTERNAL;
        } else if (plugin.getHookManager().isNexoLoaded() && com.nexomc.nexo.api.NexoItems.idFromItem(item) != null) {
            detectedValue = "nexo:" + com.nexomc.nexo.api.NexoItems.idFromItem(item);
            detectedType = MatchType.EXTERNAL;
        } else {
            detectedValue = item.getType().name();
            detectedType = MatchType.MATERIAL;
        }

        saveIngredient(detectedType, displayMat, detectedValue, amount);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2);
        new RecipeEditorMenu(playerMenuUtility, plugin).open();
    }

    private void saveIngredient(MatchType type, Material mat, String value, int amount) {
        char key = (char) ('A' + slotIndex);
        String path = "recipes." + recipeId + ".ingredients." + key;
        String shapePath = "recipes." + recipeId + ".shape";
        ConfigurationSection config = RecipesConfig.get();

        if (type == null && value == null && amount == -1) {
            String currentType = config.getString(path + ".type");
            if (currentType != null) {

            }
        }

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

        RecipesConfig.save();
        plugin.getCraftingManager().loadRecipes(true);
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        CustomRecipe recipe = plugin.getCraftingManager().getRecipes().get(recipeId);
        RecipeIngredient ing = (recipe != null) ? recipe.getIngredient(slotIndex) : null;

        ItemStack currentIcon;
        if (ing != null) {
            Material mat = ing.getMaterial() != null ? ing.getMaterial() : Material.BARRIER;
            currentIcon = makeItem(mat, "§aCurrent Ingredient",
                    "§7Type: " + ing.getMatchType().name(),
                    "§7Value: " + ing.getData(),
                    "§7Amount: §e" + ing.getAmount());
            currentIcon.setAmount(Math.max(1, ing.getAmount()));
        } else {
            currentIcon = makeItem(Material.BARRIER, "§cEmpty Slot", "§7No item set.");
        }
        inventory.setItem(4, currentIcon);

        inventory.setItem(10, makeItem(Material.HOPPER, "§bScan Hand", "§7Detect item from hand."));
        inventory.setItem(12, makeItem(Material.GRASS_BLOCK, "§aVanilla Material", "§7Select from list."));
        inventory.setItem(13, makeItem(Material.NETHER_STAR, "§6Buffed Item", "§7Select custom item."));
        inventory.setItem(14, makeItem(Material.NAME_TAG, "§dExternal Plugin", "§7Enter ItemsAdder/Nexo ID via Chat."));

        inventory.setItem(16, makeItem(Material.GOLD_NUGGET, "§eSet Amount", "§7Current: " + (ing != null ? ing.getAmount() : 1), "§7(Requires special handling!)"));

        inventory.setItem(31, makeItem(Material.RED_STAINED_GLASS_PANE, "§cClear Slot"));
        inventory.setItem(35, makeItem(Material.BARRIER, "§cBack"));
    }
}