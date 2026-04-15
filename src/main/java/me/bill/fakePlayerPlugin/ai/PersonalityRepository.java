package me.bill.fakePlayerPlugin.ai;

import me.bill.fakePlayerPlugin.FakePlayerPlugin;
import me.bill.fakePlayerPlugin.util.FppLogger;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PersonalityRepository {

    private static final String[] SAMPLE_FILES = {"friendly.txt", "grumpy.txt", "noob.txt"};

    private final FakePlayerPlugin plugin;

    private final Map<String, String> personalities = new ConcurrentHashMap<>();

    public PersonalityRepository(FakePlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        File folder = getFolder();
        if (!folder.exists()) {
            boolean ok = folder.mkdirs();
            FppLogger.debug("PersonalityRepository: created personalities/ folder → " + ok);
        }
        extractSamples(folder);
        load(folder);
    }

    public void reload() {
        personalities.clear();
        File folder = getFolder();
        if (folder.exists()) {
            load(folder);
            FppLogger.debug(
                    "PersonalityRepository: reloaded — "
                            + personalities.size()
                            + " personalities.");
        } else {
            FppLogger.warn("PersonalityRepository: personalities/ folder not found during reload.");
        }
    }

    public String get(String name) {
        if (name == null) return null;
        return personalities.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean has(String name) {
        if (name == null) return false;
        return personalities.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<>(personalities.keySet());
        Collections.sort(names);
        return Collections.unmodifiableList(names);
    }

    public int size() {
        return personalities.size();
    }

    private File getFolder() {
        return new File(plugin.getDataFolder(), "personalities");
    }

    private void extractSamples(File folder) {
        for (String sample : SAMPLE_FILES) {
            File target = new File(folder, sample);
            if (!target.exists()) {
                try (InputStream in = plugin.getResource("personalities/" + sample)) {
                    if (in != null) {
                        Files.copy(in, target.toPath());
                        FppLogger.debug("PersonalityRepository: extracted sample → " + sample);
                    }
                } catch (IOException e) {
                    FppLogger.warn(
                            "PersonalityRepository: could not extract "
                                    + sample
                                    + " — "
                                    + e.getMessage());
                }
            }
        }
    }

    private void load(File folder) {
        File[] files =
                folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".txt"));

        if (files == null || files.length == 0) {
            FppLogger.debug("PersonalityRepository: personalities/ folder is empty.");
            return;
        }

        int loaded = 0;
        for (File file : files) {
            String rawName = file.getName();

            String name = rawName.substring(0, rawName.length() - 4).toLowerCase(Locale.ROOT);
            try {
                String content = Files.readString(file.toPath()).trim();
                if (content.isEmpty()) {
                    FppLogger.warn("PersonalityRepository: '" + rawName + "' is empty — skipped.");
                    continue;
                }
                personalities.put(name, content);
                loaded++;
                FppLogger.debug(
                        "PersonalityRepository: loaded '"
                                + name
                                + "' ("
                                + content.length()
                                + " chars)");
            } catch (IOException e) {
                FppLogger.warn(
                        "PersonalityRepository: failed to read '"
                                + rawName
                                + "' — "
                                + e.getMessage());
            }
        }
        FppLogger.debug("PersonalityRepository: " + loaded + " personality file(s) loaded.");
    }
}
