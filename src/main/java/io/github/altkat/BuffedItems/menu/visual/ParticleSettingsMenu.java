package io.github.altkat.BuffedItems.menu.visual;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.selector.ParticleSelectorMenu;
import io.github.altkat.BuffedItems.menu.selector.ParticleShapeSelectorMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleDisplay;
import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleShape;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ParticleSettingsMenu extends Menu {

    private final BuffedItems plugin;
    private ParticleDisplay currentParticle;
    private final boolean isPassive;

    private static final List<Particle> COLORABLE_PARTICLES = Arrays.asList(Particle.DUST, Particle.ENTITY_EFFECT, Particle.NOTE);
    private static final List<Particle> MATERIAL_PARTICLES = Arrays.asList(Particle.BLOCK, Particle.ITEM, Particle.FALLING_DUST);


    public ParticleSettingsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.isPassive = "PASSIVE_VISUALS".equals(playerMenuUtility.getTargetSlot());
        loadCurrentParticle();
    }

    private void loadCurrentParticle() {
        String itemId = playerMenuUtility.getItemToEditId();
        int index = playerMenuUtility.getEditIndex();
        String baseVisualsPath = isPassive ? 
                "items." + itemId + ".passive_effects.visuals.particles" : 
                "items." + itemId + ".active_ability.visuals.cast.particles";
        
        List<Map<?, ?>> particlesList = ItemsConfig.get().getMapList(baseVisualsPath);
        if (index >= 0 && index < particlesList.size()) {
            Map<?, ?> rawParticle = particlesList.get(index);
            currentParticle = parseParticleMap(rawParticle);
        } else {
            currentParticle = new ParticleDisplay(Particle.FLAME);
        }
    }
    
    private ParticleDisplay parseParticleMap(Map<?, ?> map) {
        String typeStr = map.get("type") != null ? map.get("type").toString() : "FLAME";
        Particle particle = Particle.valueOf(typeStr.toUpperCase());
        
        String shapeStr = map.get("shape") != null ? map.get("shape").toString() : "POINT";
        ParticleShape shape;
        try { shape = ParticleShape.valueOf(shapeStr.toUpperCase()); } catch (IllegalArgumentException e) { shape = ParticleShape.POINT; }

        int count = map.get("count") instanceof Number ? ((Number) map.get("count")).intValue() : 1;
        double speed = map.get("speed") instanceof Number ? ((Number) map.get("speed")).doubleValue() : 0.0;
        
        double ox = map.get("offset_x") instanceof Number ? ((Number) map.get("offset_x")).doubleValue() : 0.0;
        double oy = map.get("offset_y") instanceof Number ? ((Number) map.get("offset_y")).doubleValue() : 0.0;
        double oz = map.get("offset_z") instanceof Number ? ((Number) map.get("offset_z")).doubleValue() : 0.0;
        Vector offset = new Vector(ox, oy, oz);

        double radius = map.get("radius") instanceof Number ? ((Number) map.get("radius")).doubleValue() : 1.0;
        double height = map.get("height") instanceof Number ? ((Number) map.get("height")).doubleValue() : 1.0;
        double period = map.get("period") instanceof Number ? ((Number) map.get("period")).doubleValue() : 20.0;
        int duration = map.get("duration") instanceof Number ? ((Number) map.get("duration")).intValue() : 0;
        int delay = map.get("delay") instanceof Number ? ((Number) map.get("delay")).intValue() : 0;

        Color color = null;
        if (map.containsKey("color")) {
            String cStr = map.get("color").toString();
            try {
                if (cStr.startsWith("#")) cStr = cStr.substring(1);
                int r = Integer.valueOf(cStr.substring(0, 2), 16);
                int g = Integer.valueOf(cStr.substring(2, 4), 16);
                int b = Integer.valueOf(cStr.substring(4, 6), 16);
                color = Color.fromRGB(r, g, b);
            } catch (Exception ignored) {}
        }

        String materialData = map.get("material_data") != null ? map.get("material_data").toString() : null;
        
        io.github.altkat.BuffedItems.utility.item.data.visual.VisualTriggerMode mode = io.github.altkat.BuffedItems.utility.item.data.visual.VisualTriggerMode.CONTINUOUS;
        if (map.containsKey("mode")) {
            try { mode = io.github.altkat.BuffedItems.utility.item.data.visual.VisualTriggerMode.valueOf(map.get("mode").toString().toUpperCase()); } catch(Exception ignored){}
        }

        return new ParticleDisplay(mode, particle, shape, count, speed, offset, radius, height, period, duration, delay, color, materialData);
    }
    
    @Override
    public String getMenuName() {
        return "Edit Particle";
    }

    @Override
    public int getSlots() {
        return 45;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();
        
        if (currentParticle == null) {
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: No particle session active."));
            new ParticleListMenu(playerMenuUtility, plugin).open();
            return;
        }

        switch (e.getSlot()) {
            case 10: 
                new ParticleSelectorMenu(playerMenuUtility, plugin).open();
                break;
            case 11: 
                new ParticleShapeSelectorMenu(playerMenuUtility, plugin).open();
                break;
            case 12:
                if (isPassive) {
                    toggleParticleMode();
                }
                break;
            case 13:
                if (COLORABLE_PARTICLES.contains(currentParticle.getParticle())) {
                    startChatInput(p, "particle.color", "§eFormat: R;G;B (e.g. 255;0;0) or #FF0000");
                }
                break;
            case 14:
                 if (MATERIAL_PARTICLES.contains(currentParticle.getParticle())) {
                    startChatInput(p, "particle.material", "§eEnter Block/Item Material (e.g. STONE)");
                }
                break;
            case 16:
                startChatInput(p, "particle.period", "§eEnter Period/Speed (Double)");
                break;
            case 19: 
                startChatInput(p, "particle.count", "§eEnter Count (Integer)");
                break;
            case 20: 
                startChatInput(p, "particle.speed", "§eEnter Speed/Extra (Double)");
                break;
            case 21: 
                startChatInput(p, "particle.radius", "§eEnter Radius (Double)");
                break;
            case 22:
                startChatInput(p, "particle.height", "§eEnter Height (Double)");
                break;
            case 25:
                startChatInput(p, "particle.delay", "§eEnter Delay (Ticks, Integer)");
                break;
            case 30:
                startChatInput(p, "particle.offset_x", "§eEnter Offset X (Double)");
                break;
            case 31:
                startChatInput(p, "particle.offset_y", "§eEnter Offset Y (Double)");
                break;
            case 32:
                startChatInput(p, "particle.offset_z", "§eEnter Offset Z (Double)");
                break;
            case 34:
                // Only allow Duration edit if NOT continuous
                if (isPassive && currentParticle.getTriggerMode() == io.github.altkat.BuffedItems.utility.item.data.visual.VisualTriggerMode.CONTINUOUS) {
                    p.sendMessage(ConfigManager.fromSectionWithPrefix("§cDuration is only available in ON_EQUIP mode."));
                    return;
                }
                startChatInput(p, "particle.duration", "§eEnter Duration (Ticks, Integer)");
                break;
            case 36:
                new ParticlePresetsMenu(playerMenuUtility, plugin).open();
                break;
            case 44:
                playerMenuUtility.setEditIndex(-1);
                new ParticleListMenu(playerMenuUtility, plugin).open();
                break;
        }
    }
    
    private void toggleParticleMode() {
        String itemId = playerMenuUtility.getItemToEditId();
        int index = playerMenuUtility.getEditIndex();
        String path = "items." + itemId + ".passive_effects.visuals.particles";
        
        List<Map<?, ?>> list = ItemsConfig.get().getMapList(path);
        if (index >= 0 && index < list.size()) {
            Map<Object, Object> map = (Map<Object, Object>) list.get(index);
            Map<Object, Object> mutableMap = new java.util.LinkedHashMap<>(map);
            
            String current = mutableMap.getOrDefault("mode", "CONTINUOUS").toString();
            String next = current.equals("CONTINUOUS") ? "ON_EQUIP" : "CONTINUOUS";
            
            mutableMap.put("mode", next);
            list.set(index, mutableMap);
            
            ItemsConfig.get().set(path, list);
            ItemsConfig.saveAsync();
            plugin.getItemManager().reloadSingleItem(itemId);
            loadCurrentParticle(); // Reload local object
            this.open();
        }
    }

    private void startChatInput(Player p, String path, String prompt) {
        playerMenuUtility.setWaitingForChatInput(true);
        playerMenuUtility.setChatInputPath(path);
        p.closeInventory();
        p.sendMessage(ConfigManager.fromSectionWithPrefix(prompt));
        p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to return)"));
    }

    @Override
    public void setMenuItems() {
        if (currentParticle == null) return;
        setFillerGlass();

        // --- IDENTITY (Row 2) ---
        inventory.setItem(10, makeItem(Material.FIREWORK_STAR, "§eParticle Type", 
                "§7Current: §f" + currentParticle.getParticle().name(), 
                "", 
                "§7Select the base visual of the effect.", 
                "§7Example: FLAME, SMOKE, SOUL_FIRE_FLAME...",
                "", "§eClick to change"));

        inventory.setItem(11, makeItem(Material.PAPER, "§eShape", 
                "§7Current: §f" + currentParticle.getShape().getDisplayName(), 
                "", 
                "§7Determines how particles are arranged",
                "§7around you.",
                "§fPOINT: §7Single point.",
                "§fCIRCLE: §7Rotating ring.",
                "§fHELIX: §7Spiral upward.",
                "§fWING: §7Wings on your back.",
                "", "§eClick to change"));

        if (isPassive) {
            inventory.setItem(12, makeItem(Material.COMPARATOR, "§eTrigger Mode",
                    "§7Current: §f" + currentParticle.getTriggerMode().name(),
                    "", 
                    "§fCONTINUOUS: §7Effect loops forever",
                    "§7while the item is active.",
                    "§fON_EQUIP: §7Effect plays only once",
                    "§7when you equip/hold the item.",
                    "", "§eClick to Toggle"));
        } else {
             inventory.setItem(12, makeItem(Material.BARRIER, "§cTrigger Mode", "§7Fixed for Active Skills"));
        }

        if (COLORABLE_PARTICLES.contains(currentParticle.getParticle())) {
            ItemStack colorItem = new ItemStack(Material.RED_DYE);
            ItemMeta colorMeta = colorItem.getItemMeta();
            colorMeta.setDisplayName("§eColor");
            List<String> colorLore = new ArrayList<>();
            if (currentParticle.getColor() != null) {
                colorLore.add("§7Current: §c" + currentParticle.getColor().getRed() + " §a" + currentParticle.getColor().getGreen() + " §9" + currentParticle.getColor().getBlue());
            } else {
                colorLore.add("§7Current: §fNone");
            }
            colorLore.add("");
            colorLore.add("§7Applies RGB color to the particle.");
            colorLore.add("§7Required for: §fDUST, NOTE, ENTITY_EFFECT.");
            colorLore.add("");
            colorLore.add("§eClick to set RGB");
            colorMeta.setLore(colorLore);
            colorItem.setItemMeta(colorMeta);
            inventory.setItem(13, colorItem);
        } else {
            String supported = COLORABLE_PARTICLES.stream().map(Enum::name).collect(Collectors.joining(", "));
            inventory.setItem(13, makeItem(Material.GRAY_DYE, "§7Color (Unavailable)",
                                          "§7This particle type does not support colors.",
                                          "§7Supported Types:",
                                          "§8" + supported));
        }

        if (MATERIAL_PARTICLES.contains(currentParticle.getParticle())) {
            inventory.setItem(14, makeItem(Material.STONE, "§eMaterial Data", 
                    "§7Current: §f" + (currentParticle.getMaterialData() != null ? currentParticle.getMaterialData() : "None"), 
                    "", 
                    "§7Defines which Block/Item is used",
                    "§7for the visual.",
                    "§7Required for: §fBLOCK, ITEM, FALLING_DUST.",
                    "", "§eClick to set Material"));
        } else {
            String supported = MATERIAL_PARTICLES.stream().map(Enum::name).collect(Collectors.joining(", "));
            inventory.setItem(14, makeItem(Material.BEDROCK, "§7Material Data (Unavailable)",
                                          "§7This particle type does not support material data.",
                                          "§7Supported Types:",
                                          "§8" + supported));
        }

        inventory.setItem(16, makeItem(Material.COMPASS, "§ePeriod", 
                "§7Value: §f" + currentParticle.getPeriod(), 
                "",
                "§7Controls animation speed.",
                "§7For CIRCLE/HELIX: Lower = Faster rotation.",
                "§7(Ticks per full rotation cycle)",
                "", "§eClick to edit"));

        // --- PHYSICS & OFFSETS (Row 3) ---
        inventory.setItem(19, makeItem(Material.BEACON, "§eCount", 
                "§7Value: §f" + currentParticle.getCount(), 
                "",
                "§7Amount of particles spawned per cycle.",
                "§7Higher = More dense visual.",
                "", "§eClick to edit"));

        inventory.setItem(20, makeItem(Material.FEATHER, "§eSpeed", 
                "§7Value: §f" + currentParticle.getSpeed(), 
                "",
                "§7Initial movement speed of particles.",
                "§7Can also affect size/randomness depending",
                "§7on the particle type.",
                "", "§eClick to edit"));

        inventory.setItem(21, makeItem(Material.SLIME_BALL, "§eRadius", 
                "§7Value: §f" + currentParticle.getRadius(), 
                "",
                "§7Horizontal size of the shape.",
                "§7Affects CIRCLE, HELIX, SPHERE, WING.",
                "", "§eClick to edit"));

        inventory.setItem(22, makeItem(Material.BAMBOO, "§eHeight", 
                "§7Value: §f" + currentParticle.getHeight(), 
                "",
                "§7Vertical size/stretch of the shape.",
                "§7Affects HELIX, VORTEX, WING.",
                "", "§eClick to edit"));
        
        inventory.setItem(25, makeItem(Material.REPEATER, "§eDelay", 
                "§7Value: §f" + currentParticle.getDelay() + " ticks", 
                "",
                "§7Time to wait before starting the effect.",
                "§7(20 ticks = 1 second)",
                "", "§eClick to edit"));

        // --- OFFSETS (Row 4) ---
        inventory.setItem(30, makeItem(Material.RED_STAINED_GLASS_PANE, "§eOffset X", 
                "§7Value: §f" + currentParticle.getOffset().getX(), 
                "",
                "§7Adjustment on East/West axis.",
                "", "§eClick to edit"));

        inventory.setItem(31, makeItem(Material.GREEN_STAINED_GLASS_PANE, "§eOffset Y", 
                "§7Value: §f" + currentParticle.getOffset().getY(), 
                "",
                "§7Adjustment on Up/Down axis.",
                "", "§eClick to edit"));

        inventory.setItem(32, makeItem(Material.BLUE_STAINED_GLASS_PANE, "§eOffset Z", 
                "§7Value: §f" + currentParticle.getOffset().getZ(), 
                "",
                "§7Adjustment on North/South axis.",
                "", "§eClick to edit"));

        if (isPassive && currentParticle.getTriggerMode() == io.github.altkat.BuffedItems.utility.item.data.visual.VisualTriggerMode.CONTINUOUS) {
            inventory.setItem(34, makeItem(Material.GRAY_DYE, "§7Duration (Locked)",
                    "§7Not available in CONTINUOUS mode.",
                    "§7Continuous effects loop forever.",
                    "",
                    "§cSwitch Trigger Mode to ON_EQUIP to enable."));
        } else {
            inventory.setItem(34, makeItem(Material.CLOCK, "§eDuration", 
                    "§7Value: §f" + currentParticle.getDuration() + " ticks", 
                    "",
                    "§7How long the effect should play.",
                    "§7(0 = Spawns only once)",
                    "", "§eClick to edit"));
        }

        inventory.setItem(36, makeItem(Material.BOOK, "§dPresets", 
                "§7Choose from a list of", 
                "§7ready-made particle effects.", 
                "", "§eClick to Browse"));

        inventory.setItem(44, makeItem(Material.BARRIER, "§cBack", "§7Return to Particle List"));
    }
}
