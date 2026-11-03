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

    /**
     * Logs at the given severity with the prefix. {@link AtlantisLogger.Severity#NONE} logs nothing.
     *
     * @param severity the level to log at
     * @param message the message, with {@code {}} placeholders for args
     * @param args values for the placeholders. A trailing {@link Throwable} is logged with its stack trace.
     */
    public static void log(AtlantisLogger.Severity severity, String message, Object... args) {
        AtlantisLogger.log(severity, PREFIX + message, args);
    }

    /**
     * Logs at the given severity with the prefix and a stack trace. {@link AtlantisLogger.Severity#NONE} logs
     * nothing.
     *
     * @param severity the level to log at
     * @param message the message. This method does not replace placeholders.
     * @param err the error whose stack trace to log
     */
    public static void log(AtlantisLogger.Severity severity, String message, Throwable err) {
        AtlantisLogger.log(severity, PREFIX + message, err);
    }

    /**
     * Logs at the info level with the prefix.
     *
     * @param message the message, with {@code {}} placeholders for args
     * @param args values for the placeholders. A trailing {@link Throwable} is logged with its stack trace.
     */
    public static void info(String message, Object... args) {
        AtlantisLogger.info(PREFIX + message, args);
    }

    /**
     * Logs at the info level with the prefix and a stack trace.
     *
     * @param message the message. This method does not replace placeholders.
     * @param err the error whose stack trace to log
     */
    public static void info(String message, Throwable err) {
        AtlantisLogger.info(PREFIX + message, err);
    }

    /**
     * Logs at the warn level with the prefix.
     *
     * @param message the message, with {@code {}} placeholders for args
     * @param args values for the placeholders. A trailing {@link Throwable} is logged with its stack trace.
     */
    public static void warn(String message, Object... args) {
        AtlantisLogger.warn(PREFIX + message, args);
    }

    /**
     * Logs at the warn level with the prefix and a stack trace.
     *
     * @param message the message. This method does not replace placeholders.
     * @param err the error whose stack trace to log
     */
    public static void warn(String message, Throwable err) {
        AtlantisLogger.warn(PREFIX + message, err);
    }

    /**
     * Logs at the error level with the prefix.
     *
     * @param message the message, with {@code {}} placeholders for args
     * @param args values for the placeholders. A trailing {@link Throwable} is logged with its stack trace.
     */
    public static void error(String message, Object... args) {
        AtlantisLogger.error(PREFIX + message, args);
    }

    /**
     * Logs at the error level with the prefix and a stack trace.
     *
     * @param message the message. This method does not replace placeholders.
     * @param err the error whose stack trace to log
     */
    public static void error(String message, Throwable err) {
        AtlantisLogger.error(PREFIX + message, err);
    }

    /**
     * Logs at the debug level with the prefix.
     *
     * @param message the message, with {@code {}} placeholders for args
     * @param args values for the placeholders. A trailing {@link Throwable} is logged with its stack trace.
     */
    public static void debug(String message, Object... args) {
        AtlantisLogger.debug(PREFIX + message, args);
    }

    /**
     * Logs at the debug level with the prefix and a stack trace.
     *
     * @param message the message. This method does not replace placeholders.
     * @param err the error whose stack trace to log
     */
    public static void debug(String message, Throwable err) {
        AtlantisLogger.debug(PREFIX + message, err);
    }
}
