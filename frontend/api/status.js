const fs = require('fs')
const path = require('path')
const yaml = require('js-yaml')

// Vercel Serverless function: returns plugin status JSON.
// Behavior: try reading local plugin resource files (if the repo is deployed as-is).
// If not found (typical on Vercel), and PLUGIN_API_URL env var is set, proxy to that remote API.

async function fetchCandidate(url) {
  try {
    const controller = new AbortController()
    const id = setTimeout(() => controller.abort(), 5000)
    const res = await fetch(url, { signal: controller.signal })
    clearTimeout(id)
    if (!res.ok) return { error: `HTTP ${res.status}` }
    const contentType = res.headers.get('content-type') || ''
    if (contentType.includes('application/json')) return await res.json()
    // try to parse text as JSON
    const text = await res.text()
    try { return JSON.parse(text) } catch (e) { return { error: 'invalid-json' } }
  } catch (e) {
    return { error: String(e) }
  }
}

function safeReadYaml(filePath) {
  try {
    if (!fs.existsSync(filePath)) return null
    const content = fs.readFileSync(filePath, 'utf8')
    return yaml.load(content)
  } catch (e) {
    return { error: String(e) }
  }
}

module.exports = async (req, res) => {
  res.setHeader('Content-Type', 'application/json')
  const projectRoot = path.resolve(process.cwd())

  const pluginYml = path.join(projectRoot, 'src', 'main', 'resources', 'plugin.yml')
  const configYml = path.join(projectRoot, 'src', 'main', 'resources', 'config.yml')
  const botNames = path.join(projectRoot, 'src', 'main', 'resources', 'bot-names.yml')
  const botMessages = path.join(projectRoot, 'src', 'main', 'resources', 'bot-messages.yml')
  const languageEn = path.join(projectRoot, 'src', 'main', 'resources', 'language', 'en.yml')
  const skinsDir = path.join(projectRoot, 'src', 'main', 'resources', 'skins')
  const pomProps = path.join(projectRoot, 'target', 'maven-archiver', 'pom.properties')
  const targetDir = path.join(projectRoot, 'target')

  // If local plugin files exist, return parsed data
  if (fs.existsSync(pluginYml) || fs.existsSync(configYml)) {
    const plugin = safeReadYaml(pluginYml)
    const config = safeReadYaml(configYml)
    const names = safeReadYaml(botNames)
    const messages = safeReadYaml(botMessages)
    const language = safeReadYaml(languageEn)
    const skins = fs.existsSync(skinsDir) ? fs.readdirSync(skinsDir) : null

    let pom = null
    if (fs.existsSync(pomProps)) {
      try {
        const raw = fs.readFileSync(pomProps, 'utf8')
        pom = raw.split('\n').reduce((acc, line) => {
          const idx = line.indexOf('=')
          if (idx > 0) acc[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
          return acc
        }, {})
      } catch (e) { pom = { error: String(e) } }
    }

    let jar = null
    if (fs.existsSync(targetDir)) {
      try {
        const files = fs.readdirSync(targetDir)
        const fppFiles = files.filter(f => f.startsWith('fpp-') && f.endsWith('.jar'))
        if (fppFiles.length) jar = fppFiles.sort().reverse()[0]
      } catch (e) { jar = { error: String(e) } }
    }

    return res.status(200).json({
      timestamp: new Date().toISOString(),
      projectRoot,
      plugin,
      config,
      botNames: names,
      botMessages: messages,
      language,
      skins,
      pom,
      builtJar: jar,
    })
  }

  // Local files not found — try remote plugin API configured via env var
  const pluginApi = process.env.PLUGIN_API_URL || process.env.NEXT_PUBLIC_PLUGIN_API_URL
  if (!pluginApi) {
    return res.status(200).json({
      timestamp: new Date().toISOString(),
      projectRoot,
      plugin: null,
      config: null,
      botNames: null,
      botMessages: null,
      language: null,
      skins: null,
      pom: null,
      builtJar: null,
      message: 'No local plugin files found. Set PLUGIN_API_URL in Vercel environment variables to point to the plugin server or hosted API.'
    })
  }

  const candidates = [
    pluginApi,
    pluginApi.endsWith('/') ? pluginApi + 'api/status' : pluginApi + '/api/status',
    pluginApi.endsWith('/') ? pluginApi + 'api/check-update' : pluginApi + '/api/check-update',
    pluginApi.endsWith('/') ? pluginApi + 'status' : pluginApi + '/status',
    pluginApi.endsWith('/') ? pluginApi + 'latest' : pluginApi + '/latest',
  ]

  for (const c of candidates) {
    const result = await fetchCandidate(c)
    if (result && !result.error) return res.status(200).json(result)
  }

  return res.status(502).json({ error: 'Could not retrieve plugin data from PLUGIN_API_URL', tried: candidates })
}

