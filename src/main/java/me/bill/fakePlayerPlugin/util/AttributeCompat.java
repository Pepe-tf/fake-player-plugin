package me.bill.fakePlayerPlugin.util;

import org.bukkit.attribute.Attribute;

public final class AttributeCompat {
  private AttributeCompat() {}

  public static Attribute maxHealth() {
    try {
      return Attribute.valueOf("MAX_HEALTH");
    } catch (IllegalArgumentException ignored) {
      return Attribute.valueOf("GENERIC_MAX_HEALTH");
    }
  }
}
