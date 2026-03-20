const fetch = require('node-fetch')

module.exports = async (req, res) => {
  res.setHeader('Content-Type', 'application/json')
  const pluginApi = process.env.PLUGIN_API_URL || process.env.NEXT_PUBLIC_PLUGIN_API_URL
  if (!pluginApi) return res.status(400).json({ error: 'PLUGIN_API_URL not configured' })

  const candidates = [
    pluginApi.endsWith('/') ? pluginApi + 'api/check-update' : pluginApi + '/api/check-update',
    pluginApi.endsWith('/') ? pluginApi + 'api/status' : pluginApi + '/api/status',
    pluginApi.endsWith('/') ? pluginApi + 'latest' : pluginApi + '/latest',
  ]

  for (const c of candidates) {
    try {
      const controller = new AbortController()
      const id = setTimeout(() => controller.abort(), 5000)
      const r = await fetch(c, { signal: controller.signal })
      clearTimeout(id)
      if (!r.ok) continue
      const json = await r.json()
      return res.status(200).json(json)
    } catch (e) {
      // try next
    }
  }

  return res.status(502).json({ error: 'Could not contact plugin API', tried: candidates })
}

