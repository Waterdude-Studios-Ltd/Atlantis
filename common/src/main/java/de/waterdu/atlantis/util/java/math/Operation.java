package de.waterdu.atlantis.util.java.math;

import java.util.Map;

/**
 * Applies one {@link Operator} to a running value, with a number or a variable as the second value.
 */
public class Operation {

    private Operator operator;
    private double value = 0;
    private String variable = "";

    /**
     * @param value a number, or the name of a variable
     */
    public Operation(Operator operator, String value) {
        this.operator = operator;
        try {
            this.value = Double.parseDouble(value);
        } catch (Exception e) {
            this.variable = value;
        }
    }

    /**
     * @return the operator of this operation
     */
    public Operator operator() {
        return this.operator;
    }

    /**
     * Sets the operator of this operation.
     *
     * @param operator the new operator
     */
    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    /**
     * @return the number that the operation uses when it has no variable
     */
    public double value() {
        return this.value;
    }

    /**
     * Sets the number that the operation uses when it has no variable.
     *
     * @param value the new number
     */
    public void setValue(double value) {
        this.value = value;
    }

    /**
     * @return the name of the variable that the operation uses, or an empty string if it uses a number
     */
    public String variable() {
        return this.variable;
    }

    /**
     * Sets the name of the variable that the operation uses. An empty string makes it use {@link #value()}.
     *
     * @param variable the variable name
     */
    public void setVariable(String variable) {
        this.variable = variable;
    }

    /**
     * Applies the operator to the input and the value or variable of this operation.
     *
     * @param input the current value
     * @param variables the known variables, by name
     * @return the result
     */
    public double calculate(double input, Map<String, Double> variables) {
        return this.operator.calculate(input, this.input(variables));
    }

    /**
     * Gets the second value of the operation. This is the value of the variable if one is set, otherwise the number.
     *
     * @param variables the known variables, by name
     * @return the second value
     */
    private double input(Map<String, Double> variables) {
        if (this.variable.isEmpty()) {
            return this.value;
        }
        return variables.get(this.variable);
    }
}
