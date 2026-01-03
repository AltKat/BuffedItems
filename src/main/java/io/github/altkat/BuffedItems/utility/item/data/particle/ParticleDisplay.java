package io.github.altkat.BuffedItems.utility.item.data.particle;

import io.github.altkat.BuffedItems.utility.item.data.visual.VisualTriggerMode;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

public class ParticleDisplay {
    private VisualTriggerMode triggerMode;
    private Particle particle;
    private ParticleShape shape;
    private int count;
    private double speed;
    private Vector offset;
    
    // Shape Specific Parameters
    private double radius;
    private double height;
    private double period;
    private int duration;
    private int delay;
    
    // Color/Data
    private Color color; 
    private String materialData;

    public ParticleDisplay(Particle particle) {
        this.triggerMode = VisualTriggerMode.CONTINUOUS;
        this.particle = particle;
        this.shape = ParticleShape.POINT;
        this.count = 1;
        this.speed = 0.0;
        this.offset = new Vector(0, 0, 0);
        this.radius = 1.0;
        this.height = 1.0;
        this.period = 20.0;
        this.duration = 0;
        this.delay = 0;
    }

    public ParticleDisplay(VisualTriggerMode triggerMode, Particle particle, ParticleShape shape, int count, double speed, Vector offset, double radius, double height, double period, int duration, int delay, Color color, String materialData) {
        this.triggerMode = triggerMode != null ? triggerMode : VisualTriggerMode.CONTINUOUS;
        this.particle = particle;
        this.shape = shape;
        this.count = count;
        this.speed = speed;
        this.offset = offset;
        this.radius = radius;
        this.height = height;
        this.period = period;
        this.duration = duration;
        this.delay = delay;
        this.color = color;
        this.materialData = materialData;
    }

    public VisualTriggerMode getTriggerMode() { return triggerMode; }
    public void setTriggerMode(VisualTriggerMode triggerMode) { this.triggerMode = triggerMode; }

    public Particle getParticle() { return particle; }
    public void setParticle(Particle particle) { this.particle = particle; }

    public ParticleShape getShape() { return shape; }
    public void setShape(ParticleShape shape) { this.shape = shape; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public Vector getOffset() { return offset; }
    public void setOffset(Vector offset) { this.offset = offset; }

    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public double getPeriod() { return period; }
    public void setPeriod(double period) { this.period = period; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }

    public Color getColor() { return color; }
    public void setColor(Color color) { this.color = color; }

    public String getMaterialData() { return materialData; }
    public void setMaterialData(String materialData) { this.materialData = materialData; }
    
    public ParticleDisplay clone() {
        return new ParticleDisplay(triggerMode, particle, shape, count, speed, offset.clone(), radius, height, period, duration, delay, color, materialData);
    }
}