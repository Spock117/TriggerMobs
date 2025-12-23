# Building and Testing TriggerMobs Mod

## Prerequisites

1. **Java 17** - Required for Minecraft 1.20.1 Forge development
2. **NukaTeamGunLib (NTGL)** - This mod is a required dependency
3. **Minecraft 1.20.1 Forge 47.4.0** - The target Minecraft version

## Setup for Development

### Obtaining NTGL Dependency

1. **Download or build NTGL:**
   - Download NTGL from its releases page, or
   - Build NTGL yourself (requires Gradle 7.x due to ForgeGradle 5.x limitation)

2. **Place NTGL jar in libs folder:**
   ```bash
   # Create libs folder if it doesn't exist
   mkdir -p common/libs
   
   # Copy the NTGL jar (exclude sources/javadoc jars)
   # The jar should be named ntgl-*.jar
   cp /path/to/ntgl-*.jar common/libs/
   # Remove sources/javadoc jars if copied
   rm -f common/libs/*-sources.jar common/libs/*-javadoc.jar
   ```

   The build system will automatically find and use the NTGL jar from `common/libs/` during compilation.

## Building TriggerMobs

1. Navigate to the TriggerMobs directory:
   ```bash
   cd triggermobs
   ```

2. Build the mod:
   ```bash
   ./gradlew build
   ```

   Or on Windows:
   ```bash
   gradlew.bat build
   ```

3. The built mod jar will be in:
   - `triggermobs/forge/build/libs/triggermobs-forge-<version>.jar`

## Testing the Mod

### Setup Test Environment

1. **Create a test Minecraft instance** (or use the built-in run configurations)

2. **Install required mods in the mods folder:**
   - `ntgl-<version>.jar` (NukaTeamGunLib - REQUIRED)
   - `triggermobs-forge-<version>.jar` (TriggerMobs - your mod)
   - Any other dependencies NTGL requires

3. **Start the game** and create a test world

### Using Gradle Run Tasks

The mod includes run configurations. You can run directly from Gradle:

```bash
# Run client
./gradlew :forge:runClient

# Run server
./gradlew :forge:runServer
```

**Note:** Make sure NTGL is in the mods folder of the run directory, or it will crash on startup.

### Manual Testing Steps

1. **Give a hostile mob a gun:**
   - Use commands or creative mode to give a zombie/pillager/etc. an NTGL gun
   - Example command: `/give @e[type=zombie,limit=1] ntgl:pistol10mm`

2. **Test features:**
   - ✅ Mobs should automatically use guns when holding them
   - ✅ Mobs should auto-reload when ammo runs out
   - ✅ Mobs should have moderate inaccuracy (not perfect aim)
   - ✅ Mobs should strafe while shooting
   - ✅ All hostile mobs should work (zombies, pillagers, vindicators, etc.)

3. **Verify behavior:**
   - Mobs should stop and aim when in range
   - Mobs should shoot at targets
   - Mobs should reload automatically
   - Shots should have some spread/inaccuracy

## Deployment

### For Distribution

When distributing your mod:

1. **Build the mod** as described above

2. **Create a distribution package** that includes:
   - `triggermobs-forge-<version>.jar`
   - **Instructions** that NTGL is required
   - **Version compatibility** information

3. **User Installation:**
   - Users must install NTGL first
   - Then install TriggerMobs
   - Both mods go in the `mods/` folder

### Important Notes

- **NTGL is a hard dependency** - The mod will not work without it
- **Mob AI Tweaks is NOT required** - It was only used as reference during development
- The mod uses **compileOnly** for NTGL, meaning:
  - NTGL is needed at compile time
  - NTGL must be in the mods folder at runtime
  - NTGL is NOT bundled with TriggerMobs

## Troubleshooting

### Build Errors

**Error: Could not find ntgl**
- Solution: Download or build NTGL and copy the jar to `common/libs/`
- Ensure the jar is named `ntgl-*.jar` and is not a sources or javadoc variant

**Error: Mixin errors**
- Solution: Ensure you're using the correct Minecraft/Forge version (1.20.1, Forge 47.4.0)
- Check that mixin configuration is correct

### Runtime Errors

**Mod crashes on startup**
- Check that NTGL is installed and compatible version
- Check that both mods are for Minecraft 1.20.1 Forge 47.4.0

**Mobs don't use guns**
- Verify the mob is holding an NTGL gun (IWeapon instance)
- Check that the gun is in main hand or offhand
- Verify the goal was added (check logs for mixin application)

**Mobs have perfect accuracy**
- Check that InaccuracyHelper is working
- Verify the shoot method is applying offsets

## Development Tips

1. **Use the runClient task** for quick testing
2. **Check the logs** for mixin application messages
3. **Use breakpoints** in MobGunAttackGoal to debug AI behavior
4. **Test with different mob types** to ensure all hostile mobs work

## File Structure

```
triggermobs/
├── common/
│   ├── libs/              # Place NTGL jar here for development
│   └── src/main/java/...  # Mod source code
├── forge/
│   └── build/libs/        # Built mod jar appears here
└── build.gradle           # Root build file
```

