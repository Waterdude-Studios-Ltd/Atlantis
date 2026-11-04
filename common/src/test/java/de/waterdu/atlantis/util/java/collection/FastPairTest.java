package de.waterdu.atlantis.util.java.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link FastPair}.
 */
class FastPairTest {

    /**
     * Checks that {@link FastPair#a()} and {@link FastPair#b()} return the values of the pair.
     */
    @Test
    void accessors() {
        var pair = FastPair.of("a", 1);

        assertThat(pair.a()).isEqualTo("a");
        assertThat(pair.b()).isEqualTo(1);
    }

    /**
     * Checks that fast pairs with the same values are equal and have the same hash code, and that a fast pair is
     * not equal to a {@link Pair}.
     */
    @Test
    void equalityAndHashCode() {
        assertThat(FastPair.of("a", 1)).isEqualTo(FastPair.of("a", 1)).hasSameHashCodeAs(FastPair.of("a", 1));
        assertThat(FastPair.of("a", 1)).isNotEqualTo(FastPair.of("a", 2)).isNotEqualTo(Pair.of("a", 1));
        assertThat(FastPair.of(null, null)).isEqualTo(FastPair.of(null, null));
    }

    /**
     * Checks the text of {@link FastPair#toString()}, and that {@link FastPair#unary} creates a pair equal to
     * {@link Pair#unary}.
     */
    @Test
    void toStringAndUnary() {
        assertThat(FastPair.of(null, "b")).hasToString("FastPair{[null],[b]}");
        assertThat(FastPair.unary("a", "b")).isEqualTo(Pair.unary("a", "b"));
    }
}
