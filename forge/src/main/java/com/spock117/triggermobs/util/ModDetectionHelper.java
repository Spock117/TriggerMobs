package com.spock117.triggermobs.util;

import net.minecraftforge.fml.ModList;

/**
 * Helper class to detect if optional mods are installed.
 */
public class ModDetectionHelper {
    private static final String CGS_MOD_ID = "cgs";
    private static final String GUARD_VILLAGERS_MOD_ID = "guardvillagers";

    /**
     * Checks if Create:Gunsmithing mod is installed.
     * @return true if Create:Gunsmithing is loaded
     */
    public static boolean isCreateGunsmithingLoaded() {
        return ModList.get().isLoaded(CGS_MOD_ID);
    }

    /**
     * Checks if Guard Villagers mod is installed.
     * @return true if Guard Villagers is loaded
     */
    public static boolean isGuardVillagersLoaded() {
        return ModList.get().isLoaded(GUARD_VILLAGERS_MOD_ID);
    }
}

