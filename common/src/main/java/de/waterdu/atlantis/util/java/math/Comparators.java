package de.waterdu.atlantis.util.java.math;

public enum Comparators {

    EQUAL("=="),
    GREATER_THAN_OR_EQUAL(">="),
    GREATER_THAN(">"),
    LESS_THAN_OR_EQUAL("<="),
    LESS_THAN("<"),
    NOT_EQUAL("!="),
    ;

    private final String representation;

    Comparators(String representation) {
        this.representation = representation;
    }

    public String getRepresentation() {
        return this.representation;
    }

    public static Comparators find(String formula) {
        for (var comparator : Comparators.values()) {
            if (formula.contains(comparator.representation)) {
                return comparator;
            }
        }
        return Comparators.EQUAL;
    }

    public boolean matches(String representation) {
        return this.representation.equalsIgnoreCase(representation);
    }

    public double evaluate(double inA, double inB, double outT, double outF) {
        return switch (this) {
            case EQUAL -> inA == inB ? outT : outF;
            case NOT_EQUAL -> inA != inB ? outT : outF;
            case LESS_THAN -> inA < inB ? outT : outF;
            case LESS_THAN_OR_EQUAL -> inA <= inB ? outT : outF;
            case GREATER_THAN -> inA > inB ? outT : outF;
            case GREATER_THAN_OR_EQUAL -> inA >= inB ? outT : outF;
        };
    }

    public static Comparators get(String name) {
        for (var operator : Comparators.values()) {
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
