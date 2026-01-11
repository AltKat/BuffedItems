package io.github.altkat.BuffedItems.menu.crafting;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.manager.crafting.MatchType;
import io.github.altkat.BuffedItems.manager.crafting.RecipeIngredient;
import io.github.altkat.BuffedItems.manager.crafting.RecipeType;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class PublicRecipeListMenu extends PaginatedMenu {

    private final BuffedItems plugin;

    public PublicRecipeListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.maxItemsPerPage = 36;
    }

    @Override
    public String getMenuName() {
        return "Server Recipes (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        List<CustomRecipe> recipes = getValidRecipes();

        if (e.getCurrentItem() == null) return;
        if (handlePageChange(e, recipes.size())) return;

        if (e.getSlot() == 49) {
            e.getWhoClicked().closeInventory();
            return;
        }

        if (e.getSlot() >= 9 && e.getSlot() < 45) {
            int index = maxItemsPerPage * page + (e.getSlot() - 9);
            if (index >= recipes.size()) return;

            CustomRecipe recipe = recipes.get(index);
            new RecipePreviewMenu(playerMenuUtility, plugin, recipe).open();
        }
    }

    @Override
    public void setMenuItems() {
        ItemStack filler = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inventory.setItem(i, filler);
        for (int i = 45; i < 54; i++) inventory.setItem(i, filler);

        addMenuControls();

        inventory.setItem(49, makeItem(Material.BARRIER, "§cClose Menu"));

        List<CustomRecipe> recipes = getValidRecipes();

        for (int i = 0; i < maxItemsPerPage; i++) {
            int index = maxItemsPerPage * page + i;
            if (index >= recipes.size()) break;

            CustomRecipe recipe = recipes.get(index);

            ItemStack icon;
            BuffedItem item = plugin.getItemManager().getBuffedItem(recipe.getResultItemId());
            if (item != null) {
                icon = new ItemBuilder(item, plugin).build();
            } else {
                icon = makeItem(Material.CHEST, "§f" + recipe.getId());
            }

            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                if (recipe.getAmount() > 1 && item != null) {
                   Component originalName = meta.hasDisplayName() ? meta.displayName() : ConfigManager.fromSection("§f" + item.getItemDisplay().getDisplayName());
                   meta.displayName(ConfigManager.fromSection("§e" + recipe.getAmount() + "x ").append(originalName));
                }

                List<Component> lore = new ArrayList<>();
                lore.add(Component.empty());
                
                String typeName = formatRecipeType(recipe.getType());
                lore.add(ConfigManager.fromSection("§7Station: §f" + typeName));
                lore.add(Component.empty());
                
                lore.add(ConfigManager.fromSection("§7Ingredients:"));
                Map<String, Integer> ingredientCounts = aggregateIngredients(recipe);
                if (ingredientCounts.isEmpty()) {
                     lore.add(ConfigManager.fromSection(" §8- None"));
                } else {
                    for (Map.Entry<String, Integer> entry : ingredientCounts.entrySet()) {
                        lore.add(ConfigManager.fromSection(" §8• §7" + entry.getKey() + " §8x" + entry.getValue()));
                    }
                }

                lore.add(Component.empty());
                lore.add(ConfigManager.fromSection("§eClick to view details"));

                meta.lore(lore);
                icon.setItemMeta(meta);
            }

            icon.setAmount(1);
            inventory.setItem(9 + i, icon);
        }
    }

    private List<CustomRecipe> getValidRecipes() {
        return plugin.getCraftingManager().getRecipes().values().stream()
                .filter(CustomRecipe::isValid)
                .filter(CustomRecipe::isEnabled)
                .sorted(Comparator.comparing(CustomRecipe::getId))
                .collect(Collectors.toList());
    }
    
    private String formatRecipeType(RecipeType type) {
        switch (type) {
            case SHAPED: return "Crafting Table";
            case SHAPELESS: return "Crafting Table (Shapeless)";
            case FURNACE: return "Furnace";
            case BLAST_FURNACE: return "Blast Furnace";
            case SMOKER: return "Smoker";
            case CAMPFIRE: return "Campfire";
            default: return formatEnumName(type.name());
        }
    }
    
    private String formatEnumName(String name) {
        String lower = name.toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : lower.split(" ")) {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private Map<String, Integer> aggregateIngredients(CustomRecipe recipe) {
        Map<String, Integer> counts = new HashMap<>();

        if (recipe.getType() == RecipeType.SHAPED) {
             List<String> shape = recipe.getShape();
             if (shape != null) {
                 for (String row : shape) {
                     for (char c : row.toCharArray()) {
                         if (c == ' ') continue;
                         RecipeIngredient ing = recipe.getIngredient(c);
                         if (ing != null) {
                             addIngredientCount(counts, ing, 1);
                         }
                     }
                 }
             }
        } else {
             for (RecipeIngredient ing : recipe.getIngredients().values()) {
                 addIngredientCount(counts, ing, 1);
             }
        }
        
        return counts;
    }
    
    private void addIngredientCount(Map<String, Integer> counts, RecipeIngredient ing, int multiplier) {
        String name = getIngredientName(ing);
        int amount = ing.getAmount() * multiplier;
        counts.put(name, counts.getOrDefault(name, 0) + amount);
    }
    
    private String getIngredientName(RecipeIngredient ing) {
        if (ing.getMatchType() == MatchType.BUFFED_ITEM) {
            BuffedItem bi = plugin.getItemManager().getBuffedItem(ing.getData());
            if (bi != null) {
                String rawName = bi.getItemDisplay().getDisplayName();
                return ConfigManager.toSection(ConfigManager.fromLegacy(rawName));
            }
            return ing.getData();
        } else if (ing.getMatchType() == MatchType.MATERIAL) {
            if (ing.getMaterial() != null) return formatEnumName(ing.getMaterial().name());
        } else if (ing.getMatchType() == MatchType.EXACT) {
             if (ing.getExactReferenceItem() != null) {
                 if (ing.getExactReferenceItem().getItemMeta() != null && ing.getExactReferenceItem().getItemMeta().hasDisplayName()) {
                     return ConfigManager.toSection(ing.getExactReferenceItem().getItemMeta().displayName());
                 }
                 return formatEnumName(ing.getExactReferenceItem().getType().name());
             }
        }
        return "Unknown Item";
    }
}