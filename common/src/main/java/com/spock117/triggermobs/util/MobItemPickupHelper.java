package com.spock117.triggermobs.util;

import com.nukateam.ntgl.common.foundation.item.interfaces.IWeapon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Set;

public class MobItemPickupHelper {
    
    // Set of mob registry names that should have custom pickup behavior
    private static final Set<String> MOBS_WITH_CUSTOM_PICKUP = Set.of(
        "minecraft:zombie",
        "minecraft:zombie_villager",
        "minecraft:husk",
        "minecraft:drowned",
        "minecraft:skeleton",
        "minecraft:stray",
        "minecraft:wither_skeleton",
        "minecraft:pillager",
        "minecraft:vindicator",
        "minecraft:evoker",
        "minecraft:witch",
        "minecraft:villager",
        "minecraft:piglin",
        "minecraft:piglin_brute",
        "minecraft:zombified_piglin"
    );
    
    /**
     * Checks if the mob should have custom item pickup behavior.
     */
    public static boolean shouldHandlePickup(Mob mob) {
        ResourceLocation entityTypeKey = mob.getType().builtInRegistryHolder().key().location();
        return entityTypeKey != null && MOBS_WITH_CUSTOM_PICKUP.contains(entityTypeKey.toString());
    }
    
    /**
     * Checks if an item is a weapon (NTGL, vanilla, or from other mods).
     * This includes:
     * - NTGL weapons (IWeapon interface)
     * - Vanilla weapons (swords, axes, tridents, bows, crossbows)
     * - Other mod weapons (items with significant attack damage)
     */
    public static boolean isWeapon(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        
        Item item = itemStack.getItem();
        
        // Check for NTGL weapons
        if (item instanceof IWeapon) {
            return true;
        }
        
        // FIRST: Exclude tools that are clearly not weapons
        // This must come before checking for TieredItem/DiggerItem
        if (item instanceof PickaxeItem || 
            item instanceof ShovelItem || 
            item instanceof HoeItem) {
            return false; // These are tools, not weapons
        }
        
        // Check for vanilla weapons
        if (item instanceof SwordItem || 
            item instanceof AxeItem || 
            item instanceof TridentItem || 
            item instanceof BowItem || 
            item instanceof CrossbowItem) {
            return true;
        }
        
        // Check for other mod weapons that extend TieredItem or DiggerItem
        // We've already excluded pickaxes, shovels, and hoes above
        // DiggerItem includes axes and swords (which we already checked), but also catches modded weapons
        if (item instanceof TieredItem || item instanceof DiggerItem) {
            // At this point, we've excluded tools, so if it's a TieredItem or DiggerItem,
            // it's likely a modded weapon (swords and axes are already checked above)
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if an item is a tool (pickaxe, shovel, hoe).
     */
    public static boolean isTool(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return false;
        }
        
        Item item = itemStack.getItem();
        return item instanceof PickaxeItem || 
               item instanceof ShovelItem || 
               item instanceof HoeItem;
    }
    
    /**
     * Checks if an item is a weapon or tool (allowed items for mobs to hold).
     */
    public static boolean isWeaponOrTool(ItemStack itemStack) {
        return isWeapon(itemStack) || isTool(itemStack);
    }
    
    /**
     * Checks if a mob currently has a weapon equipped (main hand or offhand).
     */
    public static boolean hasWeaponEquipped(Mob mob) {
        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand = mob.getOffhandItem();
        
        return isWeapon(mainHand) || isWeapon(offHand);
    }
    
    /**
     * Determines if new armor should replace current armor.
     * Simple comparison: prefer armor with higher durability or enchantments.
     */
    public static boolean shouldReplaceArmor(ItemStack current, ItemStack newArmor) {
        // If current armor is damaged and new is not, replace
        if (current.isDamaged() && !newArmor.isDamaged()) {
            return true;
        }
        
        // If new armor has more enchantments, prefer it
        int currentEnchants = current.getEnchantmentTags().size();
        int newEnchants = newArmor.getEnchantmentTags().size();
        if (newEnchants > currentEnchants) {
            return true;
        }
        
        // If current armor is more damaged, replace
        if (current.isDamaged() && newArmor.isDamaged()) {
            float currentDurability = (float) (current.getMaxDamage() - current.getDamageValue()) / current.getMaxDamage();
            float newDurability = (float) (newArmor.getMaxDamage() - newArmor.getDamageValue()) / newArmor.getMaxDamage();
            return newDurability > currentDurability;
        }
        
        // Default: don't replace if current is in good condition
        return false;
    }
}

