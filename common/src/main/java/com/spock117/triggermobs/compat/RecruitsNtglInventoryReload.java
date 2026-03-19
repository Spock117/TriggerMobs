package com.spock117.triggermobs.compat;

import com.nukateam.ntgl.common.data.WeaponData;
import com.nukateam.ntgl.common.data.holders.WeaponMode;
import com.nukateam.ntgl.common.foundation.item.interfaces.IWeapon;
import com.nukateam.ntgl.common.util.util.InventoryUtil;
import com.nukateam.ntgl.common.util.util.WeaponModifierHelper;
import com.nukateam.ntgl.common.util.util.WeaponStateHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;

/**
 * NTGL's {@code EntityReloadTracker} refills mob magazines for free. {@code ReloadTracker} uses
 * {@code setMaxAmmo} for non-players. For recruits we instead pull ammo from their
 * {@code getInventory()} container (same idea as player reload, but using
 * {@link InventoryUtil#findAmmo(Container, com.nukateam.ntgl.common.data.holders.AmmoHolder)}).
 */
public final class RecruitsNtglInventoryReload {

    private RecruitsNtglInventoryReload() {
    }

    /**
     * Moves ammo from the recruit's inventory into the gun's internal count, mirroring
     * {@code ReloadTracker#addAmmo(WeaponData, int)} but using a {@link Container}.
     */
    public static void tryReloadFromRecruitInventory(PathfinderMob mob, ItemStack weapon) {
        if (weapon.isEmpty() || !(weapon.getItem() instanceof IWeapon)) {
            return;
        }
        if (WeaponStateHelper.isAmmoIgnored(weapon)) {
            return;
        }

        Container container;
        try {
            Object inv = mob.getClass().getMethod("getInventory").invoke(mob);
            if (!(inv instanceof Container c)) {
                return;
            }
            container = c;
        } catch (Throwable t) {
            return;
        }

        var data = new WeaponData(weapon, mob).setWeaponMode(WeaponMode.PRIMARY);
        int maxAmmo = WeaponModifierHelper.getMaxAmmo(data);

        int guard = 0;
        while (WeaponStateHelper.getAmmoCount(data) < maxAmmo && guard++ < 256) {
            var ammoHandler = WeaponStateHelper.getCurrentAmmo(data);
            var context = InventoryUtil.findAmmo(container, ammoHandler);
            var ammoStack = context.stack();
            if (ammoStack.isEmpty()) {
                break;
            }

            var tag = weapon.getTag();
            if (tag == null) {
                break;
            }

            int value = ammoHandler.getValue(ammoStack);
            int currentAmount = WeaponStateHelper.getAmmoCount(data);
            int amount = Math.min(ammoStack.getCount() * value, maxAmmo - currentAmount);
            int ammoCount = WeaponStateHelper.getAmmoCount(data);
            amount = Math.min(amount, maxAmmo - ammoCount);
            if (amount <= 0) {
                break;
            }

            WeaponStateHelper.addAmmo(data, amount);
            context.shrink(amount, ammoHandler, mob);
            data = new WeaponData(weapon, mob).setWeaponMode(WeaponMode.PRIMARY);
        }
    }
}
