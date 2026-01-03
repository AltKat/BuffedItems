package io.github.altkat.BuffedItems.menu.visual;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.base.Menu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleDisplay;
import io.github.altkat.BuffedItems.utility.item.data.visual.ParticlePreset;
import io.github.altkat.BuffedItems.utility.item.data.visual.VisualTriggerMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParticlePresetsMenu extends Menu {

    private final BuffedItems plugin;
    private final boolean isPassive;

    public ParticlePresetsMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.isPassive = "PASSIVE_VISUALS".equals(playerMenuUtility.getTargetSlot());
    }

    @Override
    public String getMenuName() {
        return "Select a Preset";
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();

        if (e.getSlot() == 22) {
            new ParticleSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() < ParticlePreset.values().length) {
            ParticlePreset preset = ParticlePreset.values()[e.getSlot()];
            applyPreset(p, preset);
        }
    }

    private void applyPreset(Player p, ParticlePreset preset) {
        String itemId = playerMenuUtility.getItemToEditId();
        int index = playerMenuUtility.getEditIndex();
        String path = isPassive ? 
                "items." + itemId + ".passive_effects.visuals.particles" : 
                "items." + itemId + ".active_ability.visuals.cast.particles";

        List<Map<?, ?>> list = ItemsConfig.get().getMapList(path);
        
        if (index >= 0 && index < list.size()) {
            // Get existing mode to preserve it if we want, OR default to CONTINUOUS
            // Since presets are shapes, maybe we should respect the user's current mode toggle?
            // Let's grab the current mode from the existing config
            Map<?, ?> existing = list.get(index);
            String currentMode = existing.containsKey("mode") ? existing.get("mode").toString() : "CONTINUOUS";

            Map<String, Object> newMap = serializeDisplay(preset.getDisplay());
            // Restore the mode (Presets define shape/particle, not necessarily the trigger logic)
            newMap.put("mode", currentMode);

            // Update list
            // Create a mutable copy of the list
            List<Map<?, ?>> mutableList = new java.util.ArrayList<>(list);
            mutableList.set(index, newMap);

            ItemsConfig.get().set(path, mutableList);
            ItemsConfig.saveAsync();
            plugin.getItemManager().reloadSingleItem(itemId);
            
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aApplied preset: §e" + preset.getName()));
            new ParticleSettingsMenu(playerMenuUtility, plugin).open();
        } else {
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§cError: Could not find particle index."));
            new ParticleListMenu(playerMenuUtility, plugin).open();
        }
    }

    private Map<String, Object> serializeDisplay(ParticleDisplay display) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", display.getParticle().name());
        map.put("shape", display.getShape().name());
        map.put("count", display.getCount());
        map.put("speed", display.getSpeed());
        
        if (display.getRadius() != 1.0) map.put("radius", display.getRadius());
        if (display.getHeight() != 1.0) map.put("height", display.getHeight());
        if (display.getPeriod() != 20.0) map.put("period", display.getPeriod());
        if (display.getDelay() > 0) map.put("delay", display.getDelay());
        if (display.getDuration() > 0) map.put("duration", display.getDuration());
        
        map.put("offset_x", display.getOffset().getX());
        map.put("offset_y", display.getOffset().getY());
        map.put("offset_z", display.getOffset().getZ());

        if (display.getColor() != null) {
            org.bukkit.Color c = display.getColor();
            map.put("color", String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue()));
        }
        
        if (display.getMaterialData() != null) {
            map.put("material_data", display.getMaterialData());
        }

        return map;
    }

    @Override
    public void setMenuItems() {
        setFillerGlass();
        inventory.setItem(22, makeItem(Material.BARRIER, "§cCancel"));

        int slot = 0;
        for (ParticlePreset preset : ParticlePreset.values()) {
            inventory.setItem(slot++, makeItem(preset.getIcon(), "§e" + preset.getName(), preset.getDescription()));
        }
    }
}
