package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.menu.visual.ParticleSettingsMenu;
import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleShape;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParticleShapeSelectorMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final List<ParticleShape> shapes;
    private final boolean isPassive;

    public ParticleShapeSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.shapes = Arrays.asList(ParticleShape.values());
        this.maxItemsPerPage = 45;
        this.isPassive = "PASSIVE_VISUALS".equals(playerMenuUtility.getTargetSlot());
    }

    @Override
    public String getMenuName() {
        return "Select Shape";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();

        if (e.getCurrentItem().getType() == Material.BARRIER && e.getSlot() == 53) {
            new ParticleSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() < shapes.size()) {
            ParticleShape selected = shapes.get(e.getSlot());
            updateParticleInConfig("shape", selected.name());
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aParticle shape set to: §e" + selected.getDisplayName()));
            new ParticleSettingsMenu(playerMenuUtility, plugin).open();
        }
    }

    private void updateParticleInConfig(String key, Object value) {
        String itemId = playerMenuUtility.getItemToEditId();
        int particleIndex = playerMenuUtility.getEditIndex();
        String baseVisualsPath = isPassive ? 
                "items." + itemId + ".passive_effects.visuals.particles" : 
                "items." + itemId + ".active_ability.visuals.cast.particles";
        
        List<Map<?, ?>> particlesList = ItemsConfig.get().getMapList(baseVisualsPath);
        if (particleIndex < 0 || particleIndex >= particlesList.size()) return;

        Map<String, Object> particleMap = new LinkedHashMap<>((Map<String, Object>) particlesList.get(particleIndex));
        particleMap.put(key, value);

        List<Map<String, Object>> newParticlesList = new ArrayList<>();
        for(Map<?, ?> m : particlesList) {
            newParticlesList.add(new LinkedHashMap<>((Map<String, Object>) m));
        }
        newParticlesList.set(particleIndex, particleMap);

        ItemsConfig.get().set(baseVisualsPath, newParticlesList);
        ItemsConfig.saveAsync();
        plugin.getItemManager().reloadSingleItem(itemId);
    }

    @Override
    public void setMenuItems() {
        addMenuControls();
        inventory.setItem(53, makeItem(Material.BARRIER, "§cCancel"));

        for (int i = 0; i < shapes.size(); i++) {
            ParticleShape shape = shapes.get(i);
            inventory.setItem(i, makeItem(Material.PAPER, "§e" + shape.name(), "§7" + shape.getDisplayName(), "", "§eClick to select"));
        }
        setFillerGlass();
    }
}