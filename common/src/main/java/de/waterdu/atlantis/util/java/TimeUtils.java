package de.waterdu.atlantis.util.java;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.time.ZoneOffset;
import java.util.function.BiFunction;

@EventBusSubscriber
public final class TimeUtils {

    public static final Timezone UTC = Timezone.of("UTC");

    public static final long SECOND_IN_MILLIS = 1000L;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60L;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60L;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24L;
    public static final long DAY_IN_SECONDS = 60L * 60L * 24L;
    public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7L;
    public static final long MONTH_IN_MILLIS = DAY_IN_MILLIS * 30L;
    public static final long YEAR_IN_MILLIS = DAY_IN_MILLIS * 365L;

    public static long GLOBAL_TICK = 0;
    public static long GLOBAL_SECOND_TICK = 0;

    private TimeUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        GLOBAL_TICK++;
        GLOBAL_SECOND_TICK++;
        if (GLOBAL_SECOND_TICK >= 20) {
            GLOBAL_SECOND_TICK = 0;
        }
    }

    // Note: do not access from non-main thread!
    public static boolean isSecond() {
        return GLOBAL_SECOND_TICK == 0;
    }

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

    public static long daysSinceEpoch() {
        return UTC.now().toEpochSecond(ZoneOffset.UTC) / DAY_IN_SECONDS;
    }

    public static String[] mmssForTicks(long ticks) {
        return TimeUtils.mmssForMillis(ticks * 50);
    }

    public static String[] mmssForMillis(long milliseconds) {
        var secondsLeft = milliseconds / 1000L;
        var minutesLeft = secondsLeft / 60L;
        secondsLeft = secondsLeft - (minutesLeft * 60);
        return new String[]{String.valueOf(minutesLeft), (secondsLeft < 10 ? "0" : "") + secondsLeft, String.valueOf(secondsLeft)};
    }
}
