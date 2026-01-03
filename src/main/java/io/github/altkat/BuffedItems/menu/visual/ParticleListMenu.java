package io.github.altkat.BuffedItems.menu.visual;

import io.github.altkat.BuffedItems.BuffedItems;
import io.github.altkat.BuffedItems.manager.config.ConfigManager;
import io.github.altkat.BuffedItems.manager.config.ItemsConfig;
import io.github.altkat.BuffedItems.menu.active.ActiveItemCastVisualsMenu;
import io.github.altkat.BuffedItems.menu.base.PaginatedMenu;
import io.github.altkat.BuffedItems.menu.passive.PassiveItemVisualsMenu;
import io.github.altkat.BuffedItems.menu.utility.PlayerMenuUtility;
import io.github.altkat.BuffedItems.utility.item.BuffedItem;
import io.github.altkat.BuffedItems.utility.item.data.particle.ParticleDisplay;
import io.github.altkat.BuffedItems.utility.item.data.visual.PassiveVisuals;
import io.github.altkat.BuffedItems.utility.item.data.visual.CastVisuals;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParticleListMenu extends PaginatedMenu {

    private final BuffedItems plugin;
    private final boolean isPassive;
    private List<ParticleDisplay> particles;

    public ParticleListMenu(PlayerMenuUtility playerMenuUtility, BuffedItems plugin) {
        super(playerMenuUtility);
        this.plugin = plugin;
        String context = playerMenuUtility.getTargetSlot();
        this.isPassive = "PASSIVE_VISUALS".equals(context);
        loadParticles();
        this.maxItemsPerPage = 45;
    }

    private void loadParticles() {
        String itemId = playerMenuUtility.getItemToEditId();
        BuffedItem item = plugin.getItemManager().getBuffedItem(itemId);
        if (item == null) {
            this.particles = new ArrayList<>();
            return;
        }

        if (isPassive) {
            PassiveVisuals visuals = item.getPassiveVisuals();
            this.particles = visuals != null ? visuals.getParticles() : new ArrayList<>();
        } else {
            CastVisuals visuals = item.getActiveAbility().getVisuals().getCast();
            this.particles = visuals != null ? visuals.getParticles() : new ArrayList<>();
        }
    }

    @Override
    public String getMenuName() {
        return (isPassive ? "Passive" : "Cast") + " Particles (Page " + (page + 1) + ")";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();
        
        if (handlePageChange(e, particles.size())) return;

        if (e.getCurrentItem().getType() == Material.BARRIER && e.getSlot() == 53) {
            if (isPassive) new PassiveItemVisualsMenu(playerMenuUtility, plugin).open();
            else new ActiveItemCastVisualsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getCurrentItem().getType() == Material.ANVIL && e.getSlot() == 49) {
            addNewParticle();
            loadParticles(); 
            playerMenuUtility.setEditIndex(particles.size() - 1);
            new ParticleSettingsMenu(playerMenuUtility, plugin).open();
            return;
        }

        if (e.getSlot() < maxItemsPerPage) {
            int index = maxItemsPerPage * page + e.getSlot();
            if (index >= particles.size()) return;

            if (e.isLeftClick()) {
                playerMenuUtility.setEditIndex(index);
                new ParticleSettingsMenu(playerMenuUtility, plugin).open();
            } else if (e.isRightClick()) {
                removeParticle(index);
                p.sendMessage(ConfigManager.fromSectionWithPrefix("§aParticle removed."));
                loadParticles();
                super.open();
            }
        }
    }

    private void addNewParticle() {
        String itemId = playerMenuUtility.getItemToEditId();
        String path = isPassive ? 
                "items." + itemId + ".passive_effects.visuals.particles" : 
                "items." + itemId + ".active_ability.visuals.cast.particles";
        
        List<Map<?, ?>> currentList = ItemsConfig.get().getMapList(path);
        Map<String, Object> newParticle = new LinkedHashMap<>();
        newParticle.put("type", "FLAME");
        newParticle.put("shape", "POINT");
        newParticle.put("count", 1);
        
        List<Map<String, Object>> newList = new ArrayList<>();
        for(Map<?, ?> m : currentList) {
            newList.add((Map<String, Object>) m);
        }
        newList.add(newParticle);
        
        ItemsConfig.get().set(path, newList);
        ItemsConfig.saveAsync();
        plugin.getItemManager().reloadSingleItem(itemId);
    }

    private void removeParticle(int index) {
        String itemId = playerMenuUtility.getItemToEditId();
        String path = isPassive ? 
                "items." + itemId + ".passive_effects.visuals.particles" : 
                "items." + itemId + ".active_ability.visuals.cast.particles";
        
        List<Map<?, ?>> currentList = ItemsConfig.get().getMapList(path);
        if (index < 0 || index >= currentList.size()) return;

        List<Map<String, Object>> newList = new ArrayList<>();
        for(Map<?, ?> m : currentList) {
            newList.add((Map<String, Object>) m);
        }
        newList.remove(index);
        
        ItemsConfig.get().set(path, newList);
        ItemsConfig.saveAsync();
        plugin.getItemManager().reloadSingleItem(itemId);
    }

    @Override
    public void setMenuItems() {
        addMenuControls();
        inventory.setItem(53, makeItem(Material.BARRIER, "§cBack"));
        inventory.setItem(49, makeItem(Material.ANVIL, "§aAdd Particle", "§7Click to add a new particle layer"));

        for (int i = 0; i < maxItemsPerPage; i++) {
            index = maxItemsPerPage * page + i;
            if (index >= particles.size()) break;

            ParticleDisplay pd = particles.get(index);
            List<String> lore = new ArrayList<>();
            if (isPassive) {
                lore.add("§7Mode: §e" + pd.getTriggerMode().name());
            }
            lore.add("§7Shape: §f" + pd.getShape().getDisplayName());
            lore.add("§7Count: §f" + pd.getCount());
            lore.add("§7Speed: §f" + pd.getSpeed());
            
            if (pd.getDelay() > 0) {
                lore.add("§7Delay: §f" + (pd.getDelay() / 20.0) + "s");
            }

            if (pd.getColor() != null) {
                lore.add("§7Color: §c" + pd.getColor().getRed() + " §a" + pd.getColor().getGreen() + " §9" + pd.getColor().getBlue());
            }

            if (pd.getMaterialData() != null) {
                lore.add("§7Material: §f" + pd.getMaterialData());
            }

            lore.add("");
            lore.add("§aLeft-Click to Edit");
            lore.add("§cRight-Click to Delete");

            inventory.setItem(i, makeItem(Material.ORANGE_DYE, "§e" + pd.getParticle().name(), lore.toArray(new String[0])));
        }
        setFillerGlass();
    }
}