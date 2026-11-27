# Temple of Nihility - Version 1.0.0-beta.5 Release Notes

**Release Date**: 2026-11-27  
**Branches**: release/neoforge-26.1.2 and release/neoforge-26.2 (synced with main)

## Two Versions Available

This release provides two parallel builds for different NeoForge versions:

### NeoForge 26.1.2 (Stable)
- **File**: `temple-of-nihility-neoforge-26.1.2-1.0.0-beta.5.jar`
- **Size**: 1.37 MB (1,440,510 bytes)
- **SHA-256**: `1f7ccc7fab694e4e14f9edb61d56cd02eead3dd2140db1b748887c9e35040428`
- **NeoForge**: 26.1.2.124
- **Minecraft**: 26.1.2
- **Target**: Stable compatibility with established mod ecosystem

### NeoForge 26.2 (Latest)
- **File**: `temple-of-nihility-neoforge-26.2-1.0.0-beta.5.jar`
- **Size**: 1.37 MB (1,440,387 bytes)
- **SHA-256**: `b3ba4d19f8846a66073a5b014812fa28e6cc4683a3530b27188a3e2f265bfd46`
- **NeoForge**: 26.2.0.93
- **Minecraft**: 26.2
- **Target**: Latest NeoForge features and API improvements

## Major Changes from Beta.4

### 1. Compact Bilingual Nihility Energy HUD & Non-Spam Combat Feedback
- **Dedicated Nihility Energy HUD**: Anchored cleanly right of the hotbar (`NihilityHudOverlay` + `NihilityHudState`), automatically shifting clear of vanilla Hotbar Attack Indicator and clamping safely on narrow screens. Displays real-time Void Power (`0-tick` instant sync), 4pc Zero Counter countdown, Chaos Overdrive timer, Chaos Gauntlet 1-5 combo pips, Abyss Soul Shield, and Zero Ward readiness; dims smoothly to 48% opacity when full and out of combat, and tints crimson-rose below the 12-energy skill threshold.
- **Coalesced Combat Micro-Toasts**: Replaces frequent Phase Dodge, Rift Dagger Backstab, and Curios combat ActionBar/chat spam with max-3 stacked micro-toasts that merge repeated triggers in place (`Phase Dodge ×N` / `相位闪避 ×N`) and sum net Void Power deltas.
- **Peripheral Shimmer & Audio Throttle**: Adds subtle 2px peripheral screen-edge shimmer on Phase Dodge / Aegis / Death Saves and a 10-tick audio cooldown (`CombatFeedbackPolicy`) so dense mob fights stay clean and readable.
- **Full Bilingual Localization**: Every HUD badge and all 16 combat feedback toast labels are localized in both `zh_cn.json` and `en_us.json`, with dynamic toast width expansion so English labels never clip.

### 2. 38 Animated 32×32 8-Frame Four-Aspect Pixel Art Icons
- **7 Nihility Weapons & Tools**: Repainted in 32×32 8-frame animated pitch-black negative-space pixel art with cold-white fracture rims and subtle erasure shimmer (zero semi-transparent edge artifacts).
- **4 Nihility Armor Pieces + 27 Curios Accessories**: All 31 icons upgraded to 32×32 8-frame animated pixel art following strict Four-Aspect visual identity (Nihility pitch-black/cold-white/violet, Chaos obsidian/gilt/blood-ruby, Void slate/spatial-cyan/silver, Abyss basalt/ember-crimson/bone-ash).

### 3. Four-Aspect Combat, Armor, Curios & Gameplay Polish
- **Weapons & 1P/3P Choreography**: Full Four-Aspect weapon combat overhaul (Zero Blade Existence Reduction stacks + Zero Slash projectile shatter, Boundary Greatblade 3-wave armored poise & armor shatter, Ruin Dagger rear-cone backstab crit & Missing Step mark detonation, Nihility Axe Shatter Cleave) with calibrated grips, 1P/3P swing choreography, and +4 Void Power melee recovery loop.
- **2pc / 4pc Nihility Armor & 27 Curios**: 2pc grants +2 Void Power/s, faster debuff decay, and +60 cap; 4pc adds +140 cap, auto-repair, and 25% Phase Dodge (+12 energy refund + 4s 1.55× armor-piercing Zero Counter Strike). Added 6 bespoke Four-Aspect combat VFX triggers (Phase Dodge ring, Ward hexagonal membrane, Abyss Siphon curved arc, Chaos Gauntlet shockwave, Abyss Slam wave, Sentinel Deflect burst).
- **Gameplay Bug Fixes**:
  - Fixed Chaos Crown Overdrive 50% energy discount not applying to `PlayerWeaponActions.canPrepare` and `NihilityAxe` pre-checks.
  - Fixed Void Power being wiped on `PlayerEvent.Clone` (now retains 100% across End portal returns and 50% on death respawn) and added immediate HUD sync on login/respawn/dimension travel.
  - Updated in-game Guidebook (`《虚无圣典》`) Pages 12 & 13 in both Chinese and English to match current combat, armor, Curios, and HUD mechanics.
- **Structure, Boss & Spawn Eggs**: Includes the overhauled 3-tier Temple structure, 5-tier creature silhouettes, Saint Boss Bar & standoff AI, plus all 5 creature spawn eggs from Beta.4.

## Build Verification

Both versions passed full Gradle compilation, unit tests, and resource/definition verification:
- Compilation successful (Java 25, Gradle 9.2.1, ModDevGradle 2.0.148)
- **93 unit tests passed across 16 test classes** (0 failures, 0 errors), including `CombatFeedbackPolicyTest`
- `verifyExplorationDefinitions`, `verifyNoGameplayCommandRouting`, `verifyResourcesAndLanguages`, `verifyRitualDefinitions` all passed

## Dependency Requirements

### NeoForge 26.1.2 Version
```
- Minecraft: 26.1.2
- NeoForge: 26.1.2.124+
- Java: 25
- Curios API: 15.0.0+26.1.2 (required)
- JEI: 29.53.0.112 (optional)
- Waystones: 26.1.2.16+ (optional)
```

### NeoForge 26.2 Version
```
- Minecraft: 26.2
- NeoForge: 26.2.0.93+
- Java: 25
- Curios API: 15.0.0+ / 16.0.0+26.2 (required)
- JEI: 30.7.0.40+ (optional)
- Waystones: 26.2.0.3+ (optional)
```

## Installation

1. Download the JAR matching your Minecraft / NeoForge version (`26.1.2` or `26.2`).
2. Verify the SHA-256 checksum matches the values listed above.
3. Place the JAR file in your `mods/` directory alongside Curios API.
4. Launch Minecraft with NeoForge.

## Upgrading from Beta.4

Beta.5 is compatible with Beta.4 save files. Simply replace the old JAR file in your `mods/` folder.
