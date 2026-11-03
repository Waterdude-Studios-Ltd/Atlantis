package de.waterdu.atlantis.util.java.math;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.util.java.JsonUtils;
import de.waterdu.atlantis.util.java.collection.ArrayUtils;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates named variables from lines of simple expressions, for example {@code damage=base*2}.
 */
public final class Formula {

    private final Map<String, Variable> variables = new LinkedHashMap<>();
    private final Collection<String> lines;

    /**
     * Creates a formula and parses each line that contains {@code =} into a variable.
     *
     * @param lines the lines of the formula
     */
    private Formula(Collection<String> lines) {
        this.lines = lines;
        for (var segment : lines) {
            segment = segment.replace(" ", "");
            if (segment.contains("=")) {
                var equalsSplit = new String[]{segment.substring(0, segment.indexOf("=")), segment.substring(segment.indexOf("=") + 1)};
                var variable = equalsSplit[0];
                var formula = equalsSplit[1];
                this.variables.put(variable, new Variable(formula));
            }
        }
    }

    /**
     * Splits the formula at spaces. Each part is one line, so a line cannot contain spaces.
     */
    public static Formula of(String formula) {
        return Formula.of(formula.split(" "));
    }

    /**
     * Creates a formula from the given lines.
     *
     * @param lines the lines of the formula
     * @return the formula
     */
    public static Formula of(String... lines) {
        return Formula.of(Arrays.asList(lines));
    }

    /**
     * Each line has the form {@code name=expression}. Lines without {@code =} are ignored, and spaces are removed.
     */
    public static Formula of(Collection<String> lines) {
        return new Formula(lines);
    }

    /**
     * Uses each line of the text block that is not blank as one line.
     */
    public static Formula byLines(String textblock) {
        return Formula.of(ArrayUtils.mapNotNull(String.class, s -> {
            var trimmed = s.trim();
            if (trimmed.isBlank()) {
                return null;
            }
            return trimmed;
        }, textblock.split("\n")));
    }

    /**
     * Calculates the lines in order. A line can use the default variables, the constants and the results of earlier
     * lines. Operators apply from left to right, without precedence.
     *
     * @return the default variables, the constants and the result of each line
     */
    public Map<String, Double> process(Map<String, Double> defaultVariables) {
        var variables = new LinkedHashMap<>(defaultVariables);
        for (var constant : Constant.values()) {
            for (var representation : constant.representations()) {
                variables.put(representation, constant.value());
            }
        }
        for (var entry : this.variables.entrySet()) {
            var name = String.valueOf(entry.getKey());
            var value = entry.getValue().calculate(variables);
            variables.put(name, value);
        }
        return variables;
    }

    /**
     * @return the lines of the formula, as given when it was created
     */
    public Collection<String> lines() {
        return this.lines;
    }

    /**
     * Creates a new formula from an immutable copy of the lines.
     *
     * @return the copy
     */
    public Formula copy() {
        return new Formula(List.copyOf(this.lines));
    }

    /**
     * Converts a {@link Formula} to and from a JSON array of its lines.
     */
    public static class TypeAdapter implements JsonSerializer<Formula>, JsonDeserializer<Formula> {

        /**
         * Creates a formula from a JSON array of lines.
         *
         * @param json the JSON array
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context
         * @return the formula
         */
        @Override
        public Formula deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new Formula(JsonUtils.deserializeList(json.getAsJsonArray(), JsonElement::getAsString));
        }

        /**
         * Writes the lines of the formula as a JSON array of strings.
         *
         * @param src the formula
         * @param typeOfSrc the type of the formula
         * @param context the serialization context
         * @return the JSON array
         */
        @Override
        public JsonElement serialize(Formula src, Type typeOfSrc, JsonSerializationContext context) {
            return JsonUtils.serializeList(src.lines(), JsonPrimitive::new);
        }
    }
}
