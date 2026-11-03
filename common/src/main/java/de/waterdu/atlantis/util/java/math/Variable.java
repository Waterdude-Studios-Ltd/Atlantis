package de.waterdu.atlantis.util.java.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Holds the parsed expression of one {@link Formula} line, as a list of operations or as a comparison.
 */
public class Variable {

    private double startingValue = 0.0;
    private List<Operation> operators = new ArrayList<>();
    private Comparator comparator = null;

    /**
     * Parses an expression. A function such as {@code SQRT(x)} or a comparison such as {@code a>b?c:d} takes the
     * whole expression. Otherwise the expression is a chain of values and operators, and {@code RAND} adds a random
     * value.
     *
     * @param formula the expression, without spaces
     */
    public Variable(String formula) {
        var modified = false;
        while (!formula.isEmpty()) {
            if (formula.contains("(")) {
                var name = formula.substring(0, formula.indexOf("("));
                var operator = Operator.get(name);
                formula = formula.substring(formula.indexOf("("));
                formula = formula.replace("(", "").replace(")", "");
                this.operators.add(new Operation(operator, formula));
                formula = "";
            } else if (formula.contains("?")) {
                var comparator = Comparators.find(formula);
                var split = formula.split("\\?");
                var splitLHS = split[0].split(comparator.getRepresentation());
                var splitRHS = split[1].split(":");
                this.comparator = new Comparator(comparator, splitLHS[0], splitLHS[1], splitRHS[0], splitRHS[1]);
                formula = "";
            }
            else {
                if (formula.contains("RAND") || formula.contains("rand")) {
                    modified = true;
                    this.operators.add(new Operation(Operator.RAND, "0"));
                    formula = formula.replace("RAND", "").replace("rand", "");
                } else {
                    var symbols = new ArrayList<String>();
                    var values = new ArrayList<String>();
                    var patternSymbols = Pattern.compile("[+\\-/*^%]");
                    var matcherSymbols = patternSymbols.matcher(formula);
                    var patternValues = Pattern.compile("[^+\\-/*^%]+");
                    var matcherValues = patternValues.matcher(formula);
                    while (matcherSymbols.find()) {
                        var s = matcherSymbols.group();
                        if (!s.isEmpty()) {
                            symbols.add(s);
                        }
                    }
                    while (matcherValues.find()) {
                        var v = matcherValues.group();
                        if (!v.isEmpty()) {
                            values.add(v);
                        }
                    }
                    var symbolIndex = 0;
                    var valueIndex = 0;
                    for (var value : values) {
                        if (!modified) {
                            var leading = symbols.size() == values.size() ? Operator.get(symbols.get(symbolIndex++)) : Operator.ADD;
                            this.operators.add(new Operation(leading, values.get(valueIndex++)));
                            modified = true;
                        } else {
                            this.operators.add(new Operation(Operator.get(symbols.get(symbolIndex++)), values.get(valueIndex++)));
                        }
                    }
                    formula = "";
                }
            }
        }
    }

    /**
     * @return the starting value. {@link #calculate} does not use it.
     */
    public double startingValue() {
        return this.startingValue;
    }

    /**
     * Sets the starting value. {@link #calculate} does not use it.
     *
     * @param startingValue the new starting value
     */
    public void setStartingValue(double startingValue) {
        this.startingValue = startingValue;
    }

    /**
     * @return the operations that apply in order
     */
    public List<Operation> operators() {
        return this.operators;
    }

    /**
     * Sets the operations that apply in order.
     *
     * @param operators the new operations
     */
    public void setOperators(List<Operation> operators) {
        this.operators = operators;
    }

    /**
     * @return the comparison of the expression, or {@code null} if it has none
     */
    public Comparator comparator() {
        return this.comparator;
    }

    /**
     * Sets the comparison. If it is not {@code null}, it replaces the operations in {@link #calculate}.
     *
     * @param comparator the new comparison, or {@code null}
     */
    public void setComparator(Comparator comparator) {
        this.comparator = comparator;
    }

    /**
     * Calculates the value of the expression. If there is a comparison, it gives the result. Otherwise the
     * operations apply in order to a value that starts at 0.
     *
     * @param variables the known variables, by name
     * @return the value
     */
    public double calculate(Map<String, Double> variables) {
        var value = 0.0;
        if (this.comparator != null) {
            return this.comparator.evaluate(variables);
        }
        for (var operator : this.operators) {
            value = operator.calculate(value, variables);
        }
        return value;
    }
}
