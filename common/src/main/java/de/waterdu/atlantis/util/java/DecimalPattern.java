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

public class DecimalPattern {

    private final String pattern;
    private final Lazy<DecimalFormat> formatter;

    private DecimalPattern(String pattern) {
        this.pattern = pattern;
        this.formatter = Lazy.of(() -> new DecimalFormat(pattern));
    }

    public static DecimalPattern of(String pattern) {
        return new DecimalPattern(pattern);
    }

    public String format(long number) {
        return this.formatter.get().format(number);
    }

    public String format(double number) {
        return this.formatter.get().format(number);
    }

    public DecimalFormat formatter() {
        return this.formatter.get();
    }

    public String pattern() {
        return this.pattern;
    }

    public static class TypeAdapter implements JsonSerializer<DecimalPattern>, JsonDeserializer<DecimalPattern> {

        @Override
        public DecimalPattern deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return DecimalPattern.of(json.getAsString());
        }

        @Override
        public JsonElement serialize(DecimalPattern src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.pattern());
        }
    }
}
