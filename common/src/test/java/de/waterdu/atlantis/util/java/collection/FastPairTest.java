package de.waterdu.atlantis.util.java.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FastPairTest {

    @Test
    void accessors() {
        var pair = FastPair.of("a", 1);

        assertThat(pair.a()).isEqualTo("a");
        assertThat(pair.b()).isEqualTo(1);
    }

    @Test
    void equalityAndHashCode() {
        assertThat(FastPair.of("a", 1)).isEqualTo(FastPair.of("a", 1)).hasSameHashCodeAs(FastPair.of("a", 1));
        assertThat(FastPair.of("a", 1)).isNotEqualTo(FastPair.of("a", 2)).isNotEqualTo(Pair.of("a", 1));
        assertThat(FastPair.of(null, null)).isEqualTo(FastPair.of(null, null));
    }

    @Test
    void toStringAndUnary() {
        assertThat(FastPair.of(null, "b")).hasToString("FastPair{[null],[b]}");
        assertThat(FastPair.unary("a", "b")).isEqualTo(Pair.unary("a", "b"));
    }
}
