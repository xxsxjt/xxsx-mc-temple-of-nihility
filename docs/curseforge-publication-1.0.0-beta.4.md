# CurseForge publication record: Temple of Nihility 1.0.0-beta.4

## Status: published

| Item | Value |
|---|---|
| Project ID | `1616748` |
| File ID | `9363296` |
| Display name | `Temple of Nihility 1.0.0-beta.4 - MC 26.2 NeoForge` |
| Release type | Beta |
| Status | Approved |
| Uploaded | 2026-10-29 |
| Public page | `https://www.curseforge.com/minecraft/mc-mods/temple-of-nihility/files/9363296` |
| CDN download | `https://edge.forgecdn.net/files/9363/296/temple-of-nihility-neoforge-26.2-1.0.0-beta.4.jar` |

## Artifact

- File: `temple-of-nihility-neoforge-26.2-1.0.0-beta.4.jar`
- Size: 1,753,277 B
- SHA-256: `f7e142661a8b7b77d9a4015a3296f278715379edac38484ff1dd31a855c0d31b`
- Target: Minecraft 26.2 / NeoForge 26.2.0.93 / Java 25

The published binary was downloaded back from the CDN after upload and its SHA-256 matches the local build exactly, so the remote artifact is verified, not assumed.

## Submitted metadata

- Environment: Client
- Modloader: NeoForge
- Java: 25
- Minecraft: 26.2
- Release type: Beta
- Changelog: `docs/RELEASE-1.0.0-beta.4.md`, converted to HTML and pasted into the Authors rich-text editor (7954 characters)

Dependencies carried over from the project defaults:

| Project | Type |
|---|---|
| Curios API | Required |
| Waystones | Optional |
| Just Enough Items (JEI) | Optional |

## Why the API path could not be used

The Upload API token is valid for reads (`GET /api/game/versions` returns `200`), but every `POST` to `minecraft.curseforge.com` is answered by a Cloudflare JS challenge, and the Authors `_api` endpoints return `401` for a token. The working path is the authenticated browser session.

Upload mechanics that actually worked, for reuse:

1. Open `#/projects/1616748/files/create` in a signed-in Authors session.
2. The "Choose File" control is a `<button>`; there is no usable `input[type=file]`. Walk the React fiber from that button up to depth 14 to reach `memoizedProps.onFilesDropped`, then call it with a real `File` built from an `arrayBuffer` fetched off `127.0.0.1` (a `file://` URL will not work).
3. `onFilesDropped` fills the display name and sets Environment / Modloader / Java / Minecraft chips automatically from the jar's metadata. Read them back rather than trusting the chips blindly.
4. Release Type defaults to `Release`; switch it to `Beta` via the combobox.
5. Submit through the `Add File` button's `__reactProps.onClick`. The upload posts to `/_api/project-files/{projectId}/upload-file-details` and `/_api/project-files/upload-file-chunk`, not to `/api/projects/{id}/uploadFile`.
6. Wait for status to move from `Uploading` to `Under Review` to `Approved`.
7. Edit the file afterwards (`#/projects/1616748/files/{fileId}`) to fix the display name and add the changelog. Set the changelog through `window.tinymce.get(0).setContent(html)`; that marks the form dirty so `Submit` becomes enabled.

## Not yet done

- The 26.1.2 build (`temple-of-nihility-neoforge-26.1.2-1.0.0-beta.4.jar`, SHA-256 `25dbce8952acb5d062d0c00eb44337d1ddcd7685fb43dde2350266b621e8a3a8`) has no CurseForge file entry yet. A file can only carry one set of game versions, so it needs a separate upload if it should be published there too.
- The project is currently unlisted ("only viewable for anyone who has the link"), per the Authors files page notice. Changing that is a project-level setting, not a file-level one.
