package de.waterdu.atlantis.util.java.math;

import de.waterdu.atlantis.util.java.RandomUtils;

public enum Operator {

    ADD("+", false),
    SUBTRACT("-", false),
    MULTIPLY("*", false),
    DIVIDE("/", false),
    POWER("^", false),
    MODULO("%", false),
    LOG("_", false),
    LN("LN", true),
    SQRT("SQRT", true),
    CBRT("CBRT", true),
    RAND("RAND", false),
    SIN("SIN", true),
    COS("COS", true),
    TAN("TAN", true),
    SINH("SINH", true),
    COSH("COSH", true),
    TANH("TANH", true),
    ASIN("ASIN", true),
    ACOS("ACOS", true),
    ATAN("ATAN", true),
    SPHERE("SPHERE", true);

    private final String representation;
    private final boolean bracketed;

    Operator(String representation, boolean bracketed) {
        this.representation = representation;
        this.bracketed = bracketed;
    }

    public boolean matches(String representation) {
        return this.representation.equalsIgnoreCase(representation);
    }

    public boolean isBracketed() {
        return this.bracketed;
    }

    public double calculate(double currentValue, double operatorValue) {
        return switch (this) {
            case ADD -> currentValue + operatorValue;
            case SUBTRACT -> currentValue - operatorValue;
            case MULTIPLY -> currentValue * operatorValue;
            case DIVIDE -> currentValue / operatorValue;
            case POWER -> Math.pow(currentValue, operatorValue);
            case MODULO -> currentValue % operatorValue;
            case LOG -> Math.log(currentValue) / Math.log(operatorValue);
            case LN -> Math.log(operatorValue);
            case SQRT -> Math.sqrt(operatorValue);
            case CBRT -> Math.cbrt(operatorValue);
            case RAND -> RandomUtils.getRandom().nextDouble();
            case SIN -> Math.sin(operatorValue);
            case COS -> Math.cos(operatorValue);
            case TAN -> Math.tan(operatorValue);
            case SINH -> Math.sinh(operatorValue);
            case COSH -> Math.cosh(operatorValue);
            case TANH -> Math.tanh(operatorValue);
            case ASIN -> Math.asin(operatorValue);
            case ACOS -> Math.acos(operatorValue);
            case ATAN -> Math.atan(operatorValue);
            case SPHERE -> {
                if (operatorValue < 0.5) {
                    operatorValue *= 2;
                } else {
                    operatorValue *= 2;
                    var toTake = operatorValue - 1.0;
                    operatorValue -= toTake * 2;
                }
                yield operatorValue;
            }
        };
    }

    public static Operator get(String name) {
        for (Operator operator : Operator.values()) {
            if (operator.name().equalsIgnoreCase(name)) {
                return operator;
            }
            if (operator.representation.equalsIgnoreCase(name)) {
                return operator;
            }
        }
        return null;
    }
}
