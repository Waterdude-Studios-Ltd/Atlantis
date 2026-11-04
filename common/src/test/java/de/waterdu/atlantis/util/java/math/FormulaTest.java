package de.waterdu.atlantis.util.java.math;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests {@link Formula}.
 */
class FormulaTest {

    /**
     * Processes a formula of one line and gets the result of {@code x}.
     *
     * @param line the formula line, which must set {@code x}
     * @param variables the variables that the formula starts with
     * @return the value of {@code x}
     */
    private static double evaluate(String line, Map<String, Double> variables) {
        return Formula.of(new String[]{line}).process(variables).get("x");
    }

    /**
     * Processes a formula of one line with no start variables and gets the result of {@code x}.
     *
     * @param line the formula line, which must set {@code x}
     * @return the value of {@code x}
     */
    private static double evaluate(String line) {
        return evaluate(line, Map.of());
    }

    /**
     * Checks that the arithmetic operators, spaces and leading signs give the expected result. Operators apply from
     * left to right, with no precedence.
     *
     * @param line the formula line
     * @param expected the expected value of {@code x}
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x=7          | 7",
            "x=1+2        | 3",
            "x=1+2*3      | 9",
            "x=10-4/2     | 3",
            "x=2^10       | 1024",
            "x=10%3       | 1",
            "x=1.5*2      | 3",
            "x = 4 * 5    | 20",
            "x=-5+3       | -2",
            "x=+5-3       | 2",
    })
    void arithmetic(String line, double expected) {
        assertThat(evaluate(line)).isCloseTo(expected, within(1e-9));
    }

    /**
     * Checks that each bracketed function gives the expected result, in upper or lower case.
     *
     * @param line the formula line
     * @param expected the expected value of {@code x}
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x=SQRT(16)   | 4",
            "x=CBRT(27)   | 3",
            "x=LN(1)      | 0",
            "x=SIN(0)     | 0",
            "x=COS(0)     | 1",
            "x=TAN(0)     | 0",
            "x=SINH(0)    | 0",
            "x=COSH(0)    | 1",
            "x=TANH(0)    | 0",
            "x=ASIN(1)    | 1.5707963267948966",
            "x=ACOS(1)    | 0",
            "x=ATAN(0)    | 0",
            "x=sqrt(9)    | 3",
            "x=SPHERE(0.25) | 0.5",
            "x=SPHERE(0.75) | 0.5",
    })
    void bracketedFunctions(String line, double expected) {
        assertThat(evaluate(line)).isCloseTo(expected, within(1e-9));
    }

    /**
     * Checks that functions accept variables, and that the constants {@code e}, {@code pi} and {@code tau} are
     * available.
     */
    @Test
    void functionsOfVariablesAndConstants() {
        assertThat(evaluate("x=SQRT(a)", Map.of("a", 81.0))).isEqualTo(9);
        assertThat(evaluate("x=LN(e)")).isCloseTo(1, within(1e-12));
        assertThat(evaluate("x=2*pi")).isCloseTo(2 * Math.PI, within(1e-12));
        assertThat(evaluate("x=tau/pi")).isCloseTo(2, within(1e-12));
    }

    /**
     * Checks that each line can use the variables of the lines before it, and that the result holds the computed
     * variables, the start variables and the constants.
     */
    @Test
    void variablesAreComputedInOrderAndReturned() {
        var result = Formula.of("a=2 b=a*3 c=b+level").process(Map.of("level", 10.0));

        assertThat(result).containsEntry("a", 2.0).containsEntry("b", 6.0).containsEntry("c", 16.0).containsEntry("level", 10.0);
        assertThat(result).containsEntry("pi", Math.PI).containsEntry("g", 9.80665);
    }

    /**
     * Checks that a line can replace a start variable with a value computed from it.
     */
    @Test
    void laterLinesOverrideDefaults() {
        assertThat(Formula.of("x=x*2").process(Map.of("x", 4.0))).containsEntry("x", 8.0);
    }

    /**
     * Checks that each comparison operator picks the correct branch of a ternary.
     *
     * @param line the formula line
     * @param a the value of {@code a}
     * @param b the value of {@code b}
     * @param expected the expected value of {@code x}
     */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x=a>b?a:b    | 3 | 5 | 5",
            "x=a>b?a:b    | 7 | 5 | 7",
            "x=a>=b?1:0   | 5 | 5 | 1",
            "x=a<b?1:0    | 5 | 5 | 0",
            "x=a<=b?1:0   | 5 | 5 | 1",
            "x=a==b?10:20 | 2 | 2 | 10",
            "x=a!=b?10:20 | 2 | 2 | 20",
            "x=a!=b?10:20 | 2 | 3 | 10",
    })
    void ternaryComparisons(String line, double a, double b, double expected) {
        assertThat(evaluate(line, Map.of("a", a, "b", b))).isEqualTo(expected);
    }

    /**
     * Checks that {@code RAND} gives a value from 0 to 1, in upper or lower case.
     */
    @Test
    void randIsWithinItsScale() {
        for (var i = 0; i < 200; i++) {
            assertThat(evaluate("x=RAND*10")).isBetween(0.0, 10.0);
            assertThat(evaluate("x=5+rand")).isBetween(5.0, 6.0);
        }
    }

    /**
     * Checks that a line with no {@code =} sets no variable.
     */
    @Test
    void linesWithoutAssignmentsAreIgnored() {
        assertThat(Formula.of("note", "x=1").process(Map.of())).containsEntry("x", 1.0).doesNotContainKey("note");
    }

    /**
     * Checks that {@link Formula#byLines} trims each line and skips blank lines.
     */
    @Test
    void byLinesSkipsBlankLines() {
        var formula = Formula.byLines("""
                a = 3

                  b = a ^ 2
                """);

        assertThat(formula.lines()).containsExactly("a = 3", "b = a ^ 2");
        assertThat(formula.process(Map.of())).containsEntry("b", 9.0);
    }

    /**
     * Checks that a copy is a new instance with the same lines and the same result.
     */
    @Test
    void copyIsEquivalent() {
        var formula = Formula.of(List.of("a=2", "b=a+1"));
        var copy = formula.copy();

        assertThat(copy).isNotSameAs(formula);
        assertThat(copy.lines()).containsExactlyElementsOf(formula.lines());
        assertThat(copy.process(Map.of())).isEqualTo(formula.process(Map.of()));
    }

    /**
     * Checks that an underscore inside a variable name does not parse as the {@code _} log operator.
     */
    @Test
    void underscoreIsPartOfANameNotTheLogOperator() {
        assertThat(evaluate("x=log_base+1", Map.of("log_base", 2.0))).isEqualTo(3.0);
    }

    /**
     * Checks that a formula that uses an unknown variable throws a {@link NullPointerException}.
     */
    @Test
    void unknownVariableFails() {
        assertThatNullPointerException().isThrownBy(() -> evaluate("x=missing+1"));
    }

    /**
     * Checks that {@link Formula.TypeAdapter} reads a formula from a JSON array of lines and writes it back the same.
     */
    @Test
    void typeAdapterRoundTrips() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapter(Formula.class, new Formula.TypeAdapter()).create();

        var formula = gson.fromJson("[\"a=2\",\"x=a*a\"]", Formula.class);

        assertThat(formula.process(Map.of())).containsEntry("x", 4.0);
        assertThat(gson.toJson(formula)).isEqualTo("[\"a=2\",\"x=a*a\"]");
    }
}
