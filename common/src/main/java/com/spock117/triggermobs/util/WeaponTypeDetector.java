package com.spock117.triggermobs.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Utility class to detect weapon types, specifically Create:Gunsmithing weapons.
 */
public class WeaponTypeDetector {
    private static final String CGS_MOD_ID = "cgs";
    
    /**
     * Enum representing different Create:Gunsmithing weapon types.
     */
    public enum CGSWeaponType {
        FLINTLOCK,
        REVOLVER,
        SHOTGUN,
        NAILGUN,
        GATLING,
        BLAZEGUN,
        LAUNCHER,
        HAMMER,
        GRENADE,
        UNKNOWN
    }
    
    /**
     * Gets the registry name of an item using reflection to avoid Forge dependency in common module.
     * @param item The item to get the registry name for
     * @return The ResourceLocation registry name, or null if not available
     */
    private static ResourceLocation getRegistryName(Item item) {
        if (item == null) {
            return null;
        }
        
        // Try to use ForgeRegistries via reflection (available at runtime on Forge)
        try {
            Class<?> forgeRegistries = Class.forName("net.minecraftforge.registries.ForgeRegistries");
            Object itemsRegistry = forgeRegistries.getField("ITEMS").get(null);
            java.lang.reflect.Method getKey = itemsRegistry.getClass().getMethod("getKey", Object.class);
            return (ResourceLocation) getKey.invoke(itemsRegistry, item);
        } catch (Exception e) {
            // ForgeRegistries not available, try alternative approach
            // Check item description ID which contains namespace
            String descriptionId = item.getDescriptionId();
            if (descriptionId != null && descriptionId.contains(".")) {
                // Description ID format: "item.cgs.weapon_name" or similar
                String[] parts = descriptionId.split("\\.");
                if (parts.length >= 2 && parts[1].equals(CGS_MOD_ID)) {
                    // Extract the item name from description ID
                    String itemName = parts.length > 2 ? parts[parts.length - 1] : "";
                    return ResourceLocation.tryParse(CGS_MOD_ID + ":" + itemName);
                }
            }
            return null;
        }
    }
    
    /**
     * Checks if the given item is from Create:Gunsmithing mod.
     * @param item The item to check
     * @return true if the item is from Create:Gunsmithing
     */
    public static boolean isCGSWeapon(Item item) {
        if (item == null) {
            return false;
        }
        
        ResourceLocation registryName = getRegistryName(item);
        if (registryName == null) {
            return false;
        }
        
        return registryName.getNamespace().equals(CGS_MOD_ID);
    }
    
    /**
     * Checks if the given item stack is from Create:Gunsmithing mod.
     * @param stack The item stack to check
     * @return true if the item is from Create:Gunsmithing
     */
    public static boolean isCGSWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        
        return isCGSWeapon(stack.getItem());
    }
    
    /**
     * Identifies the specific Create:Gunsmithing weapon type.
     * @param item The item to identify
     * @return The weapon type, or UNKNOWN if not a CGS weapon or not recognized
     */
    public static CGSWeaponType getCGSWeaponType(Item item) {
        if (!isCGSWeapon(item)) {
            return CGSWeaponType.UNKNOWN;
        }
        
        ResourceLocation registryName = getRegistryName(item);
        if (registryName == null) {
            return CGSWeaponType.UNKNOWN;
        }
        
        String path = registryName.getPath();
        
        // Check for specific weapon types by registry name
        if (path.equals("flintlock")) {
            return CGSWeaponType.FLINTLOCK;
        } else if (path.equals("revolver")) {
            return CGSWeaponType.REVOLVER;
        } else if (path.equals("shotgun")) {
            return CGSWeaponType.SHOTGUN;
        } else if (path.equals("nailgun")) {
            return CGSWeaponType.NAILGUN;
        } else if (path.equals("gatling")) {
            return CGSWeaponType.GATLING;
        } else if (path.equals("blazegun")) {
            return CGSWeaponType.BLAZEGUN;
        } else if (path.equals("launcher")) {
            return CGSWeaponType.LAUNCHER;
        } else if (path.equals("hammer")) {
            return CGSWeaponType.HAMMER;
        } else if (path.equals("frag_grenade")) {
            return CGSWeaponType.GRENADE;
        }
        
        return CGSWeaponType.UNKNOWN;
    }
    
    /**
     * Identifies the specific Create:Gunsmithing weapon type from an item stack.
     * @param stack The item stack to identify
     * @return The weapon type, or UNKNOWN if not a CGS weapon or not recognized
     */
    public static CGSWeaponType getCGSWeaponType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return CGSWeaponType.UNKNOWN;
        }
        
        return getCGSWeaponType(stack.getItem());
    }
}

