package io.github.altkat.BuffedItems.menu.crafting;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.MainMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RecipeListMenu extends PaginatedMenu {

    private final BuffedItems plugin;

    public RecipeListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Crafting Recipes";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        List<CustomRecipe> recipes = getSortedRecipes();

        if (e.getCurrentItem() == null) return;
        if (handlePageChange(e, recipes.size())) return;

        if (e.getSlot() == 49) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("create_recipe");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter a unique ID for the new recipe (e.g. 'magic_wand_craft')."));
            return;
        }

        if (e.getSlot() == 45) {
            boolean current = RecipesConfig.get().getBoolean("settings.register-to-book", true);
            RecipesConfig.get().set("settings.register-to-book", !current);
            RecipesConfig.save();

            plugin.getCraftingManager().loadRecipes(true);

            p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1, 1);
            this.open();
            return;
        }

        if (e.getSlot() == 53) {
            new MainMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() < 45) {
            int index = maxItemsPerPage * page + e.getSlot();
            if (index >= recipes.size()) return;

            CustomRecipe recipe = recipes.get(index);

            if (e.getClick() == ClickType.LEFT) {
                playerMenuUtility.setRecipeToEditId(recipe.getId());
                new RecipeEditorMenu(playerMenuUtility, plugin).open();
            }
            else if (e.getClick() == ClickType.RIGHT) {
                RecipesConfig.get().set("recipes." + recipe.getId(), null);
                RecipesConfig.save();
                plugin.getCraftingManager().loadRecipes(true);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§cRecipe '" + recipe.getId() + "' deleted."));
                this.open();
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuControls();
        setFillerGlass();

        boolean isEnabled = RecipesConfig.get().getBoolean("settings.register-to-book", true);
        String status = isEnabled ? "§aEnabled" : "§cDisabled";
        Material toggleIcon = isEnabled ? Material.KNOWLEDGE_BOOK : Material.BOOK;

        inventory.setItem(45, makeItem(toggleIcon,
                "§6Recipe Book Registration",
                "§7Determines if custom recipes are",
                "§7added to the vanilla Recipe Book.",
                "",
                "§7Current: " + status,
                "",
                "§eClick to Toggle"));

        inventory.setItem(49, makeItem(Material.ANVIL, "§aCreate New Recipe", "§7Click to create a new crafting recipe."));
        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack to Main Menu"));


        List<CustomRecipe> recipes = getSortedRecipes();

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= recipes.size()) break;

            CustomRecipe recipe = recipes.get(index);
            ItemStack icon;

            if (recipe.isValid()) {
                BuffedItem item = plugin.getItemManager().getBuffedItem(recipe.getResultItemId());
                if (item != null) {
                    icon = new ItemBuilder(item, plugin).build();
                    icon.setAmount(Math.max(1, recipe.getAmount()));
                } else {
                    icon = makeItem(Material.BEDROCK, "§cUnknown Item", "§7ID: " + recipe.getResultItemId());
                }
            } else {
                icon = new ItemStack(Material.BARRIER);
            }

            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                String colorCode = recipe.isValid() ? "§a" : "§c";
                meta.displayName(ConfigManager.fromSection(colorCode + recipe.getId()));

                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());

                if (recipe.isValid()) {
                    lore.add(ConfigManager.fromSection("§7Result: §f" + recipe.getResultItemId()));
                    lore.add(ConfigManager.fromSection("§7Amount: §e" + recipe.getAmount()));
                } else {
                    lore.add(ConfigManager.fromSection("§c§lCONFIGURATION ERRORS:"));
                    for (String err : recipe.getErrorMessages()) {
                        lore.add(ConfigManager.fromSection("§c- " + err));
                    }
                }

                lore.add(Component.empty());
                lore.add(ConfigManager.fromSection("§eLeft-Click to Edit"));
                lore.add(ConfigManager.fromSection("§cRight-Click to Delete"));

                meta.lore(lore);
                icon.setItemMeta(meta);
            }

            icon.setAmount(1);

            inventory.setItem(i, icon);
        }
    }

    private List<CustomRecipe> getSortedRecipes() {
        List<CustomRecipe> list = new ArrayList<>(plugin.getCraftingManager().getRecipes().values());
        list.sort(Comparator.comparing(CustomRecipe::getId));
        return list;
    }
}