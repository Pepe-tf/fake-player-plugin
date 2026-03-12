package me.bill.fakePlayerPlugin.fakeplayer;

/**
 * Immutable snapshot of a resolved Mojang skin — a base64-encoded
 * texture value and its RSA signature.
 *
 * <p>A {@code null} signature is acceptable; Paper / Minecraft will still
 * render the skin without it on offline-mode servers, though it may
 * produce a signed-profile warning in the server log.
 */
public final class SkinProfile {

    private final String value;
    private final String signature;

    /** Source tag used in debug messages (e.g. "name:Notch", "file:cool.png", "url:…"). */
    private final String source;

    public SkinProfile(String value, String signature, String source) {
        this.value     = value;
        this.signature = signature;
        this.source    = source != null ? source : "unknown";
    }

    /** Base64-encoded texture blob (never {@code null} on a valid profile). */
    public String getValue()     { return value; }

    /** RSA signature returned by Mojang, or {@code null} for unsigned skins. */
    @SuppressWarnings("unused")
    public String getSignature() { return signature; }

    /** Human-readable source label — used for debug logging only. */
    public String getSource()    { return source; }

    /** {@code true} if this profile contains actual texture data. */
    public boolean isValid()     { return value != null && !value.isBlank(); }

    @Override
    public String toString() {
        return "SkinProfile{source='" + source + "', signed=" + (signature != null) + '}';
    }
}


