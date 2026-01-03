package io.github.altkat.BuffedItems.utility.item.data.visual;

import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleDisplay;
import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleShape;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public enum ParticlePreset {

    CHERRY_SPIRAL(
            "Cherry Spiral",
            Material.PINK_PETALS,
            new String[]{
                    "§7A beautiful spiral of cherry leaves",
                    "§7swirling around the player.",
                    "",
                    "§7Shape: §fHELIX",
                    "§7Particle: §dCHERRY_LEAVES"
            },
            createDisplay(Particle.CHERRY_LEAVES, ParticleShape.HELIX, 3, 0.05, 0.7, 1, 40)
    ),

    ANGEL_HALO(
            "Angel Halo",
            Material.GLOWSTONE_DUST,
            new String[]{
                    "§7A ring of white light hovering",
                    "§7above the player's head.",
                    "",
                    "§7Shape: §fCIRCLE",
                    "§7Particle: §fDUST (White)"
            },
            createDisplayHalo(Particle.DUST, ParticleShape.CIRCLE, 10, 0.0, 0.4, 0.0, 20)
    ),

    GOLDEN_WINGS(
            "Golden Wings",
            Material.GOLD_INGOT,
            new String[]{
                    "§7Majestic golden wings formed",
                    "§7on the player's back.",
                    "",
                    "§7Shape: §fWING",
                    "§7Particle: §6DUST (Gold)"
            },
            createGoldenWings()
    ),

    LOVERS_MARK(
            "Lover's Mark",
            Material.HEART_OF_THE_SEA,
            new String[]{
                    "§7A single heart pulsing",
                    "§7above your head.",
                    "",
                    "§7Shape: §fPOINT",
                    "§7Particle: §cHEART"
            },
            createLoversMark()
    ),

    RADIOACTIVE(
            "Radioactive",
            Material.SLIME_BALL,
            new String[]{
                    "§7Emit dangerous radioactive",
                    "§7particles from your body.",
                    "",
                    "§7Shape: §fOFFSET",
                    "§7Particle: §aDUST (Lime)"
            },
            createRadioactive()
    ),

    GALACTIC_SHIELD(
            "Galactic Shield",
            Material.ENDER_EYE,
            new String[]{
                    "§7A protective sphere of",
                    "§7cosmic energy.",
                    "",
                    "§7Shape: §fSPHERE",
                    "§7Particle: §fEND_ROD"
            },
            createGalacticSphere()
    ),

    INFERNO_BLAST(
            "Inferno Blast",
            Material.FIRE_CHARGE,
            new String[]{
                    "§7An explosion of fire",
                    "§7bursting outwards.",
                    "",
                    "§7Shape: §fBURST",
                    "§7Particle: §6FLAME"
            },
            createInfernoBlast()
    ),

    VOID_VORTEX(
            "Void Vortex",
            Material.OBSIDIAN,
            new String[]{
                    "§7A consuming vortex of",
                    "§7soul fire.",
                    "",
                    "§7Shape: §fVORTEX",
                    "§7Particle: §bSOUL_FIRE"
            },
            createVoidVortex()
    );

    private final String name;
    private final Material icon;
    private final String[] description;
    private final ParticleDisplay display;

    ParticlePreset(String name, Material icon, String[] description, ParticleDisplay display) {
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.display = display;
    }

    public String getName() { return name; }
    public Material getIcon() { return icon; }
    public String[] getDescription() { return description; }
    public ParticleDisplay getDisplay() { return display; }

    // Helper for standard shapes
    private static ParticleDisplay createDisplay(Particle p, ParticleShape shape, int count, double speed, double radius, double height, double period) {
        ParticleDisplay pd = new ParticleDisplay(p);
        pd.setShape(shape);
        pd.setCount(count);
        pd.setSpeed(speed);
        pd.setRadius(radius);
        pd.setHeight(height);
        pd.setPeriod(period);
        return pd;
    }

    // Helper for Angel Halo specific setup (DUST, white color, offset)
    private static ParticleDisplay createDisplayHalo(Particle p, ParticleShape shape, int count, double speed, double radius, double height, double period) {
        ParticleDisplay pd = new ParticleDisplay(p);
        pd.setShape(shape);
        pd.setCount(count);
        pd.setSpeed(speed);
        pd.setRadius(radius);
        pd.setHeight(height);
        pd.setPeriod(period);
        pd.setOffset(new Vector(0, 1, 0)); // Halo offset above head
        if (p == Particle.DUST) {
            pd.setColor(Color.fromRGB(255, 255, 255)); // White color for DUST
        }
        return pd;
    }

    // Helper for specific complex setup (Gold Wings)
    private static ParticleDisplay createGoldenWings() {
        ParticleDisplay pd = new ParticleDisplay(Particle.DUST);
        pd.setShape(ParticleShape.WING);
        pd.setCount(10); // Density
        pd.setSpeed(0);
        pd.setRadius(0.5); // Wing Span
        pd.setOffset(new Vector(0, 0, -0.3)); // Back of player
        pd.setColor(Color.fromRGB(255, 215, 0)); // Gold
        return pd;
    }

    private static ParticleDisplay createLoversMark() {
        ParticleDisplay pd = new ParticleDisplay(Particle.HEART);
        pd.setShape(ParticleShape.POINT);
        pd.setCount(1);
        pd.setOffset(new Vector(0, 1, 0));
        pd.setPeriod(40); // Slower, more natural pulse
        return pd;
    }

    private static ParticleDisplay createRadioactive() {
        ParticleDisplay pd = new ParticleDisplay(Particle.DUST);
        pd.setShape(ParticleShape.OFFSET);
        pd.setCount(5);
        pd.setRadius(0.8);
        pd.setOffset(new Vector(0, 1.0, 0)); // Center on body
        pd.setColor(Color.fromRGB(50, 255, 50));
        return pd;
    }

    private static ParticleDisplay createGalacticSphere() {
        ParticleDisplay pd = new ParticleDisplay(Particle.END_ROD);
        pd.setShape(ParticleShape.SPHERE);
        pd.setRadius(1.5);
        pd.setCount(15);
        pd.setPeriod(10);
        return pd;
    }

    private static ParticleDisplay createInfernoBlast() {
        ParticleDisplay pd = new ParticleDisplay(Particle.FLAME);
        pd.setShape(ParticleShape.BURST);
        pd.setCount(40);
        pd.setSpeed(0.15);
        pd.setPeriod(20);
        return pd;
    }

    private static ParticleDisplay createVoidVortex() {
        ParticleDisplay pd = new ParticleDisplay(Particle.SOUL_FIRE_FLAME);
        pd.setShape(ParticleShape.VORTEX);
        pd.setRadius(1.2);
        pd.setHeight(2.2);
        pd.setCount(10);  // More particles for a complete spiral look
        pd.setPeriod(40); // Smooth rotation over 2 seconds
        pd.setOffset(new Vector(0, -1.0, 0)); // Centered on player
        return pd;
    }
}
