package de.waterdu.atlantis.trident;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.java.ExceptionUtils;

/**
 * {@link AtlantisLogger} that puts {@value #PREFIX} before every message. This makes the output from Trident and Tines
 * easy to see in the log. Tines should use this logger.
 */
public final class TridentLogger {

    public static final String PREFIX = "♆ ";

    private TridentLogger() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void log(AtlantisLogger.Severity severity, String message, Object... args) {
        AtlantisLogger.log(severity, PREFIX + message, args);
    }

    public static void log(AtlantisLogger.Severity severity, String message, Throwable err) {
        AtlantisLogger.log(severity, PREFIX + message, err);
    }

    public static void info(String message, Object... args) {
        AtlantisLogger.info(PREFIX + message, args);
    }

    public static void info(String message, Throwable err) {
        AtlantisLogger.info(PREFIX + message, err);
    }

    public static void warn(String message, Object... args) {
        AtlantisLogger.warn(PREFIX + message, args);
    }

    public static void warn(String message, Throwable err) {
        AtlantisLogger.warn(PREFIX + message, err);
    }

    public static void error(String message, Object... args) {
        AtlantisLogger.error(PREFIX + message, args);
    }

    public static void error(String message, Throwable err) {
        AtlantisLogger.error(PREFIX + message, err);
    }

    public static void debug(String message, Object... args) {
        AtlantisLogger.debug(PREFIX + message, args);
    }

    public static void debug(String message, Throwable err) {
        AtlantisLogger.debug(PREFIX + message, err);
    }
}
