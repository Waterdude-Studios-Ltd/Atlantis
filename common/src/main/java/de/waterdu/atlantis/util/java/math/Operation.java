package de.waterdu.atlantis.util.java.math;

import java.util.Map;

public class Operation {

    private Operator operator;
    private double value = 0;
    private String variable = "";

    public Operation(Operator operator, String value) {
        this.operator = operator;
        try {
            this.value = Double.parseDouble(value);
        } catch (Exception e) {
            this.variable = value;
        }
    }

    public Operator operator() {
        return this.operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public double value() {
        return this.value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String variable() {
        return this.variable;
    }

    public void setVariable(String variable) {
        this.variable = variable;
    }

    public double calculate(double input, Map<String, Double> variables) {
        return this.operator.calculate(input, this.input(variables));
    }

    private double input(Map<String, Double> variables) {
        if (this.variable.isEmpty()) {
            return this.value;
        }
        return variables.get(this.variable);
    }
}
