package de.waterdu.atlantis.util.java.math;

import java.util.Map;

/**
 * Compares two values with one of the {@link Comparators}, and gives one of two results.
 */
public class Comparator {

    private Comparators comparator;
    private String[] values;

    /**
     * Compares {@code inA} with {@code inB} and gives {@code outT} if the comparison is true, otherwise {@code outF}.
     * Each value is a variable name or a number.
     */
    public Comparator(Comparators comparator, String inA, String inB, String outT, String outF) {
        this.comparator = comparator;
        this.values = new String[]{inA, inB, outT, outF};
    }

    /**
     * @return the comparison to do
     */
    public Comparators comparator() {
        return this.comparator;
    }

    /**
     * Sets the comparison to do.
     *
     * @param comparator the new comparison
     */
    public void setComparator(Comparators comparator) {
        this.comparator = comparator;
    }

    /**
     * @return the values {@code inA}, {@code inB}, {@code outT} and {@code outF}, in that order
     */
    public String[] values() {
        return this.values;
    }

    /**
     * Sets the values.
     *
     * @param values the values {@code inA}, {@code inB}, {@code outT} and {@code outF}, in that order
     */
    public void setValues(String[] values) {
        this.values = values;
    }

    /**
     * Resolves each value from the variables or as a number, then does the comparison.
     *
     * @param variables the values of the variables, by name
     * @return {@code outT} if the comparison is true, otherwise {@code outF}
     * @throws NumberFormatException if a value is not a variable and not a number
     */
    public double evaluate(Map<String, Double> variables) {
        return this.comparator.evaluate(
                this.value(variables, 0),
                this.value(variables, 1),
                this.value(variables, 2),
                this.value(variables, 3)
        );
    }

    /**
     * Gets the value at the index. If it is not a variable name, it is parsed as a number.
     *
     * @param variables the values of the variables, by name
     * @param index the index in {@link #values()}
     * @return the value of the variable, or the parsed number
     */
    private double value(Map<String, Double> variables, int index) {
        var value = variables.get(this.values[index]);
        if (value == null) {
            return Double.parseDouble(this.values[index]);
        }
        return value;
    }
}
