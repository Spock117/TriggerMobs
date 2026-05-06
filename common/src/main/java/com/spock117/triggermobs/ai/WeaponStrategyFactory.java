package com.spock117.triggermobs.ai;

import com.nukateam.ntgl.common.util.util.WeaponStateHelper;
import com.spock117.triggermobs.ai.strategies.*;
import com.spock117.triggermobs.util.WeaponTypeDetector;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;

/**
 * Factory class to create appropriate weapon AI strategies based on weapon type.
 * Wraps base strategies in AttachmentAwareStrategy when the weapon has attachments.
 */
public class WeaponStrategyFactory {
    
    /**
     * Creates an appropriate weapon strategy for the given weapon.
     * @param weapon The weapon item stack
     * @return The weapon strategy, or GenericWeaponStrategy if weapon is not recognized
     */
    public static WeaponAIStrategy createStrategy(ItemStack weapon, PathfinderMob shooter) {
        if (weapon == null || weapon.isEmpty()) {
            return new GenericWeaponStrategy();
        }
        
        WeaponAIStrategy base = createBaseStrategy(weapon);
        if (hasAttachments(weapon, shooter)) {
            return new AttachmentAwareStrategy(base, weapon);
        }
        return base;
    }

    private static WeaponAIStrategy createBaseStrategy(ItemStack weapon) {
        if (!WeaponTypeDetector.isCGSWeapon(weapon)) {
            return new GenericWeaponStrategy();
        }
        
        WeaponTypeDetector.CGSWeaponType weaponType = WeaponTypeDetector.getCGSWeaponType(weapon);
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

    private static boolean hasAttachments(ItemStack weapon, PathfinderMob shooter) {
        if (shooter == null || shooter.level() == null) {
            return false;
        }
        var items = WeaponStateHelper.getAttachmentItems(weapon, shooter.level().registryAccess());
        return items != null && !items.isEmpty() && items.stream().anyMatch(s -> !s.isEmpty());
    }
}

