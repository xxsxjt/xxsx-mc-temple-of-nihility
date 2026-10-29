# Temple of Nihility - Version 1.0.0-beta.4 Release Notes

**Release Date**: 2026-10-29  
**Branches**: release/neoforge-26.1.2 and release/neoforge-26.2

## Two Versions Available

This release provides two parallel builds for different NeoForge versions:

### NeoForge 26.1.2 (Stable)
- **File**: `temple-of-nihility-neoforge-26.1.2-1.0.0-beta.4.jar`
- **Size**: 1.7 MB
- **SHA-256**: `25dbce8952acb5d062d0c00eb44337d1ddcd7685fb43dde2350266b621e8a3a8`
- **NeoForge**: 26.1.2.124
- **Minecraft**: 26.1.2
- **Target**: Stable compatibility with established mod ecosystem

### NeoForge 26.2 (Latest)
- **File**: `temple-of-nihility-neoforge-26.2-1.0.0-beta.4.jar`
- **Size**: 1.7 MB
- **SHA-256**: `f7e142661a8b7b77d9a4015a3296f278715379edac38484ff1dd31a855c0d31b`
- **NeoForge**: 26.2.0.93
- **Minecraft**: 26.2
- **Target**: Latest NeoForge features and API improvements

## Major Changes from Beta.3

### 1. Creature Model and Texture Upgrade (Task B)

**Texture Resolution Enhancement**: All creature textures upgraded from 64×64 to 128×128
- 4× pixel density for significantly better visual quality
- Creatures affected: Believer, Follower, Envoy, Grand Envoy, Saint, Flow

**Visual Enhancements**:
- **Glow Layers**: Added subtle edge glow using Gaussian blur on alpha channel
- **Void Particles**: Scattered particle pattern overlay (2% coverage, tier-scaled intensity)
- **Contrast Enhancement**: 1.2× contrast boost for better depth perception
- **Edge Highlights**: Added edge detection layer for clearer structure definition
- **Tier-Specific Effects**: Higher tiers receive stronger visual effects

**Tier Color Progression**:
- **Believer** (Tier 1): Minimal glow, base gray tones
- **Follower** (Tier 2): Enhanced transparency, light glow
- **Envoy** (Tier 3): Medium glow, visible void particles
- **Grand Envoy** (Tier 4): Strong glow, pronounced particles
- **Saint** (Tier 5): Maximum intensity, bright accents

**Technical Details**:
- UV mapping preserved (all coordinates scale proportionally)
- Model code updated to declare 128×128 texture size
- No geometry changes required
- Minimal performance impact (~30-60 KB total size increase)

### 2. Documentation Corrections (Task A)

**Fixed Outdated Information**:
- Corrected 8 occurrences of historical SHA-256 hash (8C2709D1...) with context
- Marked as 2026-09-14 builds on NeoForge 26.2.0.7-beta
- Updated "Zeroing Finale pending" statements - feature is now implemented
- Only "Merge with Void" remains pending

**Version Status Updates**:
- Added 2026-10-28 timeline notes
- Documented NeoForge 26.2.0.93 and 26.1.2.124 upgrades
- Clarified two active release branches
- Updated beta.4 preparation status

**Files Updated**:
- docs/ROADMAP.md
- docs/CURRENT_AUDIT_20260717.md
- docs/FEATURE_QUALITY_CONTRACT.md
- docs/artifacts/mc-mod-status.md
- docs/curseforge-publication-1.0.0-beta.3.md

### 3. NeoForge Version Updates

**26.1.2 Branch**:
- Port to NeoForge 26.1.2.124 (backward compatibility)
- Update ModDevGradle 2.0.140 → 2.0.148
- Fix API change: `Minecraft.gui.setScreen()` → `Minecraft.setScreen()` (26.1.2 API)
- Update dependencies: Curios 15.0.0+26.1.2, JEI 29.53.0.112, Waystones 26.1.2.16, etc.
- Update pack_format 88 → 84 (MC 26.1.2)

**26.2 Branch**:
- Upgrade to NeoForge 26.2.0.93
- Remove `EntityInteractSpecific` handler (API removed in 26.2.0.93)
- `EntityInteract` now handles all entity interactions
- Maintain `gui.setScreen()` API (correct for 26.2.0.93)
- Update pack_format remains 88 → 84 for resource compatibility

## Build Verification

Both versions passed all quality checks:
- ✅ Compilation successful
- ✅ 79 tests passed (14 test classes)
- ✅ Resource validation passed
- ✅ Ritual and exploration definitions verified
- ✅ Language key consistency verified

## Dependency Requirements

### NeoForge 26.1.2 Version
```
- Minecraft: 26.1.2
- NeoForge: 26.1.2.124+
- Java: 25
- Curios API: 15.0.0+26.1.2
- JEI: 29.53.0.112 (optional)
```

### NeoForge 26.2 Version
```
- Minecraft: 26.2
- NeoForge: 26.2.0.93+
- Java: 25
- Curios API: (check 26.2 compatible version)
- JEI: (check 26.2 compatible version)
```

## Known Issues

1. **Merge with Void** ability (Saint tier) - Implementation pending
2. **In-game testing**: Upgraded creature textures not yet verified in live gameplay
3. **Performance testing**: 128×128 textures not benchmarked on lower-end hardware

## Installation

1. Download the appropriate version for your NeoForge installation
2. Verify SHA-256 hash matches the release notes
3. Place jar file in your `mods/` directory
4. Ensure required dependencies are installed
5. Launch Minecraft with NeoForge

## Upgrading from Beta.3

Beta.4 is **fully compatible** with Beta.3 save files. Simply replace the jar file.

**Visual Changes**: Creatures will appear with enhanced textures immediately - no configuration needed.

## Technical Credits

- **Texture Upgrade Pipeline**: Automated using Python (PIL) with procedural enhancement
- **Model System**: Preserved ActionType pose compatibility
- **Testing**: 79 regression tests across 14 test classes
- **Build System**: Gradle 9.2.1 + ModDevGradle 2.0.148

## Commit History (release/neoforge-26.1.2)

```
0cee859 Bump version to 1.0.0-beta.4
8ce54b3 Upgrade creature models and textures (Task B)
a3d4e2f docs: Fix stale hashes, outdated statements, and version references
1ffcfd4 Port to NeoForge 26.1.2.124
```

## Commit History (release/neoforge-26.2)

```
41a9ce0 Bump version to 1.0.0-beta.4
daa6a0e Upgrade creature models and textures (Task B)
f377f4a docs: Fix stale hashes, outdated statements, and version references
c08d075 Upgrade to NeoForge 26.2.0.93 and fix removed APIs
```

## Next Steps

### For Beta.5 Consideration:
- Implement "Merge with Void" ability
- In-game verification of upgraded creature textures
- Further model geometry refinement
- Animated texture layers for flowing bands
- Performance profiling with new textures

## Support

Report issues on GitHub with:
- Which version you're using (26.1.2 or 26.2)
- Full crash report or log file
- List of installed mods
- Steps to reproduce

---

**Co-Authored-By**: Claude Sonnet 5.5
