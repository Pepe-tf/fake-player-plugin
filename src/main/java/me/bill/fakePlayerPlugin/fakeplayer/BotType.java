package me.bill.fakePlayerPlugin.fakeplayer;

public enum BotType {
  AFK,

  PVP;

  public static BotType parse(String s) {
    if (s == null) return AFK;
    return "pvp".equalsIgnoreCase(s) ? PVP : AFK;
  }

  public static boolean isValid(String s) {
    if (s == null) return false;
    String lo = s.toLowerCase();
    return lo.equals("afk") || lo.equals("pvp");
  }
}
