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

/**
 * Matches permission strings against a pattern with wildcards.
 */
public class AtlantisPermission {

    private final String node;
    private final Pattern pattern;

    /**
     * Characters in the pattern match only themselves, except for these wildcards.
     * In this list, a name character is a letter, a digit, {@code _}, {@code .} or {@code -}.
     * <ul>
     *     <li>{@code *} matches one segment that starts with a letter and has no dots.</li>
     *     <li>{@code +} matches one or more name characters.</li>
     *     <li>{@code ?} matches zero or more name characters.</li>
     *     <li>{@code ^} matches one or more digits.</li>
     *     <li>{@code .+} matches nothing, or a dot followed by name characters.
     *     For example, {@code a.+} matches {@code a} and {@code a.b.c}.</li>
     * </ul>
     *
     * @param node the pattern. It must not be empty or contain spaces or underscores.
     */
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

    /**
     * Converts the pattern to a regular expression. The wildcards are described in
     * {@link #AtlantisPermission(String)}, and all other characters are quoted.
     *
     * @param node the pattern
     * @return the regular expression
     */
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

    /**
     * @return {@code true} if the pattern matches the full permission
     */
    public boolean matches(String permission) {
        return this.pattern.matcher(permission).matches();
    }

    /**
     * Reads and writes a permission as a JSON string that holds its pattern.
     */
    public static class TypeAdapter implements JsonDeserializer<AtlantisPermission>, JsonSerializer<AtlantisPermission> {

        /**
         * Creates a permission from the pattern in the JSON string.
         *
         * @param json the JSON string
         * @param typeOfT the type to read
         * @param context the deserialization context
         * @return the permission
         */
        @Override
        public AtlantisPermission deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new AtlantisPermission(json.getAsString());
        }

        /**
         * Writes the pattern of the permission as a JSON string.
         *
         * @param src the permission
         * @param typeOfSrc the type to write
         * @param context the serialization context
         * @return the JSON string
         */
        @Override
        public JsonElement serialize(AtlantisPermission src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.node);
        }
    }
}
