package de.waterdu.atlantis.util.java.interfaces;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ResettableLazy}.
 */
class ResettableLazyTest {

    /**
     * Creates a resettable lazy value.
     *
     * @param concurrent {@code true} to use {@link ResettableLazy#concurrentOf}, {@code false} to use
     *     {@link ResettableLazy#of}
     * @param supplier the supplier of the value
     * @return the resettable lazy value
     */
    private static ResettableLazy<Integer> create(boolean concurrent, Supplier<Integer> supplier) {
        return concurrent ? ResettableLazy.concurrentOf(supplier) : ResettableLazy.of(supplier);
    }

    /**
     * Checks that the value is cached until {@link ResettableLazy#reset()} is called.
     *
     * @param concurrent {@code true} to test the concurrent variant
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cachesUntilReset(boolean concurrent) {
        var calls = new AtomicInteger();
        var lazy = create(concurrent, calls::incrementAndGet);

        assertThat(lazy.get()).isEqualTo(1);
        assertThat(lazy.get()).isEqualTo(1);
        lazy.reset();
        assertThat(lazy.get()).isEqualTo(2);
    }

    /**
     * Checks that {@link ResettableLazy#getAndReset()} returns the current value and makes the next get compute a
     * new one.
     *
     * @param concurrent {@code true} to test the concurrent variant
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void getAndResetRecomputes(boolean concurrent) {
        var calls = new AtomicInteger();
        var lazy = create(concurrent, calls::incrementAndGet);

        assertThat(lazy.getAndReset()).isEqualTo(1);
        assertThat(lazy.getAndReset()).isEqualTo(2);
        assertThat(lazy.get()).isEqualTo(2);
    }
}
