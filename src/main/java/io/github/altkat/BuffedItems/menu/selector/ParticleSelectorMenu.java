package io.github.altkat.BuffedItems.menu.selector;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.menu.visual.ParticleListMenu;
import io.github.altkat.BuffedItems.menu.visual.ParticleSettingsMenu;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.*;
import java.util.stream.Collectors;

public class ParticleSelectorMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final List<Particle> particles;
    private final boolean isPassive;

    public ParticleSelectorMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        this.particles = Arrays.stream(Particle.values())
                .filter(p -> !p.name().startsWith("LEGACY"))
                .sorted(Comparator.comparing(Enum::name))
                .collect(Collectors.toList());
        this.maxItemsPerPage = 45;
        this.isPassive = "PASSIVE_VISUALS".equals(playerMenuUtility.getTargetSlot());
    }

    @Override
    public String getMenuName() {
        return "Select Particle (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();
        
        if (handlePageChange(e, particles.size())) {
            return;
        }

        if (e.getCurrentItem().getType() == Material.BARRIER && e.getSlot() == 53) {
            new ParticleSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }
        
        if (e.getCurrentItem().getType() == Material.ANVIL && e.getSlot() == 49) {
            playerMenuUtility.setWaitingForChatInput(true);
            playerMenuUtility.setChatInputPath("particle.manual_type_input");
            p.closeInventory();
            p.sendMessage(ConfigManager.fromSectionWithPrefix("§aEnter Particle name in chat (e.g., 'FLAME')."));
            p.sendMessage(ConfigManager.fromSection("§7(Type 'cancel' to exit)"));
            return;
        }

        if (e.getSlot() < this.maxItemsPerPage) {
             int index = maxItemsPerPage * page + e.getSlot();
             if (index >= particles.size()) return;
             
             Particle selected = particles.get(index);
             updateParticleInConfig("type", selected.name());
             p.sendMessage(ConfigManager.fromSectionWithPrefix("§aParticle type set to: §e" + selected.name()));
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
        inventory.setItem(49, makeItem(Material.ANVIL, "§bEnter Manually", "§7Click to type the particle name in chat."));


        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= particles.size()) break;

            Particle particle = particles.get(index);
            inventory.setItem(i, makeItem(Material.FIREWORK_STAR, "§e" + particle.name(), "§7Click to select"));
        }
        setFillerGlass();
    }
}
