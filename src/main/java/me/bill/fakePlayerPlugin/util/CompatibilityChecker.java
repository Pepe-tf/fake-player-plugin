package me.bill.fakePlayerPlugin.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;

public final class CompatibilityChecker {

  private CompatibilityChecker() {}

  public static final class Result {

    public final boolean restricted;

    public final boolean isPaper;

    public final boolean isVersionSupported;

    public final String detectedVersion;

    public final List<String> failureLangKeys;

    Result(
        boolean restricted,
        boolean isPaper,
        boolean isVersionSupported,
        String detectedVersion,
        List<String> failureLangKeys) {
      this.restricted = restricted;
      this.isPaper = isPaper;
      this.isVersionSupported = isVersionSupported;
      this.detectedVersion = detectedVersion;
      this.failureLangKeys = List.copyOf(failureLangKeys);
    }

    @Override
    public String toString() {
      return "CompatibilityResult{restricted="
          + restricted
          + ", paper="
          + isPaper
          + ", version="
          + detectedVersion
          + (isVersionSupported ? "✔" : "✗")
          + '}';
    }
  }

  public static Result check() {
    String detectedVersion = "unknown";
    try {
      detectedVersion = extractMcVersion();
    } catch (Throwable ignored) {
    }
    FppLogger.debug(
        "Compatibility check skipped - all features enabled (MC " + detectedVersion + ").");
    return new Result(false, true, true, detectedVersion, List.of());
  }

  @SuppressWarnings("unused")
  public static Component buildWarningComponent(Result result) {
    return null;
  }

  public static String extractMcVersion() {
    try {
      String bv = org.bukkit.Bukkit.getBukkitVersion();
      return bv.contains("-") ? bv.split("-", 2)[0] : bv;
    } catch (Throwable ignored) {
    }
    return "0.0.0";
  }

  public static boolean isVersionAtLeast(String version, String required) {
    int[] v = parseVersionParts(version);
    int[] r = parseVersionParts(required);
    int len = Math.max(v.length, r.length);
    for (int i = 0; i < len; i++) {
      int vi = i < v.length ? v[i] : 0;
      int ri = i < r.length ? r[i] : 0;
      if (vi != ri) return vi > ri;
    }
    return true;
  }

  private static int[] parseVersionParts(String v) {
    if (v == null || v.isBlank()) return new int[0];
    String[] raw = v.split("\\.", -1);
    int[] parts = new int[raw.length];
    for (int i = 0; i < raw.length; i++) {
      Matcher m = Pattern.compile("^(\\d+)").matcher(raw[i]);
      parts[i] = m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
    return parts;
  }
}
