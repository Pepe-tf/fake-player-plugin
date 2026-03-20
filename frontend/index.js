const express = require("express");
const cors = require("cors");
const fs = require("fs");
const path = require("path");
const yaml = require("js-yaml");
const fetch = require("node-fetch");
const semver = require("semver");

const app = express();
const port = process.env.PORT || 3000;

app.use(cors());

function findProjectRoot(startDir) {
  if (process.env.PLUGIN_ROOT) return path.resolve(process.env.PLUGIN_ROOT);
  let cur = path.resolve(startDir);
  for (let i = 0; i < 8; i++) {
    const candidateA = path.join(cur, "src", "main", "resources", "plugin.yml");
    const candidateB = path.join(cur, "pom.xml");
    const candidateC = path.join(cur, "target");
    if (
      fs.existsSync(candidateA) ||
      fs.existsSync(candidateB) ||
      fs.existsSync(candidateC)
    )
      return cur;
    const parent = path.dirname(cur);
    if (parent === cur) break;
    cur = parent;
  }
  return path.resolve(__dirname);
}

function safeReadYaml(filePath) {
  try {
    if (!fs.existsSync(filePath)) return null;
    const content = fs.readFileSync(filePath, "utf8");
    return yaml.load(content);
  } catch (e) {
    return { error: String(e) };
  }
}

const projectRoot = findProjectRoot(__dirname);
console.log("Using project root:", projectRoot);

app.get("/api/status", (req, res) => {
  const pluginYmlPath = path.join(
    projectRoot,
    "src",
    "main",
    "resources",
    "plugin.yml"
  );
  const configYmlPath = path.join(
    projectRoot,
    "src",
    "main",
    "resources",
    "config.yml"
  );
  const botNamesPath = path.join(
    projectRoot,
    "src",
    "main",
    "resources",
    "bot-names.yml"
  );
  const botMessagesPath = path.join(
    projectRoot,
    "src",
    "main",
    "resources",
    "bot-messages.yml"
  );
  const languageEnPath = path.join(
    projectRoot,
    "src",
    "main",
    "resources",
    "language",
    "en.yml"
  );
  const skinsDir = path.join(projectRoot, "src", "main", "resources", "skins");
  const pomPropsPath = path.join(
    projectRoot,
    "target",
    "maven-archiver",
    "pom.properties"
  );
  const targetJarDir = path.join(projectRoot, "target");

  const plugin = safeReadYaml(pluginYmlPath);
  const config = safeReadYaml(configYmlPath);
  const botNames = safeReadYaml(botNamesPath);
  const botMessages = safeReadYaml(botMessagesPath);
  const language = safeReadYaml(languageEnPath);

  let pom = null;
  try {
    if (fs.existsSync(pomPropsPath)) {
      const raw = fs.readFileSync(pomPropsPath, "utf8");
      pom = raw.split("\n").reduce((acc, line) => {
        const idx = line.indexOf("=");
        if (idx > 0)
          acc[line.substring(0, idx).trim()] = line.substring(idx + 1).trim();
        return acc;
      }, {});
    }
  } catch (e) {
    pom = { error: String(e) };
  }

  let jar = null;
  try {
    if (fs.existsSync(targetJarDir)) {
      const files = fs.readdirSync(targetJarDir);
      const fppFiles = files.filter(
        (f) => f.startsWith("fpp-") && f.endsWith(".jar")
      );
      if (fppFiles.length) jar = fppFiles.sort().reverse()[0];
    }
  } catch (e) {
    jar = { error: String(e) };
  }

  const skins = (() => {
    try {
      if (fs.existsSync(skinsDir)) return fs.readdirSync(skinsDir);
      return null;
    } catch (e) {
      return { error: String(e) };
    }
  })();

  res.json({
    timestamp: new Date().toISOString(),
    projectRoot,
    plugin,
    config,
    botNames,
    botMessages,
    language,
    skins,
    pom,
    builtJar: jar,
  });
});

// Update checker
const UPDATE_API_URL =
  process.env.UPDATE_API_URL || "https://api.fpp.com/latest";
let cachedUpdate = null;
let cachedAt = 0;
const CACHE_TTL_MS = 5 * 60 * 1000;

async function fetchRemoteUpdate() {
  const now = Date.now();
  if (cachedUpdate && now - cachedAt < CACHE_TTL_MS) return cachedUpdate;
  try {
    const r = await fetch(UPDATE_API_URL, { timeout: 5000 });
    if (!r.ok) throw new Error("HTTP " + r.status);
    const json = await r.json();
    cachedUpdate = json;
    cachedAt = Date.now();
    return json;
  } catch (e) {
    return { error: String(e) };
  }
}

function readLocalVersion(root) {
  try {
    const p = path.join(root, "src", "main", "resources", "plugin.yml");
    if (fs.existsSync(p)) {
      const obj = yaml.load(fs.readFileSync(p, "utf8"));
      if (obj && obj.version) return String(obj.version);
    }
  } catch (e) {}
  try {
    const p2 = path.join(root, "target", "maven-archiver", "pom.properties");
    if (fs.existsSync(p2)) {
      const raw = fs.readFileSync(p2, "utf8");
      const props = raw.split("\n").reduce((acc, l) => {
        const i = l.indexOf("=");
        if (i > 0) acc[l.substring(0, i).trim()] = l.substring(i + 1).trim();
        return acc;
      }, {});
      if (props.version) return String(props.version);
    }
  } catch (e) {}
  return null;
}

app.get("/api/check-update", async (req, res) => {
  const remote = await fetchRemoteUpdate();
  if (remote && remote.error)
    return res.status(502).json({ error: remote.error });

  const localVersion = readLocalVersion(projectRoot);
  const remoteVersion =
    remote && remote.version ? String(remote.version) : null;

  let updateAvailable = null;
  try {
    if (
      localVersion &&
      remoteVersion &&
      semver.valid(localVersion) &&
      semver.valid(remoteVersion)
    ) {
      updateAvailable = semver.lt(localVersion, remoteVersion);
    } else if (localVersion && remoteVersion) {
      updateAvailable = localVersion !== remoteVersion;
    }
  } catch (e) {
    updateAvailable = null;
  }

  res.json({
    localVersion,
    remoteVersion,
    updateAvailable,
    remote,
    checkedAt: new Date().toISOString(),
  });
});

app.get("/", (req, res) => res.redirect("/api/status"));

app.listen(port, () =>
  console.log("FPP frontend API listening at http://localhost:" + port)
);
