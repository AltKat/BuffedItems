package io.github.altkat.BuffedItems.manager.upgrade;

import io.github.altkat.BuffedItems.manager.cost.ICost;

import java.util.ArrayList;
import java.util.List;

public class UpgradeRecipe {

    private final String id;
    private final String displayName;
    private final ICost baseCost;
    private final List<ICost> ingredients;
    private final String resultItemId;
    private final int resultAmount;
    private final double successRate;
    private final boolean preventFailureLoss;

    private final boolean valid;
    private final List<String> errorMessages;

    public UpgradeRecipe(String id, String displayName, ICost baseCost, List<ICost> ingredients, String resultItemId, int resultAmount, double successRate, boolean preventFailureLoss, boolean valid, List<String> errorMessages) {
        this.id = id;
        this.displayName = displayName;
        this.baseCost = baseCost;
        this.ingredients = ingredients;
        this.resultItemId = resultItemId;
        this.resultAmount = resultAmount;
        this.successRate = successRate;
        this.preventFailureLoss = preventFailureLoss;
        this.valid = valid;
        this.errorMessages = (errorMessages != null) ? errorMessages : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public ICost getBaseCost() { return baseCost; }
    public List<ICost> getIngredients() { return ingredients; }
    public String getResultItemId() { return resultItemId; }
    public int getResultAmount() { return resultAmount; }
    public double getSuccessRate() { return successRate; }
    public boolean isPreventFailureLoss() { return preventFailureLoss; }

    public boolean isValid() { return valid; }
    public List<String> getErrorMessages() { return errorMessages; }
}