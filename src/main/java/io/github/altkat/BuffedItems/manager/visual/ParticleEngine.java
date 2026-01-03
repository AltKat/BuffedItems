package io.github.altkat.BuffedItems.manager.visual;

import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleDisplay;
import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleShape;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class ParticleEngine {

    private static final Random random = new Random();

    public static void spawnScheduled(Plugin plugin, Entity entity, ParticleDisplay display) {
        if (display == null) return;

        if (display.getDelay() > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.isValid()) {
                        executeSpawn(plugin, entity, display);
                    }
                }
            }.runTaskLater(plugin, display.getDelay());
        } else {
            executeSpawn(plugin, entity, display);
        }
    }

    private static void executeSpawn(Plugin plugin, Entity entity, ParticleDisplay display) {
        if (display.getDuration() <= 0) {
            spawn(entity.getLocation().add(0, 1, 0), display, 0);
        } else {
            new BukkitRunnable() {
                long tick = 0;
                @Override
                public void run() {
                    if (!entity.isValid() || tick >= display.getDuration()) {
                        this.cancel();
                        return;
                    }
                    spawn(entity.getLocation().add(0, 1, 0), display, tick);
                    tick++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    public static void spawn(Location origin, ParticleDisplay display, long currentTick) {
        if (display == null || display.getParticle() == null) return;

        Vector offset = display.getOffset().clone();
        if (origin.getYaw() != 0) {
            offset.rotateAroundY(-Math.toRadians(origin.getYaw()));
        }
        Location center = origin.clone().add(offset);
        
        ParticleShape shape = display.getShape();

        switch (shape) {
            case POINT:
                spawnSingle(center, display);
                break;
            case OFFSET:
                spawnOffset(center, display);
                break;
            case CIRCLE:
                spawnCircle(center, display, currentTick);
                break;
            case HELIX:
                spawnHelix(center, display, currentTick);
                break;
            case SPHERE:
                spawnSphere(center, display);
                break;
            case BURST:
                spawnBurst(center, display);
                break;
            case VORTEX:
                spawnVortex(center, display, currentTick);
                break;
            case WING:
                spawnWing(center, display);
                break;
        }
    }

    private static void spawnSingle(Location loc, ParticleDisplay data) {
        spawnParticle(loc, data, 0, 0, 0, 0);
    }

    private static void spawnOffset(Location loc, ParticleDisplay data) {
        double r = data.getRadius();
        for (int i = 0; i < data.getCount(); i++) {
            Location target = loc.clone().add(
                    (random.nextDouble() * 2 - 1) * r,
                    (random.nextDouble() * 2 - 1) * r,
                    (random.nextDouble() * 2 - 1) * r
            );
            spawnParticle(target, data, 0, 0, 0, 0);
        }
    }

    private static void spawnCircle(Location center, ParticleDisplay data, long tick) {
        double radius = data.getRadius();
        double period = data.getPeriod() == 0 ? 20 : data.getPeriod();
        
        double angle = ((double) tick / period) * Math.PI * 2;
        
        if (data.getCount() > 1) {
            double inc = (Math.PI * 2) / data.getCount();
            for (int i = 0; i < data.getCount(); i++) {
                double a = angle + (i * inc);
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                spawnParticle(center.clone().add(x, 0, z), data, 0, 0, 0, 0);
            }
        } else {
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            spawnParticle(center.clone().add(x, 0, z), data, 0, 0, 0, 0);
        }
    }

    private static void spawnHelix(Location center, ParticleDisplay data, long tick) {
        double radius = data.getRadius();
        double height = data.getHeight();
        double period = data.getPeriod() == 0 ? 20 : data.getPeriod();
        
        double angle = ((double) tick / period) * Math.PI * 2;
        double y = (tick % period) / period * height;
        
        double x1 = Math.cos(angle) * radius;
        double z1 = Math.sin(angle) * radius;
        spawnParticle(center.clone().add(x1, y, z1), data, 0, 0, 0, 0);
        
        double x2 = Math.cos(angle + Math.PI) * radius;
        double z2 = Math.sin(angle + Math.PI) * radius;
        spawnParticle(center.clone().add(x2, y, z2), data, 0, 0, 0, 0);
    }
    
    private static void spawnVortex(Location center, ParticleDisplay data, long tick) {
        double radius = data.getRadius();
        double height = data.getHeight();
        double period = data.getPeriod() == 0 ? 20 : data.getPeriod();
        int count = data.getCount() <= 0 ? 1 : data.getCount();

        for (int i = 0; i < count; i++) {
            // Offset each particle's progress if multiple particles are spawned
            double offset = (double) i / count;
            double progress = ((tick + (offset * period)) % period) / period;

            double currentY = progress * height;
            double currentRadius = progress * radius;
            double angle = progress * Math.PI * 4; // 2 full rotations

            double x = Math.cos(angle) * currentRadius;
            double z = Math.sin(angle) * currentRadius;

            spawnParticle(center.clone().add(x, currentY, z), data, 0, 0, 0, 0);
        }
    }

    private static void spawnSphere(Location center, ParticleDisplay data) {
        double r = data.getRadius();
        for (int i = 0; i < data.getCount(); i++) {
            double u = Math.random();
            double v = Math.random();
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            double x = r * Math.sin(phi) * Math.cos(theta);
            double y = r * Math.sin(phi) * Math.sin(theta);
            double z = r * Math.cos(phi);
            spawnParticle(center.clone().add(x, y, z), data, 0, 0, 0, 0);
        }
    }

    private static void spawnBurst(Location center, ParticleDisplay data) {
        int count = data.getCount() <= 0 ? 10 : data.getCount();
        double speed = data.getSpeed() <= 0 ? 0.1 : data.getSpeed();
        
        Object extra = parseData(data);
        center.getWorld().spawnParticle(data.getParticle(), center, count, 0.2, 0.2, 0.2, speed, extra);
    }

        private static void spawnWing(Location center, ParticleDisplay data) {
            double scale = data.getRadius() <= 0 ? 1.0 : data.getRadius();
            int points = data.getCount() <= 0 ? 15 : data.getCount();
            double step = 2.5 / points;
            
            double yaw = Math.toRadians(center.getYaw());
    
            for (double x = 0; x <= 2.5; x += step) {
                // Parabolic curve: starts at (0,0), peaks around x=1.2, then dips
                double y = (-Math.pow(x - 1.2, 2) + 1.44) * 0.8;
    
                double finalX = x * scale;
                double finalY = y * scale;
                
                Vector rightWing = new Vector(finalX, finalY, 0);
                rightWing.rotateAroundY(-yaw);
                spawnParticle(center.clone().add(rightWing), data, 0, 0, 0, 0);
    
                Vector leftWing = new Vector(-finalX, finalY, 0);
                leftWing.rotateAroundY(-yaw);
                spawnParticle(center.clone().add(leftWing), data, 0, 0, 0, 0);
            }
        }
    private static void spawnParticle(Location loc, ParticleDisplay data, double ox, double oy, double oz, double speed) {
        double finalSpeed = speed == 0 ? data.getSpeed() : speed;
        
        // DUST
        if (data.getParticle() == Particle.DUST && data.getColor() != null) {
            Object extra = parseData(data);
            if (extra instanceof Particle.DustOptions) {
                loc.getWorld().spawnParticle(data.getParticle(), loc, 1, ox, oy, oz, finalSpeed, extra);
            }
            return;
        }

        // ENTITY_EFFECT (Mob Spell)
        if ((data.getParticle() == Particle.ENTITY_EFFECT) && data.getColor() != null) {
             double r = data.getColor().getRed() / 255.0;
             double g = data.getColor().getGreen() / 255.0;
             double b = data.getColor().getBlue() / 255.0;
             if (r == 0) r = 0.001; 
             loc.getWorld().spawnParticle(data.getParticle(), loc, 0, r, g, b, 1);
             return;
        }

        // NOTE
        if (data.getParticle() == Particle.NOTE && data.getColor() != null) {
             double note = data.getColor().getRed() / 255.0;
             loc.getWorld().spawnParticle(data.getParticle(), loc, 0, note, 0, 0, 1);
             return;
        }
        
        Object extra = parseData(data);
        loc.getWorld().spawnParticle(data.getParticle(), loc, 1, ox, oy, oz, finalSpeed, extra);
    }

    private static Object parseData(ParticleDisplay data) {
        if (data.getParticle() == Particle.DUST) {
            Color c = data.getColor() != null ? data.getColor() : Color.RED;
            float size = (float) data.getRadius(); 
            if (size <= 0.1) size = 1.0f;
            return new Particle.DustOptions(c, size);
        }
        
        if (data.getParticle() == Particle.ITEM) {
            String matName = data.getMaterialData();
            Material mat = Material.STONE;
            if (matName != null) {
                try { mat = Material.valueOf(matName.toUpperCase()); } catch (Exception ignored) {}
            }
            return new ItemStack(mat);
        }
        
        if (data.getParticle() == Particle.BLOCK || data.getParticle() == Particle.FALLING_DUST) {
             String matName = data.getMaterialData();
            Material mat = Material.STONE;
            if (matName != null) {
                try { mat = Material.valueOf(matName.toUpperCase()); } catch (Exception ignored) {}
            }
            return mat.createBlockData();
        }
        
        return null;
    }
}