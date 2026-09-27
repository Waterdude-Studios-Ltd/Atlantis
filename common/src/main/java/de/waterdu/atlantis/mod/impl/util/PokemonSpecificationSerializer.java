package de.waterdu.atlantis.mod.impl.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.wrapper.pokemon.parsing.UPokemonSpec;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.java.collection.UnaryMap;
import de.waterdu.atlantis.util.java.collection.UnaryPair;
import de.waterdu.atlantis.util.text.TextUtils;

public class PokemonSpecificationSerializer {

    private static final String SPECIES_SPEC = "species";
    private static final String DELIMITER = Mods.pokemon().specDelimiter();

    private PokemonSpecificationSerializer() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static JsonObject serialize(UPokemonSpec<?, ?> spec) {
        return PokemonSpecificationSerializer.serialize(spec.originalString());
    }

    public static JsonObject serialize(ConfigurablePokemon pokemon) {
        return PokemonSpecificationSerializer.serialize(pokemon.originalSpec());
    }

    public static JsonObject serialize(String spec) {
        var split = spec.split(" ");
        var map = UnaryMap.<String>create(split.length);
        for (var i = 0; i < split.length; i++) {
            var pair = split[i].split(DELIMITER);
            if (i == 0) {
                map.put(SPECIES_SPEC, Mods.pokemon().species(pair[0]).name());
            } else {
                map.put(pair[0], pair.length == 1 ? "" : pair[1]);
            }
        }
        return PokemonSpecificationSerializer.serialize(map);
    }

    public static JsonObject serialize(UnaryMap<String> specs) {
        var object = new JsonObject();
        for (var pair : specs.pairs()) {
            SpecValue.parse(pair).addToJsonObject(object);
        }
        return object;
    }

    public static ConfigurablePokemon deserialize(JsonObject object) {
        var builder = new StringBuilder();
        for (var entry : object.asMap().entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(" ");
            }
            builder.append(SpecValue.parse(entry.getKey(), entry.getValue()).toFullSpec());
        }
        return ConfigurablePokemon.of(builder.toString());
    }

    private static class SpecValue<T> {

        private final String spec;
        private final T value;

        private final boolean isBoolean;
        private final boolean isNumber;
        private final boolean isString;

        private SpecValue(String spec, T value) {
            this.spec = spec;
            this.value = value;
            this.isBoolean = value instanceof Boolean;
            this.isNumber = value instanceof Double;
            this.isString = value instanceof String;
        }

        public static SpecValue<?> parse(UnaryPair<String> spec) {
            if (spec.getB().isEmpty()) {
                if (spec.getA().startsWith("!")) {
                    return new SpecValue<>(spec.getA().substring(1), false);
                } else {
                    return new SpecValue<>(spec.getA(), true);
                }
            } else if (TextUtils.isNumberParsable(spec.getB())) {
                return new SpecValue<>(spec.getA(), Double.parseDouble(spec.getB()));
            } else {
                return new SpecValue<>(spec.getA(), spec.getB());
            }
        }

        public static SpecValue<?> parse(String spec, JsonElement element) {
            var value = element.getAsString();
            if (value.equals("true") || value.equals("false")) {
                return new SpecValue<>(spec, Boolean.parseBoolean(value));
            } else if (TextUtils.isNumberParsable(value)) {
                return new SpecValue<>(spec, Double.parseDouble(value));
            } else {
                return new SpecValue<>(spec, value);
            }
        }

        public String toFullSpec() {
            if (SPECIES_SPEC.equalsIgnoreCase(this.spec)) {
                return this.value.toString();
            }
            return this.spec + DELIMITER + this.value.toString();
        }

        public JsonPrimitive toJson() {
            if (this.isBoolean) {
                return new JsonPrimitive((Boolean) this.value);
            } else if (this.isNumber) {
                return new JsonPrimitive((Double) this.value);
            } else if (this.isString) {
                return new JsonPrimitive((String) this.value);
            } else {
                return new JsonPrimitive("");
            }
        }

        public void addToJsonObject(JsonObject object) {
            object.add(this.spec, this.toJson());
        }
    }
}
