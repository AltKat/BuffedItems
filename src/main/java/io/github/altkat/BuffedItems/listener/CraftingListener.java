package io.github.altkat.BuffedItems.listener;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.crafting.CustomRecipe;
import io.github.altkat.BuffedItems.manager.crafting.RecipeIngredient;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public class CraftingListener implements Listener {

    private final BuffedItems plugin;

    public CraftingListener(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        CraftingInventory inv = e.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        // 1. Özel tarif kontrolü yap
        CustomRecipe match = plugin.getCraftingManager().findRecipe(matrix);

        if (match != null) {
            // EŞLEŞME VAR: Sonucu yerleştir.
            BuffedItem resultItem = plugin.getItemManager().getBuffedItem(match.getResultItemId());

            if (resultItem != null) {
                ItemStack resultStack = new ItemBuilder(resultItem, plugin).build();
                resultStack.setAmount(match.getAmount());
                inv.setResult(resultStack);
            } else {
                inv.setResult(null); // Item config'den silinmişse boş ver.
            }
        } else {
            // EŞLEŞME YOK:
            // Eğer sonuç slotunda bir BuffedItem belirdiyse (Vanilla eşleşmesi yüzünden), bunu engelle.
            Recipe bukkitRecipe = e.getRecipe();
            if (bukkitRecipe != null) {
                ItemStack result = inv.getResult();
                // isBuffedItem metodu artık ItemManager'da mevcut olduğu için hata vermeyecek.
                if (plugin.getItemManager().isBuffedItem(result)) {
                    inv.setResult(null);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent e) {
        if (!(e.getInventory() instanceof CraftingInventory)) return;

        CraftingInventory inv = (CraftingInventory) e.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        if (matrix.length < 9) return;

        CustomRecipe match = plugin.getCraftingManager().findRecipe(matrix);
        if (match == null) return; // Bizim tarifimiz değilse karışma.

        // --- GÜVENLİK ADIMI: SHIFT CLICK ENGELLEME ---
        if (e.isShiftClick()) {
            e.setCancelled(true);
            if (e.getWhoClicked() instanceof Player) {
                // Config dosyasında bu mesaj yoksa varsayılanı kullanırız.
                e.getWhoClicked().sendMessage(ConfigManager.fromLegacyWithPrefix("&cCustom crafting with Shift-Click is currently disabled for safety."));
            }
            return;
        }
        // ---------------------------------------------

        // Miktar düşürme mantığı (Amount > 1 olan malzemeler için)
        for (int i = 0; i < matrix.length; i++) {
            if (i >= 9) break;

            RecipeIngredient ingredient = match.getIngredient(i);
            ItemStack itemInSlot = matrix[i];

            if (ingredient != null && itemInSlot != null && itemInSlot.getType() != Material.AIR) {
                int required = ingredient.getAmount();

                // Vanilla sistemi zaten her slottan 1 tane silecek.
                // Eğer gereksinim 1'den büyükse, kalanı biz silmeliyiz.
                if (required > 1) {
                    int currentAmount = itemInSlot.getAmount();
                    int amountToReduceExtra = required - 1;

                    // Not: Bu işlem event gerçekleştikten sonra envanterde kalan miktarı düzenler.
                    if (currentAmount > amountToReduceExtra) {
                        itemInSlot.setAmount(currentAmount - amountToReduceExtra);
                    } else {
                        itemInSlot.setAmount(0);
                    }
                }
            }
        }
        // Matrix'i güncelle
        inv.setMatrix(matrix);
    }
}