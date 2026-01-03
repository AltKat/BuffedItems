package io.github.altkat.BuffedItems.listener.handler;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.menu.visual.ParticleSettingsMenu;
import io.github.altkat.BuffedItems.menu.selector.ParticleSelectorMenu;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParticleInputHandler implements ChatInputHandler {

    private final BuffedItems plugin;

    public ParticleInputHandler(BuffedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean shouldHandle(String path) {
        return path.startsWith("particle.");
    }

    @Override
    public void onCancel(Player player, PlayerMenuUtility pmu, String path) {
        if (path.equals("particle.manual_type_input")) {
            new ParticleSelectorMenu(pmu, plugin).open();
        } else {
            new ParticleSettingsMenu(pmu, plugin).open();
        }
    }

    @Override
    public void handle(Player player, PlayerMenuUtility pmu, String input, String path, String itemId) {
        String baseVisualsPath = pmu.getTargetSlot().equals("PASSIVE_VISUALS") ?
                "items." + itemId + ".passive_effects.visuals.particles" :
                "items." + itemId + ".active_ability.visuals.cast.particles";
        int particleIndex = pmu.getEditIndex();

        if (particleIndex == -1) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: No particle selected for editing."));
            new ParticleSettingsMenu(pmu, plugin).open();
            return;
        }
        
        List<Map<?, ?>> particlesList = ItemsConfig.get().getMapList(baseVisualsPath);
        if (particleIndex < 0 || particleIndex >= particlesList.size()) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Particle index out of bounds."));
            new ParticleSettingsMenu(pmu, plugin).open();
            return;
        }

        Map<String, Object> particleMap = new LinkedHashMap<>((Map<String, Object>) particlesList.get(particleIndex));

        try {
            switch (path) {
                case "particle.count":
                    int count = Integer.parseInt(input);
                    if (count < 0) throw new NumberFormatException();
                    particleMap.put("count", count);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aCount set to: " + count));
                    break;
                case "particle.speed":
                    double speed = Double.parseDouble(input);
                    particleMap.put("speed", speed);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aSpeed set to: " + speed));
                    break;
                case "particle.duration":
                    int duration = Integer.parseInt(input);
                    if (duration < 0) throw new NumberFormatException();
                    particleMap.put("duration", duration);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aDuration set to: " + duration + " ticks"));
                    break;
                case "particle.radius":
                    double radius = Double.parseDouble(input);
                    particleMap.put("radius", radius);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aRadius set to: " + radius));
                    break;
                case "particle.height":
                    double height = Double.parseDouble(input);
                    particleMap.put("height", height);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aHeight set to: " + height));
                    break;
                case "particle.period":
                    double period = Double.parseDouble(input);
                    particleMap.put("period", period);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aPeriod set to: " + period));
                    break;
                case "particle.delay":
                    int delay = Integer.parseInt(input);
                    if (delay < 0) throw new NumberFormatException();
                    particleMap.put("delay", delay);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aDelay set to: " + delay + " ticks"));
                    break;
                case "particle.offset_x":
                    double ox = Double.parseDouble(input);
                    particleMap.put("offset_x", ox);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aOffset X set to: " + ox));
                    break;
                case "particle.offset_y":
                    double oy = Double.parseDouble(input);
                    particleMap.put("offset_y", oy);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aOffset Y set to: " + oy));
                    break;
                case "particle.offset_z":
                    double oz = Double.parseDouble(input);
                    particleMap.put("offset_z", oz);
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aOffset Z set to: " + oz));
                    break;
                case "particle.color":
                    Color color = parseColor(input);
                    if (color == null) throw new IllegalArgumentException("Invalid color format");
                    particleMap.put("color", String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()));
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aColor updated!"));
                    break;
                case "particle.material":
                    particleMap.put("material_data", input.toUpperCase());
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aMaterial data set to: " + input.toUpperCase()));
                    break;
                case "particle.manual_type_input":
                    Particle particle = Particle.valueOf(input.toUpperCase()); // Will throw IAE if invalid
                    particleMap.put("type", particle.name());
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§aParticle type set to: " + particle.name()));
                    break;
                default:
                    player.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Unknown particle input path."));
                    pmu.setWaitingForChatInput(true);
                    return;
            }
            
            List<Map<String, Object>> newParticlesList = new ArrayList<>();
            for(Map<?, ?> m : particlesList) {
                newParticlesList.add(new LinkedHashMap<>((Map<String, Object>) m));
            }
            newParticlesList.set(particleIndex, particleMap);

            ItemsConfig.get().set(baseVisualsPath, newParticlesList);
            ItemsConfig.saveAsync();
            plugin.getItemManager().reloadSingleItem(itemId);

        } catch (NumberFormatException e) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid number format."));
            pmu.setWaitingForChatInput(true);
            return;
        } catch (IllegalArgumentException e) {
            player.sendMessage(ConfigManager.fromSectionWithPrefix("§cInvalid input: " + e.getMessage()));
            pmu.setWaitingForChatInput(true);
            return;
        }

        pmu.setWaitingForChatInput(false);
        new ParticleSettingsMenu(pmu, plugin).open();
    }
    
    private Color parseColor(String input) {
        try {
            if (input.contains(";")) {
                String[] parts = input.split(";");
                return Color.fromRGB(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2])
                );
            } else if (input.startsWith("#") || input.length() == 6) {
                String hex = input.startsWith("#") ? input.substring(1) : input;
                return Color.fromRGB(
                        Integer.parseInt(hex.substring(0, 2), 16),
                        Integer.parseInt(hex.substring(2, 4), 16),
                        Integer.parseInt(hex.substring(4, 6), 16)
                );
            }
        } catch (Exception ignored) {}
        return null;
    }
}
