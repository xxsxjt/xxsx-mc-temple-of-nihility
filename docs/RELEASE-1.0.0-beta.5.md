# Temple of Nihility - Version 1.0.0-beta.5 Release Notes

**Release Date**: 2026-11-27  
**Primary Branch**: `main` & `release/neoforge-26.1.2` (NeoForge 26.1.2)  
**Parallel Branch**: `release/neoforge-26.2` (NeoForge 26.2)  
**License**: GNU Affero General Public License v3.0 only (`AGPL-3.0-only`)

## Two Versions Available

This release provides two parallel builds for different NeoForge versions:

### NeoForge 26.1.2 (Primary / Recommended)
- **File**: `temple-of-nihility-neoforge-26.1.2-1.0.0-beta.5.jar`
- **Size**: 1.37 MB (1,440,506 bytes)
- **SHA-256**: `42a86f3324314bce3aa05eccbc500635b3d0112e2c9b8b3c5863898caac346fa`
- **NeoForge**: 26.1.2.124+
- **Minecraft**: 26.1.2
- **Target**: Primary active development & stable compatibility with the 26.1.2 mod ecosystem

### NeoForge 26.2 (Parallel)
- **File**: `temple-of-nihility-neoforge-26.2-1.0.0-beta.5.jar`
- **Size**: 1.37 MB (1,440,381 bytes)
- **SHA-256**: `785d962422efb6af533cfba8b09882fea5fd64824fc778fd0cd08cf30ab29089`
- **NeoForge**: 26.2.0.93+
- **Minecraft**: 26.2
- **Target**: Parallel build for Minecraft 26.2 / NeoForge 26.2 ([Release page](https://github.com/xxsxjt/xxsx-mc-temple-of-nihility/releases/tag/v1.0.0-beta.5-neoforge26.2))

## Major Changes from Beta.4

### 1. Compact Bilingual Nihility Energy HUD & Non-Spam Combat Feedback
- **Dedicated Nihility Energy HUD**: Anchored cleanly right of the hotbar (`NihilityHudOverlay` + `NihilityHudState`), automatically shifting clear of vanilla Hotbar Attack Indicator and clamping safely on narrow screens. Displays real-time Void Power (`0-tick` instant sync), 4pc Zero Counter countdown, Chaos Overdrive timer, Chaos Gauntlet 1-5 combo pips, Abyss Soul Shield, and Zero Ward readiness; dims smoothly to 48% opacity when full and out of combat, and tints crimson-rose below the 12-energy skill threshold.
- **Coalesced Combat Micro-Toasts**: Replaces frequent Phase Dodge, Rift Dagger Backstab, and Curios combat ActionBar/chat spam with max-3 stacked micro-toasts that merge repeated triggers in place (`Phase Dodge ×N` / `相位闪避 ×N`) and sum net Void Power deltas.
- **Peripheral Shimmer & Audio Throttle**: Adds subtle 2px peripheral screen-edge shimmer on Phase Dodge / Aegis / Death Saves and a 10-tick audio cooldown (`CombatFeedbackPolicy`).
- **Full Bilingual Localization**: Every HUD badge and all 16 combat feedback toast labels are localized in both `zh_cn.json` and `en_us.json`, with dynamic toast width expansion so English labels never clip.

### 2. 38 Animated 32×32 8-Frame Four-Aspect Pixel Art Icons
- **7 Nihility Weapons & Tools**: Repainted in 32×32 8-frame animated pitch-black negative-space pixel art with cold-white fracture rims and subtle erasure shimmer (zero semi-transparent edge artifacts).
- **4 Nihility Armor Pieces + 27 Curios Accessories**: All 31 icons upgraded to 32×32 8-frame animated pixel art following strict Four-Aspect visual identity.

### 3. Four-Aspect Combat, Armor, Curios, Metadata & Gameplay Polish
- **Weapons & 1P/3P Choreography**: Full Four-Aspect weapon combat overhaul (Zero Blade Existence Reduction stacks + Zero Slash projectile shatter, Boundary Greatblade 3-wave armored poise & armor shatter, Ruin Dagger rear-cone backstab crit & Missing Step mark detonation, Nihility Axe Shatter Cleave) with calibrated grips, 1P/3P swing choreography, and +4 Void Power melee recovery loop.
- **2pc / 4pc Nihility Armor & 27 Curios**: 2pc grants +2 Void Power/s, faster debuff decay, and +60 cap; 4pc adds +140 cap, auto-repair, and 25% Phase Dodge (+12 energy refund + 4s 1.55× armor-piercing Zero Counter Strike). Added 6 bespoke Four-Aspect combat VFX triggers.
- **Gameplay & Metadata Fixes**:
  - Fixed Chaos Crown Overdrive 50% energy discount not applying to `PlayerWeaponActions.canPrepare` and `NihilityAxe` pre-checks.
  - Fixed Void Power being wiped on `PlayerEvent.Clone` (now retains 100% across End portal returns and 50% on death respawn) and added immediate HUD sync on login/respawn/dimension travel.
  - Fixed `META-INF/neoforge.mods.toml` version (`1.0.0-beta.5`) and open-source license (`AGPL-3.0-only` with root `LICENSE` and refreshed `README.md`).
  - Updated in-game Guidebook (`《虚无圣典》`) Pages 12 & 13 in both Chinese and English.
  - Preserved all 5 Beta.4 creature spawn eggs alongside the overhauled 3-tier Temple structure, 5-tier silhouettes, and Saint Boss Bar.

## Build Verification

Both versions passed full Gradle compilation, unit tests, and resource/definition verification:
- Compilation successful (Java 25, Gradle 9.2.1, ModDevGradle 2.0.148)
- **93 unit tests passed across 16 test classes** (0 failures, 0 errors), including `CombatFeedbackPolicyTest`
- `verifyExplorationDefinitions`, `verifyNoGameplayCommandRouting`, `verifyResourcesAndLanguages`, `verifyRitualDefinitions` all passed

## Dependency Requirements

### NeoForge 26.1.2 Version (Primary)
```
- Minecraft: 26.1.2
- NeoForge: 26.1.2.124+
- Java: 25
- Curios API: 15.0.0+26.1.2 (required)
- JEI: 29.53.0.112+ (optional)
- Waystones: 26.1.2.16+ (optional)
```

### NeoForge 26.2 Version (Parallel)
```
- Minecraft: 26.2
- NeoForge: 26.2.0.93+
- Java: 25
- Curios API: 15.0.0+ / 16.0.0+26.2 (required)
- JEI: 30.7.0.40+ (optional)
- Waystones: 26.2.0.3+ (optional)
```

## Installation

1. Download the JAR matching your Minecraft / NeoForge version (`26.1.2` is marked Latest; `26.2` is available on its parallel release tag).
2. Verify the SHA-256 checksum matches the values listed above.
3. Place the JAR file in your `mods/` directory alongside Curios API.
4. Launch Minecraft with NeoForge.
