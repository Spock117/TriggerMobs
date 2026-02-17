package com.spock117.triggermobs.util;

import net.minecraftforge.fml.ModList;

/**
 * Helper class to detect if optional mods are installed.
 */
public class ModDetectionHelper {
    private static final String CGS_MOD_ID = "cgs";
    private static final String GUARD_VILLAGERS_MOD_ID = "guardvillagers";
    private static final String TCONSTRUCT_EMERGENCE_MOD_ID = "tconstruct_emergence";

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

    /**
     * Checks if TConstruct-Emergence mod is installed.
     * Used to avoid conflicts when both mods assign weapons on mob spawn.
     * @return true if TConstruct-Emergence is loaded
     */
    public static boolean isTConstructEmergenceLoaded() {
        return ModList.get().isLoaded(TCONSTRUCT_EMERGENCE_MOD_ID);
    }
}

