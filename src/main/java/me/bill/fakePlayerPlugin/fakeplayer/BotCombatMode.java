package me.bill.fakePlayerPlugin.fakeplayer;

public enum BotCombatMode {
  CRYSTAL,

  SWORD,

  FIST;

  public String getDisplayName() {
    return switch (this) {
      case CRYSTAL -> "Crystal PVP";
      case SWORD -> "Sword";
      case FIST -> "Fist";
    };
  }
}
