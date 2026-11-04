package de.waterdu.atlantis.util.java.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the unary pairs that {@link Pair#unary} and {@link Pair.Immutable#unary} create.
 */
class UnaryPairTest {

    /**
     * Checks that a mutable unary pair can be changed and checks its text.
     */
    @Test
    void mutableUnaryPair() {
        var pair = Pair.unary("a", "b");
        pair.setB("c");

        assertThat(pair.getB()).isEqualTo("c");
        assertThat(pair).hasToString("UnaryPair{[a],[c]}");
    }

    /**
     * Checks that the setters of an immutable unary pair throw an {@link UnsupportedOperationException}, and checks
     * its text.
     */
    @Test
    void immutableUnaryPairRejectsChanges() {
        var pair = Pair.unary("a", "b").immutable();

        assertThatThrownBy(() -> pair.setA("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> pair.setB("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThat(pair).hasToString("UnaryPair.Immutable{[a],[b]}");
        assertThatThrownBy(() -> Pair.Immutable.unary("a", "b").setA("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Checks that a mutable copy of an immutable unary pair can be changed.
     */
    @Test
    void mutableCopyOfImmutable() {
        var copy = Pair.Immutable.unary("a", "b").mutable();
        copy.setA("x");

        assertThat(copy.getA()).isEqualTo("x");
    }

    /**
     * Checks that a unary pair is equal to a pair and an immutable unary pair with the same values.
     */
    @Test
    void equality() {
        assertThat(Pair.unary(1, 2)).isEqualTo(Pair.of(1, 2)).isEqualTo(Pair.Immutable.unary(1, 2));
    }
}
