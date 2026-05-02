#!/usr/bin/env node
/**
 * MythicSkyWars Feature Scraper
 * Scans the Java codebase and extracts commands, permissions, placeholders,
 * listeners, managers, menus, events, and config files.
 *
 * Usage: node scripts/scrape-features.js
 */

const fs = require("fs");
const path = require("path");

const SRC_DIR = path.join(__dirname, "..", "SkyWarsReloadedCore", "src", "main", "java");
const RES_DIR = path.join(__dirname, "..", "SkyWarsReloadedCore", "src", "main", "resources");
const BASE_PKG = path.join(SRC_DIR, "com", "walrusone", "skywarsreloaded");

// ─── Helpers ───────────────────────────────────────────────────────────────────

function walkDir(dir, ext = ".java") {
  let results = [];
  if (!fs.existsSync(dir)) return results;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      results = results.concat(walkDir(full, ext));
    } else if (entry.name.endsWith(ext)) {
      results.push(full);
    }
  }
  return results;
}

function readFile(filePath) {
  return fs.readFileSync(filePath, "utf8");
}

function heading(title) {
  console.log(`\n${"=".repeat(60)}`);
  console.log(` ${title}`);
  console.log("=".repeat(60));
}

// ─── 1. Registered Commands (plugin.yml) ───────────────────────────────────────

heading("REGISTERED COMMANDS (plugin.yml)");
const pluginYml = readFile(path.join(RES_DIR, "plugin.yml"));
const cmdMatches = pluginYml.match(/^  (\w+):\s*$/gm) || [];
const registeredCmds = [];
let inCommands = false;
for (const line of pluginYml.split("\n")) {
  if (line.startsWith("commands:")) { inCommands = true; continue; }
  if (inCommands && /^[a-z]/.test(line) && !/^\s/.test(line)) { inCommands = false; }
  if (inCommands) {
    const m = line.match(/^\s{2}(\w+):/);
    if (m) {
      registeredCmds.push(m[1]);
      console.log(`  /${m[1]}`);
    }
  }
}

// ─── 2. Subcommands (from BaseCmd classes) ─────────────────────────────────────

heading("SUBCOMMANDS (from command classes)");
const cmdFiles = walkDir(path.join(BASE_PKG, "commands"));
const subcommands = [];
for (const file of cmdFiles) {
  const content = readFile(file);
  const cmdMatch = content.match(/cmdName\s*=\s*"([^"]+)"/);
  if (!cmdMatch) continue;
  const cmd = cmdMatch[1];
  const dir = path.basename(path.dirname(file));
  const permMatch = content.match(/hasPermission\("(sw\.[^"]+)"\)/);
  const perm = permMatch ? permMatch[1] : "";
  subcommands.push({ cmd, dir, perm });
  console.log(`  /sw ${cmd}  (${dir})${perm ? `  [${perm}]` : ""}`);
}

// ─── 3. Permissions (plugin.yml) ───────────────────────────────────────────────

heading("PERMISSIONS (plugin.yml)");
const ymlPerms = [];
let inPerms = false;
for (const line of pluginYml.split("\n")) {
  if (line.startsWith("permissions:")) { inPerms = true; continue; }
  if (inPerms) {
    const m = line.match(/^\s{2}(sw\.[^:]+):/);
    if (m) {
      ymlPerms.push(m[1]);
      console.log(`  ${m[1]}`);
    }
  }
}

// ─── 4. Permissions (from Java hasPermission calls) ────────────────────────────

heading("PERMISSIONS (from Java code)");
const allJavaFiles = walkDir(SRC_DIR);
const codePerms = new Set();
for (const file of allJavaFiles) {
  const content = readFile(file);
  const matches = content.matchAll(/hasPermission\("(sw\.[^"]+)"\)/g);
  for (const m of matches) {
    codePerms.add(m[1]);
  }
}
[...codePerms].sort().forEach((p) => console.log(`  ${p}`));

// ─── 5. Placeholders (PlaceholderAPI) ──────────────────────────────────────────

heading("PLACEHOLDERS (PlaceholderAPI)");
const placeholders = new Set();
for (const file of allJavaFiles) {
  const content = readFile(file);
  if (!content.includes("PlaceholderExpansion") && !content.includes("onPlaceholderRequest") && !content.includes("onRequest")) continue;

  // Match case "xxx": patterns
  for (const m of content.matchAll(/case\s+"([^"]+)"/g)) {
    const ph = m[1];
    if (ph && !["true", "false", "null", "player"].includes(ph)) {
      placeholders.add(ph);
    }
  }
  // Match equalsIgnoreCase("xxx") patterns
  for (const m of content.matchAll(/equalsIgnoreCase\("([^"]+)"\)/g)) {
    const ph = m[1];
    if (ph && !["true", "false", "null", "player"].includes(ph)) {
      placeholders.add(ph);
    }
  }
  // Match equals("xxx") in placeholder context
  for (const m of content.matchAll(/\.equals\("([^"]+)"\)/g)) {
    const ph = m[1];
    if (ph && ph.startsWith("swr_") || ph.match(/^(wins|losses|kills|deaths|xp|level|games|elo)/)) {
      placeholders.add(ph);
    }
  }
}
[...placeholders].sort().forEach((p) => console.log(`  %swr_${p}%`));

// ─── 6. Config/Resource Files ──────────────────────────────────────────────────

heading("CONFIG/RESOURCE FILES");
if (fs.existsSync(RES_DIR)) {
  fs.readdirSync(RES_DIR)
    .filter((f) => f.endsWith(".yml"))
    .sort()
    .forEach((f) => console.log(`  ${f}`));
}

// ─── 7. Event Listeners ────────────────────────────────────────────────────────

heading("EVENT LISTENERS");
const listenersDir = path.join(BASE_PKG, "listeners");
if (fs.existsSync(listenersDir)) {
  fs.readdirSync(listenersDir)
    .filter((f) => f.endsWith(".java"))
    .map((f) => f.replace(".java", ""))
    .sort()
    .forEach((f) => console.log(`  ${f}`));
}

// ─── 8. Managers & Utilities ───────────────────────────────────────────────────

heading("MANAGERS & UTILITIES");
const utilsDir = path.join(BASE_PKG, "utilities");
const managersDir = path.join(BASE_PKG, "managers");
const managers = [];
if (fs.existsSync(utilsDir)) {
  managers.push(...fs.readdirSync(utilsDir).filter((f) => f.endsWith(".java")).map((f) => f.replace(".java", "")));
}
if (fs.existsSync(managersDir)) {
  managers.push(...fs.readdirSync(managersDir).filter((f) => f.endsWith(".java")).map((f) => f.replace(".java", "")));
}
managers.sort().forEach((f) => console.log(`  ${f}`));

// ─── 9. Menus/GUIs ────────────────────────────────────────────────────────────

heading("MENUS/GUIs");
const menusDir = path.join(BASE_PKG, "menus");
if (fs.existsSync(menusDir)) {
  walkDir(menusDir)
    .map((f) => path.relative(menusDir, f).replace(/\\/g, "/").replace(".java", ""))
    .sort()
    .forEach((f) => console.log(`  ${f}`));
}

// ─── 10. API Events ───────────────────────────────────────────────────────────

heading("API EVENTS");
const eventsDir = path.join(BASE_PKG, "events");
if (fs.existsSync(eventsDir)) {
  fs.readdirSync(eventsDir)
    .filter((f) => f.endsWith(".java"))
    .map((f) => f.replace(".java", ""))
    .sort()
    .forEach((f) => console.log(`  ${f}`));
}

// ─── Summary ──────────────────────────────────────────────────────────────────

heading("SUMMARY");
console.log(`  Commands:     ${registeredCmds.length} registered + ${subcommands.length} subcommands`);
console.log(`  Permissions:  ${ymlPerms.length} (plugin.yml) + ${codePerms.size} (code)`);
console.log(`  Placeholders: ${placeholders.size}`);
console.log(`  Listeners:    ${fs.existsSync(listenersDir) ? fs.readdirSync(listenersDir).filter((f) => f.endsWith(".java")).length : 0}`);
console.log(`  Managers:     ${managers.length}`);
console.log(`  Menus:        ${fs.existsSync(menusDir) ? walkDir(menusDir).length : 0}`);
console.log(`  Events:       ${fs.existsSync(eventsDir) ? fs.readdirSync(eventsDir).filter((f) => f.endsWith(".java")).length : 0}`);
console.log(`  Config files: ${fs.existsSync(RES_DIR) ? fs.readdirSync(RES_DIR).filter((f) => f.endsWith(".yml")).length : 0}`);
console.log("");
