package io.github.altkat.BuffedItems.menu.crafting;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.manager.crafting.MatchType;
import io.github.altkat.BuffedItems.manager.crafting.RecipeType;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.BuffedItemSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RecipeEditorMenu extends Menu {

    private final BuffedItems plugin;
    private final String recipeId;

    private final int[] gridSlots = {
            10, 11, 12,
            19, 20, 21,
            28, 29, 30
    };

    private final int RESULT_SLOT = 24;

    public RecipeEditorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.recipeId = playerMenuUtility.getRecipeToEditId();
    }

    @Override
    public String getMenuName() {
        return "Edit Recipe: " + recipeId;
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        ConfigurationSection section = RecipesConfig.get().getConfigurationSection("recipes." + recipeId);
        if (section == null) return;

        String typeStr = section.getString("type", "SHAPED");
        RecipeType currentType;
        try { currentType = RecipeType.valueOf(typeStr.toUpperCase()); }
        catch (Exception ex) { currentType = RecipeType.SHAPED; }

        boolean isCooking = currentType == RecipeType.FURNACE || currentType == RecipeType.BLAST_FURNACE || currentType == RecipeType.SMOKER || currentType == RecipeType.CAMPFIRE;

        // 1. Recipe Type Toggle (Slot 4)
        if (e.getSlot() == 4) {
            RecipeType[] types = RecipeType.values();
            int nextIndex = (currentType.ordinal() + 1) % types.length;
            RecipeType nextType = types[nextIndex];

            RecipesConfig.get().set("recipes." + recipeId + ".type", nextType.name());
            playerMenuUtility.setUnsavedChanges(true);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aRecipe type changed to: §e" + nextType.name()));
            open();
            return;
        }

        // 2. Grid Interaction
        for (int i = 0; i < gridSlots.length; i++) {
            if (e.getSlot() == gridSlots[i]) {
                // If cooking, only slot 20 (index 4) is allowed
                if (isCooking && i != 4) return;

                playerMenuUtility.setSelectedRecipeSlot(i);
                new IngredientSettingsMenu(playerMenuUtility, plugin).open();
                return;
            }
        }

        // 3. Cooking Specific Settings
        if (isCooking) {
            if (e.getSlot() == 38) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("recipe_cook_time");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Cook Time in seconds."));
                return;
            }
            if (e.getSlot() == 40 && currentType != RecipeType.CAMPFIRE) {
                playerMenuUtility.setWaitingForChatInput(true);
                playerMenuUtility.setChatInputPath("recipe_experience");
                p.closeInventory();
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Experience amount (e.g. 0.7)."));
                return;
            }
        }

        if (e.getSlot() == RESULT_SLOT) {
            new BuffedItemSelectorMenu(playerMenuUtility, plugin, BuffedItemSelectorMenu.SelectionContext.CRAFTING_RESULT).open();
            return;
        }

        if (e.getSlot() == RESULT_SLOT + 9) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("recipe_result_amount");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter result amount in chat."));
            return;
        }

        if (e.getSlot() == 50) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("recipe_permission");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter permission node (or 'none' to remove)."));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            return;
        }

        if (e.getSlot() == 49) {
            boolean current = RecipesConfig.get().getBoolean("recipes." + recipeId + ".enabled", true);
            RecipesConfig.get().set("recipes." + recipeId + ".enabled", !current);

            playerMenuUtility.setUnsavedChanges(true);
            open();
            return;
        }

        if (e.getSlot() == 53) {
            if (playerMenuUtility.hasUnsavedChanges()) {
                RecipesConfig.saveAsync();
                plugin.getCraftingManager().loadRecipes(true);

                playerMenuUtility.setUnsavedChanges(false);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aChanges saved successfully."));
            }

            new RecipeListMenu(playerMenuUtility, plugin).open();
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();

        ConfigurationSection section = RecipesConfig.get().getConfigurationSection("recipes." + recipeId);
        if (section == null) {
            inventory.setItem(22, makeItem(Material.BARRIER, "§cError", "§7Recipe path not found in config!"));
            return;
        }

        String typeStr = section.getString("type", "SHAPED");
        RecipeType currentType;
        try { currentType = RecipeType.valueOf(typeStr.toUpperCase()); }
        catch (Exception ex) { currentType = RecipeType.SHAPED; }

        boolean isCooking = currentType == RecipeType.FURNACE || currentType == RecipeType.BLAST_FURNACE || currentType == RecipeType.SMOKER || currentType == RecipeType.CAMPFIRE;

        // 1. Recipe Type Button (Slot 4)
        Material typeMat = Material.CRAFTING_TABLE;
        if (currentType == RecipeType.SHAPELESS) typeMat = Material.BOOK;
        else if (currentType == RecipeType.FURNACE) typeMat = Material.FURNACE;
        else if (currentType == RecipeType.BLAST_FURNACE) typeMat = Material.BLAST_FURNACE;
        else if (currentType == RecipeType.SMOKER) typeMat = Material.SMOKER;
        else if (currentType == RecipeType.CAMPFIRE) typeMat = Material.CAMPFIRE;

        String shapedLore = (currentType == RecipeType.SHAPED) ? "§8- §f§l[SHAPED (3x3 Grid)]" : "§8- §7SHAPED (3x3 Grid)";
        String shapelessLore = (currentType == RecipeType.SHAPELESS) ? "§8- §f§l[SHAPELESS (List)]" : "§8- §7SHAPELESS (List)";
        String furnaceLore = (currentType == RecipeType.FURNACE) ? "§8- §f§l[FURNACE (Smelting)]" : "§8- §7FURNACE (Smelting)";
        String blastFurnaceLore = (currentType == RecipeType.BLAST_FURNACE) ? "§8- §f§l[BLAST_FURNACE (Smelting)]" : "§8- §7BLAST_FURNACE (Smelting)";
        String smokerLore = (currentType == RecipeType.SMOKER) ? "§8- §f§l[SMOKER (Cooking)]" : "§8- §7SMOKER (Cooking)";
        String campfireLore = (currentType == RecipeType.CAMPFIRE) ? "§8- §f§l[CAMPFIRE (Cooking)]" : "§8- §7CAMPFIRE (Cooking)";

        inventory.setItem(4, makeItem(typeMat, "§eRecipe Type: §b" + currentType.name(),
                "§7Click to cycle through types.", "",
                "§7Available:", 
                shapedLore, 
                shapelessLore, 
                furnaceLore, 
                blastFurnaceLore, 
                smokerLore, 
                campfireLore));

        // 2. Dynamic Grid / Input Rendering
        ConfigurationSection ingredientsSec = section.getConfigurationSection("ingredients");

        for (int i = 0; i < 9; i++) {
            int slot = gridSlots[i];
            
            if (isCooking) {
                if (i == 4) { // Center slot (Slot 20) is the Furnace Input
                    renderIngredientSlot(ingredientsSec, slot, "E");
                } else {
                    inventory.setItem(slot, makeItem(Material.GRAY_STAINED_GLASS_PANE, " "));
                }
            } else {
                char key = (char) ('A' + i);
                renderIngredientSlot(ingredientsSec, slot, String.valueOf(key));
            }
        }

        // 3. Header / Info Icons
                if (isCooking) {
                    inventory.setItem(22, makeItem(Material.FURNACE, "§eSmelting Input", "§7Place the item to be cooked", "§7in the center slot."));
                    
                    double cookSeconds = section.getDouble("cook_time", 10.0);
                    double experience = section.getDouble("experience", 0.7);
                    
                    inventory.setItem(38, makeItem(Material.CLOCK, "§eCook Time", "§7Current: §b" + cookSeconds + "s", "", "§aClick to Edit"));
                    
                    if (currentType != RecipeType.CAMPFIRE) {
                        String xpString = (experience % 1 == 0) ? String.format("%.0f", experience) : String.valueOf(experience);
                        inventory.setItem(40, makeItem(Material.EXPERIENCE_BOTTLE, "§eExperience", "§7Current: §b" + xpString, "", "§aClick to Edit"));
                    } else {
                        inventory.setItem(40, makeItem(Material.GRAY_STAINED_GLASS_PANE, " "));
                    }
                    
                } else {            inventory.setItem(22, makeItem(Material.CRAFTING_TABLE, "§eCrafting Grid", currentType == RecipeType.SHAPED ? "§7Configure the 3x3 pattern." : "§7Add items to the list."));
        }
        
        inventory.setItem(23, makeItem(Material.ARROW, "§7->"));

        // 4. Common Controls (Result, Status, Permission, Save)
        renderResultAndControls(section);
    }

    private void renderIngredientSlot(ConfigurationSection ingredientsSec, int slot, String key) {
        if (ingredientsSec != null && ingredientsSec.contains(key)) {
            ConfigurationSection ingSec = ingredientsSec.getConfigurationSection(key);
            if (ingSec != null) {
                inventory.setItem(slot, getIngredientIconFromConfig(ingSec));
            } else {
                inventory.setItem(slot, makeItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Empty Slot", "§eClick to Set"));
            }
        } else {
            inventory.setItem(slot, makeItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Empty Slot", "§eClick to Set"));
        }
    }

    private void renderResultAndControls(ConfigurationSection section) {
        String resultId = section.getString("result.item");
        int amount = section.getInt("result.amount", 1);
        String permission = section.getString("permission", "None");
        boolean isTableEnabled = section.getBoolean("enabled", true);

        ItemStack resultStack;
        if (resultId != null) {
            BuffedItem resultItem = plugin.getItemManager().getBuffedItem(resultId);
            if (resultItem != null) {
                resultStack = new ItemBuilder(resultItem, plugin).build();
            } else {
                resultStack = makeItem(Material.BARRIER, "§cUnknown ID", "§7" + resultId);
            }
        } else {
            resultStack = makeItem(Material.BARRIER, "§cSelect Result", "§7Click to choose output item.");
        }

        ItemMeta meta = resultStack.getItemMeta();
        if (resultStack.getType().getMaxStackSize() == 1 && amount > 1) {
            String originalName = (meta.hasDisplayName()) ? meta.getDisplayName() : formatMaterialName(resultStack.getType());
            meta.displayName(ConfigManager.fromSection("§e" + amount + "x " + originalName));
        }

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(ConfigManager.fromSection("§7Amount: §e" + amount));
        lore.add(ConfigManager.fromSection("§eClick to Change Result"));

        meta.lore(lore);
        resultStack.setItemMeta(meta);
        resultStack.setAmount(Math.max(1, amount));

        inventory.setItem(RESULT_SLOT, resultStack);
        inventory.setItem(RESULT_SLOT + 9, makeItem(Material.GOLD_NUGGET, "§6Result Amount", "§7Current: §e" + amount, "", "§aClick to Edit"));
        inventory.setItem(50, makeItem(Material.PAPER, "§eRecipe Permission", "§7Current: §f" + (permission == null ? "None" : permission), "", "§aClick to Edit"));

        ItemStack tableIcon;
        if (isTableEnabled) {
            tableIcon = makeItem(Material.LIME_DYE, "§aRecipe Status: ENABLED", "§7Players §acan §7craft this recipe.", "", "§eClick to Disable");
        } else {
            tableIcon = makeItem(Material.GRAY_DYE, "§cRecipe Status: DISABLED", "§7Players §ccannot §7craft this recipe.", "", "§eClick to Enable");
        }
        inventory.setItem(49, tableIcon);
        inventory.setItem(53, makeItem(Material.GREEN_STAINED_GLASS, "§aSave & Back"));
    }

    private ItemStack getIngredientIconFromConfig(ConfigurationSection sec) {
        String typeStr = sec.getString("type", "MATERIAL");
        String value = sec.getString("value", "BEDROCK");
        int amount = sec.getInt("amount", 1);

        MatchType type;
        try {
            type = MatchType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            type = MatchType.MATERIAL;
        }

        ItemStack stack;

        if (type == MatchType.BUFFED_ITEM) {
            BuffedItem bi = plugin.getItemManager().getBuffedItem(value);
            if (bi != null) {
                stack = new ItemBuilder(bi, plugin).build();
            } else {
                stack = new ItemStack(Material.BARRIER);
            }
        }
        else if (type == MatchType.EXACT) {
            stack = io.github.altkat.BuffedItems.utility.Serializer.fromBase64(value);
            if (stack == null) stack = new ItemStack(Material.BARRIER);
        }
        else {
            Material mat = Material.BEDROCK;
            if (type == MatchType.MATERIAL) {
                mat = Material.getMaterial(value);
                if (mat == null) mat = Material.BEDROCK;
            }
            stack = new ItemStack(mat);
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        String originalName;
        if (meta.hasDisplayName()) {
            originalName = meta.getDisplayName();
        } else {
            originalName = "§f" + formatMaterialName(stack.getType());
        }


        if (amount > 1) {
            meta.displayName(ConfigManager.fromSection("§e" + amount + "x " + originalName));
        } else {
            if (type == MatchType.MATERIAL) {
                meta.displayName(ConfigManager.fromSection(originalName));
            }
        }

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        if (!lore.isEmpty()) lore.add(Component.empty());
        lore.add(ConfigManager.fromSection("§8----------------"));
        lore.add(ConfigManager.fromSection("§7Type: §f" + type.name()));
        if (type == MatchType.MATERIAL || type == MatchType.BUFFED_ITEM) {
            lore.add(ConfigManager.fromSection("§7Value: §e" + value));
        } else {
            lore.add(ConfigManager.fromSection("§7Value: §e(Base64 Data)"));
        }
        lore.add(ConfigManager.fromSection("§7Amount: §e" + amount));
        lore.add(Component.empty());
        lore.add(ConfigManager.fromSection("§eClick to Edit"));

        meta.lore(lore);
        stack.setItemMeta(meta);
        stack.setAmount(Math.max(1, amount));

        return stack;
    }

    private String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}