package io.github.altkat.BuffedItems.utility.set;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuffedSet {
    private final String id;
    private final String displayName;
    private final List<String> itemIds;
    private final Map<Integer, SetBonus> bonuses;

    private final boolean valid;
    private final List<String> errorMessages;

    public BuffedSet(String id, String displayName, List<String> itemIds, Map<Integer, SetBonus> bonuses, boolean valid, List<String> errorMessages) {
        this.id = id;
        this.displayName = displayName;
        this.itemIds = itemIds;
        this.bonuses = bonuses;
        this.valid = valid;
        this.errorMessages = (errorMessages != null) ? errorMessages : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getItemIds() { return itemIds; }
    public Map<Integer, SetBonus> getBonuses() { return bonuses; }

    public boolean isValid() { return valid; }
    public List<String> getErrorMessages() { return errorMessages; }

    public SetBonus getBonusFor(int count) {
        return bonuses.get(count);
    }
}