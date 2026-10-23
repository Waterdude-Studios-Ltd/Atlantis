package de.waterdu.atlantis.util.java.interfaces;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class ResettableLazyTest {

    private static ResettableLazy<Integer> create(boolean concurrent, Supplier<Integer> supplier) {
        return concurrent ? ResettableLazy.concurrentOf(supplier) : ResettableLazy.of(supplier);
    }

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
