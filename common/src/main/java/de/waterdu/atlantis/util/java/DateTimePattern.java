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
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Holds a date and time pattern and the {@link DateTimeFormatter} for it. The formatter is created on first use.
 */
public class DateTimePattern {

    private final String pattern;
    private final Lazy<DateTimeFormatter> formatter;

    /**
     * Creates a pattern.
     *
     * @param pattern the pattern for {@link DateTimeFormatter#ofPattern(String)}
     */
    private DateTimePattern(String pattern) {
        this.pattern = pattern;
        this.formatter = Lazy.of(() -> DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Creates a pattern.
     *
     * @param pattern the pattern for {@link DateTimeFormatter#ofPattern(String)}
     * @return the new pattern
     */
    public static DateTimePattern of(String pattern) {
        return new DateTimePattern(pattern);
    }

    /**
     * Formats a date or time with this pattern.
     *
     * @param temporal the date or time to format
     * @return the formatted text
     */
    public String format(TemporalAccessor temporal) {
        return this.formatter.get().format(temporal);
    }

    /**
     * @return the formatter for this pattern
     */
    public DateTimeFormatter formatter() {
        return this.formatter.get();
    }

    /**
     * @return the pattern text
     */
    public String pattern() {
        return this.pattern;
    }

    /**
     * Serializes a {@link DateTimePattern} as its pattern text.
     */
    public static class TypeAdapter implements JsonSerializer<DateTimePattern>, JsonDeserializer<DateTimePattern> {

        /**
         * Creates a pattern from the JSON string.
         *
         * @param json the JSON string that holds the pattern
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context
         * @return the pattern
         */
        @Override
        public DateTimePattern deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return DateTimePattern.of(json.getAsString());
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
        public JsonElement serialize(DateTimePattern src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.pattern());
        }
    }
}
