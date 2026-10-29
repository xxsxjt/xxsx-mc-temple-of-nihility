# CurseForge publication record: Temple of Nihility 1.0.0-beta.4

## Status

**Not uploaded.** The Upload API token now works for reads, but every write path is blocked.

Checked 2026-10-29 with the replacement token stored in the shared vault (alias `curseforge_codex_api_token`):

| Check | Endpoint | Result |
|---|---|---|
| Token read capability | `GET https://minecraft.curseforge.com/api/game/versions` with `X-Api-Token` | `200`, 7,629 version entries |
| Write capability | `POST` to any `minecraft.curseforge.com` endpoint, including `POST /api/game/versions` | `403` Cloudflare "Just a moment..." JS challenge |
| Authors form data | `GET https://authors.curseforge.com/_api/project-files/1616748/create-project-file-form-data` | `401 Unauthorized` |
| Studios API | `GET https://api.curseforge.com/v1/mods/1616748` with `x-api-key` | `403` |

The Authors `_api` 401 is a token-capability boundary, not a project-permission problem: the same 401 reproduces on an unrelated project that uploaded successfully months earlier. The Authors upload token is not a Studios `/v1` key.

Diagnostic codes are distinguishable and must not be collapsed:

| Response | Meaning |
|---|---|
| `400 {"errorCode":3,"errorMessage":"API token is malformed..."}` | the value itself is malformed |
| `403 {"errorCode":403,"errorMessage":"Invalid API token provided."}` | authenticated but rejected for this method |
| `401` with "You must provide an API token" | no token was sent |
| `403` HTML titled "Just a moment..." | Cloudflare challenge, not a CurseForge verdict |

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

## How to upload

The API path is closed by the Cloudflare challenge, so use the authenticated browser session. The full procedure is in `D:/Codex/WORKSPACES/Projects/mc-agent/memory/curseforge-authors-upload.md`, section "Working fallback: drive the authenticated Authors page".

Short version:

1. Open `https://authors.curseforge.com/#/projects/1616748/files/create` in a browser session where the account shows **Manage Project**.
2. Feed the jar to the React `onFilesDropped` handler; the visible control is a `<button>`, not a file input, and the native picker cannot be driven.
3. Set Environment / Modloader / Minecraft through the native input value setter plus a real click on the `[role=option]`, then read `gameVersions` back from the fiber state before submitting.
4. Submit via the `Add File` button's `__reactProps.onClick` if the DOM button stays disabled.
5. Re-open `#/projects/1616748/files/{fileId}` afterwards to verify the tags; a stale chip from a previous form is the usual cause of wrong MC tags.

Do not reuse the beta.3 version IDs (`16498` / `14454` / `10150` / `9638` / `9639`) blindly. The memory card requires reading the current project-form IDs, because global catalog IDs and project-form IDs are not interchangeable.

`node scripts/publish-curseforge.cjs --dry-run` still works and confirms the token is live; it exits 5 if the upload POST is Cloudflare-challenged.

## Verification still required after upload

- Returned file ID and `fileStatus`
- `isMarkedForManualRelease` flag
- Selected game versions on the Authors file record
- Public listing visibility (a manual-release file stays non-public until approval plus manual release; a public `404` in that window is expected)
