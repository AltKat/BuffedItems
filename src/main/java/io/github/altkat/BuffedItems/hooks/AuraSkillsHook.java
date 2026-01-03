package io.github.altkat.BuffedItems.hooks;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.stat.Stat;
import dev.aurelium.auraskills.api.stat.Stats;
import dev.aurelium.auraskills.api.user.SkillsUser;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class AuraSkillsHook {

    private final AuraSkillsApi auraSkills;
    private final Map<Attribute, Stat> statMap = new HashMap<>();

    public AuraSkillsHook() {
        this.auraSkills = AuraSkillsApi.get();
        
        // Map Vanilla Attributes to AuraSkills Stats
        statMap.put(Attribute.GENERIC_MAX_HEALTH, Stats.HEALTH);
        statMap.put(Attribute.GENERIC_ATTACK_DAMAGE, Stats.STRENGTH);
        statMap.put(Attribute.GENERIC_MOVEMENT_SPEED, Stats.SPEED);
        statMap.put(Attribute.GENERIC_LUCK, Stats.LUCK);
        statMap.put(Attribute.GENERIC_ARMOR_TOUGHNESS, Stats.TOUGHNESS);
    }

    public boolean checkMana(Player player, double amount) {
        if (auraSkills == null) return false;
        SkillsUser user = auraSkills.getUser(player.getUniqueId());
        if (user == null) return false;
        return user.getMana() >= amount;
    }

    public boolean consumeMana(Player player, double amount) {
        if (auraSkills == null) return false;
        SkillsUser user = auraSkills.getUser(player.getUniqueId());
        if (user == null) return false;
        
        double current = user.getMana();
        if (current >= amount) {
            user.setMana(current - amount);
            return true;
        }
        return false;
    }
    
    public double getMana(Player player) {
        if (auraSkills == null) return 0;
        SkillsUser user = auraSkills.getUser(player.getUniqueId());
        if (user == null) return 0;
        return user.getMana();
    }

    /**
     * Tries to add a stat modifier if the attribute is mapped to an AuraSkill stat.
     * @return true if handled by AuraSkills, false if it should be handled by Vanilla.
     */
    public boolean tryAddStatModifier(Player player, Attribute attribute, String name, double amount) {
        if (auraSkills == null) return false;
        
        Stat stat = statMap.get(attribute);
        if (stat == null) return false; // Not a mapped stat, let vanilla handle it

        SkillsUser user = auraSkills.getUser(player.getUniqueId());
        if (user == null) return false;

        user.addStatModifier(new dev.aurelium.auraskills.api.stat.StatModifier(
                name,
                stat,
                amount
        ));
        return true;
    }

    /**
     * Removes a stat modifier by name.
     */
    public void removeStatModifier(Player player, String name) {
        if (auraSkills == null) return;

        SkillsUser user = auraSkills.getUser(player.getUniqueId());
        if (user == null) return;

        user.removeStatModifier(name);
    }
}
