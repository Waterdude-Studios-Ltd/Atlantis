package de.waterdu.atlantis.util.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilsTest {

    @Test
    void constants() {
        assertThat(TimeUtils.MINUTE_IN_MILLIS).isEqualTo(60_000L);
        assertThat(TimeUtils.HOUR_IN_MILLIS).isEqualTo(3_600_000L);
        assertThat(TimeUtils.DAY_IN_MILLIS).isEqualTo(86_400_000L);
        assertThat(TimeUtils.DAY_IN_SECONDS).isEqualTo(86_400L);
        assertThat(TimeUtils.WEEK_IN_MILLIS).isEqualTo(7 * TimeUtils.DAY_IN_MILLIS);
    }

    @Test
    void tickAdvancesAndMarksEverySecond() {
        while (!TimeUtils.isSecond()) {
            TimeUtils.tick();
        }
        var start = TimeUtils.GLOBAL_TICK;
        var seconds = 0;
        for (var i = 0; i < 60; i++) {
            TimeUtils.tick();
            if (TimeUtils.isSecond()) {
                seconds++;
            }
        }

        assertThat(TimeUtils.GLOBAL_TICK - start).isEqualTo(60);
        assertThat(seconds).isEqualTo(3);
        assertThat(TimeUtils.isSecond()).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0        | 0 seconds",
            "1000     | 1 second",
            "59999    | 59 seconds",
            "60000    | 1 minute, 0 seconds",
            "3661000  | 1 hour, 1 minute, 1 second",
            "7322000  | 2 hours, 2 minutes, 2 seconds",
            "3600000  | 1 hour, 0 seconds",
    })
    void formatTime(long millis, String expected) {
        assertThat(TimeUtils.formatTime(millis, (unit, amount) -> amount + " " + unit)).isEqualTo(expected);
    }

    @Test
    void mmss() {
        assertThat(TimeUtils.mmssForMillis(65_000)).containsExactly("1", "05", "5");
        assertThat(TimeUtils.mmssForMillis(600_000)).containsExactly("10", "00", "0");
        assertThat(TimeUtils.mmssForTicks(20 * 75)).containsExactly("1", "15", "15");
    }

    @Test
    void daysSinceEpoch() {
        var expected = Instant.now().getEpochSecond() / TimeUtils.DAY_IN_SECONDS;

        assertThat(TimeUtils.daysSinceEpoch()).isBetween(expected - 1, expected + 1);
    }
}
