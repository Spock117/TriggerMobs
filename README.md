# TriggerMobs Mod

A Minecraft Forge 1.20.1 mod that enables hostile mobs to use NTGL (NukaTeamGunLib) guns with automatic reloading, improved AI, and moderate inaccuracy. Version 1.1.0 adds optional Create:Gunsmithing support with weapon-specific AI behaviors!

## Features

- ✅ **Auto Reload** - Mobs automatically reload when needed
- ✅ **Universal Support** - All hostile mobs that can hold items can use guns
- ✅ **Improved AI** - Strafing, better targeting, and movement behavior
- ✅ **Moderate Inaccuracy** - Mobs have ±3-5 degree spread (not perfect aim)
- ✅ **Smart Item Pickup** (v1.1.0) - Mobs only pick up weapons and tools, automatically dropping other items
- ✅ **Dual-Wielding Support** (v1.1.0) - One-handed weapons can be dual-wielded when mobs pick up compatible weapons
- ✅ **Optional Create:Gunsmithing Support** (v1.1.0) - Weapon-specific AI behaviors for Create:Gunsmithing weapons when installed

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

2. **Build TriggerMobs:**
   ```bash
   ./gradlew build
   ```

3. **Run for testing:**
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

See [BUILD_AND_TEST.md](BUILD_AND_TEST.md) for detailed build and testing instructions.

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

## Important Notes

- **NTGL is required** - This mod will not work without NukaTeamGunLib installed
- **Gun pack is required** - Add a gun pack that uses NTGL

## Changelog

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
  - `events/` - Forge event handlers (TriggerMobsEvents)

## License

GPL v3 (GNU General Public License version 3)

