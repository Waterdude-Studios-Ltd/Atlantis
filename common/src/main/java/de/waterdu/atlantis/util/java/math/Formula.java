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

public final class Formula {

    private final Map<String, Variable> variables = new LinkedHashMap<>();
    private final Collection<String> lines;

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

    public static Formula of(String formula) {
        return Formula.of(formula.split(" "));
    }

    public static Formula of(String... lines) {
        return Formula.of(Arrays.asList(lines));
    }

    public static Formula of(Collection<String> lines) {
        return new Formula(lines);
    }

    public static Formula byLines(String textblock) {
        return Formula.of(ArrayUtils.mapNotNull(String.class, s -> {
            var trimmed = s.trim();
            if (trimmed.isBlank()) {
                return null;
            }
            return trimmed;
        }, textblock.split("\n")));
    }

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

    public Collection<String> lines() {
        return this.lines;
    }

    public Formula copy() {
        return new Formula(List.copyOf(this.lines));
    }

    public static class TypeAdapter implements JsonSerializer<Formula>, JsonDeserializer<Formula> {

        @Override
        public Formula deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new Formula(JsonUtils.deserializeList(json.getAsJsonArray(), JsonElement::getAsString));
        }

        @Override
        public JsonElement serialize(Formula src, Type typeOfSrc, JsonSerializationContext context) {
            return JsonUtils.serializeList(src.lines(), JsonPrimitive::new);
        }
    }
}
