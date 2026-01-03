package io.github.altkat.BuffedItems.utility.item.data;

import org.bukkit.Color;
import java.util.List;
import java.util.Optional;

public class ItemDisplay {
    private final String displayName;
    private final List<String> lore;
    private final boolean glow;
    private final Integer customModelData;
    private final String customModelDataRaw;
    private final Color color;

    public ItemDisplay(String displayName, List<String> lore, boolean glow, Integer customModelData, String customModelDataRaw, Color color) {
        this.displayName = displayName;
        this.lore = lore;
        this.glow = glow;
        this.customModelData = customModelData;
        this.customModelDataRaw = customModelDataRaw;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public boolean hasGlow() {
        return glow;
    }

    public Optional<Integer> getCustomModelData() {
        return Optional.ofNullable(customModelData);
    }

    public Optional<String> getCustomModelDataRaw() {
        return Optional.ofNullable(customModelDataRaw);
    }
    
    public Optional<Color> getColor() {
        return Optional.ofNullable(color);
    }
}