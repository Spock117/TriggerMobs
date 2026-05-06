package com.spock117.triggermobs.integration;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.Item;

import java.lang.reflect.Method;

/**
 * Optional integration with TConstruct-Emergence: assigns TC-E ranged weapons (longbow) to mobs
 * via reflection so TC-E is not a compile dependency. Used so skeleton-like mobs can get
 * weaponChance CGS and (1 - weaponChance) TC-E longbow, giving them 100% weapon rate without
 * raising TC-E's global weaponChance.
 */
public final class TCEmergenceIntegration {

    private static Method tryAssignRangedWeapon;
    private static Item longbowItem;
    private static boolean initialized;
    private static boolean available;

    /**
     * Tries to assign a TConstruct-Emergence longbow to the mob. Uses reflection to call
     * MaterialPicker.tryAssignRangedWeapon(mob, TinkerTools.longbow.get(), bonusTier).
     * Bonus tier is 0 (no nearby-player advancement bonus).
     *
     * @param mob the mob to equip (e.g. skeleton, stray, wither_skeleton)
     * @return true if TC-E was loaded and the call succeeded (mob was equipped)
     */
    public static boolean tryAssignLongbow(PathfinderMob mob) {
        if (!ensureInitialized()) {
            return false;
        }
        if (tryAssignRangedWeapon == null || longbowItem == null) {
            return false;
        }
        try {
            tryAssignRangedWeapon.invoke(null, mob, longbowItem, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAvailable() {
        ensureInitialized();
        return available;
    }

    private static boolean ensureInitialized() {
        if (initialized) {
            return available;
        }
        initialized = true;
        try {
            Class<?> tinkerTools = Class.forName("slimeknights.tconstruct.tools.TinkerTools");
            Object longbowHolder = tinkerTools.getField("longbow").get(null);
            Method getMethod = longbowHolder.getClass().getMethod("get");
            longbowItem = (Item) getMethod.invoke(longbowHolder);
            Class<?> materialPicker = Class.forName("dev.xkmc.tconstruct_emergence.content.materials.MaterialPicker");
            tryAssignRangedWeapon = materialPicker.getMethod("tryAssignRangedWeapon", PathfinderMob.class, Item.class, int.class);
            available = longbowItem != null && tryAssignRangedWeapon != null;
        } catch (Throwable t) {
            available = false;
        }
        return available;
    }

    private TCEmergenceIntegration() {}
}
