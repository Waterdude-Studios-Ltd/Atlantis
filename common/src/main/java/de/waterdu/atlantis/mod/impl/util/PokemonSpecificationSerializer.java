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

/**
 * Converts Pokémon spec strings to JSON objects and back. Each spec part becomes one key, and the first part is stored
 * under {@code species}.
 */
public class PokemonSpecificationSerializer {

    private static final String SPECIES_SPEC = "species";

    private PokemonSpecificationSerializer() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Gets the string between the key and the value of a spec part in the active Pokémon mod.
     *
     * @return the spec delimiter
     */
    private static String delimiter() {
        return Mods.pokemon().specDelimiter();
    }

    /**
     * Converts the original string of a spec to a JSON object.
     *
     * @param spec the spec
     * @return the JSON object
     */
    public static JsonObject serialize(UPokemonSpec<?, ?> spec) {
        return PokemonSpecificationSerializer.serialize(spec.originalString());
    }

    /**
     * Converts the spec string of a configurable Pokémon to a JSON object.
     *
     * @param pokemon the configurable Pokémon
     * @return the JSON object
     */
    public static JsonObject serialize(ConfigurablePokemon pokemon) {
        return PokemonSpecificationSerializer.serialize(pokemon.originalSpec());
    }

    /**
     * Converts a spec string to a JSON object. The first part is the species, and its name is looked up with the
     * Pokémon delegate. A part without a value becomes {@code true}, or {@code false} if it starts with {@code !}.
     *
     * @param spec the spec string, with parts separated by spaces
     * @return the JSON object
     */
    public static JsonObject serialize(String spec) {
        var split = spec.split(" ");
        var map = UnaryMap.<String>create(split.length);
        for (var i = 0; i < split.length; i++) {
            var pair = split[i].split(delimiter());
            if (i == 0) {
                map.put(SPECIES_SPEC, Mods.pokemon().species(pair[0]).name());
            } else {
                map.put(pair[0], pair.length == 1 ? "" : pair[1]);
            }
        }
        return PokemonSpecificationSerializer.serialize(map);
    }

    /**
     * Converts a map of spec keys and values to a JSON object. Values become booleans, numbers or strings.
     *
     * @param specs the spec keys mapped to their values. An empty value is a boolean flag.
     * @return the JSON object
     */
    public static JsonObject serialize(UnaryMap<String> specs) {
        var object = new JsonObject();
        for (var pair : specs.pairs()) {
            SpecValue.parse(pair).addToJsonObject(object);
        }
        return object;
    }

    /**
     * Converts a JSON object back to a spec string, in the order of its keys.
     *
     * @param object the JSON object
     * @return the configurable Pokémon of the spec string
     */
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

    /**
     * Holds the key and the typed value of one spec part.
     *
     * @param <T> the type of the value
     */
    private static class SpecValue<T> {

        private final String spec;
        private final T value;

        private final boolean isBoolean;
        private final boolean isNumber;
        private final boolean isString;

        /**
         * Creates a spec value and records whether the value is a boolean, a number or a string.
         *
         * @param spec the key of the spec part
         * @param value the value of the spec part
         */
        private SpecValue(String spec, T value) {
            this.spec = spec;
            this.value = value;
            this.isBoolean = value instanceof Boolean;
            this.isNumber = value instanceof Number;
            this.isString = value instanceof String;
        }

        /**
         * Parses a key and value pair from a spec string. An empty value becomes {@code true}, or {@code false} with
         * the {@code !} removed from the key if the key starts with {@code !}.
         *
         * @param spec the key and the value
         * @return the spec value
         */
        public static SpecValue<?> parse(UnaryPair<String> spec) {
            if (spec.getB().isEmpty()) {
                if (spec.getA().startsWith("!")) {
                    return new SpecValue<>(spec.getA().substring(1), false);
                } else {
                    return new SpecValue<>(spec.getA(), true);
                }
            } else if (TextUtils.isNumberParsable(spec.getB())) {
                return new SpecValue<>(spec.getA(), SpecValue.number(spec.getB()));
            } else {
                return new SpecValue<>(spec.getA(), spec.getB());
            }
        }

        /**
         * Parses a key and a JSON value. The value becomes a boolean, a number or a string.
         *
         * @param spec the key
         * @param element the JSON value, read as a string
         * @return the spec value
         */
        public static SpecValue<?> parse(String spec, JsonElement element) {
            var value = element.getAsString();
            if (value.equals("true") || value.equals("false")) {
                return new SpecValue<>(spec, Boolean.parseBoolean(value));
            } else if (TextUtils.isNumberParsable(value)) {
                return new SpecValue<>(spec, SpecValue.number(value));
            } else {
                return new SpecValue<>(spec, value);
            }
        }

        /**
         * Parses a number as a {@link Long}, or as a {@link Double} if it is not a whole number.
         *
         * @param value the text to parse
         * @return the number
         */
        private static Number number(String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return Double.parseDouble(value);
            }
        }

        /**
         * Converts this value to a spec part.
         *
         * @return only the value for the {@code species} key, otherwise the key, the delimiter and the value
         */
        public String toFullSpec() {
            if (SPECIES_SPEC.equalsIgnoreCase(this.spec)) {
                return this.value.toString();
            }
            return this.spec + delimiter() + this.value.toString();
        }

        /**
         * Converts the value to a JSON primitive.
         *
         * @return the JSON primitive, or an empty string if the value is not a boolean, a number or a string
         */
        public JsonPrimitive toJson() {
            if (this.isBoolean) {
                return new JsonPrimitive((Boolean) this.value);
            } else if (this.isNumber) {
                return new JsonPrimitive((Number) this.value);
            } else if (this.isString) {
                return new JsonPrimitive((String) this.value);
            } else {
                return new JsonPrimitive("");
            }
        }

        /**
         * Adds this value to the object under its key.
         *
         * @param object the JSON object to add to
         */
        public void addToJsonObject(JsonObject object) {
            object.add(this.spec, this.toJson());
        }
    }
}
