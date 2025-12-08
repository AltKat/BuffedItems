package io.github.altkat.BuffedItems.menu.crafting;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.RecipesConfig;
import io.github.altkat.BuffedItems.manager.crafting.MatchType;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.BuffedItemSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
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
        return 45;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getCurrentItem() == null) return;

        for (int i = 0; i < gridSlots.length; i++) {
            if (e.getSlot() == gridSlots[i]) {
                playerMenuUtility.setSelectedRecipeSlot(i);
                new IngredientSettingsMenu(playerMenuUtility, plugin).open();
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

        if (e.getSlot() == 40) {
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

        ConfigurationSection ingredientsSec = section.getConfigurationSection("ingredients");

        for (int i = 0; i < 9; i++) {
            int slot = gridSlots[i];
            char key = (char) ('A' + i);

            if (ingredientsSec != null && ingredientsSec.contains(String.valueOf(key))) {
                ConfigurationSection ingSec = ingredientsSec.getConfigurationSection(String.valueOf(key));
                if (ingSec != null) {
                    ItemStack icon = getIngredientIconFromConfig(ingSec);
                    inventory.setItem(slot, icon);
                } else {
                    inventory.setItem(slot, makeItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Empty Slot", "§eClick to Set"));
                }
            } else {
                inventory.setItem(slot, makeItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§7Empty Slot", "§eClick to Set"));
            }
        }

        String resultId = section.getString("result.item");
        int amount = section.getInt("result.amount", 1);

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

        inventory.setItem(22, makeItem(Material.CRAFTING_TABLE, "§eCrafting Grid", "§7Configure the 3x3 pattern."));
        inventory.setItem(23, makeItem(Material.ARROW, "§7->"));
        inventory.setItem(40, makeItem(Material.BARRIER, "§cBack"));
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