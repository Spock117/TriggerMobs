# TriggerMobs Mod

A Minecraft Forge 1.20.1 mod that enables hostile mobs to use NTGL (NukaTeamGunLib) guns with automatic reloading, improved AI, and moderate inaccuracy. Optional Create:Gunsmithing support adds weapon-specific AI, and **built-in CGS gun spawning** (v1.3.0+) equips mobs and guards with Create:Gunsmithing weapons on spawn—no InControl needed. Version 1.4.0 adds **Loot Integrations** compatibility so CGS guns, attachments, and ammo can appear in combat-focused structure chests, fully configurable via the TriggerMobs config.

## Features

- ✅ **Auto Reload** - Mobs automatically reload when needed
- ✅ **Universal Support** - All hostile mobs that can hold items can use guns
- ✅ **Improved AI** - Strafing, better targeting, and movement behavior
- ✅ **Configurable Inaccuracy** - Mobs have configurable spread (aiAccuracyNerf); guards have separate guardAccuracyNerf (typically more skilled)
- ✅ **Smart Item Pickup** (v1.1.0) - Mobs only pick up weapons and tools, automatically dropping other items
- ✅ **Dual-Wielding Support** (v1.1.0) - One-handed weapons can be dual-wielded when mobs pick up compatible weapons
- ✅ **Optional Create:Gunsmithing Support** (v1.1.0) - Weapon-specific AI behaviors for Create:Gunsmithing weapons when installed
- ✅ **Built-in CGS Gun Spawning** (v1.3.0) - Mobs and guards spawn with Create:Gunsmithing weapons. Configurable per-mob weapon pools, dual wield chance, attachments, and drop chance. No InControl or datapack needed for mob CGS spawning.
- ✅ **Loot Integrations chest loot** (v1.4.0) - Optional integration with [Loot Integrations](https://www.curseforge.com/minecraft/mc-mods/loot-integrations) and its addons: CGS guns, attachments, and ammo are injected into combat/adventure structure chests (vanilla dungeons, pillager outposts, strongholds, Integrated Villages, Underground Villages, etc.) via datapack-style JSONs shipped with TriggerMobs. Controlled by `includeLootGuns`, `includeLootAttachments`, and `includeLootAmmo` in `triggermobs-common.toml`.
- ✅ **Optional Guard Villagers Support** - When [Guard Villagers](https://www.curseforge.com/minecraft/mc-mods/guard-villagers) is installed, guards can use NTGL and Create:Gunsmithing guns. Guards have separate accuracy (guardAccuracyNerf) and their own CGS spawn config (guardCgsSpawningEnabled, guardWeaponChance).

## Requirements

- **Minecraft:** 1.20.1
- **Forge:** 47.4.0
- **NukaTeamGunLib (NTGL):** Required dependency (must be installed separately) (Tested on ntgl-1.20.1-3.0.4)
- **A Gun Pack that uses NTGL:** Required - NTGL requires a gun pack to provide weapons. This mod works with all NTGL gun packs, including:
  - **Create:Gunsmithing** - A popular NTGL gun pack
  - Any other mod that provides weapons using NTGL

**Note:** This mod provides universal AI behavior for all NTGL weapons. When Create:Gunsmithing is installed, mobs will use specialized AI tailored to each weapon type (flintlock, revolver, shotgun, etc.). For other NTGL gun packs, all weapons use the same generic attack patterns and behavior.

## Quick Start

### For Development

1. **Obtain NTGL jar:**
   - Download NTGL from its releases page or build it yourself
   - Place the NTGL jar file in `common/libs/` directory
   - The jar should be named `ntgl-*.jar` (excluding `-sources` and `-javadoc` variants)

2. **Optional - Guard Villagers (for building with guard support):**
   - Place a Guard Villagers JAR (e.g. `guardvillagers-1.20.1-*.jar`) in `common/libs/` to enable optional compile-time support. The mod still works at runtime with Guard Villagers without this step.

3. **Build TriggerMobs:**
   ```bash
   ./gradlew build
   ```

4. **Run for testing:**
   ```bash
   ./gradlew :forge:runClient
   ```

### For Users

1. Install **NukaTeamGunLib** first (place in `mods/` folder)
2. Install a **gun pack that uses NTGL** (like Create:Gunsmithing) - place in `mods/` folder
3. Install **TriggerMobs** (place in `mods/` folder)
4. Start Minecraft and enjoy!

**Note:** NTGL itself doesn't provide weapons - you need a gun pack like Create:Gunsmithing to get actual guns. TriggerMobs will work with any NTGL gun pack.

## How It Works

- All hostile mobs (zombies, pillagers, vindicators, evokers, witches, etc.) automatically get gun attack behavior
- The mod automatically enables item pickup (`CanPickUpLoot`) for humanoid mobs so they can pick up weapons from the ground
- **Smart Item Pickup** (v1.1.0): Mobs will only pick up weapons and tools, automatically dropping any other items they might be holding. This ensures mobs stay focused on combat.
- **Dual-Wielding** (v1.1.0): When a mob picks up a second one-handed weapon, it can dual-wield compatible weapons (e.g., two flintlocks or two revolvers).
- When a mob holds an NTGL gun (IWeapon), it will:
  - Use the gun to attack targets
  - Strafing while shooting
  - Auto-reload when ammo runs out
  - Have moderate inaccuracy for balanced gameplay
  - Use weapon-specific AI when Create:Gunsmithing is installed (v1.1.0)

## Building

See [BUILD_AND_TEST.md](BUILD_AND_TEST.md) for detailed build and testing instructions, including notes on Loot Integrations/Loot Integrations addons used for chest loot testing.

### Troubleshooting Build Issues

#### Manifold Plugin Error

If you encounter an error like `error: plug-in not found: Manifold` during compilation:

The project originally included Manifold plugin configuration but doesn't actually use it. To fix:

1. **Remove Manifold plugin references:**
   - Remove `id 'xyz.wagyourtail.manifold'` from `build.gradle`
   - Remove Manifold plugin and dependencies from `buildSrc/src/main/groovy/common.gradle`
   - Remove `manifold_version` from `gradle.properties`

2. **Clean and rebuild:**
   ```bash
   ./gradlew clean build
   ```

The project build files have been updated to remove Manifold. If you're building from an older version, follow the steps above.

## Compatibility

- **Guard Villagers:** Optional. If Guard Villagers is installed, guards will use NTGL and Create:Gunsmithing guns when they have a target (hostile mobs, angry-at players, village defenders). Guards get the same gun behavior as hostile mobs (strafing, reload, weapon-specific AI). For development builds, placing the Guard Villagers JAR in `common/libs/` (e.g. `guardvillagers-*.jar`) enables optional compile-time support.
- **TConstruct-Emergence / Tinkers addons:** When [TConstruct-Emergence](https://www.curseforge.com/minecraft/mc-mods/tconstruct-emergence) (or similar) equips mobs with Tinkers' Construct tools (e.g. skeletons with longbows), TriggerMobs recognizes those as weapons and does not drop them. Only items in the `tconstruct:modifiable` tag (actual tools/weapons) are treated as weapons—not materials, casts, or other TConstruct items. Addon weapons from **Tinkers-Rapier** and **TinkersKatanas** (and other addons that register to TConstruct tags or use namespaces `tinker_rapier` / `tinkerskatanas`) are also recognized so mobs keep them.
- **TC-E bow-mob split (vanilla weapons removed):** When vanilla weapons (e.g. bows) are removed from mob loot by another mod or datapack, set **`vanillaWeaponsRemoved`** to true in config. Skeleton/stray/wither_skeleton then always get a weapon: **weaponChance** (e.g. 0.2) get CGS guns, the rest get a TConstruct-Emergence longbow. This keeps bow mobs armed without raising TC-E’s global weaponChance (which would over-arm zombies). If `vanillaWeaponsRemoved` is false, the split is disabled and normal chances apply.
- **Recruiting with guns:** TriggerMobs adds NTGL/Create:Gunsmithing weapons to Guard Villagers’ “convertible” item tag. You can **right-click a villager while crouching** with an NTGL gun (e.g. flintlock, revolver, shotgun) to convert them into a guard **holding that gun**. Works with all Create:Gunsmithing weapons. Other NTGL gun packs can be supported by adding their items to the tag `guardvillagers:convertible_guard_items` via a datapack.
- **Guard spawn equipment:** When Guard Villagers is installed, TriggerMobs **overrides** the guard equipment loot table. By default (Guard Villagers’ loot table `guardvillagers:entities/guard_armor`), guards spawn with:
  - **Main hand:** CGS guns (from TriggerMobs config) or TConstruct/other from datapack
  - **Off hand:** 10% bread (1–8), or 50% shield
  - **Armor:** from Guard Villagers’ armor set table
  TriggerMobs handles CGS gun spawning via config (`guardCgsSpawningEnabled`, `guardWeaponChance`, `mobWeaponOverrides` for `guardvillagers:guard`). Blazegun is excluded from guard and mob pools (not usable by mobs). Guards may spawn with TConstruct weapons from a datapack; TriggerMobs can overwrite main hand with CGS when enabled. When `guardCgsSpawningEnabled` is true, TriggerMobs removes CGS pools from the guard loot table. Armor and off-hand (bread/shield) come from the datapack or Guard Villagers defaults.

## Important Notes

- **NTGL is required** - This mod will not work without NukaTeamGunLib installed
- **Gun pack is required** - Add a gun pack that uses NTGL

## Changelog

### Version 1.4.0

- **Loot Integrations chest loot:** Added optional integration with Loot Integrations and its addon packs. TriggerMobs now ships datapack-style JSONs under the `lootintegrations` namespace that inject CGS guns, CGS attachments, and CGS ammo into combat/adventure structure chests (vanilla dungeons, pillager outposts, strongholds, bastions, ancient cities, Integrated Villages, Underground Villages, etc.). Controlled via new config flags `includeLootGuns`, `includeLootAttachments`, and `includeLootAmmo` in `triggermobs-common.toml`.
- **CGS-only loot tables:** Introduced internal TriggerMobs loot tables (`triggermobs:loot/triggermobs_loot_guns`, `triggermobs:loot/triggermobs_loot_attachments`, `triggermobs:loot/triggermobs_loot_ammo`) that contain only Create:Gunsmithing weapons, attachments, and ammo (no NTGL base ammo). These are used exclusively as sources for Loot Integrations.

### Version 1.3.5

- **Blazegun removed from mob/guard pools:** Blazegun is not usable by mobs and has been removed from the CGS spawn pool, default guard weapon override, and Guard Villagers datapack (guard armor loot table and convertible items tag). Guards and other mobs will no longer be assigned blazeguns.

### Version 1.3.4

- **TCMobArmor + TinkersThings integration:** When **TCMobArmor** and **TinkersThings** are both installed and the TC-E bow-mob split is active (`vanillaWeaponsRemoved` + CGS + TC-E), the “else” branch (mobs that don’t get a CGS gun) now tries TCMobArmor’s weighted ranged pool first via reflection (`tryAssignRangedFromPool`). If that assigns a weapon, done; otherwise if the entity is in `tconstruct_emergence:valid_bow_mobs`, TC-E longbow is assigned. Allows TCMobArmor to drive shortbow/blowpipe/longbow/crossbow/blockram when TC-E ranged is disabled.

### Version 1.3.3

- **Configurable fire rate**: New `fireRateDelayMultiplier` in config (default 4.0, min 1.0). Multiplies the delay between shots for all mobs and weapon types—higher values slow down firing. Use this if mobs fire too fast.

### Version 1.3.2

- **TC-E bow-mob split:** When **`vanillaWeaponsRemoved`** is true and TConstruct-Emergence + Create:Gunsmithing are loaded, skeleton/stray/wither_skeleton always get a weapon: `weaponChance` (e.g. 0.2) get CGS, the rest get TC-E longbow. Use when a mod or datapack removes vanilla bows so bow mobs stay armed without raising TC-E’s global weaponChance.
- **TConstruct-Emergence / Tinkers compatibility:** Smart item pickup now treats only TConstruct **tools/weapons** as weapons (via `tconstruct:modifiable` tag), not all TConstruct items (materials, casts, etc.). Fixes mobs (e.g. skeletons) spawning without weapons when TConstruct-Emergence had equipped them—TriggerMobs no longer strips those items. Addon weapons from **Tinkers-Rapier** and **TinkersKatanas** are also recognized (tag or namespaces `tinker_rapier`, `tinkerskatanas`).

### Version 1.3.1

- **NTGL cooldown fix:** All weapon strategies enforce attack delay ≥ weapon rate + 3 ticks (with non-negative variance) to prevent "tried to fire before cooldown finished" warnings from NTGL. AttachmentAwareStrategy and GenericWeaponStrategy updated accordingly.
- **Mod metadata:** Built mod now shows correct description, CurseForge display URL, and GitHub issue tracker URL in mod list.

### Version 1.3.0

- **Built-in CGS Gun Spawning**: Mobs spawn with Create:Gunsmithing weapons on join. Config: `cgsSpawningEnabled`, `weaponChance` (0.2), `dualWieldChance`, `mobWeaponOverrides` (per-mob weapon pools), `maxAttachmentSlots`, `allowAdvancedWeapons`, `dropChanceOverride`. Works with `valid_gun_mobs` tag and config overrides. TConstruct-Emergence compatible (skips when TC-E equipped).
- **Guard CGS Spawning**: Separate config for guards: `guardCgsSpawningEnabled`, `guardWeaponChance` (0.35). Guards can overwrite TConstruct weapons with CGS. When enabled, TriggerMobs removes CGS pools from `guard_armor` loot table. Default guard weapon pool: flintlock, revolver, shotgun, nailgun, gatling, blazegun, launcher.
- **Guard Accuracy Nerf**: New `guardAccuracyNerf` (default 0.7) for guard villagers. Guards use this instead of `aiAccuracyNerf` for shooting spread.
- **Accuracy Config**: `aiAccuracyNerf` (1.0 = ±4–8° baseline). Higher = more inaccurate, lower = more accurate.
- **Other AI Config**: `aiReactionDelayTicks`, `fireRateDelayMultiplier` (default 4.0, slows firing when > 1.0), `outOfAmmoFallbackTicks` for fallback when ammo runs out.

### Version 1.2.0

- **Optional Guard Villagers Support**: When Guard Villagers is installed, guards can use NTGL and Create:Gunsmithing guns to attack hostile mobs or players (per Guard Villagers targeting). Guards get the same gun goal (priority 2), CanPickUpLoot, and custom weapon/tool pickup behavior as other mobs.
- **Guard spawn equipment**: Override `guardvillagers:entities/guard_armor` with separate CGS and NTGL gun pools (35% each, main hand). When Create:Gunsmithing is loaded, LootTableLoadEvent removes the NTGL pool so guards only roll CGS guns. Optional dependency on Guard Villagers (load-after) in mods.toml. No CGS offhand pool (JSON-only: no sword/crossbow + gun offhand). Tag `guardvillagers:convertible_guard_items` extended so guards can be recruited with CGS weapons.
- **Guard weapon pickup from ground**: Mobs with custom pickup behavior (including guards) now actively pick up weapons and tools from the ground when their main hand or off hand is empty, so guards and other mobs can equip guns dropped nearby.
- **MobGunAttackGoal**: Set aggressive flag in start/stop for all mobs (fixes Guard attack animations when using guns).
- **Mod icon**: Added Trigger Mobs logo as mod icon in mod list.
- **Technical**: Optional Guard Villagers JAR in `common/libs` for compile-time support; ModDetectionHelper.isGuardVillagersLoaded(); GuardLootTableEvents (LootTableLoadEvent); Forge 1.20.1 compatibility (no-arg constructor for @Mod).

### Version 1.1.0

- **Optional Create:Gunsmithing Support**: Added weapon-specific AI behaviors for Create:Gunsmithing weapons
  - Flintlock & Revolver: Medium-range combat with dual-wielding support
  - Shotgun: Aggressive close-range combat with burst fire
  - Nailgun: Sustained fire with high-capacity magazines
  - Gatling Gun: Continuous suppression fire with minimal movement
  - Blazegun: Close-medium range continuous fire
  - Launcher: Long-range rocket attacks with distance management
  - Pneumatic Hammer: Melee-focused aggressive combat
  - Frag Grenade: Medium-range throwing behavior
- **Smart Item Pickup System**: Mobs now only pick up weapons and tools, automatically dropping other items
- **Dual-Wielding Support**: One-handed NTGL weapons can be dual-wielded when mobs pick up compatible weapons
- **Technical Improvements**:
  - Replaced Mixin-based system with Forge events for more reliable mob item pickup behavior
  - More stable and maintainable codebase

## Development

The mod structure:
- `common/` - Shared code between loaders
- `forge/` - Forge-specific code
- `common/src/main/java/com/spock117/triggermobs/` - Main mod code
  - `goals/` - AI goals (MobGunAttackGoal)
  - `util/` - Utility classes (InaccuracyHelper, MobItemPickupHelper)
- `forge/src/main/java/com/spock117/triggermobs/` - Forge-specific code
  - `events/` - Forge event handlers (TriggerMobsEvents, GuardLootTableEvents)

## License

GPL v3 (GNU General Public License version 3)

