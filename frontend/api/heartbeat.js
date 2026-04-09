/**
 * POST /api/heartbeat - receives periodic stats from FPP plugin instances.
 *
 * Payload (JSON):
 *   { server_id, player_count, bot_count, version }
 *
 * Storage: Vercel KV (Upstash Redis).
 * Each server's data is stored in the hash `fpp:heartbeats` keyed by server_id.
 * Staleness is determined by the `last_seen` timestamp in the stored JSON;
 * entries older than STALE_MS are ignored when reading stats.
 *
 * SETUP - enable Vercel KV in your Vercel dashboard:
 *   1. Open the project → Storage tab → Create Database → KV (Upstash)
 *   2. Vercel automatically adds KV_REST_API_URL + KV_REST_API_TOKEN env vars.
 *   3. Re-deploy - the heartbeat and online-stats endpoints will use real data.
 *
 * When KV is not configured the endpoint returns HTTP 503 so the plugin
 * silently drops the heartbeat without spamming the server log.
 */

const HASH_KEY = 'fpp:heartbeats';

/** Returns the Vercel KV client, or null if not configured. */
function getKv() {
  if (!process.env.KV_REST_API_URL || !process.env.KV_REST_API_TOKEN) return null;
  try {
    return require('@vercel/kv').kv;
  } catch {
    return null;
  }
}

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Content-Type', 'application/json');

  if (req.method === 'OPTIONS') return res.status(200).end();
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method Not Allowed' });

  // Parse body - Vercel auto-parses JSON when Content-Type is application/json
  const body = req.body || {};
  const { server_id, player_count, bot_count, version } = body;

  if (!server_id || typeof server_id !== 'string' || server_id.trim() === '') {
    return res.status(400).json({ ok: false, error: 'server_id is required' });
  }

  const store = getKv();
  if (!store) {
    // KV not set up - acknowledge silently so the plugin doesn't log errors
    return res.status(503).json({
      ok: false,
      error: 'KV not configured - enable Vercel KV in the dashboard to track live stats'
    });
  }

  const entry = {
    player_count : Math.max(0, parseInt(player_count, 10) || 0),
    bot_count    : Math.max(0, parseInt(bot_count,    10) || 0),
    version      : String(version || 'unknown').slice(0, 32),
    last_seen    : Date.now(),
  };

  try {
    await store.hset(HASH_KEY, { [server_id.trim()]: JSON.stringify(entry) });
    return res.status(200).json({ ok: true });
  } catch (err) {
    console.error('[heartbeat] KV write error:', err.message || err);
    return res.status(500).json({ ok: false, error: 'KV write failed' });
  }
};

