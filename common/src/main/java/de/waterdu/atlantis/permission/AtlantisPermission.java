package de.waterdu.atlantis.permission;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.regex.Pattern;

public class AtlantisPermission {

    private final String node;
    private final Pattern pattern;

    public AtlantisPermission(String node) {
        if (node.isEmpty()) {
            throw new IllegalArgumentException("Permission node cannot be empty");
        }
        if (node.contains(" ") || node.contains("_")) {
            throw new IllegalArgumentException("Permission node cannot contain whitespace");
        }
        this.node = node;
        this.pattern = Pattern.compile(toRegex(node));
    }

    private static String toRegex(String node) {
        var regex = new StringBuilder();
        for (var i = 0; i < node.length(); i++) {
            var c = node.charAt(i);
            if (c == '.' && i + 1 < node.length() && node.charAt(i + 1) == '+') {
                regex.append("(\\.[A-Za-z0-9_.-]+)?");
                i++;
                continue;
            }
            switch (c) {
                case '*' -> regex.append("([A-Za-z][A-Za-z0-9_-]*)");
                case '+' -> regex.append("([A-Za-z0-9_.-]+)");
                case '?' -> regex.append("([A-Za-z0-9_.-]*)");
                case '^' -> regex.append("([0-9]+)");
                default -> regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return regex.toString();
    }

    public boolean matches(String permission) {
        return this.pattern.matcher(permission).matches();
    }

    public static class TypeAdapter implements JsonDeserializer<AtlantisPermission>, JsonSerializer<AtlantisPermission> {

        @Override
        public AtlantisPermission deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new AtlantisPermission(json.getAsString());
        }

        @Override
        public JsonElement serialize(AtlantisPermission src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.node);
        }
    }
}
