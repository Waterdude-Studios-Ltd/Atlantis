package de.waterdu.atlantis.util.java.math;

import java.util.Map;

public class Comparator {

    private Comparators comparator;
    private String[] values;

    public Comparator(Comparators comparator, String inA, String inB, String outT, String outF) {
        this.comparator = comparator;
        this.values = new String[]{inA, inB, outT, outF};
    }

    public Comparators comparator() {
        return this.comparator;
    }

    public void setComparator(Comparators comparator) {
        this.comparator = comparator;
    }

    public String[] values() {
        return this.values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    public double evaluate(Map<String, Double> variables) {
        return this.comparator.evaluate(
                this.value(variables, 0),
                this.value(variables, 1),
                this.value(variables, 2),
                this.value(variables, 3)
        );
    }

    private double value(Map<String, Double> variables, int index) {
        var value = variables.get(this.values[index]);
        if (value == null) {
            return Double.parseDouble(this.values[index]);
        }
        return value;
    }
}
