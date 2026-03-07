package me.bill.fakePlayerPlugin.fakeplayer;

import me.bill.fakePlayerPlugin.util.FppLogger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches Mojang skin textures for a given player name asynchronously.
 *
 * <p>Features:
 * <ul>
 *   <li><b>Per-name cache</b> — a name is only fetched from Mojang once per
 *       server session; subsequent requests get the cached result instantly.</li>
 *   <li><b>Pending-callback deduplication</b> — if two bots with the same name
 *       are spawned simultaneously, only one HTTP request is made; both callbacks
 *       are queued and fired together when the response arrives.</li>
 *   <li><b>Serial request queue</b> — requests are processed one at a time with
 *       a 600 ms gap between them, keeping well inside Mojang's rate limit
 *       (~1 req/600 ms per IP).</li>
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

    /** Completed skin results: name → [value, signature] (null values = no skin). */
    private static final Map<String, String[]> cache = new ConcurrentHashMap<>();

    /**
     * Pending callbacks for names currently being fetched.
     * name → list of callbacks waiting for the result.
     */
    private static final Map<String, List<BiConsumer<String, String>>> pending =
            new ConcurrentHashMap<>();

    /**
     * Single-threaded executor with a 600 ms delay between jobs so we stay
     * well inside Mojang's per-IP rate limit.
     */
    private static final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "FPP-SkinFetcher");
                t.setDaemon(true);
                return t;
            });

    /** Delay between consecutive Mojang API requests (milliseconds). */
    private static final long REQUEST_GAP_MS = 650;

    /** Tracks when the next request slot is available. */
    private static long nextSlotMs = 0;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches skin data for {@code playerName} and calls
     * {@code callback(value, signature)} when done.
     * Both args are {@code null} if no skin exists for that name.
     *
     * <p>This method is safe to call from any thread, including the main thread.
     */
    public static synchronized void fetchAsync(String playerName,
                                               BiConsumer<String, String> callback) {
        // 1. Already cached — fire immediately on the calling thread
        if (cache.containsKey(playerName)) {
            String[] result = cache.get(playerName);
            callback.accept(result[0], result[1]);
            return;
        }

        // 2. Already in-flight — queue callback to be fired with the result
        if (pending.containsKey(playerName)) {
            pending.get(playerName).add(callback);
            return;
        }

        // 3. New name — create pending list, schedule the fetch
        List<BiConsumer<String, String>> callbacks = new CopyOnWriteArrayList<>();
        callbacks.add(callback);
        pending.put(playerName, callbacks);

        // Calculate when to run: now or after the next available slot
        long now   = System.currentTimeMillis();
        long delay = Math.max(0, nextSlotMs - now);
        nextSlotMs = Math.max(now, nextSlotMs) + REQUEST_GAP_MS;

        executor.schedule(() -> doFetch(playerName), delay, TimeUnit.MILLISECONDS);
    }

    /** Clears the skin cache (e.g. on plugin reload so fresh skins are fetched). */
    public static synchronized void clearCache() {
        cache.clear();
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
        } catch (Exception e) {
            FppLogger.warn("SkinFetcher error for '" + playerName + "': " + e.getMessage());
        }

        // Store in cache
        String[] result = new String[]{value, signature};
        cache.put(playerName, result);

        // Fire all waiting callbacks
        List<BiConsumer<String, String>> callbacks = pending.remove(playerName);
        if (callbacks != null) {
            final String v = value, s = signature;
            for (BiConsumer<String, String> cb : callbacks) {
                try { cb.accept(v, s); }
                catch (Exception e) {
                    FppLogger.warn("SkinFetcher callback error for '" + playerName + "': " + e.getMessage());
                }
            }
        }
    }

    // ── Mojang API calls ──────────────────────────────────────────────────────

    private static String fetchUuid(String name) {
        try {
            String json = get("https://api.mojang.com/users/profiles/minecraft/" + name);
            if (json == null || json.isEmpty()) return null;
            Matcher m = UUID_PATTERN.matcher(json);
            return m.find() ? m.group(1) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String[] fetchTextures(String rawUuid) {
        try {
            String json = get("https://sessionserver.mojang.com/session/minecraft/profile/"
                    + rawUuid + "?unsigned=false");
            if (json == null || json.isEmpty()) return null;
            Matcher vm = VALUE_PATTERN.matcher(json);
            Matcher sm = SIG_PATTERN.matcher(json);
            if (!vm.find()) return null;
            String value = vm.group(1);
            String sig   = sm.find() ? sm.group(1) : null;
            return new String[]{value, sig};
        } catch (Exception e) {
            return null;
        }
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
        } finally {
            conn.disconnect();
        }
    }
}
