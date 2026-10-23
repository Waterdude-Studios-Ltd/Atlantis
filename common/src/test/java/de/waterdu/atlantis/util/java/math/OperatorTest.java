package de.waterdu.atlantis.util.java.math;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OperatorTest {

    @ParameterizedTest
    @CsvSource({
            "ADD,      2, 3, 5",
            "SUBTRACT, 2, 3, -1",
            "MULTIPLY, 2, 3, 6",
            "DIVIDE,   3, 2, 1.5",
            "POWER,    2, 3, 8",
            "MODULO,   7, 3, 1",
            "LOG,      9, 3, 2",
            "LN,       0, 1, 0",
            "SQRT,     0, 4, 2",
            "CBRT,     0, 8, 2",
            "SIN,      0, 0, 0",
            "COS,      0, 0, 1",
            "TAN,      0, 0, 0",
            "SINH,     0, 0, 0",
            "COSH,     0, 0, 1",
            "TANH,     0, 0, 0",
            "ASIN,     0, 0, 0",
            "ACOS,     0, 1, 0",
            "ATAN,     0, 0, 0",
            "SPHERE,   0, 0, 0",
            "SPHERE,   0, 0.5, 1",
            "SPHERE,   0, 1, 0",
    })
    void calculate(Operator operator, double current, double value, double expected) {
        assertThat(operator.calculate(current, value)).isCloseTo(expected, within(1e-9));
    }

    @Test
    void randIgnoresItsInputs() {
        for (var i = 0; i < 100; i++) {
            assertThat(Operator.RAND.calculate(100, 100)).isBetween(0.0, 1.0);
        }
    }

    @Test
    void getByNameOrRepresentation() {
        assertThat(Operator.get("+")).isEqualTo(Operator.ADD);
        assertThat(Operator.get("add")).isEqualTo(Operator.ADD);
        assertThat(Operator.get("_")).isEqualTo(Operator.LOG);
        assertThat(Operator.get("sqrt")).isEqualTo(Operator.SQRT);
        assertThat(Operator.get("?")).isNull();
    }

    @Test
    void matchesAndBracketed() {
        assertThat(Operator.MULTIPLY.matches("*")).isTrue();
        assertThat(Operator.SIN.matches("sin")).isTrue();
        assertThat(Operator.SIN.matches("cos")).isFalse();
        assertThat(Operator.SIN.isBracketed()).isTrue();
        assertThat(Operator.ADD.isBracketed()).isFalse();
    }
}
