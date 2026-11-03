package de.waterdu.atlantis.util.java.math;

/**
 * Lists the comparisons that a {@link Comparator} can do, each with its symbol.
 */
public enum Comparators {

    EQUAL("=="),
    GREATER_THAN_OR_EQUAL(">="),
    GREATER_THAN(">"),
    LESS_THAN_OR_EQUAL("<="),
    LESS_THAN("<"),
    NOT_EQUAL("!="),
    ;

    private final String representation;

    /**
     * Creates a comparator with its symbol.
     *
     * @param representation the symbol, such as {@code >=}
     */
    Comparators(String representation) {
        this.representation = representation;
    }

    /**
     * @return the symbol, such as {@code >=}
     */
    public String getRepresentation() {
        return this.representation;
    }

    /**
     * @return the first comparator whose symbol the formula contains, or {@link #EQUAL} if there is none
     */
    public static Comparators find(String formula) {
        for (var comparator : Comparators.values()) {
            if (formula.contains(comparator.representation)) {
                return comparator;
            }
        }
        return Comparators.EQUAL;
    }

    /**
     * Checks if the text is the symbol of this comparator, in any case.
     *
     * @param representation the text to compare with
     * @return {@code true} if the text is the symbol
     */
    public boolean matches(String representation) {
        return this.representation.equalsIgnoreCase(representation);
    }

    /**
     * @return {@code outT} if the comparison of {@code inA} with {@code inB} is true, otherwise {@code outF}
     */
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

    /**
     * @param name the enum name or the symbol, in any case
     * @return {@code null} if no comparator matches
     */
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
