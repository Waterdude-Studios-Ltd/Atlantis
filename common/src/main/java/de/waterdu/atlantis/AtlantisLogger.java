package de.waterdu.atlantis;

import de.waterdu.atlantis.util.java.ExceptionUtils;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class AtlantisLogger {

    private static final Logger LOGGER = LogManager.getLogger("Atlantis");

    private AtlantisLogger() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Logs at the given severity. {@link Severity#NONE} logs nothing.
     *
     * @param severity the level to log at
     * @param message the message, with {@code {}} placeholders for args
     * @param args values for the placeholders. A trailing {@link Throwable} is logged with its stack trace.
     */
    public static void log(Severity severity, String message, Object... args) {
        switch (severity) {
            case ERROR -> AtlantisLogger.error(message, args);
            case WARN -> AtlantisLogger.warn(message, args);
            case INFO -> AtlantisLogger.info(message, args);
            case DEBUG -> AtlantisLogger.debug(message, args);
        }
    }

    /**
     * Logs at the given severity with a stack trace. {@link Severity#NONE} logs nothing.
     *
     * @param severity the level to log at
     * @param message the message. This method does not replace placeholders.
     * @param err the error whose stack trace to log
     */
    public static void log(Severity severity, String message, Throwable err) {
        switch (severity) {
            case ERROR -> AtlantisLogger.error(message, err);
            case WARN -> AtlantisLogger.warn(message, err);
            case INFO -> AtlantisLogger.info(message, err);
            case DEBUG -> AtlantisLogger.debug(message, err);
        }
    }

    public static void info(String message, Object... args) {
        AtlantisLogger.LOGGER.info(message, args);
    }

    public static void info(String message, Throwable err) {
        AtlantisLogger.LOGGER.info(message, err);
    }

    public static void warn(String message, Object... args) {
        AtlantisLogger.LOGGER.warn(message, args);
    }

    public static void warn(String message, Throwable err) {
        AtlantisLogger.LOGGER.warn(message, err);
    }

    public static void error(String message, Object... args) {
        AtlantisLogger.LOGGER.error(message, args);
    }

    public static void error(String message, Throwable err) {
        AtlantisLogger.LOGGER.error(message, err);
    }

    public static void debug(String message, Object... args) {
        AtlantisLogger.LOGGER.debug(message, args);
    }

    public static void debug(String message, Throwable err) {
        AtlantisLogger.LOGGER.debug(message, err);
    }

    /**
     * Returns a no-op builder when debug logging is disabled. This builder formats nothing that is passed to it.
     *
     * @return a debug-level {@link LogBuilder}
     */
    public static LogBuilder debugBuilder() {
        return LOGGER.atDebug();
    }

    public enum Severity {
        ERROR,
        WARN,
        INFO,
        DEBUG,
        /**
         * Disables logging, for example in settings that choose a level.
         */
        NONE,
    }
}
