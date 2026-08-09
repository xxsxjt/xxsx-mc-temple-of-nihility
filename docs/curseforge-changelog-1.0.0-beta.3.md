> **IN-DEVELOPMENT TEST BUILD**
>
> This beta targets Minecraft 26.2 with NeoForge 26.2.0.7-beta and Java 25. Back up your worlds before testing. Content, balance, structures, and save formats may still change.

## Highlights

- Added temple complexes with distinct functional areas, ritual progression, neutral guardians, trading, and the first Saint challenge loop.
- Added dedicated creature combat behavior, including Envoy projectile deflection, Grand Envoy domain control, Follower projectile defense, and Saint-only Erasure Line and Missing Zone abilities.
- Added the Codex of Nihility with 21 in-game guide pages covering progression, structures, rituals, storage, transport, creatures, and challenges.
- Expanded the Nihility Vault network, terminal storage, energy, item/fluid/energy transport, Curios equipment, and optional JEI and Waystones integrations.
- Added the new Temple of Nihility project icon and expanded bilingual English/Chinese localization.
- Fixed all seven Nihility ritual JSON files failing to decode on NeoForge 26.2 by resolving item components only after registries are available.

## Verification

- Clean Gradle build completed successfully.
- 74 automated tests passed with no failures.
- Resource, language, ritual, exploration, command, and packaged-jar validation passed.

Runtime verification is still incomplete for multiplayer behavior, every UI scale, all particle and rendering paths, natural structure generation, optional mod integrations, and migration from older test worlds.

## Dependencies

- Required: Curios API
- Optional: Just Enough Items (JEI), Waystones
