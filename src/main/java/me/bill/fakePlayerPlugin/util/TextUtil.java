package me.bill.fakePlayerPlugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Utility class for text formatting.
 * <p>
 * • Converts legacy {@code &} colour codes (including {@code &x} hex codes) to Adventure Components.<br>
 * • Provides the small-caps Unicode font mapper used throughout FPP.
 */
public final class TextUtil {

    private TextUtil() {}

    // ── Small-caps Unicode mapping ───────────────────────────────────────────

    private static final String NORMAL  = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String SMALL_CAPS =
            "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘQʀꜱᴛᴜᴠᴡxʏᴢ" +   // lower  (a-z)
            "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘQʀꜱᴛᴜᴠᴡxʏᴢ";  // upper  (A-Z)

    /**
     * Converts every ASCII letter in {@code text} to its small-caps Unicode equivalent.
     * Non-letter characters are left unchanged.
     */
    public static String toSmallCaps(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            int idx = NORMAL.indexOf(c);
            sb.append(idx >= 0 ? SMALL_CAPS.charAt(idx) : c);
        }
        return sb.toString();
    }

    // ── Colour parsing ───────────────────────────────────────────────────────

    /**
     * Parses a string that may contain:
     * <ul>
     *   <li>Legacy {@code &} colour codes (e.g. {@code &a}, {@code &l})</li>
     *   <li>Hex colour codes in the {@code &x&R&R&G&G&B&B} format used by Spigot/Paper</li>
     * </ul>
     * and returns an Adventure {@link Component}.
     */
    public static Component colorize(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /**
     * Shorthand: applies {@link #toSmallCaps(String)} then {@link #colorize(String)}.
     */
    public static Component format(String text) {
        return colorize(toSmallCaps(text));
    }
}


