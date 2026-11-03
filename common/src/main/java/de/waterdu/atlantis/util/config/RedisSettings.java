package de.waterdu.atlantis.util.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.file.storage.Redis;
import de.waterdu.atlantis.util.java.JsonUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the connection settings of a Redis server, either as a URL or as explicit values.
 */
public abstract class RedisSettings {

    @SuppressWarnings("unchecked")
    protected static final TypeToken<Map<String, String>> STRING_STRING_MAP_TYPE = (TypeToken<Map<String, String>>) TypeToken.getParameterized(Map.class, String.class, String.class);

    protected Map<String, String> properties;

    /**
     * @param properties the extra properties of the connection
     */
    private RedisSettings(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Creates a Redis storage that connects with these settings.
     *
     * @param modId the ID of the mod that uses the storage
     * @return the Redis storage, which connects immediately
     */
    public abstract Redis storage(String modId);

    /**
     * Holds the connection settings of a Redis server as a URL.
     */
    public static class URL extends RedisSettings {

        private final String url;

        /**
         * Creates settings with a placeholder URL.
         */
        public URL() {
            this("redis://username:password@ip:port/");
        }

        /**
         * Creates settings with no extra properties.
         *
         * @param url the Redis URL
         */
        public URL(String url) {
            this(url, new HashMap<>());
        }

        /**
         * @param url the Redis URL
         * @param properties the extra properties of the connection
         */
        public URL(String url, Map<String, String> properties) {
            super(properties);
            this.url = url;
        }

        /**
         * Creates a Redis storage that connects to the URL.
         *
         * @param modId the ID of the mod that uses the storage. It is not used.
         * @return the Redis storage
         */
        @Override
        public Redis storage(String modId) {
            return new Redis(this.url);
        }
    }

    /**
     * @return new explicit settings for {@code 127.0.0.1:6379} with placeholder credentials
     */
    public static RedisSettings getDefault() {
        return new Explicit();
    }

    /**
     * Holds the connection settings of a Redis server as an IP address, port, username and password.
     */
    public static class Explicit extends RedisSettings {

        private final String ip;
        private final int port;
        private final String username;
        private final String password;

        /**
         * Creates settings for {@code 127.0.0.1:6379} with placeholder credentials.
         */
        public Explicit() {
            this("127.0.0.1", 6379, "username", "password");
        }

        /**
         * Creates settings with no extra properties.
         *
         * @param ip the IP address or host name of the server
         * @param port the port of the server
         * @param username the username, or an empty string for no authentication
         * @param password the password
         */
        public Explicit(String ip, int port, String username, String password) {
            this(ip, port, username, password, new HashMap<>());
        }

        /**
         * @param ip the IP address or host name of the server
         * @param port the port of the server
         * @param username the username, or an empty string for no authentication
         * @param password the password
         * @param properties the extra properties of the connection
         */
        public Explicit(String ip, int port, String username, String password, Map<String, String> properties) {
            super(properties);
            this.ip = ip;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        /**
         * Creates a Redis storage that connects to the IP address and port with the credentials.
         *
         * @param modId the ID of the mod that uses the storage. It is not used.
         * @return the Redis storage
         */
        @Override
        public Redis storage(String modId) {
            return new Redis(this.ip, this.port, this.username, this.password.toCharArray());
        }
    }

    /**
     * Serializes Redis settings to JSON and deserializes them from JSON.
     */
    public static class TypeAdapter implements JsonSerializer<RedisSettings>, JsonDeserializer<RedisSettings> {

        /**
         * Reads a JSON string as {@link URL} settings. Reads a JSON object as {@link URL} settings if it has a
         * {@code url} property, and as {@link Explicit} settings if not.
         *
         * @param json the JSON string or object
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context, which reads the properties
         * @return the Redis settings
         */
        @Override
        public RedisSettings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                return new URL(json.getAsString());
            } else {
                var object = json.getAsJsonObject();
                var properties = JsonUtils.getOrDefault(object, "properties", RedisSettings.STRING_STRING_MAP_TYPE, new HashMap<>(), context);
                RedisSettings settings;
                if (object.has("url")) {
                    settings = new URL(
                            object.get("url").getAsString(),
                            properties
                    );
                } else {
                    settings = new Explicit(
                            object.get("ip").getAsString(),
                            object.get("port").getAsInt(),
                            object.get("username").getAsString(),
                            object.get("password").getAsString(),
                            properties
                    );
                }
                return settings;
            }
        }

        /**
         * Writes the settings to a JSON object. The properties are written only if there are any.
         *
         * @param src the settings to serialize
         * @param typeOfSrc the type of the settings
         * @param context the serialization context, which writes the properties
         * @return the JSON object
         */
        @Override
        public JsonElement serialize(RedisSettings src, Type typeOfSrc, JsonSerializationContext context) {
            var object = new JsonObject();
            if (src instanceof URL url) {
                object.addProperty("url", url.url);
            } else if (src instanceof Explicit explicit) {
                object.addProperty("ip", explicit.ip);
                object.addProperty("port", explicit.port);
                object.addProperty("username", explicit.username);
                object.addProperty("password", explicit.password);
            }
            if (!src.properties.isEmpty()) {
                object.add("properties", context.serialize(src.properties));
            }
            return object;
        }
    }
}