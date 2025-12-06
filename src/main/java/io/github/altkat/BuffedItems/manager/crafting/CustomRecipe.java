package io.github.altkat.BuffedItems.manager.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomRecipe {
    private final String id;
    private final String resultItemId;
    private final int amount;
    private final boolean shaped;
    private final Map<Integer, RecipeIngredient> ingredients;

    private boolean valid;
    private final List<String> errorMessages;

    public CustomRecipe(String id, String resultItemId, int amount, boolean shaped) {
        this.id = id;
        this.resultItemId = resultItemId;
        this.amount = amount;
        this.shaped = shaped;
        this.ingredients = new HashMap<>();
        this.valid = true;
        this.errorMessages = new ArrayList<>();
    }

    public void addIngredient(int slot, RecipeIngredient ingredient) {
        ingredients.put(slot, ingredient);
    }

    public RecipeIngredient getIngredient(int slot) {
        return ingredients.get(slot);
    }

    public Map<Integer, RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public String getId() { return id; }
    public String getResultItemId() { return resultItemId; }
    public int getAmount() { return amount; }
    public boolean isShaped() { return shaped; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public List<String> getErrorMessages() { return errorMessages; }

    public void addErrorMessage(String message) {
        this.errorMessages.add(message);
        this.valid = false;
    }
}