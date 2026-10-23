package de.waterdu.atlantis.util.java;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class RandomUtilsTest {

    private static final int SAMPLES = 2000;

    @Test
    void sharedRandom() {
        assertThat(RandomUtils.getRandom()).isSameAs(RandomUtils.getRandom());
    }

    @Test
    void intsAreInclusiveAndReachBothBounds() {
        var seen = new HashSet<Integer>();
        for (var i = 0; i < SAMPLES; i++) {
            var value = RandomUtils.getBetween(1, 4);
            assertThat(value).isBetween(1, 4);
            seen.add(value);
        }
        assertThat(seen).containsExactlyInAnyOrder(1, 2, 3, 4);
    }

    @Test
    void swappedBoundsAreAccepted() {
        for (var i = 0; i < SAMPLES; i++) {
            assertThat(RandomUtils.getBetween(10, 5)).isBetween(5, 10);
            assertThat(RandomUtils.getBetween((byte) 3, (byte) -3)).isBetween((byte) -3, (byte) 3);
            assertThat(RandomUtils.getBetween((short) 300, (short) 200)).isBetween((short) 200, (short) 300);
            assertThat(RandomUtils.getBetween(9L, 2L)).isBetween(2L, 9L);
            assertThat(RandomUtils.getBetween(2.0F, 1.0F)).isBetween(1.0F, 2.0F);
            assertThat(RandomUtils.getBetween(2.0, 1.0)).isBetween(1.0, 2.0);
        }
    }

    @Test
    void equalBounds() {
        assertThat(RandomUtils.getBetween(7, 7)).isEqualTo(7);
        assertThat(RandomUtils.getBetween(7L, 7L)).isEqualTo(7L);
        assertThat(RandomUtils.getBetween(1.5, 1.5)).isEqualTo(1.5);
    }

    @RepeatedTest(5)
    void floatingPointValuesStayWithinBounds() {
        for (var i = 0; i < SAMPLES; i++) {
            assertThat(RandomUtils.getBetween(0.0, 1.0)).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
            assertThat(RandomUtils.getBetween(-1.0F, 1.0F)).isGreaterThanOrEqualTo(-1.0F).isLessThanOrEqualTo(1.0F);
        }
    }

    @Test
    void longsSupportRangesWiderThanAnInt() {
        var min = 0L;
        var max = 10L * Integer.MAX_VALUE;
        var sawAboveInt = false;
        for (var i = 0; i < SAMPLES; i++) {
            var value = RandomUtils.getBetween(min, max);
            assertThat(value).isBetween(min, max);
            sawAboveInt |= value > Integer.MAX_VALUE;
        }
        assertThat(sawAboveInt).isTrue();
    }
}
