package io.github.altkat.BuffedItems.menu.upgrade;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.UpgradesConfig;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import io.github.altkat.BuffedItems.manager.cost.types.BuffedItemCost;
import io.github.altkat.BuffedItems.manager.cost.types.ItemCost;
import io.github.altkat.BuffedItems.manager.upgrade.FailureAction;
import io.github.altkat.BuffedItems.manager.upgrade.UpgradeRecipe;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class UpgradeMenu extends Menu {

    private final BuffedItems plugin;
    private final int INPUT_SLOT = 10;
    private final int OUTPUT_SLOT = 16;
    private final int INFO_SLOT = 13;
    private final int NEXT_RECIPE_SLOT = 14;

    private List<UpgradeRecipe> matchingRecipes = new ArrayList<>();
    private int currentRecipeIndex = 0;

    public UpgradeMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
    }

    @Override
    public String getMenuName() {
        return "Item Upgrade Station";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getClickedInventory() == e.getView().getBottomInventory()) {
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
                ItemStack currentInput = inventory.getItem(INPUT_SLOT);
                if (currentInput == null || currentInput.getType() == Material.AIR) {
                    if (e.getCurrentItem() != null) {
                        inventory.setItem(INPUT_SLOT, e.getCurrentItem().clone());
                        e.setCurrentItem(null);
                        updateMenuState();
                    }
                }
            } else {
                e.setCancelled(false);
            }
            return;
        }

        if (e.getClickedInventory() == inventory) {

            if (e.getSlot() == INPUT_SLOT) {
                e.setCancelled(false);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    currentRecipeIndex = 0;
                    updateMenuState();
                });
                return;
            }

            e.setCancelled(true);

            if (e.getCurrentItem() == null) return;

            if (e.getSlot() == OUTPUT_SLOT) {
                handleUpgrade((Player) e.getWhoClicked());
            }
            else if (e.getSlot() == NEXT_RECIPE_SLOT && e.getCurrentItem().getType() == Material.LIME_STAINED_GLASS_PANE) {
                currentRecipeIndex++;
                if (currentRecipeIndex >= matchingRecipes.size()) {
                    currentRecipeIndex = 0;
                }
                updateVisualsForCurrentRecipe();
                Player p = (Player) e.getWhoClicked();
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            }
            else if (e.getSlot() == 22 && e.getCurrentItem().getType() == Material.BOOK) {
                new RecipeBrowserMenu(playerMenuUtility, plugin).open();
                return;
            }
        }
    }

    @Override
    public void handleClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
        ItemStack inputItem = inventory.getItem(INPUT_SLOT);
        if (inputItem != null && inputItem.getType() != Material.AIR) {
            Player player = (Player) e.getPlayer();
            for (ItemStack drop : player.getInventory().addItem(inputItem).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        inventory.setItem(INPUT_SLOT, null);
        inventory.setItem(OUTPUT_SLOT, makeItem(Material.BARRIER, "§cNo Recipe Found", "§7Place a valid item to see upgrades."));
        inventory.setItem(11, makeItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        inventory.setItem(12, makeItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        inventory.setItem(INFO_SLOT, makeItem(Material.ANVIL, "§eUpgrade Info", "§7Waiting for item..."));
        inventory.setItem(NEXT_RECIPE_SLOT, makeItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        inventory.setItem(15, makeItem(Material.ARROW, "§e->"));

        boolean showBrowser = UpgradesConfig.get().getBoolean("settings.browser-button", true);

        if (showBrowser) {
            inventory.setItem(22, makeItem(Material.BOOK, "§aBrowse All Recipes",
                    "§7Click to view a list of",
                    "§7all available upgrades on the server."));
        }
    }

    private void updateMenuState() {
        ItemStack inputItem = inventory.getItem(INPUT_SLOT);
        matchingRecipes.clear();

        if (inputItem == null || inputItem.getType() == Material.AIR) {
            inventory.setItem(OUTPUT_SLOT, makeItem(Material.BARRIER, "§cNo Item", "§7Place an item in the left slot."));
            inventory.setItem(INFO_SLOT, makeItem(Material.ANVIL, "§eUpgrade Info", "§7Waiting for item..."));
            inventory.setItem(NEXT_RECIPE_SLOT, makeItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            return;
        }

        matchingRecipes = findAllRecipes(inputItem);

        if (matchingRecipes.isEmpty()) {
            inventory.setItem(OUTPUT_SLOT, makeItem(Material.RED_STAINED_GLASS_PANE, "§cInvalid Item", "§7No upgrade recipe found for this item."));
            inventory.setItem(INFO_SLOT, makeItem(Material.ANVIL, "§eUpgrade Info", "§cThis item cannot be upgraded."));
            inventory.setItem(NEXT_RECIPE_SLOT, makeItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        } else {
            if (currentRecipeIndex >= matchingRecipes.size()) currentRecipeIndex = 0;
            updateVisualsForCurrentRecipe();
        }
    }

    private void updateVisualsForCurrentRecipe() {
        UpgradeRecipe recipe = matchingRecipes.get(currentRecipeIndex);
        ItemStack inputItem = inventory.getItem(INPUT_SLOT);

        BuffedItem resultBuffedItem = plugin.getItemManager().getBuffedItem(recipe.getResultItemId());
        ItemStack resultStack = (resultBuffedItem != null)
                ? new ItemBuilder(resultBuffedItem, plugin).build()
                : new ItemStack(Material.BARRIER);
        resultStack.setAmount(recipe.getResultAmount());
        inventory.setItem(OUTPUT_SLOT, resultStack);

        if (matchingRecipes.size() > 1) {
            inventory.setItem(NEXT_RECIPE_SLOT, makeItem(Material.LIME_STAINED_GLASS_PANE,
                    "§aNext Recipe",
                    "§7Click to view the next available upgrade.",
                    "§7(" + (currentRecipeIndex + 1) + "/" + matchingRecipes.size() + ")"));
        } else {
            inventory.setItem(NEXT_RECIPE_SLOT, makeItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        List<String> lore = new ArrayList<>();
        lore.add("§7Recipe: §r" + ConfigManager.toSection(ConfigManager.fromLegacy(recipe.getDisplayName())));
        lore.add("");
        lore.add("§7Requirements:");

        Player p = playerMenuUtility.getOwner();
        boolean canAfford = true;

        ICost baseCost = recipe.getBaseCost();
        lore.add("§a✔ " + baseCost.getDisplayString() + " §8(Input Item)");

        for (ICost cost : recipe.getIngredients()) {
            boolean has = cost.hasEnough(p);
            if (!has) canAfford = false;
            lore.add((has ? "§a✔ " : "§c✖ ") + cost.getDisplayString());
        }

        lore.add("");
        lore.add("§7Success Rate: §b" + recipe.getSuccessRate() + "%");
        lore.add("");

        if (canAfford) {
            lore.add("§aClick the Output Item to Upgrade!");
        } else {
            lore.add("§cYou do not meet the requirements.");
        }

        inventory.setItem(INFO_SLOT, makeItem(Material.BOOK, "§aRecipe Found!", lore.toArray(new String[0])));
    }

    private void handleUpgrade(Player p) {
        ItemStack inputItem = inventory.getItem(INPUT_SLOT);
        if (inputItem == null || matchingRecipes.isEmpty()) return;

        if (currentRecipeIndex >= matchingRecipes.size()) currentRecipeIndex = 0;
        UpgradeRecipe recipe = matchingRecipes.get(currentRecipeIndex);

        int requiredBase = 1;

        if (inputItem.getAmount() < requiredBase) {
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§cInput item amount error!"));
            return;
        }

        List<String> missingRequirements = new ArrayList<>();
        for (ICost cost : recipe.getIngredients()) {
            if (!cost.hasEnough(p)) {
                String failureMsg = cost.getFailureMessage();
                missingRequirements.add(plugin.getHookManager().processPlaceholders(p, failureMsg));
            }
        }

        if (!missingRequirements.isEmpty()) {
            sendMessageWithPapi(p,"upgrade-requirements-not-met");
            for (String error : missingRequirements) {
                p.sendMessage(ConfigManager.fromLegacy(" &7- " + error));
            }
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);
            return;
        }

        boolean success = (Math.random() * 100) <= recipe.getSuccessRate();

        if (success) {
            inputItem.setAmount(inputItem.getAmount() - requiredBase);
            inventory.setItem(INPUT_SLOT, inputItem);

            for (ICost cost : recipe.getIngredients()) {
                cost.deduct(p);
            }

            BuffedItem resultBuffedItem = plugin.getItemManager().getBuffedItem(recipe.getResultItemId());
            ItemStack resultStack;

            if (resultBuffedItem != null) {
                resultStack = new ItemBuilder(resultBuffedItem, plugin).build();
                resultStack.setAmount(recipe.getResultAmount());

                org.bukkit.inventory.meta.ItemMeta meta = resultStack.getItemMeta();

                if (meta != null) {
                    if (meta.hasDisplayName()) {
                        net.kyori.adventure.text.Component originalName = meta.displayName();
                        if (originalName != null) {
                            String legacyName = ConfigManager.toSection(originalName);
                            String parsedName = plugin.getHookManager().processPlaceholders(p, legacyName);
                            meta.displayName(ConfigManager.fromSection(parsedName));
                        }
                    }

                    if (meta.hasLore()) {
                        List<net.kyori.adventure.text.Component> originalLore = meta.lore();
                        if (originalLore != null) {
                            List<net.kyori.adventure.text.Component> parsedLore = originalLore.stream()
                                    .map(ConfigManager::toSection)
                                    .map(line -> plugin.getHookManager().processPlaceholders(p, line))
                                    .map(ConfigManager::fromSection)
                                    .collect(java.util.stream.Collectors.toList());
                            meta.lore(parsedLore);
                        }
                    }
                    resultStack.setItemMeta(meta);
                }

            } else {
                resultStack = new ItemStack(Material.BARRIER);
            }

            java.util.HashMap<Integer, ItemStack> leftovers = p.getInventory().addItem(resultStack);

            if (!leftovers.isEmpty()) {
                for (ItemStack item : leftovers.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), item);
                }
                sendMessageWithPapi(p,"upgrade-inventory-full");
            }

            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
            sendMessageWithPapi(p,"upgrade-success");;
            plugin.getEffectApplicatorTask().markPlayerForUpdate(p.getUniqueId());

        } else {
            FailureAction action = recipe.getFailureAction();

            if (action == FailureAction.LOSE_EVERYTHING) {
                inputItem.setAmount(inputItem.getAmount() - requiredBase);
                inventory.setItem(INPUT_SLOT, inputItem);
                for (ICost cost : recipe.getIngredients()) cost.deduct(p);

                sendMessageWithPapi(p,"upgrade-failure-lose-everything");
            }
            else if (action == FailureAction.KEEP_BASE_ONLY) {
                for (ICost cost : recipe.getIngredients()) cost.deduct(p);

                sendMessageWithPapi(p,"upgrade-failure-keep-base");
            }
            else if (action == FailureAction.KEEP_EVERYTHING) {
                sendMessageWithPapi(p,"upgrade-failure-keep-everything");
            }

            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1, 1);
        }

        updateMenuState();
    }

    private List<UpgradeRecipe> findAllRecipes(ItemStack item) {
        List<UpgradeRecipe> found = new ArrayList<>();
        if (item == null || plugin.getUpgradeManager() == null) return found;

        for (UpgradeRecipe recipe : plugin.getUpgradeManager().getRecipes().values()) {
            if (!recipe.isValid()) continue;

            if (isCostSatisfiedByInput(recipe.getBaseCost(), item)) {
                found.add(recipe);
            }
        }
        return found;
    }

    private boolean isCostSatisfiedByInput(ICost cost, ItemStack input) {
        if (input == null) return false;

        if (cost instanceof BuffedItemCost) {
            String reqId = ((BuffedItemCost) cost).getRequiredItemId();
            if (reqId == null) return false;
            if (!input.hasItemMeta()) return false;
            String inputId = input.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "buffeditem_id"), PersistentDataType.STRING);
            return reqId.equals(inputId);
        }
        else if (cost instanceof ItemCost) {
            Material reqMat = ((ItemCost) cost).getMaterial();
            return input.getType() == reqMat;
        }
        return false;
    }

    private void sendMessageWithPapi(Player p, String configKey) {
        String rawMsg = plugin.getConfig().getString("messages." + configKey);
        if (rawMsg == null) return;

        String parsedMsg = plugin.getHookManager().processPlaceholders(p, rawMsg);
        p.sendMessage(ConfigManager.fromLegacyWithPrefix(parsedMsg));
    }
}