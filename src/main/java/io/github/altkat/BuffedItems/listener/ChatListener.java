package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.listener.handler.*;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.menu.active.*;
import io.github.altkat.BuffedItems.menu.crafting.IngredientSettingsMenu;
import io.github.altkat.BuffedItems.menu.crafting.RecipeEditorMenu;
import io.github.altkat.BuffedItems.menu.crafting.RecipeListMenu;
import io.github.altkat.BuffedItems.menu.editor.EnchantmentListMenu;
import io.github.altkat.BuffedItems.menu.editor.ItemEditorMenu;
import io.github.altkat.BuffedItems.menu.editor.LoreEditorMenu;
import io.github.altkat.BuffedItems.menu.editor.PermissionSettingsMenu;
import io.github.altkat.BuffedItems.menu.passive.EffectListMenu;
import io.github.altkat.BuffedItems.menu.selector.EnchantmentSelectorMenu;
import io.github.altkat.BuffedItems.menu.selector.MaterialSelectorMenu;
import io.github.altkat.BuffedItems.menu.selector.TypeSelectorMenu;
import io.github.altkat.BuffedItems.menu.set.SetBonusEffectSelectorMenu;
import io.github.altkat.BuffedItems.menu.set.SetBonusesMenu;
import io.github.altkat.BuffedItems.menu.set.SetEditorMenu;
import io.github.altkat.BuffedItems.menu.set.SetListMenu;
import io.github.altkat.BuffedItems.menu.upgrade.IngredientListMenu;
import io.github.altkat.BuffedItems.menu.upgrade.UpgradeRecipeEditorMenu;
import io.github.altkat.BuffedItems.menu.upgrade.UpgradeRecipeListMenu;
import io.github.altkat.BuffedItems.menu.utility.ItemListMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final BuffedItems plugin;
    private final BasicInputHandler basicInputHandler;
    private final LoreInputHandler loreInputHandler;
    private final CostInputHandler costInputHandler;
    private final ActiveSettingsInputHandler activeSettingsInputHandler;
    private final EffectInputHandler effectInputHandler;
    private final CreationInputHandler creationInputHandler;
    private final UpgradeInputHandler upgradeInputHandler;
    private final IngredientInputHandler ingredientInputHandler;
    private final SetInputHandler setInputHandler;
    private final RecipeInputHandler recipeInputHandler;

    public ChatListener(BuffedItems plugin) {
        this.plugin = plugin;
        this.basicInputHandler = new BasicInputHandler(plugin);
        this.loreInputHandler = new LoreInputHandler(plugin);
        this.costInputHandler = new CostInputHandler(plugin);
        this.activeSettingsInputHandler = new ActiveSettingsInputHandler(plugin);
        this.effectInputHandler = new EffectInputHandler(plugin);
        this.creationInputHandler = new CreationInputHandler(plugin);
        this.upgradeInputHandler = new UpgradeInputHandler(plugin);
        this.ingredientInputHandler = new IngredientInputHandler(plugin);
        this.setInputHandler = new SetInputHandler(plugin);
        this.recipeInputHandler = new RecipeInputHandler(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent e) {
        Player player = e.getPlayer();
        PlayerMenuUtility pmu = BuffedItems.getPlayerMenuUtility(player);

        if (!pmu.isWaitingForChatInput()) {
            return;
        }

        e.setCancelled(true);

        String input = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .build()
                .serialize(e.message());
        String path = pmu.getChatInputPath();
        String itemId = pmu.getItemToEditId();

        ConfigManager.sendDebugMessage(ConfigManager.DEBUG_VERBOSE,
                () -> "[Chat] Processing input from " + player.getName() + ": path=" + path + ", input=" + input);

        Bukkit.getScheduler().runTask(plugin, () -> {
            handleChatInput(player, pmu, input, path, itemId);
        });
    }

    private void closeChatInput(PlayerMenuUtility pmu) {
        pmu.setWaitingForChatInput(false);
        pmu.setChatInputPath(null);
    }

    private void handleChatInput(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        if (path == null) {
            closeChatInput(pmu);
            return;
        }

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cOperation cancelled."));
            handleCancelAction(player, pmu, path);
            closeChatInput(pmu);
            return;
        }

        // --- SPECIAL HANDLERS ---
        if (path.equals("enchantment_search")) {
            handleEnchantmentSearch(player, pmu, input);
            return;
        }

        // --- MODULE-LEVEL ROUTING ---
        if (path.equals("create_upgrade")) { // Handle create_upgrade specifically
            upgradeInputHandler.handle(player, pmu, input, path, itemId);
            return;
        }
        if (path.startsWith("upgrade")) {
            if (path.startsWith("upgrade.ingredients.") || path.startsWith("upgrade.base.")) {
                ingredientInputHandler.handle(player, pmu, input, path, itemId);
            } else {
                upgradeInputHandler.handle(player, pmu, input, path, itemId);
            }
            return;
        }
        if (path.startsWith("recipe_") || path.equals("create_recipe")) {
            recipeInputHandler.handle(player, pmu, input, path, itemId);
            return;
        }
        if (path.equals("createnewitem") || path.equals("duplicateitem")) {
            creationInputHandler.handle(player, pmu, input, path, itemId);
            return;
        }
        // --- SET SYSTEM ROUTING (Specific Before General) ---
        if (path.startsWith("set.potion.") || path.startsWith("set.attribute.")) {
            effectInputHandler.handle(player, pmu, input, path, itemId);
            return;
        }
        if (path.equals("create_set") || path.equals("set_display_name") || path.equals("create_bonus_tier") || path.equals("set_add_item")) {
            setInputHandler.handle(player, pmu, input, path, itemId);
            return;
        }

        // --- ITEM-SPECIFIC HANDLERS (Order is crucial: most specific to most general) ---

        // 1. Most specific prefixes for active abilities
        if (path.startsWith("active_ability.actions.effects.")) {
            effectInputHandler.handle(player, pmu, input, path, itemId);
        } else if (path.startsWith("active_ability.costs.")) {
            costInputHandler.handle(player, pmu, input, path, itemId);
        }
        // 2. Broader paths for basic properties, including specific permission overrides
        else if (path.startsWith("display.") || path.equals("permission") || path.equals("active_ability.permission") || path.equals("passive_effects.permission") || path.equals("material.manual")) {
            basicInputHandler.handle(player, pmu, input, path, itemId);
        }
        // 3. General active ability settings (catches the rest of active_ability.*)
        else if (path.startsWith("active_ability.") || path.startsWith("usage.")) {
            activeSettingsInputHandler.handle(player, pmu, input, path, itemId);
        }
        // 4. Passive effects and other generic handlers
        else if (path.startsWith("potion_effects") || path.startsWith("attributes") || path.startsWith("enchantments")) {
            effectInputHandler.handle(player, pmu, input, path, itemId);
        }
        // 5. Default case for unknown paths
        else {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Unknown input path: " + path));
            ConfigManager.sendDebugMessage(ConfigManager.DEBUG_INFO,
                    () -> "[Chat] Attempted to set unknown path via chat: " + path);
            closeChatInput(pmu);
        }
    }

    private void handleCancelAction(Player player, PlayerMenuUtility pmu, String path) {
        if (path.startsWith("upgrade.") || path.equals("create_upgrade")) {
            if (path.equals("create_upgrade")) {
                new UpgradeRecipeListMenu(pmu, plugin).open();
                return;
            }

            if (path.startsWith("upgrade.ingredients.")) {
                if (path.startsWith("upgrade.ingredients.edit.")) {
                    new IngredientListMenu(pmu, plugin).open();
                } else if (path.equals("upgrade.ingredients.add.ITEM_QUANTITY")) {
                    pmu.setMaterialContext(PlayerMenuUtility.MaterialSelectionContext.INGREDIENT);
                    new MaterialSelectorMenu(pmu, plugin).open();
                } else if (path.startsWith("upgrade.ingredients.add.")) {
                    new TypeSelectorMenu(pmu, plugin, PlayerMenuUtility.MaterialSelectionContext.INGREDIENT).open();
                } else {
                    new IngredientListMenu(pmu, plugin).open();
                }
            } else {
                new UpgradeRecipeEditorMenu(pmu, plugin).open();
            }
            return;
        }

        if (path.equals("createnewitem") || path.equals("duplicateitem")) {
            new ItemListMenu(pmu, plugin).open();
            return;
        }

        if (path.equals("display_name") || path.equals("custom_model_data") || path.equals("material.manual")) {
            new ItemEditorMenu(pmu, plugin).open();
            return;
        }

        if(path.equals("permission") || path.equals("active_permission") || path.equals("passive_permission")){
            new PermissionSettingsMenu(pmu, plugin).open();
            return;
        }

        if (path.startsWith("lore.")) {
            new LoreEditorMenu(pmu, plugin).open();
            return;
        }

        if (path.startsWith("usage-limit.commands.")) {
            new CommandListMenu(pmu, plugin, CommandListMenu.CommandContext.DEPLETION).open();
            return;
        }

        if (path.startsWith("usage-limit.")) {
            new UsageLimitSettingsMenu(pmu, plugin).open();
            return;
        }

        if (path.startsWith("active.")) {
            if (path.startsWith("active.costs.")) {
                if (path.startsWith("active.costs.edit.")) {
                    new CostListMenu(pmu, plugin).open();
                } else if (path.equals("active.costs.add.ITEM_QUANTITY")) {
                    pmu.setMaterialContext(PlayerMenuUtility.MaterialSelectionContext.COST);
                    new MaterialSelectorMenu(pmu, plugin).open();
                } else if (path.startsWith("active.costs.add.")) {
                    new TypeSelectorMenu(pmu, plugin, PlayerMenuUtility.MaterialSelectionContext.COST).open();
                } else {
                    new CostListMenu(pmu, plugin).open();
                }
                return;
            }

            if (path.startsWith("active.commands.")) {
                new CommandListMenu(pmu, plugin, CommandListMenu.CommandContext.ACTIVE).open();
                return;
            }

            if (path.startsWith("active.msg.")) {
                new ActiveItemVisualsMenu(pmu, plugin).open();
                return;
            }

            if (path.startsWith("active.sounds.")) {
                new ActiveItemSoundsMenu(pmu, plugin).open();
                return;
            }

            if (path.startsWith("active.potion_effects")) {
                new EffectListMenu(pmu, plugin, EffectListMenu.EffectType.POTION_EFFECT, "ACTIVE").open();
                return;
            }
            if (path.startsWith("active.attributes")) {
                new EffectListMenu(pmu, plugin, EffectListMenu.EffectType.ATTRIBUTE, "ACTIVE").open();
                return;
            }

            new ActiveItemSettingsMenu(pmu, plugin).open();
            return;
        }

        if (path.startsWith("potion_effects.")) {
            String slot = pmu.getTargetSlot();
            new EffectListMenu(pmu, plugin, EffectListMenu.EffectType.POTION_EFFECT, slot).open();
            return;
        }
        if (path.startsWith("attributes.")) {
            String slot = pmu.getTargetSlot();
            new EffectListMenu(pmu, plugin, EffectListMenu.EffectType.ATTRIBUTE, slot).open();
            return;
        }

        if (path.startsWith("enchantments.")) {
            new EnchantmentListMenu(pmu, plugin).open();
            return;
        }
        if (path.equals("enchantment_search")) {
            new EnchantmentSelectorMenu(pmu, plugin).open();
            return;
        }

        if (path.startsWith("create_set")) {
            new SetListMenu(pmu, plugin).open();
            return;
        }

        if (path.equals("set_display_name")) {
            new SetEditorMenu(pmu, plugin).open();
            return;
        }

        if (path.equals("create_bonus_tier")) {
            new SetBonusesMenu(pmu, plugin).open();
            return;
        }

        if (path.startsWith("set.potion.") || path.startsWith("set.attribute.")) {
            new SetBonusEffectSelectorMenu(pmu, plugin).open();
            return;
        }

        if (path.equals("create_recipe")) {
            new RecipeListMenu(pmu, plugin).open();
            return;
        }

        if (path.equals("recipe_result_amount") || path.equals("recipe_result_manual") || path.equals("recipe_permission")) {
            new RecipeEditorMenu(pmu, plugin).open();
            return;
        }

        if (path.equals("recipe_ingredient_amount") || path.equals("recipe_ingredient_buffed_manual")
                || path.equals("recipe_ingredient_material_manual")) {

            new IngredientSettingsMenu(pmu, plugin, false).open();
            return;
        }
    }

    private void handleEnchantmentSearch(Player player, PlayerMenuUtility pmu, String input) {
        closeChatInput(pmu);
        EnchantmentSelectorMenu menu = new EnchantmentSelectorMenu(pmu, plugin);
        menu.searchEnchantments(input);
        menu.open();

        if (input.equalsIgnoreCase("clear")) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSearch cleared. Showing all enchantments."));
        } else {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSearching for: §e" + input));
        }
    }
}