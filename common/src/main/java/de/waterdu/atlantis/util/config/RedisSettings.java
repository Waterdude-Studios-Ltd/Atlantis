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

public abstract class RedisSettings {

    @SuppressWarnings("unchecked")
    protected static final TypeToken<Map<String, String>> STRING_STRING_MAP_TYPE = (TypeToken<Map<String, String>>) TypeToken.getParameterized(Map.class, String.class, String.class);

    protected Map<String, String> properties;

    private RedisSettings(Map<String, String> properties) {
        this.properties = properties;
    }

    public abstract Redis storage(String modid);

    public static class URL extends RedisSettings {

        private final String url;

        public URL() {
            this("redis://username:password@ip:port/");
        }

        public URL(String url) {
            this(url, new HashMap<>());
        }

        public URL(String url, Map<String, String> properties) {
            super(properties);
            this.url = url;
        }

        @Override
        public Redis storage(String modid) {
            return new Redis(this.url);
        }
    }

    public static RedisSettings getDefault() {
        return new Explicit();
    }

    public static class Explicit extends RedisSettings {

        private final String ip;
        private final int port;
        private final String username;
        private final String password;

        public Explicit() {
            this("127.0.0.1", 6379, "username", "password");
        }

        public Explicit(String ip, int port, String username, String password) {
            this(ip, port, username, password, new HashMap<>());
        }

        public Explicit(String ip, int port, String username, String password, Map<String, String> properties) {
            super(properties);
            this.ip = ip;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        @Override
        public Redis storage(String modid) {
            return new Redis(this.ip, this.port, this.username, this.password.toCharArray());
        }
    }

    public static class TypeAdapter implements JsonSerializer<RedisSettings>, JsonDeserializer<RedisSettings> {

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