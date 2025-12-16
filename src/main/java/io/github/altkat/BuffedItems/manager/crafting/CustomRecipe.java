package io.github.altkat.BuffedItems.manager.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomRecipe {
    private final String id;
    private final String resultItemId;
    private final int amount;
    private final List<String> shape;
    private final String permission;
    private final Map<Character, RecipeIngredient> ingredients;

    private boolean valid;
    private final List<String> errorMessages;

    private boolean enabled = true;

    public CustomRecipe(String id, String resultItemId, int amount, List<String> shape, String permission) {
        this.id = id;
        this.resultItemId = resultItemId;
        this.amount = amount;
        this.shape = shape;
        this.permission = permission;
        this.ingredients = new HashMap<>();
        this.valid = true;
        this.errorMessages = new ArrayList<>();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void addIngredient(char key, RecipeIngredient ingredient) {
        ingredients.put(key, ingredient);
    }
    public RecipeIngredient getIngredient(char key) {
        return ingredients.get(key);
    }
    public Map<Character, RecipeIngredient> getIngredients() {
        return ingredients;
    }
    public String getId() { return id; }
    public String getResultItemId() { return resultItemId; }
    public int getAmount() { return amount; }
    public List<String> getShape() {
        return shape;
    }
    public String getPermission() { return permission; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public List<String> getErrorMessages() { return errorMessages; }
    public void addErrorMessage(String message) {
        this.errorMessages.add(message);
        this.valid = false;
    }
}