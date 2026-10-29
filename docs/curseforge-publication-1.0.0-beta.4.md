# CurseForge publication record: Temple of Nihility 1.0.0-beta.4

## Status

**Not uploaded.** The upload attempt is blocked by a rejected credential, not by the artifact.

Checked 2026-10-29:

| Check | Endpoint | Result |
|---|---|---|
| Upload API token | `GET https://minecraft.curseforge.com/api/game/versions` with `X-Api-Token` | `403 {"errorCode":403,"errorMessage":"Invalid API token provided."}` |
| Authors form data | `GET https://authors.curseforge.com/_api/project-files/1616748/create-project-file-form-data` | `401 Unauthorized` |

The token stored in the shared local vault (alias `curseforge_codex_api_token`, `D:/Users/xxsx/AppData/Local/RuntimeData`) is a 36-character UUID-shaped value. CurseForge does not accept it. Regenerate the token at the Authors API-token settings page and re-store it in the vault; the value must never be printed, logged, or written into this repository.

## Artifacts ready to upload

| Target | File | Size | SHA-256 |
|---|---|---:|---|
| Minecraft 26.1.2 / NeoForge 26.1.2.124 | `temple-of-nihility-neoforge-26.1.2-1.0.0-beta.4.jar` | 1,753,399 B | `25dbce8952acb5d062d0c00eb44337d1ddcd7685fb43dde2350266b621e8a3a8` |
| Minecraft 26.2 / NeoForge 26.2.0.93 | `temple-of-nihility-neoforge-26.2-1.0.0-beta.4.jar` | 1,753,277 B | `f7e142661a8b7b77d9a4015a3296f278715379edac38484ff1dd31a855c0d31b` |

Both hashes were recomputed from `build/libs` on 2026-10-29 and match the values published in the GitHub releases and in `docs/RELEASE-1.0.0-beta.4.md`.

## Project identity

- Project ID: `1616748`
- Previous file: `8466225` (1.0.0-beta.3, Minecraft 26.2)
- Changelog source: `docs/RELEASE-1.0.0-beta.4.md`

## Upload command (after the token is replaced)

```powershell
node scripts/publish-curseforge.cjs --dry-run
```

`--dry-run` validates the token and prints the current project-form version IDs without uploading anything. Do not reuse the IDs from the beta.3 record blindly: the memory card requires reading the IDs from the Authors form data, because global catalog IDs and project-form IDs are not interchangeable.

Then:

```powershell
node scripts/publish-curseforge.cjs `
  --jar build/libs/temple-of-nihility-neoforge-26.2-1.0.0-beta.4.jar `
  --changelog docs/RELEASE-1.0.0-beta.4.md `
  --display-name "Temple of Nihility 1.0.0-beta.4 - MC 26.2 NeoForge" `
  --release-type beta `
  --game-version-id <26.2 id> --mod-loader-id <NeoForge id> --client-id <client id>
```

The script reads the token from the vault at call time, sends it only in the `X-Api-Token` header, and prints the returned file ID plus the verified Authors record.

## Verification still required after upload

- Returned file ID and `fileStatus`
- `isMarkedForManualRelease` flag
- Selected game versions on the Authors file record
- Public listing visibility (a manual-release file stays non-public until approval plus manual release; a public `404` in that window is expected)
