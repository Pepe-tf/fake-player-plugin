package me.bill.fakePlayerPlugin.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomNameGenerator {

  private static final List<String> CONSONANTS =
      List.of("b", "c", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "r", "s", "t", "v", "w", "z");
  private static final List<String> VOWELS = List.of("a", "e", "i", "o", "u");
  private static final List<String> PREFIXES =
      List.of(
          "Pro", "The", "Its", "Dark", "Light", "Mega", "Ultra", "Shadow", "Night", "Fire",
          "Ice", "Storm", "Iron", "Diamond", "Golden", "Silver", "Neon", "Turbo", "Pixel",
          "Block", "Craft", "Void", "Ender", "Nether", "Red", "Blue", "Arc", "Nova");
  private static final List<String> SUFFIXES =
      List.of("YT", "TV", "HD", "XD", "OP", "GG", "PvP", "MC", "Live", "Plays", "Craft", "Builds", "Labs", "Go");
  private static final List<String> BASES =
      List.of(
          "Alex", "Jordan", "Taylor", "Morgan", "Riley", "Nova", "Orion", "Jasper", "Blaze",
          "Viper", "Ghost", "Hunter", "Knight", "Rogue", "Archer", "Atlas", "Felix", "Kai",
          "Rowan", "Sage", "Quinn", "Zion", "Flint", "Drake", "Raven", "Scout", "Ranger",
          "Mage", "Wizard", "Miner", "Builder", "Crafter", "Warden", "Axel", "Milo");
  private static final List<String> ADJECTIVES =
      List.of(
          "Swift", "Silent", "Brave", "Wild", "Lucky", "Epic", "Elite", "Royal", "Fast",
          "Sneaky", "Fierce", "Bold", "Clever", "Rapid", "Mystic", "Cosmic", "Hidden",
          "Ancient", "Glowing", "Frozen", "Crimson", "Azure", "Grand", "True", "Sly");
  private static final List<String> NOUNS =
      List.of(
          "Wolf", "Eagle", "Raven", "Viper", "Dragon", "Blaze", "Storm", "Shadow", "Blade",
          "Arrow", "Frost", "Steel", "Pickaxe", "Anvil", "Beacon", "Quartz", "Obsidian",
          "Lantern", "Totem", "Compass", "Portal", "Creeper", "Ender", "Redstone",
          "Crystal", "Ember", "Spark", "Knight", "Ranger", "Scout");

  private RandomNameGenerator() {}

  public static String generate() {
    String name =
        switch (ThreadLocalRandom.current().nextInt(8)) {
          case 0 -> syllableName();
          case 1 -> random(PREFIXES) + syllableName();
          case 2 -> random(ADJECTIVES) + random(NOUNS);
          case 3 -> random(BASES) + ThreadLocalRandom.current().nextInt(10, 100);
          case 4 -> "xX" + syllableName() + "Xx";
          case 5 -> syllableName() + "_" + ThreadLocalRandom.current().nextInt(10, 1000);
          case 6 -> syllableName() + random(SUFFIXES);
          default -> random(BASES);
        };
    return sanitize(name);
  }

  private static String syllableName() {
    int syllables = ThreadLocalRandom.current().nextInt(2, 4);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < syllables; i++) {
      sb.append(random(CONSONANTS));
      sb.append(random(VOWELS));
      if (ThreadLocalRandom.current().nextBoolean()) sb.append(random(CONSONANTS));
    }
    return capitalize(sb.toString());
  }

  private static String sanitize(String raw) {
    StringBuilder out = new StringBuilder();
    for (char c : raw.toCharArray()) {
      if (Character.isLetterOrDigit(c) || c == '_') out.append(c);
    }
    String result = out.toString();
    if (result.length() > 16) result = result.substring(0, 16);
    if (result.length() < 3) return "Player" + ThreadLocalRandom.current().nextInt(1000, 10000);
    return result;
  }

  private static String capitalize(String value) {
    if (value == null || value.isEmpty()) return value;
    return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
  }

  private static String random(List<String> values) {
    return values.get(ThreadLocalRandom.current().nextInt(values.size()));
  }
}
