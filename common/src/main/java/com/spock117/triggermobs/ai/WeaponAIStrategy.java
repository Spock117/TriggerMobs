package com.spock117.triggermobs.ai;

import com.nukateam.ntgl.common.data.WeaponData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for weapon-specific AI strategies.
 * Each weapon type can implement this interface to provide customized
 * movement and shooting behaviors.
 */
public interface WeaponAIStrategy {
    /**
     * Handles movement behavior for the mob based on weapon characteristics.
     * @param mob The mob using the weapon
     * @param target The target entity
     * @param distance The distance to the target
     * @param distanceSqr The squared distance to the target
     * @param hasAmmo Whether the weapon has ammo
     * @param canReload Whether the mob can reload
     * @param hasLineOfSight Whether the mob has line of sight to target
     * @param seeTime How long the mob has seen the target
     */
    void move(PathfinderMob mob, LivingEntity target, double distance, double distanceSqr, 
              boolean hasAmmo, boolean canReload, boolean hasLineOfSight, int seeTime);
    
    /**
     * Handles shooting behavior for the weapon.
     * @param mob The mob using the weapon
     * @param target The target entity
     * @param hand The hand holding the weapon
     * @param weapon The weapon item stack
     */
    void shoot(PathfinderMob mob, LivingEntity target, InteractionHand hand, ItemStack weapon);
    
    /**
     * Calculates the attack delay (cooldown) between shots based on weapon properties.
     * @param mob The mob using the weapon
     * @param weaponData The weapon data
     * @return The attack delay in ticks
     */
    int getAttackDelay(PathfinderMob mob, WeaponData weaponData);
    
    /**
     * Gets the ideal engagement distance for this weapon type.
     * @return The ideal distance in blocks
     */
    float getIdealDistance();
    
    /**
     * Gets the minimum engagement distance for this weapon type.
     * @return The minimum distance in blocks
     */
    float getMinDistance();
    
    /**
     * Gets the maximum engagement distance for this weapon type.
     * @return The maximum distance in blocks
     */
    float getMaxDistance();
    
    /**
     * Whether this weapon type can be dual wielded.
     * @return true if the weapon can be dual wielded
     */
    boolean canDualWield();
    
    /**
     * Whether this weapon should maintain distance or close in.
     * @return true if the weapon should maintain distance, false if it should close in
     */
    boolean shouldMaintainDistance();
}

