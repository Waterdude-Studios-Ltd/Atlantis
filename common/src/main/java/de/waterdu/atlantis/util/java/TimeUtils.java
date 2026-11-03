package de.waterdu.atlantis.util.java;

import org.jetbrains.annotations.ApiStatus;

import java.time.ZoneOffset;
import java.util.function.BiFunction;

/**
 * Holds time constants and the global tick counters, and formats durations.
 */
public final class TimeUtils {

    public static final Timezone UTC = Timezone.of("UTC");

    public static final long SECOND_IN_MILLIS = 1000L;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60L;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60L;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24L;
    public static final long DAY_IN_SECONDS = 60L * 60L * 24L;
    public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7L;
    /**
     * 30 days.
     */
    public static final long MONTH_IN_MILLIS = DAY_IN_MILLIS * 30L;
    /**
     * 365 days.
     */
    public static final long YEAR_IN_MILLIS = DAY_IN_MILLIS * 365L;

    /**
     * Server ticks since the game started. It does not reset when an integrated server stops.
     * Only the server thread changes it.
     */
    public static long GLOBAL_TICK = 0;
    /**
     * Counts from 0 to 19 with the server ticks, then starts at 0 again.
     */
    public static long GLOBAL_SECOND_TICK = 0;

    /**
     * Always throws, because this class only has static members.
     */
    private TimeUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Advances the tick counters. The server tick hook calls it, so do not call it from other code.
     */
    @ApiStatus.Internal
    public static void tick() {
        GLOBAL_TICK++;
        GLOBAL_SECOND_TICK++;
        if (GLOBAL_SECOND_TICK >= 20) {
            GLOBAL_SECOND_TICK = 0;
        }
    }

    // Note: do not access from non-main thread!
    /**
     * Checks if the current server tick starts a new second. This is true once every 20 ticks.
     *
     * @return {@code true} if {@link #GLOBAL_SECOND_TICK} is 0
     */
    public static boolean isSecond() {
        return GLOBAL_SECOND_TICK == 0;
    }

    /**
     * Formats a duration as hours, minutes and seconds, separated by commas. Hours and minutes are left out if they are
     * 0.
     *
     * @param time the duration in milliseconds
     * @param formatter makes the text for one unit from the unit name, for example {@code hours}, and the value
     */
    public static String formatTime(long time, BiFunction<String, Long, String> formatter) {
        var hours = time / 3600000L;
        time = time - (3600000L * hours);
        var minutes = time / 60000L;
        time = time - (60000L * minutes);
        var seconds = time / 1000L;
        var builder = new StringBuilder();
        if (hours > 0) {
            builder.append(formatter.apply(hours == 1 ? "hour" : "hours", hours)).append(", ");
        }
        if (minutes > 0) {
            builder.append(formatter.apply(minutes == 1 ? "minute" : "minutes", minutes)).append(", ");
        }
        builder.append(formatter.apply(seconds == 1 ? "second" : "seconds", seconds));
        return builder.toString();
    }

    /**
     * Calculates the number of full days that have passed since the Unix epoch (January 1, 1970, 00:00:00 UTC).
     *
     * @return the number of days since the epoch as a long value
     */
    public static long daysSinceEpoch() {
        return UTC.now().toEpochSecond(ZoneOffset.UTC) / DAY_IN_SECONDS;
    }

    /**
     * @param ticks converted to milliseconds at 50 ms for each tick
     * @return see {@link #mmssForMillis(long)}
     */
    public static String[] mmssForTicks(long ticks) {
        return TimeUtils.mmssForMillis(ticks * 50);
    }

    /**
     * @return the minutes, the seconds with two digits, and the seconds without padding
     */
    public static String[] mmssForMillis(long milliseconds) {
        var secondsLeft = milliseconds / 1000L;
        var minutesLeft = secondsLeft / 60L;
        secondsLeft = secondsLeft - (minutesLeft * 60);
        return new String[]{String.valueOf(minutesLeft), (secondsLeft < 10 ? "0" : "") + secondsLeft, String.valueOf(secondsLeft)};
    }
}
