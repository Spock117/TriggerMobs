package com.spock117.triggermobs.util;

import net.minecraftforge.fml.ModList;

/**
 * Helper class to detect if optional mods are installed.
 */
public class ModDetectionHelper {
    private static final String CGS_MOD_ID = "cgs";
    
    /**
     * Checks if Create:Gunsmithing mod is installed.
     * @return true if Create:Gunsmithing is loaded
     */
    public static boolean isCreateGunsmithingLoaded() {
        return ModList.get().isLoaded(CGS_MOD_ID);
    }
}

