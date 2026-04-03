/**
 * Online stats API endpoint
 * Returns the count of servers and users currently using FPP
 *
 * TODO: Implement real tracking when ready:
 * - Track server heartbeats in a database (MySQL/Redis)
 * - Count active servers (last seen within 5 minutes)
 * - Sum player counts from all active servers
 */

module.exports = async (req, res) => {
  // Set CORS headers (public endpoint)
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Content-Type', 'application/json');

  // Handle OPTIONS for CORS preflight
  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  // Demo data for testing the count-up animation
  // Change to null to show "---"
  res.status(200).json({
    servers: 200,   // Shows "200+" on frontend
    users: 630,     // Shows "630+" on frontend
    timestamp: new Date().toISOString()
  });

  /*
   * Example implementation with database tracking:
   *
   * const db = require('./db-connection');
   *
   * const activeServers = await db.query(`
   *   SELECT COUNT(DISTINCT server_id) as count
   *   FROM server_heartbeats
   *   WHERE last_seen > NOW() - INTERVAL 5 MINUTE
   * `);
   *
   * const totalPlayers = await db.query(`
   *   SELECT SUM(player_count) as total
   *   FROM server_heartbeats
   *   WHERE last_seen > NOW() - INTERVAL 5 MINUTE
   * `);
   *
   * res.status(200).json({
   *   servers: activeServers[0].count || 0,
   *   users: totalPlayers[0].total || 0,
   *   timestamp: new Date().toISOString()
   * });
   */
};

