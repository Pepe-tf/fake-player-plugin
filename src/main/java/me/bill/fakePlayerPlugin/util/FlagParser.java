package me.bill.fakePlayerPlugin.util;

import java.util.*;

/**
 * Lightweight flag/argument parser for FPP commands.
 *
 * <p>Supports:
 * <ul>
 *   <li>Positional arguments (non-flag tokens collected in order)
 *   <li>Boolean flags: {@code --flag}
 *   <li>Value flags:   {@code --flag value}
 *   <li>Deprecation aliases that transparently remap old flags to new ones
 *   <li>Conflict detection (mutually exclusive flags)
 *   <li>Duplicate detection
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * FlagParser p = new FlagParser(args)
 *     .deprecate("--num", "--count")
 *     .conflicts("--random", "--ping");
 * p.parse();
 * if (p.hasErrors()) { sender.sendMessage(p.errorMessage()); return true; }
 * boolean random = p.hasFlag("--random");
 * int count      = p.intFlag("--count", 1);
 * String bot     = p.positional(0).orElse(null);
 * }</pre>
 */
public final class FlagParser {

    private final String[] rawArgs;

    private final Map<String, String> deprecations = new LinkedHashMap<>();
    private final List<String[]> conflictGroups = new ArrayList<>();

    private final Map<String, String> flagValues = new LinkedHashMap<>();
    private final Set<String> presentFlags = new LinkedHashSet<>();
    private final List<String> positionals = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    private boolean parsed = false;

    public FlagParser(String[] args) {
        this.rawArgs = args;
    }

    /** Register a deprecated flag alias → canonical name. */
    public FlagParser deprecate(String old, String canonical) {
        deprecations.put(old.toLowerCase(), canonical.toLowerCase());
        return this;
    }

    /**
     * Register a mutually-exclusive conflict group.
     * e.g. {@code .conflicts("--ping", "--random")}
     */
    public FlagParser conflicts(String... flags) {
        conflictGroups.add(Arrays.stream(flags).map(String::toLowerCase).toArray(String[]::new));
        return this;
    }

    public FlagParser parse() {
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < rawArgs.length; i++) {
            String token = rawArgs[i];
            if (!token.startsWith("--")) {
                positionals.add(token);
                continue;
            }

            String flag = token.toLowerCase();

            if (deprecations.containsKey(flag)) {
                String canonical = deprecations.get(flag);
                warnings.add(
                        "⚠ <yellow>"
                                + token
                                + "</yellow><gray> is deprecated, use <white>"
                                + canonical
                                + "</white><gray> instead.");
                flag = canonical;
            }

            if (seen.contains(flag)) {
                errors.add("<red>Duplicate flag: <white>" + flag);
                continue;
            }
            seen.add(flag);
            presentFlags.add(flag);

            if (i + 1 < rawArgs.length && !rawArgs[i + 1].startsWith("--")) {
                flagValues.put(flag, rawArgs[++i]);
            } else {
                flagValues.put(flag, null);
            }
        }

        for (String[] group : conflictGroups) {
            List<String> found = new ArrayList<>();
            for (String f : group) {
                if (presentFlags.contains(f)) found.add(f);
            }
            if (found.size() > 1) {
                errors.add(
                        "<red>Cannot use "
                                + String.join(" <dark_gray>and </dark_gray>", found)
                                + " together.");
            }
        }

        parsed = true;
        return this;
    }

    /** True if any parse errors were recorded. */
    public boolean hasErrors() {
        assertParsed();
        return !errors.isEmpty();
    }

    /** First error message (MiniMessage format), or empty string. */
    public String errorMessage() {
        assertParsed();
        return errors.isEmpty() ? "" : errors.get(0);
    }

    /** All error messages. */
    public List<String> errors() {
        assertParsed();
        return Collections.unmodifiableList(errors);
    }

    /** All deprecation warnings (MiniMessage format). */
    public List<String> warnings() {
        assertParsed();
        return Collections.unmodifiableList(warnings);
    }

    /** Whether a boolean or value flag is present. */
    public boolean hasFlag(String flag) {
        assertParsed();
        return presentFlags.contains(flag.toLowerCase());
    }

    /**
     * Value of a flag, or {@code defaultValue} if absent.
     * Returns {@code null} value entry as defaultValue.
     */
    public String flag(String flag, String defaultValue) {
        assertParsed();
        String key = flag.toLowerCase();
        if (!presentFlags.contains(key)) return defaultValue;
        String v = flagValues.get(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Integer value of a flag, or {@code defaultValue}.
     * Adds an error and returns defaultValue if value is non-numeric.
     */
    public int intFlag(String flag, int defaultValue) {
        String raw = flag(flag, null);
        if (raw == null) return defaultValue;
        try {
            int v = Integer.parseInt(raw);
            if (v < 1) throw new NumberFormatException();
            return v;
        } catch (NumberFormatException e) {
            errors.add(
                    "<red>Invalid number for "
                            + flag.toLowerCase()
                            + ": <white>"
                            + raw
                            + "<red>. Expected a positive integer.");
            return defaultValue;
        }
    }

    /** Positional argument at index, or empty. */
    public Optional<String> positional(int index) {
        assertParsed();
        return index < positionals.size() ? Optional.of(positionals.get(index)) : Optional.empty();
    }

    /** All positional arguments. */
    public List<String> positionals() {
        assertParsed();
        return Collections.unmodifiableList(positionals);
    }

    /** Number of positional arguments. */
    public int positionalCount() {
        assertParsed();
        return positionals.size();
    }

    private void assertParsed() {
        if (!parsed) throw new IllegalStateException("FlagParser.parse() not yet called");
    }
}

