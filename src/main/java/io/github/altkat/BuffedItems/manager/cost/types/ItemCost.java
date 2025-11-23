package io.github.altkat.BuffedItems.manager.cost.types;

import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.cost.ICost;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.Map;

public class ItemCost implements ICost {
    private final Material material;
    private final int amount;
    private final String failureMessage;

    public ItemCost(Map<String, Object> data) {
        String matName = (String) data.getOrDefault("material", "STONE");

        Material matched = Material.matchMaterial(matName);
        if (matched == null) {
            throw new IllegalArgumentException("Invalid material name: '" + matName + "'");
        }
        this.material = matched;

        this.amount = ((Number) data.getOrDefault("amount", 1)).intValue();
        if (this.amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive. Found: " + this.amount);
        }

        String defaultMsg = ConfigManager.getDefaultCostMessage("ITEM");
        this.failureMessage = (String) data.getOrDefault("message", defaultMsg);
    }

    public int getAmount() {
        return amount;
    }

    public Material getMaterial() {
        return material;
    }

    @Override
    public String getDisplayString() {
        String name = (material != null) ? formatMaterialName(material) : "Unknown Item";
        return "§f" + amount + "x " + name;
    }

    @Override
    public boolean hasEnough(Player player) {
        if (material == null) return false;
        int count = countVanillaItems(player);
        return count >= amount;
    }

    @Override
    public void deduct(Player player) {
        if (material == null) return;
        int remainingToRemove = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isVanillaItem(item)) {
                if (item.getAmount() <= remainingToRemove) {
                    remainingToRemove -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remainingToRemove);
                    remainingToRemove = 0;
                }

                if (remainingToRemove <= 0) break;
            }
        }
    }

    @Override
    public String getFailureMessage() {
        String matName = (material != null) ? material.name() : "INVALID_ITEM";
        return ConfigManager.stripLegacy(failureMessage)
                .replace("{amount}", String.valueOf(amount))
                .replace("{material}", matName);
    }

    private int countVanillaItems(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isVanillaItem(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private boolean isVanillaItem(ItemStack item) {
        if (item == null || item.getType().isAir() || item.getType() != material) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return true;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();

        return container.getKeys().isEmpty();
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