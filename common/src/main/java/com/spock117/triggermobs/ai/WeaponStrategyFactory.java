package com.spock117.triggermobs.ai;

import com.spock117.triggermobs.ai.strategies.*;
import com.spock117.triggermobs.util.WeaponTypeDetector;
import net.minecraft.world.item.ItemStack;

/**
 * Factory class to create appropriate weapon AI strategies based on weapon type.
 */
public class WeaponStrategyFactory {
    
    /**
     * Creates an appropriate weapon strategy for the given weapon.
     * @param weapon The weapon item stack
     * @return The weapon strategy, or GenericWeaponStrategy if weapon is not recognized
     */
    public static WeaponAIStrategy createStrategy(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) {
            return new GenericWeaponStrategy();
        }
        
        // Check if it's a Create:Gunsmithing weapon by registry name
        // If CGS mod isn't loaded, the items won't exist, so this check will naturally fail
        if (!WeaponTypeDetector.isCGSWeapon(weapon)) {
            return new GenericWeaponStrategy();
        }
        
        // Get the specific weapon type
        WeaponTypeDetector.CGSWeaponType weaponType = WeaponTypeDetector.getCGSWeaponType(weapon);
        
        // Create appropriate strategy based on weapon type
        return switch (weaponType) {
            case FLINTLOCK -> new FlintlockStrategy();
            case REVOLVER -> new RevolverStrategy();
            case SHOTGUN -> new ShotgunStrategy();
            case NAILGUN -> new NailgunStrategy();
            case GATLING -> new GatlingStrategy();
            case BLAZEGUN -> new BlazegunStrategy();
            case LAUNCHER -> new LauncherStrategy();
            case HAMMER -> new HammerStrategy();
            case GRENADE -> new GrenadeStrategy();
            case UNKNOWN -> new GenericWeaponStrategy();
        };
    }
}

