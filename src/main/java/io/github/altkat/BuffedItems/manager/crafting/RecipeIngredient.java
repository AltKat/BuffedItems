package io.github.altkat.BuffedItems.manager.crafting;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class RecipeIngredient {
    private final MatchType matchType;
    private final Material material;
    private final String data;
    private final int amount;
    private ItemStack cachedPreviewItem;


    public RecipeIngredient(MatchType matchType, Material material, String data, int amount) {
        this.matchType = matchType;
        this.material = material;
        this.data = data;
        this.amount = Math.max(1, amount);
    }

    public MatchType getMatchType() { return matchType; }
    public Material getMaterial() { return material; }
    public String getData() { return data; }
    public int getAmount() { return amount; }

    public void setCachedPreviewItem(ItemStack item) {
        this.cachedPreviewItem = item;
    }

    public ItemStack getCachedPreviewItem() {
        if (cachedPreviewItem == null) {
            ItemStack stack = new ItemStack(material != null ? material : Material.BARRIER);
            stack.setAmount(amount);
            return stack;
        }
        cachedPreviewItem.setAmount(amount);
        return cachedPreviewItem;
    }
}