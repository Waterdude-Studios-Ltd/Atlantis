package de.waterdu.atlantis.util.java.math;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link Comparators} and {@link Comparator}.
 */
class ComparatorsTest {

    /**
     * Checks that {@link Comparators#evaluate} returns {@code 10} if the comparison is true and {@code 20} if not.
     *
     * @param comparator the comparator to test
     * @param a the first value to compare
     * @param b the second value to compare
     * @param expected the expected result
     */
    @ParameterizedTest
    @CsvSource({
            "EQUAL,                 1, 1, 10",
            "EQUAL,                 1, 2, 20",
            "NOT_EQUAL,             1, 2, 10",
            "LESS_THAN,             1, 2, 10",
            "LESS_THAN,             2, 2, 20",
            "LESS_THAN_OR_EQUAL,    2, 2, 10",
            "GREATER_THAN,          3, 2, 10",
            "GREATER_THAN,          2, 2, 20",
            "GREATER_THAN_OR_EQUAL, 2, 2, 10",
            "GREATER_THAN_OR_EQUAL, 1, 2, 20",
    })
    void evaluate(Comparators comparator, double a, double b, double expected) {
        assertThat(comparator.evaluate(a, b, 10, 20)).isEqualTo(expected);
    }

    /**
     * Checks that {@link Comparators#find} finds two-character symbols before one-character symbols, and gives
     * {@link Comparators#EQUAL} if the formula has no symbol.
     */
    @Test
    void findPrefersTwoCharacterOperators() {
        assertThat(Comparators.find("a>=b")).isEqualTo(Comparators.GREATER_THAN_OR_EQUAL);
        assertThat(Comparators.find("a<=b")).isEqualTo(Comparators.LESS_THAN_OR_EQUAL);
        assertThat(Comparators.find("a>b")).isEqualTo(Comparators.GREATER_THAN);
        assertThat(Comparators.find("a<b")).isEqualTo(Comparators.LESS_THAN);
        assertThat(Comparators.find("a!=b")).isEqualTo(Comparators.NOT_EQUAL);
        assertThat(Comparators.find("a==b")).isEqualTo(Comparators.EQUAL);
        assertThat(Comparators.find("ab")).isEqualTo(Comparators.EQUAL);
    }

    /**
     * Checks that {@link Comparators#get} finds a comparator by symbol or enum name in any case, and checks
     * {@link Comparators#matches} and {@link Comparators#getRepresentation()}.
     */
    @Test
    void getAndMatches() {
        assertThat(Comparators.get(">=")).isEqualTo(Comparators.GREATER_THAN_OR_EQUAL);
        assertThat(Comparators.get("less_than")).isEqualTo(Comparators.LESS_THAN);
        assertThat(Comparators.get("~")).isNull();
        assertThat(Comparators.EQUAL.matches("==")).isTrue();
        assertThat(Comparators.EQUAL.getRepresentation()).isEqualTo("==");
    }

    /**
     * Checks that {@link Comparator#evaluate} replaces variable names with their values and reads other values as
     * numbers.
     */
    @Test
    void comparatorResolvesVariablesAndLiterals() {
        var comparator = new Comparator(Comparators.GREATER_THAN, "level", "10", "bonus", "0");

        assertThat(comparator.evaluate(Map.of("level", 12.0, "bonus", 5.0))).isEqualTo(5.0);
        assertThat(comparator.evaluate(Map.of("level", 8.0, "bonus", 5.0))).isEqualTo(0.0);
        assertThat(comparator.values()).containsExactly("level", "10", "bonus", "0");
        assertThat(comparator.comparator()).isEqualTo(Comparators.GREATER_THAN);
    }
}
