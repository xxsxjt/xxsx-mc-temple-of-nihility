# CurseForge 使用记录

本文件记录 Temple of Nihility 及本机相关项目在 CurseForge 上的全部实际操作、结果与已知问题。按时间倒序。只记录可核对的事实；未验证的状态单独标注。

最后更新：2026-10-29

---

## 一、项目身份

| 项目 | 值 |
|---|---|
| CurseForge 项目 | Temple of Nihility（虚无圣殿） |
| Project ID | `1616748` |
| Authors 文件管理页 | `https://authors.curseforge.com/#/projects/1616748/files` |
| 游戏 | Minecraft（gameId `432`） |
| 类别 | Mods（classId `6`） |
| 必需依赖 | Curios API |
| 可选依赖 | Just Enough Items (JEI)、Waystones |

---

## 二、时间线

### 2026-10-29 — beta.4 发布尝试：读通了，写不通

**结果：未上传。** 阻断点不在凭据，在写入通道。

第一轮（旧 token）确实是被拒：`GET /api/game/versions` 返回 `403 Invalid API token provided`。但随后 token 被换新，状态改变：

| 检查 | 请求 | 结果 |
|---|---|---|
| 读能力 | `GET https://minecraft.curseforge.com/api/game/versions`，带 `X-Api-Token` | `200`，7,629 条版本记录 |
| 写能力 | 任意 `POST` 到 `minecraft.curseforge.com`（含 `POST /api/game/versions`） | `403`，Cloudflare "Just a moment..." JS 挑战页 |
| Authors 表单数据 | `GET https://authors.curseforge.com/_api/project-files/1616748/create-project-file-form-data?gameId=432&classId=6` | `401 Unauthorized` |
| Studios API | `GET https://api.curseforge.com/v1/mods/1616748`，带 `x-api-key` | `403` |

Authors `_api` 的 401 是**凭据能力边界，不是项目权限问题**：对一个几个月前上传成功过的无关项目请求同一端点，同样返回 401。Authors 的上传 token 不是 Studios `/v1` 的 key。

响应码必须区分，不能一律归成"token 无效"：

| 响应 | 含义 |
|---|---|
| `400 {"errorCode":3,"errorMessage":"API token is malformed..."}` | 值本身格式不对 |
| `403 {"errorCode":403,"errorMessage":"Invalid API token provided."}` | 已认证，但该方法被拒 |
| `401` 且提示必须提供 `X-Api-Token` | 根本没带 token |
| `403` 且正文是 "Just a moment..." | Cloudflare 挑战，不是 CurseForge 的判定 |

因为 POST 被 Cloudflare 拦，实际可行的路径是**驱动已登录的 Authors 网页会话**，流程见 `D:/Codex/WORKSPACES/Projects/mc-agent/memory/curseforge-authors-upload.md` 的 "Working fallback" 一节。

产物已就绪（哈希为 2026-10-29 重新计算）：

| 目标 | 文件 | 大小 | SHA-256 |
|---|---|---:|---|
| MC 26.1.2 / NeoForge 26.1.2.124 | `temple-of-nihility-neoforge-26.1.2-1.0.0-beta.4.jar` | 1,753,399 B | `25dbce8952acb5d062d0c00eb44337d1ddcd7685fb43dde2350266b621e8a3a8` |
| MC 26.2 / NeoForge 26.2.0.93 | `temple-of-nihility-neoforge-26.2-1.0.0-beta.4.jar` | 1,753,277 B | `f7e142661a8b7b77d9a4015a3296f278715379edac38484ff1dd31a855c0d31b` |

详细记录见 `docs/curseforge-publication-1.0.0-beta.4.md`。

### 2026-07-20 — beta.3 上传成功（唯一一次真实上传）

| 项目 | 值 |
|---|---|
| 上传时间 | `2026-07-20 00:55 +08:00` |
| 返回文件 ID | `8466225` |
| 上传文件 | `temple-of-nihility-neoforge-26.2-1.0.0-beta.3.jar` |
| 目标 | Minecraft 26.2 / NeoForge 26.2.0.7-beta / Java 25 |
| 上传产物 SHA-256 | `499C5A505D0DC4FF20DE6C61674B6A34D94C70F350D88CC9F5A5936C899BEF77` |
| 显示名 | `Temple of Nihility 1.0.0-beta.3 TEST - MC 26.2 NeoForge` |
| 发布类型 | `beta` |
| 手动发布 | 提交时启用 |

提交时使用的项目表单版本 ID（**注意：来自当时表单，不可直接复用到新上传**）：

| 表单项 | ID |
|---|---:|
| Minecraft 26.2 | 16498 |
| Java 25 | 14454 |
| NeoForge | 10150 |
| Client | 9638 |
| Server | 9639 |

验证状态：

- Upload API 接受产物并返回文件 ID `8466225`；版本与依赖关系元数据通过上传校验。
- 2026-09-14 复查时，公开列表可见该文件，标记为 Minecraft 26.2 / NeoForge 的 beta 文件。
- Authors 后台的审核 / 手动发布状态，以及一次真实的直接下载，**未验证**：当时对文件页与下载端点的请求返回 `403`。
- 该文件上传的是 `499C5A50...`，与后来本地 clean build 的 `8C2709D1...` 不是同一个二进制。没有做过替换上传，远端二进制哈希从未复核。

详细记录见 `docs/curseforge-publication-1.0.0-beta.3.md`。

### 2026-07-18 — 另一项目首次跑通 Upload API 流程

在 Xxsx Smart Core v1.1.11（MC 26.2 / NeoForge）上首次完整跑通上传流程，由此沉淀出可复用的操作规则（见第四节）。

| 项目 | 值 |
|---|---|
| Project ID | `1599752` |
| 文件 ID | `8456858` |
| 产物 | `xxsx-mc-smart-core-neoforge-26.2-1.1.11.jar` |
| 发布类型 | Beta，`isMarkedForManualRelease=true` |
| SHA-256 | `C36A0C86731000CF10A8CBCB813617A427F9F27D939B9800A3FEBFD662E809E0` |

---

## 三、凭据状况

token **不在** Windows 凭据管理器里（`cmdkey /list` 与 `CredRead` 都查不到 `curseforge_codex_api_token`）。曾按"Windows Credential Manager"方向排查，是找错了地方。

实际位置：

- 共享加密凭据库：`D:/Users/xxsx/AppData/Local/RuntimeData`
- 别名：`curseforge_codex_api_token`
- 元数据：`D:/Users/xxsx/AppData/Local/RuntimeData/metadata/curseforge_codex_api_token.json`（只含别名、用途、状态、允许的操作，不含值）
- 密文：`D:/Users/xxsx/AppData/Local/RuntimeData/vault/credentials.enc.json`（AES-256-GCM，数据密钥由当前用户 DPAPI 包装）

读取方式（只在本机 resolver 内部使用，禁止打印值）：

```powershell
node D:/Users/xxsx/AppData/Local/RuntimeData/tools/vault-runtime.cjs resolve curseforge_codex_api_token
```

当前状态（2026-10-29 复核）：token 已被换新，`GET /api/game/versions` 返回 `200`，**读能力正常**。元数据里记为 `validated-authors-readonly`，允许的操作仅四条只读 GET。写入通道被 Cloudflare 拦（见第二节），因此仍不能完成上传。

操作边界：不要把 `resolve` 的输出导进 shell 变量、日志或任何模型可读的流——那样等于让凭据库失效。

---

## 四、可复用的操作规则（来自 2026-07-18 实测）

1. 项目描述走 Authors 网页编辑器；Upload API 只改文件元数据，改不了项目描述。
2. Authors 编辑器对中文输入不稳定时，宁可发布纯英文描述源，也不要发布中英混杂或乱码的版本。
3. 上传前先构建并对**确切的那个 jar** 取哈希；一次上传只对应一个 jar、一个 MC 目标、一个 loader、一个发布类型。
4. token 只放在 `X-Api-Token` 请求头里。不打印、不写进 URL、不进日志。
5. **不要用 `GET /api/game/versions` 的结果当作项目表单 ID。** 必须读 Authors 的 `/_api/project-files/{projectId}/create-project-file-form-data?gameId={gameId}&classId={classId}`，用其中的 `versionsData` 当前选项。
6. `metadata` 作为 UTF-8 文本 JSON 提交，jar 作为 `file` 字段，multipart 表单。客户端文件需要带齐表单给出的 Minecraft、Modloader、Client 三个 ID。
7. 上传后用 `/_api/project-files/{projectId}/files/{fileId}` 复核：jar 名、显示名、发布类型、全部已选版本、手动发布标记、状态。

失败处理：

- `400` 且报游戏版本 / 依赖 ID 无效时，不要盲目重试。先安全地看有界的错误响应体，确认作者文件列表里没有多出文件，再把全局目录 ID 换成项目表单 ID。
- 手动发布文件在审核通过且作者手动放行前不会出现在公开列表，公开 URL 返回 `404` 属预期。此时只能报告"已提交/待处理"，不能说"已公开"。
- 浏览器保存可能超时但其实已经保存。不要立刻再点一次，先看 Save 是否已变灰、页面是否已渲染、或直接查作者端点。

---

## 五、本仓库内的相关文件

| 文件 | 用途 |
|---|---|
| `docs/curseforge-publication-1.0.0-beta.3.md` | beta.3 上传记录（文件 ID `8466225`） |
| `docs/curseforge-publication-1.0.0-beta.4.md` | beta.4 上传受阻记录与待执行的命令 |
| `docs/curseforge-changelog-1.0.0-beta.3.md` | beta.3 上传用的 changelog 正文 |
| `scripts/publish-curseforge.cjs` | 上传脚本：调用时从凭据库取 token，`--dry-run` 只验证不上传 |
| `D:/Codex/WORKSPACES/Projects/mc-agent/memory/curseforge-authors-upload.md` | 跨项目经验卡片（含 token 存储位置、Cloudflare 写阻断、浏览器 fallback 完整步骤） |

---

## 六、待办

1. 用已登录的 Authors 网页会话上传 beta.4（走 `memory/curseforge-authors-upload.md` 的浏览器 fallback；POST API 被 Cloudflare 拦，不要再试）。
2. 上传前从表单当前选项取 Environment / Modloader / Minecraft 的项目表单 ID，不要复用 beta.3 的 `16498 / 14454 / 10150 / 9638 / 9639`。
3. 上传后复核文件 ID、状态、手动发布标记、已选版本、公开可见性；特别检查有没有上一份表单残留的 MC 标签。
4. 决定是否也给 26.1.2 版本建一个文件条目（CurseForge 一个文件只能挂一组版本）。
5. 凭据库元数据已更新为 `validated-authors-readonly`，若之后开通写权限再同步更新允许操作列表。
