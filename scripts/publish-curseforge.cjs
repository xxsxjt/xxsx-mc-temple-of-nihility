"use strict";

/**
 * CurseForge upload helper for Temple of Nihility.
 *
 * The API token is read from the shared local vault
 * (D:/Users/xxsx/AppData/Local/RuntimeData) through vault-runtime.cjs resolve.
 * The value is never printed, logged, or written to disk.
 *
 * Usage:
 *   node scripts/publish-curseforge.cjs --dry-run
 *   node scripts/publish-curseforge.cjs --jar <path> --changelog <path>
 *       --display-name "<name>" --release-type beta
 *       --game-version-id <id> [--game-version-id <id> ...]
 *       --mod-loader-id <id> --client-id <id> [--server-id <id>]
 *
 * Exit codes: 0 ok, 2 auth rejected, 3 remote validation rejected, 1 local error.
 */

const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");

const VAULT_RUNTIME = "D:/Users/xxsx/AppData/Local/RuntimeData/tools/vault-runtime.cjs";
const TOKEN_ALIAS = "curseforge_codex_api_token";
const AUTHORS_API = "https://authors.curseforge.com/_api";
const GAME_ID = 432;
const CLASS_ID = 6;
const DEFAULT_PROJECT_ID = 1616748;

function fail(message, code = 1) {
  process.stderr.write(`[curseforge] ${message}\n`);
  process.exit(code);
}

function parseArgs(argv) {
  const opts = {
    projectId: DEFAULT_PROJECT_ID,
    releaseType: "beta",
    gameVersionIds: [],
    modLoaderIds: [],
    dryRun: false,
  };
  for (let i = 0; i < argv.length; i++) {
    const arg = argv[i];
    const next = () => {
      const v = argv[++i];
      if (v === undefined) fail(`missing value for ${arg}`);
      return v;
    };
    switch (arg) {
      case "--dry-run":
        opts.dryRun = true;
        break;
      case "--jar":
        opts.jar = next();
        break;
      case "--changelog":
        opts.changelog = next();
        break;
      case "--display-name":
        opts.displayName = next();
        break;
      case "--release-type":
        opts.releaseType = next();
        break;
      case "--project-id":
        opts.projectId = Number(next());
        break;
      case "--game-version-id":
        opts.gameVersionIds.push(Number(next()));
        break;
      case "--mod-loader-id":
        opts.modLoaderIds.push(Number(next()));
        break;
      case "--client-id":
        opts.clientId = Number(next());
        break;
      case "--server-id":
        opts.serverId = Number(next());
        break;
      case "--help":
        process.stdout.write(fs.readFileSync(__filename, "utf8").split("*/")[0] + "*/\n");
        process.exit(0);
        break;
      default:
        fail(`unknown argument ${arg}`);
    }
  }
  return opts;
}

function resolveToken() {
  const result = spawnSync(process.execPath, [VAULT_RUNTIME, "resolve", TOKEN_ALIAS], {
    windowsHide: true,
    stdio: ["ignore", "pipe", "ignore"],
    timeout: 20000,
    maxBuffer: 64 * 1024,
  });
  if (result.error || result.status !== 0 || !result.stdout || !result.stdout.length) {
    fail("vault credential resolution failed (alias " + TOKEN_ALIAS + ")");
  }
  const token = result.stdout.toString("utf8").replace(/[\r\n]+$/, "");
  result.stdout.fill(0);
  if (!token || /\s/.test(token)) fail("resolved credential is malformed");
  return token;
}

async function fetchFormData(token, projectId) {
  const url = `${AUTHORS_API}/project-files/${projectId}/create-project-file-form-data?gameId=${GAME_ID}&classId=${CLASS_ID}`;
  const res = await fetch(url, { headers: { "X-Api-Token": token, Accept: "application/json" } });
  const text = await res.text();
  if (res.status === 401 || res.status === 403) {
    fail(
      `CurseForge rejected the stored token (HTTP ${res.status}). ` +
        "Regenerate it at https://authors.curseforge.com/#/settings/api-tokens and re-store it in the vault.",
      2
    );
  }
  if (!res.ok) fail(`form-data request failed: HTTP ${res.status} ${text.slice(0, 300)}`, 3);
  try {
    return JSON.parse(text);
  } catch {
    fail("form-data response was not JSON", 3);
  }
}

function printVersionChoices(data) {
  const groups = data.versionsData || data.VersionsData;
  if (!Array.isArray(groups)) {
    process.stdout.write("unexpected form-data shape: " + Object.keys(data).join(",") + "\n");
    return;
  }
  for (const group of groups) {
    const label = group.gameVersion || group.name || group.type || "group";
    const values = group.versions || group.values || [];
    process.stdout.write(`${label}:\n`);
    for (const v of values) process.stdout.write(`  ${v.id}\t${v.name}\n`);
  }
}

async function upload(token, opts, metadata, jarBytes, jarName) {
  const form = new FormData();
  form.append("metadata", new Blob([JSON.stringify(metadata)], { type: "application/json" }), "metadata.json");
  form.append("file", new Blob([jarBytes], { type: "application/java-archive" }), jarName);

  const res = await fetch(`https://minecraft.curseforge.com/api/projects/${opts.projectId}/uploadFile`, {
    method: "POST",
    headers: { "X-Api-Token": token },
    body: form,
  });
  const text = await res.text();
  if (!res.ok) fail(`upload rejected: HTTP ${res.status} ${text.slice(0, 600)}`, 3);
  let body;
  try {
    body = JSON.parse(text);
  } catch {
    fail("upload response was not JSON: " + text.slice(0, 300), 3);
  }
  return body;
}

async function verify(token, projectId, fileId) {
  const res = await fetch(`${AUTHORS_API}/project-files/${projectId}/files/${fileId}`, {
    headers: { "X-Api-Token": token, Accept: "application/json" },
  });
  const text = await res.text();
  if (!res.ok) {
    process.stdout.write(`verify: HTTP ${res.status} (file id ${fileId} was returned by the upload API)\n`);
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    process.stdout.write("verify: non-JSON response\n");
    return null;
  }
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  const token = resolveToken();

  const formData = await fetchFormData(token, opts.projectId);
  if (opts.dryRun) {
    process.stdout.write("dry run: token accepted, form data retrieved\n\n");
    printVersionChoices(formData);
    return;
  }

  if (!opts.jar) fail("--jar is required unless --dry-run is used");
  const jarPath = path.resolve(opts.jar);
  if (!fs.existsSync(jarPath)) fail(`jar not found: ${jarPath}`);
  const jarName = path.basename(jarPath);
  if (!opts.displayName) opts.displayName = jarName.replace(/\.jar$/i, "");
  if (!opts.gameVersionIds.length) fail("at least one --game-version-id is required");
  if (!opts.modLoaderIds.length) fail("at least one --mod-loader-id is required");
  if (!opts.clientId) fail("--client-id is required (CurseForge form environment id)");

  let changelog = "";
  if (opts.changelog) {
    const p = path.resolve(opts.changelog);
    if (!fs.existsSync(p)) fail(`changelog not found: ${p}`);
    changelog = fs.readFileSync(p, "utf8");
  }

  const metadata = {
    changelog,
    changelogType: "markdown",
    displayName: opts.displayName,
    releaseType: opts.releaseType,
    gameVersions: [...opts.gameVersionIds, ...opts.modLoaderIds, opts.clientId, opts.serverId].filter(Boolean),
    isMarkedForManualRelease: true,
  };

  process.stdout.write(`uploading ${jarName} (${fs.statSync(jarPath).size} bytes)\n`);
  process.stdout.write(`  project ${opts.projectId}, release ${opts.releaseType}\n`);
  process.stdout.write(`  versions ${metadata.gameVersions.join(",")}\n`);

  const jarBytes = fs.readFileSync(jarPath);
  const result = await upload(token, opts, metadata, jarBytes, jarName);
  const fileId = result.id ?? result.fileId;
  process.stdout.write(`upload accepted, file id ${fileId}\n`);

  if (fileId) {
    const record = await verify(token, opts.projectId, fileId);
    if (record) {
      const picked = {
        id: record.id,
        displayName: record.displayName,
        fileName: record.fileName,
        releaseType: record.releaseType,
        fileStatus: record.fileStatus,
        isMarkedForManualRelease: record.isMarkedForManualRelease ?? record.isManualRelease,
        gameVersions: record.gameVersions,
      };
      process.stdout.write(JSON.stringify(picked, null, 2) + "\n");
    }
  }
}

main().catch((e) => fail("publish failed: " + e.message));
