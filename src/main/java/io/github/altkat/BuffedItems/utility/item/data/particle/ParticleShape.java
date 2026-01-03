package io.github.altkat.BuffedItems.utility.item.data.particle;

public enum ParticleShape {
    POINT("Point (Static)"),
    OFFSET("Random Offset"),
    CIRCLE("Circle (Orbit)"),
    HELIX("Helix (DNA)"),
    SPHERE("Sphere"),
    BURST("Burst"),
    VORTEX("Vortex"),
    WING("Wings (Simple)");

    private final String displayName;

    ParticleShape(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}