package io.github.altkat.BuffedItems.utility.attribute;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import java.util.UUID;

/**
 * A simple data-holding class (record) to store pre-parsed attribute information.
 * This prevents parsing strings and calculating UUIDs on every tick in the EffectApplicatorTask.
 */
public class ParsedAttribute {

    private final Attribute attribute;
    private final AttributeModifier.Operation operation;
    private final double amount;
    private final UUID uuid;

    /**
     * Constructs a new ParsedAttribute.
     *
     * @param attribute The Bukkit Attribute.
     * @param operation The modifier operation (e.g., ADD_NUMBER).
     * @param amount    The value of the modifier.
     * @param uuid      The pre-calculated unique UUID for this modifier.
     */
    public ParsedAttribute(Attribute attribute, AttributeModifier.Operation operation, double amount, UUID uuid) {
        this.attribute = attribute;
        this.operation = operation;
        this.amount = amount;
        this.uuid = uuid;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public AttributeModifier.Operation getOperation() {
        return operation;
    }

    public double getAmount() {
        return amount;
    }

    public UUID getUuid() {
        return uuid;
    }
}