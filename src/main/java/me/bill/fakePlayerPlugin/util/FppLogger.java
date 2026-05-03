package me.bill.fakePlayerPlugin.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class FppLogger {

  private static Logger logger = Logger.getLogger("FakePlayerPlugin");

  private FppLogger() {}

  public static void init(Logger logger) {
    FppLogger.logger = logger;
  }

  public static void info(String message) {
    logger.info(message);
  }

  public static void warn(String message) {
    logger.warning(message);
  }

  public static void error(String message) {
    logger.severe(message);
  }

  public static void debug(String message) {
    logger.log(Level.FINE, message);
  }
}
