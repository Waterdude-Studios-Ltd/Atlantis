package de.waterdu.atlantis.util.java.math;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Variable {

    private double startingValue = 0.0;
    private List<Operation> operators = new ArrayList<>();
    private Comparator comparator = null;

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

    public double startingValue() {
        return this.startingValue;
    }

    public void setStartingValue(double startingValue) {
        this.startingValue = startingValue;
    }

    public List<Operation> operators() {
        return this.operators;
    }

    public void setOperators(List<Operation> operators) {
        this.operators = operators;
    }

    public Comparator comparator() {
        return this.comparator;
    }

    public void setComparator(Comparator comparator) {
        this.comparator = comparator;
    }

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
