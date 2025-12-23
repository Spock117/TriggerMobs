# TriggerMobs Mod

A Minecraft Forge 1.20.1 mod that enables hostile mobs to use NTGL (NukaTeamGunLib) guns with automatic reloading, improved AI, and moderate inaccuracy.

## Features

- ✅ **Auto Reload** - Mobs automatically reload when needed
- ✅ **Universal Support** - All hostile mobs that can hold items can use guns
- ✅ **Improved AI** - Strafing, better targeting, and movement behavior
- ✅ **Moderate Inaccuracy** - Mobs have ±3-5 degree spread (not perfect aim)

## Requirements

- **Minecraft:** 1.20.1
- **Forge:** 47.4.0
- **NukaTeamGunLib (NTGL):** Required dependency (must be installed separately) (Tested on ntgl-1.20.1-3.0.4)
- **A Gun Pack that uses NTGL:** Required - NTGL requires a gun pack to provide weapons. This mod works with all NTGL gun packs, including:
  - **Create:Gunsmithing** - A popular NTGL gun pack
  - Any other mod that provides weapons using NTGL

**Note:** This mod provides universal AI behavior for all NTGL weapons. There is no customized weapon-specific AI - all weapons use the same attack patterns and behavior.

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
- When a mob holds an NTGL gun (IWeapon), it will:
  - Use the gun to attack targets
  - Strafing while shooting
  - Auto-reload when ammo runs out
  - Have moderate inaccuracy for balanced gameplay

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

## Development

The mod structure:
- `common/` - Shared code between loaders
- `forge/` - Forge-specific code
- `common/src/main/java/com/spock117/triggermobs/` - Main mod code
  - `goals/` - AI goals (MobGunAttackGoal)
  - `mixins/` - Mixin classes
  - `util/` - Utility classes (InaccuracyHelper)

## License

GPL v3 (GNU General Public License version 3)

