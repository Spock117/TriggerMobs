# TriggerMobs Mod

A **Minecraft 1.21.1 / NeoForge** mod that lets hostile mobs use **NTGL (NukaTeamGunLib)** guns with automatic reloading, improved ranged AI, and configurable inaccuracy. Optional **Create:Gunsmithing** adds weapon-specific strategies (flintlock, shotgun, gatling, etc.). **Built-in CGS spawning** equips mobs and Guard Villagers on join—no InControl required. **Loot Integrations** support injects CGS guns, attachments, and ammo into combat/adventure chests via shipped datapack-style tables (`includeLootGuns` / `includeLootAttachments` / `includeLootAmmo`).

**This repository targets NeoForge 1.21.1.** Older **Forge 1.20.1** releases on CurseForge may include extras (for example optional Recruits crossbowman support); those integrations are **not** shipped in this NeoForge codebase.

## New on NeoForge 1.21.1

- **NTGL mob-shot aggro mixin** — NTGL’s server handler runs a hostile sweep on unsilenced shots (`setLastHurtByMob` on everyone in sound range). TriggerMobs mixes into that path so **non-player shooters skip the sweep**, stopping allied hostiles from dogpiling the shooter after one mob fires while **still firing projectiles normally**.
- **Mob gun targeting** — Custom gun AI aligns with vanilla-style ranged goals: it keys off the mob’s **attack target** without requiring `LivingEntity.canAttack` to pass in cases where that wrongly blocked shooting (“runs at you, never fires”).
- **NTGL vs TriggerMobs goals** — For monsters (and Guard Villagers when present), NTGL’s own **`GunAttackGoal`** is stripped each tick so **one** gun pipeline runs (`MobGunAttackGoal` + strategies).
- **Piglins** — Gun AI respects vanilla piglin aggression (`AbstractPiglin`): no aiming/firing outside the aggressive state.
- **No monster-on-monster gunfire** — Monsters never use gun AI against another monster; players, villagers, iron golems, and other non-`Monster` targets are unaffected.

## Features

- ✅ **Auto reload** — Mobs reload when empty via NTGL-compatible reload tracking
- ✅ **Broad mob support** — Hostile mobs that can hold items get gun behavior; pickup enabled where configured
- ✅ **Strafe / spacing** — Movement and engagement tuned through strategies + config (`fireRateDelayMultiplier`, intervals, reaction delay)
- ✅ **Configurable inaccuracy** — `aiAccuracyNerf`; guards use `guardAccuracyNerf`
- ✅ **Smart item pickup** — Weapons/tools only; junk dropped from hands
- ✅ **Dual-wielding** — One-handed NTGL weapons when both hands hold compatible guns
- ✅ **Optional Create:Gunsmithing** — Per-weapon-type AI when CGS is installed
- ✅ **Built-in CGS spawning** — Per-mob pools, dual wield, attachments, drop behavior (`triggermobs-common.toml`)
- ✅ **Loot Integrations** — Optional chest injection for CGS loot categories (JSON uses valid number providers on NeoForge)
- ✅ **Optional Guard Villagers** — Guards get gun goals and CGS options when the mod is loaded

## Requirements

- **Minecraft:** 1.21.1  
- **NeoForge:** 21.1.228 or newer (see `gradle.properties`)  
- **NukaTeamGunLib (NTGL):** Required — e.g. `ntgl-1.21.1-3.x`  
- **A gun pack using NTGL:** Required (Create:Gunsmithing or any NTGL pack)

## Quick Start

### Development

1. Put **`ntgl-*.jar`** for **1.21.1** in `common/libs/` (no `-sources` / `-javadoc`).
2. Optional: Guard Villagers jar in `common/libs/` for compile-time linkage.
3. Build: `./gradlew build`
4. Run client: `./gradlew :neoforge:runClient`

### Users

Install **NTGL**, a **gun pack**, then **TriggerMobs** into `mods/`.

## How It Works

- **Monster** AI receives **`MobGunAttackGoal`** on join; **guards** get it when Guard Villagers is present.
- Shooting goes through **`ServerPlayHandler.handleShoot`** with **`C2SMessageShoot`**-style data (same server entry as NTGL).
- **Aggro mixin** (`NtglServerPlayHandlerAggroMixin`) cancels only NTGL’s **broadcast hostile aggro** step for **mob** shooters, not projectile spawning.

## Building

See [BUILD_AND_TEST.md](BUILD_AND_TEST.md).

## Compatibility

- **Guard Villagers** — Optional; load-after ordering in metadata.
- **TConstruct-Emergence / Tinkers** — Weapon recognition and TC-E bow-mob split config paths behave like prior releases (see config `vanillaWeaponsRemoved`, bow mob tags).
- **Loot Integrations** — Enable via installing Loot Integrations + addons; use config toggles for guns / attachments / ammo tables.

## Important Notes

- **NTGL + a gun pack are mandatory**
- **Recruits integration** exists only on legacy **Forge 1.20.1** artifacts, not in this NeoForge tree

## Changelog

### Version 1.5.4 (NeoForge / Minecraft 1.21.1)

- **1.5.4:** `neoforge.mods.toml` — optional integrations (**guardvillagers**, **cgs**) use NeoForge **`type = "optional"`**; **neoforge**, **minecraft**, and **ntgl** use **`type = "required"`**.

### Version 1.5.3 (NeoForge / Minecraft 1.21.1)

- **1.5.3:** Monsters never use gun AI against another monster (`MobGunAttackGoal`).
- Port to **NeoForge** with **`neoforge.mods.toml`** and **`[[mixins]]`** config.
- **`NtglServerPlayHandlerAggroMixin`** — Skip NTGL’s hostile sound-radius aggro sweep for **non-player** shooters.
- **`MobGunAttackGoal`** — Target validity (no monster-vs-monster shooting); piglin aggression guard; NTGL **`GunAttackGoal`** stripped for monsters/guards holding NTGL weapons.
- Config bootstrap no longer reads Forge config getters before the common spec loads (avoids spurious “config not loaded” errors).

### Version 1.5.2 (Forge 1.20.1 — Loot Integrations)

- Loot JSON: explicit **`minecraft:uniform`** where required; attachment / ammo entry fixes for CGS + Forge parser expectations.

### Version 1.5.1 — 1.5.0 (Forge 1.20.1 — Recruits)

- Optional Recruits crossbowman gun AI, finite ammo, recruit CGS replace options — **Forge 1.20.1 builds only**.

_(Older changelog entries for 1.4.x–1.1.x remain relevant to Forge 1.20.1 releases; see CurseForge file history.)_

## Development layout

- `common/` — Shared logic (goals, AI strategies, spawn, loot)
- `neoforge/` — NeoForge entry (`TriggerMobsForge`), events, mixins, resources

## License

GPL v3
