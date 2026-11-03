package de.waterdu.atlantis.util.java;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.util.java.interfaces.Lazy;

import java.lang.reflect.Type;
import java.text.DecimalFormat;

/**
 * Holds a decimal pattern and the {@link DecimalFormat} for it. The format is created on first use.
 */
public class DecimalPattern {

    private final String pattern;
    private final Lazy<DecimalFormat> formatter;

    /**
     * Creates a pattern.
     *
     * @param pattern the pattern for {@link DecimalFormat}
     */
    private DecimalPattern(String pattern) {
        this.pattern = pattern;
        this.formatter = Lazy.of(() -> new DecimalFormat(pattern));
    }

    /**
     * Creates a pattern.
     *
     * @param pattern the pattern for {@link DecimalFormat}
     * @return the new pattern
     */
    public static DecimalPattern of(String pattern) {
        return new DecimalPattern(pattern);
    }

    /**
     * Formats a number with this pattern.
     *
     * @param number the number to format
     * @return the formatted text
     */
    public String format(long number) {
        return this.formatter.get().format(number);
    }

    /**
     * Formats a number with this pattern.
     *
     * @param number the number to format
     * @return the formatted text
     */
    public String format(double number) {
        return this.formatter.get().format(number);
    }

    /**
     * @return the formatter that {@link #format(double)} uses. It is shared, so do not modify it.
     */
    public DecimalFormat formatter() {
        return this.formatter.get();
    }

    /**
     * @return the pattern text
     */
    public String pattern() {
        return this.pattern;
    }

    /**
     * Serializes a {@link DecimalPattern} as its pattern text.
     */
    public static class TypeAdapter implements JsonSerializer<DecimalPattern>, JsonDeserializer<DecimalPattern> {

        /**
         * Creates a pattern from the JSON string.
         *
         * @param json the JSON string that holds the pattern
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context
         * @return the pattern
         */
        @Override
        public DecimalPattern deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return DecimalPattern.of(json.getAsString());
        }

        /**
         * Writes the pattern text as a JSON string.
         *
         * @param src the pattern to serialize
         * @param typeOfSrc the type of the pattern
         * @param context the serialization context
         * @return the JSON string
         */
        @Override
        public JsonElement serialize(DecimalPattern src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.pattern());
        }
    }
}
