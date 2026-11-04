package de.waterdu.atlantis.util.java.interfaces;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the functional interfaces {@link TriConsumer}, {@link QuadConsumer}, {@link TriFunction},
 * {@link SQLBiFunction} and {@link EqualityTest}.
 */
class FunctionalInterfacesTest {

    /**
     * Checks that {@link TriConsumer#andThen} runs both consumers in order and rejects a {@code null} consumer.
     */
    @Test
    void triConsumerAndThen() {
        var calls = new ArrayList<String>();
        TriConsumer<String, Integer, Boolean> first = (a, b, c) -> calls.add("first " + a + b + c);

        first.andThen((a, b, c) -> calls.add("second")).accept("x", 1, true);

        assertThat(calls).containsExactly("first x1true", "second");
        assertThatThrownBy(() -> first.andThen(null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * Checks that {@link QuadConsumer#andThen} runs both consumers in order.
     */
    @Test
    void quadConsumerAndThen() {
        var calls = new ArrayList<String>();
        QuadConsumer<Integer, Integer, Integer, Integer> sum = (a, b, c, d) -> calls.add(String.valueOf(a + b + c + d));

        sum.andThen((a, b, c, d) -> calls.add("then")).accept(1, 2, 3, 4);

        assertThat(calls).containsExactly("10", "then");
    }

    /**
     * Checks that {@link TriFunction#andThen} applies the second function to the result of the first.
     */
    @Test
    void triFunctionAndThen() {
        TriFunction<Integer, Integer, Integer, Integer> sum = (a, b, c) -> a + b + c;

        assertThat(sum.andThen(value -> "=" + value).apply(1, 2, 3)).isEqualTo("=6");
    }

    /**
     * Checks that {@link SQLBiFunction#andThen} applies the second function and lets an {@link SQLException} of the
     * first function through.
     */
    @Test
    void sqlBiFunctionAndThenPropagatesSqlExceptions() throws SQLException {
        SQLBiFunction<Integer, Integer, Integer> multiply = (a, b) -> a * b;
        SQLBiFunction<Integer, Integer, Integer> failing = (a, b) -> {
            throw new SQLException("boom");
        };

        assertThat(multiply.andThen(value -> value + 1).apply(3, 4)).isEqualTo(13);
        assertThatThrownBy(() -> failing.andThen(value -> value).apply(1, 1)).isInstanceOf(SQLException.class);
    }

    /**
     * Checks that {@link EqualityTest#objectsEqual()} compares values by equality and treats two {@code null}
     * values as equal.
     */
    @Test
    void equalityTestObjectsEqual() {
        EqualityTest<String> test = EqualityTest.objectsEqual();

        assertThat(test.equals("a", "a")).isTrue();
        assertThat(test.equals("a", "b")).isFalse();
        assertThat(test.equals(null, null)).isTrue();
    }
}
