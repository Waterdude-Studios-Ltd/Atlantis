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

    public static void log(Severity severity, String message, Object... args) {
        switch (severity) {
            case ERROR -> AtlantisLogger.error(message, args);
            case WARN -> AtlantisLogger.warn(message, args);
            case INFO -> AtlantisLogger.info(message, args);
            case DEBUG -> AtlantisLogger.debug(message, args);
        }
    }

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

    public static LogBuilder debugBuilder() {
        return LOGGER.atDebug();
    }

    public enum Severity {
        ERROR,
        WARN,
        INFO,
        DEBUG,
        NONE,
    }
}
