package me.bill.fakePlayerPlugin.util;

import me.bill.fakePlayerPlugin.config.Config;
import me.bill.fakePlayerPlugin.permission.Perm;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Checks a remote update API for a newer version of FakePlayerPlugin.
 *
 * <h3>Key behaviours</h3>
 * <ul>
 *   <li>Semantic version comparison — {@code 1.4.14 > 1.4.9} is handled correctly.</li>
 *   <li>Result cache with a 5-minute TTL so repeated {@code /fpp reload} calls do not
 *       hammer the network.</li>
 *   <li>Download URL extracted from the API response and passed to the language file.</li>
 *   <li>Admin notification uses {@link Perm#ALL} consistently.</li>
 *   <li>Endpoint order: dedicated {@code /api/check-update} tried first.</li>
 * </ul>
 */
public final class UpdateChecker {

    /** Base URL of the Vercel update API. */
    private static final String API_BASE = "https://fake-player-plugin.vercel.app";

    /** Fallback download URL when the API does not return one. */
    private static final String FALLBACK_DOWNLOAD_URL =
            "https://modrinth.com/plugin/fake-player-plugin-(fpp)";

    /** Matches version tokens like {@code 1.2.3} or {@code v1.2.3}. */
    private static final Pattern VERSION_REGEX = Pattern.compile("v?\\d+(?:\\.\\d+)+");

    // ── Result cache ──────────────────────────────────────────────────────────
    private static volatile UpdateInfo cachedResult   = null;
    private static volatile long       cacheTimestamp = 0L;
    /** Do not hit the network more often than this (ms). */
    private static final long CACHE_TTL_MS = 5L * 60L * 1000L;

    private UpdateChecker() {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Runs the update check asynchronously, respecting the cache TTL.
     * Does nothing when {@code update-checker.enabled: false}.
     */
    public static void check(Plugin plugin) {
        if (!Config.updateCheckerEnabled()) {
            Config.debug("Update checker disabled in config.");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UpdateInfo info = fetchOrCached(plugin);
            Bukkit.getScheduler().runTask(plugin, () -> handleResultOnMainThread(plugin, info));
        });
    }

    /**
     * Synchronous variant — blocks the calling thread up to {@code timeoutMs}.
     * Honours the cache TTL. Returns {@code null} on timeout or when disabled.
     */
    public static UpdateInfo checkBlocking(Plugin plugin, long timeoutMs) {
        if (!Config.updateCheckerEnabled()) {
            Config.debug("Update checker disabled in config.");
            return null;
        }
        final UpdateInfo[] out = { null };
        CountDownLatch latch = new CountDownLatch(1);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            out[0] = fetchOrCached(plugin);
            latch.countDown();
        });
        try {
            if (latch.await(Math.max(100, timeoutMs), TimeUnit.MILLISECONDS)) return out[0];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return null;
    }

    /**
     * Handles a fetched result on the main server thread.
     * Logs to console and notifies online admins; persists the notification for
     * admins who join later.
     */
    public static void handleResultOnMainThread(Plugin plugin, UpdateInfo info) {
        if (info == null) return;
        if (info.error != null) {
            Config.debug("UpdateChecker: " + info.error);
            return;
        }

        String latestClean  = stripV(info.latest);
        String currentClean = stripV(info.current);

        // Semantic comparison — positive means latest > current
        boolean updateAvailable = compareVersions(info.latest, info.current) > 0;

        if (updateAvailable) {
            String dlUrl = (info.downloadUrl != null && !info.downloadUrl.isBlank())
                    ? info.downloadUrl : FALLBACK_DOWNLOAD_URL;

            Component msg = me.bill.fakePlayerPlugin.lang.Lang.get("update-available",
                    "current", currentClean, "latest", latestClean, "download_url", dlUrl);

            // Console — strip the plugin prefix since FppLogger already prepends a tag
            FppLogger.warn(stripLangPrefix(PlainTextComponentSerializer.plainText().serialize(msg)));

            // Notify online admins
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (Perm.hasOrOp(p, Perm.ALL)) p.sendMessage(msg);
            }

            // Persist for admins who join after startup
            if (plugin instanceof me.bill.fakePlayerPlugin.FakePlayerPlugin fpp) {
                fpp.setUpdateNotification(msg);
            }

        } else {
            Component ok = me.bill.fakePlayerPlugin.lang.Lang.get(
                    "update-up-to-date", "current", currentClean);
            FppLogger.success(
                    stripLangPrefix(PlainTextComponentSerializer.plainText().serialize(ok)) + "  ✔");

            if (plugin instanceof me.bill.fakePlayerPlugin.FakePlayerPlugin fpp) {
                fpp.setUpdateNotification(null);
            }
        }
    }

    /**
     * Invalidates the cached result so the next {@link #check} or
     * {@link #checkBlocking} call always performs a fresh network request.
     * Call this from {@code /fpp reload} so admins always get current info.
     */
    public static void invalidateCache() {
        cachedResult   = null;
        cacheTimestamp = 0L;
        Config.debug("UpdateChecker: cache invalidated.");
    }

    // ── Result object ─────────────────────────────────────────────────────────

    /** Parsed result of a single update check. */
    public static final class UpdateInfo {
        public String  latest;
        public boolean latestStartsWithV;
        public String  current;
        public boolean currentStartsWithV;
        public String  error;
        /** Download URL from the API response, or {@code null} when not provided. */
        public String  downloadUrl;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /** Returns cached result if still fresh; fetches from the network otherwise. */
    private static UpdateInfo fetchOrCached(Plugin plugin) {
        long now = System.currentTimeMillis();
        if (cachedResult != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            // Keep current version in sync with the running plugin (shouldn't change
            // at runtime, but ensures the object reflects a post-reload state correctly).
            cachedResult.current            = plugin.getPluginMeta().getVersion();
            cachedResult.currentStartsWithV = cachedResult.current.startsWith("v");
            Config.debug("UpdateChecker: using cached result (age "
                    + ((now - cacheTimestamp) / 1000) + "s).");
            return cachedResult;
        }
        UpdateInfo fresh = fetchUpdateInfo(plugin);
        if (fresh != null && fresh.error == null) {
            cachedResult   = fresh;
            cacheTimestamp = now;
        }
        return fresh;
    }

    /**
     * Semantically compares two version strings ({@code "v1.4.14"}, {@code "1.4.9"}, …).
     *
     * @return positive when {@code a > b}, negative when {@code a < b}, 0 when equal.
     */
    static int compareVersions(String a, String b) {
        int[] pa = parseVersionParts(stripV(a));
        int[] pb = parseVersionParts(stripV(b));
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int na = i < pa.length ? pa[i] : 0;
            int nb = i < pb.length ? pb[i] : 0;
            if (na != nb) return Integer.compare(na, nb);
        }
        return 0;
    }

    private static int[] parseVersionParts(String v) {
        if (v == null || v.isBlank()) return new int[0];
        String[] raw = v.split("\\.", -1);
        int[] parts = new int[raw.length];
        for (int i = 0; i < raw.length; i++) {
            // Accept only leading digits — handles pre-release tags like "14-SNAPSHOT"
            Matcher m = Pattern.compile("^(\\d+)").matcher(raw[i]);
            parts[i] = m.find() ? Integer.parseInt(m.group(1)) : 0;
        }
        return parts;
    }

    private static String stripV(String v) {
        return (v != null && v.startsWith("v")) ? v.substring(1) : (v != null ? v : "");
    }

    /** Removes the lang prefix from a plain-text string so FppLogger doesn't double it. */
    private static String stripLangPrefix(String plain) {
        try {
            String prefixRaw   = me.bill.fakePlayerPlugin.lang.Lang.raw("prefix");
            String prefixPlain = PlainTextComponentSerializer.plainText()
                    .serialize(TextUtil.colorize(prefixRaw));
            if (plain.startsWith(prefixPlain)) return plain.substring(prefixPlain.length()).strip();
        } catch (Throwable ignored) {}
        return plain;
    }

    // ── Network fetch ─────────────────────────────────────────────────────────

    private static UpdateInfo fetchUpdateInfo(Plugin plugin) {
        UpdateInfo info = new UpdateInfo();
        info.current            = plugin.getPluginMeta().getVersion();
        info.currentStartsWithV = info.current.startsWith("v");

        // Dedicated endpoint first; root URL last (serves HTML, not JSON)
        String[] candidates = {
                API_BASE + "/api/check-update",
                API_BASE + "/api/status",
                API_BASE + "/latest",
                API_BASE + "/api/latest",
                API_BASE,
        };

        for (String url : candidates) {
            try {
                HttpURLConnection conn =
                        (HttpURLConnection) URI.create(url).toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5_000);
                conn.setReadTimeout(5_000);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent",
                        "FakePlayerPlugin-UpdateChecker/" + info.current);

                if (conn.getResponseCode() != 200) {
                    Config.debug("UpdateChecker: HTTP " + conn.getResponseCode() + " — " + url);
                    continue;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                String body = sb.toString().trim();
                if (body.isEmpty()) { Config.debug("UpdateChecker: empty body — " + url); continue; }

                String latest      = extractVersion(body);
                String downloadUrl = extractDownloadUrl(body);

                if (latest == null || latest.isBlank()) {
                    Config.debug("UpdateChecker: no version found in response from " + url);
                    continue;
                }

                info.latest            = latest;
                info.latestStartsWithV = latest.startsWith("v");
                info.downloadUrl       = downloadUrl;
                Config.debug("UpdateChecker: latest=" + stripV(latest) + " from " + url
                        + (downloadUrl != null ? " dl=" + downloadUrl : ""));
                return info;

            } catch (java.net.SocketTimeoutException e) {
                Config.debug("UpdateChecker: timeout — " + url);
            } catch (Exception e) {
                Config.debug("UpdateChecker: " + e.getClass().getSimpleName()
                        + " — " + url + ": " + e.getMessage());
            }
        }

        info.error = "no successful response from any update API endpoint";
        return info;
    }

    /**
     * Extracts the latest version string from a raw JSON body.
     *
     * <p>Priority order:
     * <ol>
     *   <li>{@code remoteVersion} / {@code remote_version} — from {@code /api/check-update}</li>
     *   <li>{@code tag_name}, {@code version}, {@code latest}, {@code name} — common shapes</li>
     *   <li>Same keys inside a nested {@code "remote"} or {@code "data"} object</li>
     *   <li>Regex fallback — first version-like token in the body</li>
     * </ol>
     */
    private static String extractVersion(String body) {
        for (String key : new String[]{"remoteVersion", "remote_version",
                                       "tag_name", "version", "latest", "name"}) {
            String val = extractJsonString(body, key);
            if (val != null && VERSION_REGEX.matcher(val).find()) return val;
        }
        // Check inside the nested "remote" or "data" wrapper objects
        for (String wrapper : new String[]{"remote", "data"}) {
            String nested = extractNestedObject(body, wrapper);
            if (nested != null) {
                for (String key : new String[]{"version", "tag_name", "latest"}) {
                    String val = extractJsonString(nested, key);
                    if (val != null && VERSION_REGEX.matcher(val).find()) return val;
                }
            }
        }
        // Regex fallback
        Matcher m = VERSION_REGEX.matcher(body);
        return m.find() ? m.group() : null;
    }

    /**
     * Extracts a download URL from a raw JSON body.
     * Checks top-level first, then inside {@code "remote"} and {@code "data"} wrappers.
     */
    private static String extractDownloadUrl(String body) {
        for (String key : new String[]{"downloadUrl", "download_url", "website"}) {
            String val = extractJsonString(body, key);
            if (val != null && !val.isBlank()) return val;
        }
        for (String wrapper : new String[]{"remote", "data"}) {
            String nested = extractNestedObject(body, wrapper);
            if (nested != null) {
                for (String key : new String[]{"downloadUrl", "download_url", "website"}) {
                    String val = extractJsonString(nested, key);
                    if (val != null && !val.isBlank()) return val;
                }
            }
        }
        return null;
    }

    /**
     * Extracts the string value of a JSON key from a raw body (non-recursive, single pass).
     * Returns {@code null} when the key is absent or the value is not a JSON string.
     */
    private static String extractJsonString(String body, String key) {
        int idx = body.indexOf('"' + key + '"');
        if (idx < 0) return null;
        int colon = body.indexOf(':', idx + key.length() + 2);
        if (colon < 0) return null;
        int q1 = colon + 1;
        while (q1 < body.length() && body.charAt(q1) != '"') {
            char c = body.charAt(q1);
            if (c == '{' || c == '[') return null; // value is object/array, not string
            q1++;
        }
        if (q1 >= body.length()) return null;
        int q2 = body.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return body.substring(q1 + 1, q2).trim();
    }

    /**
     * Returns the raw JSON of the first object assigned to {@code key} in {@code body},
     * or {@code null} when not found.
     */
    private static String extractNestedObject(String body, String key) {
        int idx = body.indexOf('"' + key + '"');
        if (idx < 0) return null;
        int start = body.indexOf('{', idx + key.length() + 2);
        if (start < 0) return null;
        int end = findMatchingBrace(body, start);
        return end > start ? body.substring(start, end + 1) : null;
    }

    /** Returns the index of the closing brace that matches the opening brace at {@code openIdx}. */
    private static int findMatchingBrace(String s, int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if      (c == '{') depth++;
            else if (c == '}') { if (--depth == 0) return i; }
        }
        return -1;
    }
}
