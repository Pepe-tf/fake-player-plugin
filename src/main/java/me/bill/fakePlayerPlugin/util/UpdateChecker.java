package me.bill.fakePlayerPlugin.util;

import me.bill.fakePlayerPlugin.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Checks GitHub releases for a newer version of the plugin.
 *
 * <p>The check runs async, once on startup, and optionally on /fpp reload.
 * It can be disabled in config.yml via {@code update-checker.enabled: false}.
 *
 * <p>GitHub API endpoint used:
 * {@code https://api.github.com/repos/<owner>/<repo>/releases/latest}
 */
public final class UpdateChecker {

    /** GitHub repo path — update this if the repo moves. */
    private static final String REPO     = "Pepe-tf/Fake-Player-Plugin-Public-";
    private static final String API_URL  =
            "https://api.github.com/repos/" + REPO + "/releases/latest";

    private UpdateChecker() {}

    /**
     * Runs the update check asynchronously.
     * Logs a warning to console if a newer version is found.
     * Does nothing if {@code update-checker.enabled} is false in config.
     */
    public static void check(Plugin plugin) {
        if (!Config.updateCheckerEnabled()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        URI.create(API_URL).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5_000);
                conn.setReadTimeout(5_000);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "FakePlayerPlugin-UpdateChecker");

                if (conn.getResponseCode() != 200) {
                    Config.debug("UpdateChecker: HTTP " + conn.getResponseCode() + " — skipping.");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }

                String json    = sb.toString();
                String latest  = extractTagName(json);
                String current = plugin.getPluginMeta().getVersion();

                if (latest == null || latest.isBlank()) {
                    Config.debug("UpdateChecker: could not parse tag_name from response.");
                    return;
                }

                // Normalise: strip leading 'v' if present
                String latestClean  = latest.startsWith("v")  ? latest.substring(1)  : latest;
                String currentClean = current.startsWith("v") ? current.substring(1) : current;

                if (!latestClean.equals(currentClean)) {
                    FppLogger.warn("┌─────────────────────────────────────────┐");
                    FppLogger.warn("│  ꜰᴘᴘ Update Available!                  │");
                    FppLogger.warn("│  Current : v" + padRight(currentClean, 28) + "│");
                    FppLogger.warn("│  Latest  : v" + padRight(latestClean,  28) + "│");
                    FppLogger.warn("│  https://github.com/" + REPO + " │");
                    FppLogger.warn("└─────────────────────────────────────────┘");
                } else {
                    FppLogger.info("ꜰᴘᴘ is up to date (v" + currentClean + ").");
                }

            } catch (Exception e) {
                Config.debug("UpdateChecker failed: " + e.getMessage());
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Extracts the {@code tag_name} field from a GitHub releases JSON response. */
    private static String extractTagName(String json) {
        int idx = json.indexOf("\"tag_name\"");
        if (idx == -1) return null;
        int colon = json.indexOf(':', idx);
        if (colon == -1) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 == -1) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 == -1) return null;
        return json.substring(q1 + 1, q2);
    }

    private static String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }
}

