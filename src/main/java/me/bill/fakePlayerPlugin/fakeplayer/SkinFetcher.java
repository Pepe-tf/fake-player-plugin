package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.util.FppLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches Mojang skin textures for a given player name or URL asynchronously.
 *
 * <h3>Capabilities</h3>
 * <ul>
 *   <li>{@link #fetchAsync(String, BiConsumer)} — resolve skin by Minecraft player name.</li>
 *   <li>{@link #fetchByUrl(String, BiConsumer)} — build a skin profile from a raw
 *       Mojang CDN URL (e.g. {@code https://textures.minecraft.net/texture/…}).</li>
 * </ul>
 *
 * <h3>Reliability features</h3>
 * <ul>
 *   <li><b>Per-name cache</b> — fetched once per session; subsequent calls return
 *       instantly from cache.</li>
 *   <li><b>Callback deduplication</b> — simultaneous requests for the same name
 *       share one HTTP call.</li>
 *   <li><b>Rate-limited queue</b> — 200 ms between requests, well inside Mojang's
 *       limit (~1 req / 600 ms per IP).</li>
 * </ul>
 */
public final class SkinFetcher {

    private SkinFetcher() {}

    // ── Patterns ──────────────────────────────────────────────────────────────

    private static final Pattern UUID_PATTERN  =
            Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
    private static final Pattern VALUE_PATTERN =
            Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SIG_PATTERN   =
            Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");

    // ── Cache & queue ─────────────────────────────────────────────────────────

    /** name → [value, signature] (null values = no skin / not found). */
    private static final Map<String, String[]> cache = new ConcurrentHashMap<>();

    /** name → callbacks waiting for the in-flight fetch. */
    private static final Map<String, List<BiConsumer<String, String>>> pending =
            new ConcurrentHashMap<>();

    private static final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "FPP-SkinFetcher");
                t.setDaemon(true);
                return t;
            });

    /** Gap between consecutive Mojang API requests (ms). */
    private static final long REQUEST_GAP_MS = 200;
    private static long nextSlotMs = 0;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches skin data for {@code playerName} and calls
     * {@code callback(value, signature)} on the FPP-SkinFetcher thread.
     * Both args are {@code null} if no skin exists (name not on Mojang).
     * Safe to call from any thread.
     */
    public static synchronized void fetchAsync(String playerName,
                                               BiConsumer<String, String> callback) {
        // 1. Cached — fire immediately
        if (cache.containsKey(playerName)) {
            String[] r = cache.get(playerName);
            callback.accept(r[0], r[1]);
            return;
        }
        // 2. Already in-flight — queue callback
        if (pending.containsKey(playerName)) {
            pending.get(playerName).add(callback);
            return;
        }
        // 3. New fetch
        List<BiConsumer<String, String>> cbs = new CopyOnWriteArrayList<>();
        cbs.add(callback);
        pending.put(playerName, cbs);

        long now   = System.currentTimeMillis();
        long delay = Math.max(0, nextSlotMs - now);
        nextSlotMs = Math.max(now, nextSlotMs) + REQUEST_GAP_MS;
        executor.schedule(() -> doFetch(playerName), delay, TimeUnit.MILLISECONDS);
    }

    /** Returns cached skin data immediately, or {@code null} if not yet cached. */
    @SuppressWarnings("unused")
    public static synchronized String[] getCached(String playerName) {
        return cache.get(playerName);
    }

    /** Returns {@code true} if this name has already been resolved. */
    @SuppressWarnings("unused")
    public static synchronized boolean isCached(String playerName) {
        return cache.containsKey(playerName);
    }

    /**
     * Clears the entire skin cache — call on /fpp reload so bots
     * get fresh skins after a name-pool change.
     */
    public static synchronized void clearCache() {
        cache.clear();
        FppLogger.debug("SkinFetcher: cache cleared.");
    }

    /** Returns the number of names currently in the cache. */
    @SuppressWarnings("unused")
    public static int cacheSize() { return cache.size(); }

    // ── URL-based fetching ────────────────────────────────────────────────────

    /**
     * Builds a skin profile from a raw Mojang CDN texture URL such as
     * {@code https://textures.minecraft.net/texture/<hash>}.
     *
     * <p>Constructs the base64 texture-payload JSON locally — no extra HTTP
     * call needed. The resulting profile has no RSA signature ({@code null})
     * which is fine for display purposes on Paper servers.
     *
     * <p>If the URL is a full player profile URL that returns JSON containing
     * a {@code "value"} field, that field is used directly (signed).
     *
     * @param url      texture URL
     * @param callback receives (value, signature) — both non-null on success
     */
    public static void fetchByUrl(String url, BiConsumer<String, String> callback) {
        if (url == null || url.isBlank()) {
            callback.accept(null, null);
            return;
        }

        // Cache key based on URL
        String cacheKey = "url:" + url;
        synchronized (SkinFetcher.class) {
            if (cache.containsKey(cacheKey)) {
                String[] r = cache.get(cacheKey);
                callback.accept(r[0], r[1]);
                return;
            }
            if (pending.containsKey(cacheKey)) {
                pending.get(cacheKey).add(callback);
                return;
            }
            List<BiConsumer<String, String>> cbs = new CopyOnWriteArrayList<>();
            cbs.add(callback);
            pending.put(cacheKey, cbs);

            long now   = System.currentTimeMillis();
            long delay = Math.max(0, nextSlotMs - now);
            nextSlotMs = Math.max(now, nextSlotMs) + REQUEST_GAP_MS;
            executor.schedule(() -> doFetchByUrl(cacheKey, url), delay, TimeUnit.MILLISECONDS);
        }
    }

    private static void doFetchByUrl(String cacheKey, String url) {
        String value = null, signature = null;
        try {
            // Strategy 1: if it's a textures.minecraft.net URL, wrap it in JSON directly
            if (url.contains("textures.minecraft.net")) {
                String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
                value = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
                // No signature available without Mojang signing it
                FppLogger.debug("SkinFetcher: built texture payload from CDN URL.");
            } else {
                // Strategy 2: try fetching it as a JSON endpoint (Mineskin API or similar)
                String response = get(url);
                if (response != null && !response.isBlank()) {
                    Matcher vm = VALUE_PATTERN.matcher(response);
                    Matcher sm = SIG_PATTERN.matcher(response);
                    if (vm.find()) {
                        value = vm.group(1);
                        if (sm.find()) signature = sm.group(1);
                        FppLogger.debug("SkinFetcher: extracted value+sig from URL response.");
                    }
                }
            }
        } catch (Exception e) {
            FppLogger.warn("SkinFetcher URL error for '" + url + "': " + e.getMessage());
        }

        String[] result = {value, signature};
        cache.put(cacheKey, result);

        List<BiConsumer<String, String>> cbs = pending.remove(cacheKey);
        if (cbs != null) {
            for (BiConsumer<String, String> cb : cbs) {
                try { cb.accept(value, signature); }
                catch (Exception e) {
                    FppLogger.warn("SkinFetcher URL callback error: " + e.getMessage());
                }
            }
        }
    }

    // ── Internal fetch ────────────────────────────────────────────────────────

    private static void doFetch(String playerName) {
        String value = null, signature = null;
        try {
            String uuid = fetchUuid(playerName);
            if (uuid != null) {
                String[] tex = fetchTextures(uuid);
                if (tex != null) { value = tex[0]; signature = tex[1]; }
            }
            if (value != null)
                FppLogger.debug("SkinFetcher: fetched skin for '" + playerName + "'.");
            else
                FppLogger.debug("SkinFetcher: no skin found for '" + playerName + "'.");
        } catch (Exception e) {
            FppLogger.warn("SkinFetcher error for '" + playerName + "': " + e.getMessage());
        }

        String[] result = {value, signature};
        cache.put(playerName, result);

        List<BiConsumer<String, String>> cbs = pending.remove(playerName);
        if (cbs != null) {
            for (BiConsumer<String, String> cb : cbs) {
                try { cb.accept(value, signature); }
                catch (Exception e) {
                    FppLogger.warn("SkinFetcher callback error for '" + playerName + "': " + e.getMessage());
                }
            }
        }
    }

    // ── Mojang API ────────────────────────────────────────────────────────────

    private static String fetchUuid(String name) {
        try {
            String json = get("https://api.mojang.com/users/profiles/minecraft/" + name);
            if (json == null || json.isBlank()) return null;
            Matcher m = UUID_PATTERN.matcher(json);
            return m.find() ? m.group(1) : null;
        } catch (Exception e) { return null; }
    }

    private static String[] fetchTextures(String rawUuid) {
        try {
            String json = get("https://sessionserver.mojang.com/session/minecraft/profile/"
                    + rawUuid + "?unsigned=false");
            if (json == null || json.isBlank()) return null;
            Matcher vm = VALUE_PATTERN.matcher(json);
            Matcher sm = SIG_PATTERN.matcher(json);
            if (!vm.find()) return null;
            return new String[]{vm.group(1), sm.find() ? sm.group(1) : null};
        } catch (Exception e) { return null; }
    }

    private static String get(String urlStr) throws Exception {
        HttpURLConnection conn =
                (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(5_000);
        conn.setRequestProperty("User-Agent", "FakePlayerPlugin/1.0");
        int code = conn.getResponseCode();
        if (code == 204 || code == 404 || code == 429) return null;
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } finally { conn.disconnect(); }
    }
}
