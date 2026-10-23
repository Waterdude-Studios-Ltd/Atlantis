package de.waterdu.atlantis.util.java.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnaryPairTest {

    @Test
    void mutableUnaryPair() {
        var pair = Pair.unary("a", "b");
        pair.setB("c");

        assertThat(pair.getB()).isEqualTo("c");
        assertThat(pair).hasToString("UnaryPair{[a],[c]}");
    }

    @Test
    void immutableUnaryPairRejectsChanges() {
        var pair = Pair.unary("a", "b").immutable();

        assertThatThrownBy(() -> pair.setA("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> pair.setB("x")).isInstanceOf(UnsupportedOperationException.class);
        assertThat(pair).hasToString("UnaryPair.Immutable{[a],[b]}");
        assertThatThrownBy(() -> Pair.Immutable.unary("a", "b").setA("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mutableCopyOfImmutable() {
        var copy = Pair.Immutable.unary("a", "b").mutable();
        copy.setA("x");

        assertThat(copy.getA()).isEqualTo("x");
    }

    @Test
    void equality() {
        assertThat(Pair.unary(1, 2)).isEqualTo(Pair.of(1, 2)).isEqualTo(Pair.Immutable.unary(1, 2));
    }
}
