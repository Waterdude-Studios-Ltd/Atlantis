package de.waterdu.atlantis.util.java.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link Pair} and {@link Pair.Immutable}.
 */
class PairTest {

    /**
     * Checks that the setters of a mutable pair change its values.
     */
    @Test
    void mutablePair() {
        var pair = Pair.of("a", 1);
        pair.setA("b");
        pair.setB(2);

        assertThat(pair.getA()).isEqualTo("b");
        assertThat(pair.getB()).isEqualTo(2);
    }

    /**
     * Checks that pairs with the same values are equal and have the same hash code, also across mutable and
     * immutable pairs.
     */
    @Test
    void equalityAndHashCode() {
        assertThat(Pair.of("a", 1)).isEqualTo(Pair.of("a", 1)).hasSameHashCodeAs(Pair.of("a", 1));
        assertThat(Pair.of("a", 1)).isNotEqualTo(Pair.of("a", 2)).isNotEqualTo("a");
        assertThat(Pair.of(null, null)).isEqualTo(Pair.of(null, null));
        assertThat(Pair.of(null, null).hashCode()).isZero();
        assertThat(Pair.of("a", 1)).isEqualTo(Pair.Immutable.of("a", 1));
    }

    /**
     * Checks that the setters of an immutable pair throw an {@link UnsupportedOperationException}.
     */
    @Test
    void immutablePairRejectsChanges() {
        var pair = Pair.of("a", 1).immutable();

        assertThatThrownBy(() -> pair.setA("b")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> pair.setB(2)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(pair.getA()).isEqualTo("a");
    }

    /**
     * Checks that a mutable copy of an immutable pair can be changed.
     */
    @Test
    void mutableCopyOfImmutable() {
        var copy = Pair.Immutable.of("a", 1).mutable();
        copy.setA("b");

        assertThat(copy.getA()).isEqualTo("b");
    }

    /**
     * Checks that a change to a mutable copy does not change the original pair.
     */
    @Test
    void copiesAreIndependent() {
        var original = Pair.of("a", 1);
        var copy = original.mutable();
        copy.setA("b");

        assertThat(original.getA()).isEqualTo("a");
    }

    /**
     * Checks the text of {@link Pair#toString()} for mutable and immutable pairs.
     */
    @Test
    void toStrings() {
        assertThat(Pair.of("a", null)).hasToString("Pair{[a],[null]}");
        assertThat(Pair.Immutable.of("a", 1)).hasToString("Pair.Immutable{[a],[1]}");
    }
}
