package de.waterdu.atlantis.util.java.math;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VariableTest {

    @Test
    void parsesOperationsLeftToRight() {
        var variable = new Variable("a+2*b");

        assertThat(variable.operators()).extracting(Operation::operator).containsExactly(Operator.ADD, Operator.ADD, Operator.MULTIPLY);
        assertThat(variable.operators()).extracting(Operation::variable).containsExactly("a", "", "b");
        assertThat(variable.calculate(Map.of("a", 1.0, "b", 4.0))).isEqualTo(12.0);
        assertThat(variable.comparator()).isNull();
    }

    @Test
    void leadingSignIsApplied() {
        var variable = new Variable("-a");

        assertThat(variable.operators()).extracting(Operation::operator).containsExactly(Operator.SUBTRACT);
        assertThat(variable.calculate(Map.of("a", 3.0))).isEqualTo(-3.0);
    }

    @Test
    void ternary() {
        var variable = new Variable("a>b?1:2");

        assertThat(variable.comparator()).isNotNull();
        assertThat(variable.calculate(Map.of("a", 1.0, "b", 0.0))).isEqualTo(1.0);
    }

    @Test
    void operationParsesNumbersAndVariables() {
        var number = new Operation(Operator.MULTIPLY, "2.5");
        var variable = new Operation(Operator.ADD, "x");

        assertThat(number.value()).isEqualTo(2.5);
        assertThat(number.variable()).isEmpty();
        assertThat(variable.variable()).isEqualTo("x");
        assertThat(number.calculate(4, Map.of())).isEqualTo(10);
        assertThat(variable.calculate(1, Map.of("x", 2.0))).isEqualTo(3);
    }
}
