package de.waterdu.atlantis.util.java.interfaces;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FunctionalInterfacesTest {

    @Test
    void triConsumerAndThen() {
        var calls = new ArrayList<String>();
        TriConsumer<String, Integer, Boolean> first = (a, b, c) -> calls.add("first " + a + b + c);

        first.andThen((a, b, c) -> calls.add("second")).accept("x", 1, true);

        assertThat(calls).containsExactly("first x1true", "second");
        assertThatThrownBy(() -> first.andThen(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void quadConsumerAndThen() {
        var calls = new ArrayList<String>();
        QuadConsumer<Integer, Integer, Integer, Integer> sum = (a, b, c, d) -> calls.add(String.valueOf(a + b + c + d));

        sum.andThen((a, b, c, d) -> calls.add("then")).accept(1, 2, 3, 4);

        assertThat(calls).containsExactly("10", "then");
    }

    @Test
    void triFunctionAndThen() {
        TriFunction<Integer, Integer, Integer, Integer> sum = (a, b, c) -> a + b + c;

        assertThat(sum.andThen(value -> "=" + value).apply(1, 2, 3)).isEqualTo("=6");
    }

    @Test
    void sqlBiFunctionAndThenPropagatesSqlExceptions() throws SQLException {
        SQLBiFunction<Integer, Integer, Integer> multiply = (a, b) -> a * b;
        SQLBiFunction<Integer, Integer, Integer> failing = (a, b) -> {
            throw new SQLException("boom");
        };

        assertThat(multiply.andThen(value -> value + 1).apply(3, 4)).isEqualTo(13);
        assertThatThrownBy(() -> failing.andThen(value -> value).apply(1, 1)).isInstanceOf(SQLException.class);
    }

    @Test
    void equalityTestObjectsEqual() {
        EqualityTest<String> test = EqualityTest.objectsEqual();

        assertThat(test.equals("a", "a")).isTrue();
        assertThat(test.equals("a", "b")).isFalse();
        assertThat(test.equals(null, null)).isTrue();
    }
}
