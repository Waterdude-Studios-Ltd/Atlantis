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

public class DateTimePattern {

    private final String pattern;
    private final Lazy<DateTimeFormatter> formatter;

    private DateTimePattern(String pattern) {
        this.pattern = pattern;
        this.formatter = Lazy.of(() -> DateTimeFormatter.ofPattern(pattern));
    }

    public static DateTimePattern of(String pattern) {
        return new DateTimePattern(pattern);
    }

    public String format(TemporalAccessor temporal) {
        return this.formatter.get().format(temporal);
    }

    public DateTimeFormatter formatter() {
        return this.formatter.get();
    }

    public String pattern() {
        return this.pattern;
    }

    public static class TypeAdapter implements JsonSerializer<DateTimePattern>, JsonDeserializer<DateTimePattern> {

        @Override
        public DateTimePattern deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return DateTimePattern.of(json.getAsString());
        }

        @Override
        public JsonElement serialize(DateTimePattern src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.pattern());
        }
    }
}
