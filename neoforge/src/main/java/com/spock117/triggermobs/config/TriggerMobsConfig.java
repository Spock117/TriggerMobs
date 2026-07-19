package com.spock117.triggermobs.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class TriggerMobsConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;
    
    static {
        final Pair<Common, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON_SPEC = specPair.getRight();
        COMMON = specPair.getLeft();
    }
    
    public static class Common {
        public final ModConfigSpec.IntValue baseAttackIntervalTicks;
        public final ModConfigSpec.IntValue attackIntervalVariance;

        // CGS gun spawning
        public final ModConfigSpec.BooleanValue cgsSpawningEnabled;
        public final ModConfigSpec.DoubleValue weaponChance;
        public final ModConfigSpec.DoubleValue dualWieldChance;
        public final ModConfigSpec.DoubleValue dropChanceOverride;
        public final ModConfigSpec.IntValue maxAttachmentSlots;
        public final ModConfigSpec.BooleanValue allowAdvancedWeapons;
        public final ModConfigSpec.ConfigValue<List<? extends String>> mobWeaponOverrides;
        public final ModConfigSpec.BooleanValue guardCgsSpawningEnabled;
        public final ModConfigSpec.DoubleValue guardWeaponChance;
        public final ModConfigSpec.BooleanValue vanillaWeaponsRemoved;

        // AI nerf options
        public final ModConfigSpec.DoubleValue aiAccuracyNerf;
        public final ModConfigSpec.DoubleValue guardAccuracyNerf;
        public final ModConfigSpec.IntValue aiReactionDelayTicks;
        public final ModConfigSpec.DoubleValue fireRateDelayMultiplier;
        public final ModConfigSpec.IntValue outOfAmmoFallbackTicks;

        // Loot Integrations loot toggles
        public final ModConfigSpec.BooleanValue includeLootGuns;
        public final ModConfigSpec.BooleanValue includeLootAttachments;
        public final ModConfigSpec.BooleanValue includeLootAmmo;

        // Debug (leave off for normal play; spammy in logs)
        public final ModConfigSpec.BooleanValue debugGunAi;
        public final ModConfigSpec.BooleanValue debugGunSpawn;
        
        public Common(ModConfigSpec.Builder builder) {
            builder.comment("TriggerMobs mob attack configuration").push("mob_attack");
            
            this.baseAttackIntervalTicks = builder
                .comment("Base attack interval in ticks (20 ticks = 1 second). Default: 200 ticks (10 seconds). For dual wielding, this is halved.")
                .defineInRange("baseAttackIntervalTicks", 200, 20, 1000);
            
            this.attackIntervalVariance = builder
                .comment("Random variance in ticks added to attack interval (±this value). Default: 80 ticks (±4 seconds)")
                .defineInRange("attackIntervalVariance", 80, 0, 200);
            
            builder.pop();
            
            builder.comment("AI nerf options (accuracy and reaction)").push("ai_nerf");
            
            this.aiAccuracyNerf = builder
                .comment("Multiplier for mob shooting inaccuracy (degrees of spread). 1.0 = baseline (±4-8 degrees). Higher = more inaccurate (zombies are dumb). 0.5 = more accurate.")
                .defineInRange("aiAccuracyNerf", 1.0, 0.1, 5.0);

            this.guardAccuracyNerf = builder
                .comment("Same as aiAccuracyNerf but for guard villagers. Guards are typically more skilled; use lower values (e.g. 0.6) for better aim.")
                .defineInRange("guardAccuracyNerf", 0.7, 0.1, 5.0);

            this.aiReactionDelayTicks = builder
                .comment("Extra ticks before mob starts shooting (slower reaction).")
                .defineInRange("aiReactionDelayTicks", 0, 0, 200);

            this.fireRateDelayMultiplier = builder
                .comment("Multiplier applied to mob fire rate (attack delay). Values > 1.0 slow down firing. Default 4.0 = 4x longer delay between shots. Applies to all mobs and weapon types.")
                .defineInRange("fireRateDelayMultiplier", 4.0, 1.0, 20.0);

            builder.pop();

            builder.comment("CGS gun spawning (Create:Gunsmithing)").push("cgs_spawning");

            this.cgsSpawningEnabled = builder
                .comment("Master toggle for CGS weapon spawning on mobs.")
                .define("cgsSpawningEnabled", true);

            this.weaponChance = builder
                .comment("Probability that a valid mob spawns with a CGS gun. Range: 0.0 to 1.0. Default 0.2 matches InControl spawn.json.")
                .defineInRange("weaponChance", 0.2, 0.0, 1.0);

            this.dualWieldChance = builder
                .comment("Probability of spawning with two one-handed guns when first weapon is one-handed. Range: 0.0 to 1.0.")
                .defineInRange("dualWieldChance", 0.2, 0.0, 1.0);

            this.dropChanceOverride = builder
                .comment("Weapon drop chance when mob dies.")
                .defineInRange("dropChanceOverride", 0.1, 0.0, 1.0);

            this.maxAttachmentSlots = builder
                .comment("Max random attachments per weapon. 0 = base weapon only.")
                .defineInRange("maxAttachmentSlots", 2, 0, 5);

            this.allowAdvancedWeapons = builder
                .comment("Allow heavy weapons (Gatling, Launcher, Hammer) in the default spawn pool. Nailgun and Blazegun are omitted from automatic pools and loot (Create fuel); add them via mobWeaponOverrides to spawn with fuel seeded.")
                .define("allowAdvancedWeapons", true);

            this.mobWeaponOverrides = builder
                .comment("Per-mob weapon assignment. Format: mob_id=gun1 or mob_id=gun1,gun2,gun3 (comma = pick random). Example: minecraft:zombie=cgs:flintlock. Nailgun/blazegun may be listed here; spawn fuel tanks are filled automatically. Includes guardvillagers:guard when guard CGS spawning is enabled.")
                .defineList("mobWeaponOverrides", defaultMobWeaponOverrides(), s -> s instanceof String && ((String) s).contains("="));

            this.guardCgsSpawningEnabled = builder
                .comment("Enable CGS weapon spawning for Guard Villagers (guardvillagers:guard). Replaces datapack CGS pools; TConstruct weapons in datapack remain.")
                .define("guardCgsSpawningEnabled", true);

            this.guardWeaponChance = builder
                .comment("Probability that a guard spawns with a CGS gun. Range: 0.0 to 1.0. Default 0.35 (higher than regular mobs).")
                .defineInRange("guardWeaponChance", 0.35, 0.0, 1.0);

            this.vanillaWeaponsRemoved = builder
                .comment("Set to true if vanilla weapons (e.g. bows, swords) are removed from mob loot/spawn by another mod or datapack. When true and TConstruct-Emergence is loaded, skeleton/stray/wither_skeleton always get a weapon: weaponChance = CGS, rest = TC-E longbow. Keeps bow mobs armed without raising TC-E global weaponChance. Default false.")
                .define("vanillaWeaponsRemoved", false);

            builder.pop();

            builder.comment("Out-of-ammo fallback behavior").push("out_of_ammo");

            this.outOfAmmoFallbackTicks = builder
                .comment("Ticks without ammo before falling back to standard mob behavior (melee/ranged). 0 = immediate fallback.")
                .defineInRange("outOfAmmoFallbackTicks", 100, 0, 600);
            
            builder.pop();

            builder.comment("Loot Integrations compatibility - chest loot toggles").push("loot_integrations");

            this.includeLootGuns = builder
                .comment("Include TriggerMobs/NTGL guns in Loot Integrations chest loot (where enabled by datapack JSON).")
                .define("includeLootGuns", true);

            this.includeLootAttachments = builder
                .comment("Include TriggerMobs/NTGL attachments in Loot Integrations chest loot.")
                .define("includeLootAttachments", true);

            this.includeLootAmmo = builder
                .comment("Include ammo items (e.g., NTGL rounds, shotshells, arrows) in Loot Integrations chest loot.")
                .define("includeLootAmmo", true);

            builder.pop();

            builder.comment("Debug logging (writes to latest.log; leave false unless diagnosing issues)").push("debug");

            this.debugGunAi = builder
                .comment("Log why a mob with a gun chases but does not fire (ammo, reload, range, LOS, attack delay, fuel). Throttled ~every 2s per mob. Look for [TM-ai] lines.")
                .define("debugGunAi", false);

            this.debugGunSpawn = builder
                .comment("Log CGS weapon spawn prep (ammo count, ignore_ammo, fuel tanks). Look for [TM-spawn] lines.")
                .define("debugGunSpawn", false);

            builder.pop();
        }

        /** Default mob-to-weapon mapping similar to InControl spawn.json */
        private static List<String> defaultMobWeaponOverrides() {
            return Arrays.asList(
                "minecraft:zombie=cgs:flintlock",
                "minecraft:zombie_villager=cgs:revolver",
                "minecraft:drowned=cgs:flintlock",
                "minecraft:husk=cgs:shotgun",
                "minecraft:skeleton=cgs:revolver",
                "minecraft:stray=cgs:shotgun",
                "minecraft:wither_skeleton=cgs:gatling",
                "minecraft:pillager=cgs:revolver",
                "minecraft:vindicator=cgs:flintlock",
                "minecraft:evoker=cgs:shotgun",
                "minecraft:zombified_piglin=cgs:revolver,cgs:flintlock",
                "minecraft:witch=cgs:revolver",
                "guardvillagers:guard=cgs:flintlock,cgs:revolver,cgs:shotgun,cgs:gatling,cgs:launcher"
            );
        }
    }
}

