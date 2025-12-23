# Where to Find TriggerMobs Debug Logs

## Log File Locations

Minecraft Forge logs are typically found in:

### Windows
- **Latest log:** `%appdata%\.minecraft\logs\latest.log`
- **Debug log:** `%appdata%\.minecraft\logs\debug.log` (if enabled)

### Linux/Mac
- **Latest log:** `~/.minecraft/logs/latest.log`
- **Debug log:** `~/.minecraft/logs/debug.log` (if enabled)

### Custom Installation
If you're using a custom launcher or installation:
- Check your launcher's settings for the Minecraft directory
- Logs are usually in `<minecraft_directory>/logs/`

## What to Look For

### 1. Mod Initialization
When the game starts, you should see:
```
[main/INFO] [triggermobs/]: TriggerMobs mod initializing...
[main/INFO] [triggermobs/]: TriggerMobs mod initialized! Debug logging is ENABLED.
```

**If you don't see this:**
- The mod might not be loading
- Check if the mod jar is in the `mods` folder
- Check if there are any errors in the log preventing mod loading

### 2. Debug Messages
When debug is enabled, look for messages prefixed with:
```
[TriggerMobs DEBUG]
```

These appear in `latest.log`, not necessarily in `debug.log`.

## Log Levels

TriggerMobs uses `LOGGER.info()` for all debug messages, so they should appear in:
- **latest.log** - Contains INFO level and above
- **debug.log** - Only if debug logging is specifically enabled in Forge config

## If You Don't See Debug Messages

1. **Check if mod is loading:**
   - Look for "TriggerMobs mod initialized!" in `latest.log`
   - If missing, the mod isn't loading properly

2. **Verify DEBUG flag:**
   - Check `TriggerMobs.java` - `DEBUG` should be `true`
   - Rebuild the mod if you changed it

3. **Check log file:**
   - Make sure you're looking in `latest.log`, not just `debug.log`
   - `latest.log` is the main log file

4. **Search for TriggerMobs:**
   - Use Ctrl+F (or Cmd+F) to search for "TriggerMobs" in the log file
   - This will find all messages from the mod

5. **Check log level:**
   - Make sure your log viewer shows INFO level messages
   - Some log viewers filter by default

## Example Log Entries

You should see entries like:
```
[22Dec2025 18:00:22.969] [Server thread/INFO] [triggermobs/]: TriggerMobs mod initializing...
[22Dec2025 18:00:22.970] [Server thread/INFO] [triggermobs/]: TriggerMobs mod initialized! Debug logging is ENABLED.
[22Dec2025 18:00:25.123] [Server thread/INFO] [triggermobs/]: [TriggerMobs DEBUG] Added MobGunAttackGoal to zombie (ID: 123)
[22Dec2025 18:00:27.456] [Server thread/INFO] [triggermobs/]: [TriggerMobs DEBUG] Mob zombie (ID: 123) - canUse check: hasTarget=true, hasGun=true...
```

## Troubleshooting

### No messages at all
- Mod might not be installed correctly
- Check `mods.toml` for errors
- Verify the jar file is in the mods folder

### Only initialization message
- Debug might be disabled
- Check `TriggerMobs.DEBUG` is `true`
- Rebuild the mod

### Messages appear but not [TriggerMobs DEBUG]
- The logger format might be different
- Search for "TriggerMobs" instead
- Check if messages are using a different prefix

