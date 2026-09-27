package de.waterdu.atlantis.util.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.storage.Database;
import de.waterdu.atlantis.util.java.JsonUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public abstract class DatabaseSettings {

    @SuppressWarnings("unchecked")
    protected static final TypeToken<Map<String, String>> STRING_STRING_MAP_TYPE = (TypeToken<Map<String, String>>) TypeToken.getParameterized(Map.class, String.class, String.class);

    protected static final int MIN_IDLE_DEFAULT = 5;
    protected static final int CONNECTION_TIMEOUT_DEFAULT = 10000;
    protected static final int IDLE_TIMEOUT_DEFAULT = 600000;
    protected static final int LEAK_DETECTION_DEFAULT = 600000;

    protected int connections;

    protected int minIdle = DatabaseSettings.MIN_IDLE_DEFAULT;
    protected int connectionTimeout = DatabaseSettings.CONNECTION_TIMEOUT_DEFAULT;
    protected int idleTimeout = DatabaseSettings.IDLE_TIMEOUT_DEFAULT;
    protected int leakDetection = DatabaseSettings.LEAK_DETECTION_DEFAULT;

    protected Map<String, String> properties;

    private DatabaseSettings(int connections, Map<String, String> properties) {
        this.connections = connections;
        this.properties = properties;
    }

    public abstract Database storage(String modid);

    public int connections() {
        return this.connections;
    }

    public boolean areExtraSettingsDefaults() {
        return this.minIdle == DatabaseSettings.MIN_IDLE_DEFAULT &&
                this.connectionTimeout == DatabaseSettings.CONNECTION_TIMEOUT_DEFAULT &&
                this.idleTimeout == DatabaseSettings.IDLE_TIMEOUT_DEFAULT &&
                this.leakDetection == DatabaseSettings.LEAK_DETECTION_DEFAULT &&
                this.properties.isEmpty();
    }

    public static class URL extends DatabaseSettings {

        private final String url;

        public URL() {
            this("jdbc:mysql://user:password@ip:port/");
        }

        public URL(String url) {
            this(Settings.get().getDefaultDatabaseConnections(), url, new HashMap<>());
        }

        public URL(int connections, String url, Map<String, String> properties) {
            super(connections, properties);
            this.url = url;
        }

        @Override
        public Database storage(String modid) {
            return new Database(this.url, modid, this.connections());
        }
    }

    public static DatabaseSettings getDefault() {
        return new Explicit();
    }

    public static class Explicit extends DatabaseSettings {

        private final String ip;
        private final int port;
        private final String username;
        private final String password;

        public Explicit() {
            this("127.0.0.1", 3306, "username", "password");
        }

        public Explicit(String ip, int port, String username, String password) {
            this(Settings.get().getDefaultDatabaseConnections(), ip, port, username, password, new HashMap<>());
        }

        public Explicit(int connections, String ip, int port, String username, String password, Map<String, String> properties) {
            super(connections, properties);
            this.ip = ip;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        @Override
        public Database storage(String modid) {
            return new Database(modid, this.ip, this.port, this.username, this.password, this.connections());
        }
    }

    public static class TypeAdapter implements JsonSerializer<DatabaseSettings>, JsonDeserializer<DatabaseSettings> {

        @Override
        public DatabaseSettings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                var url = json.getAsString();
                var split = url.split(" ");
                if (split.length == 2) {
                    return new URL(Integer.parseInt(split[0]), split[1], new HashMap<>());
                } else {
                    return new URL(url);
                }
            } else {
                var object = json.getAsJsonObject();
                var connections = JsonUtils.getOrDefault(object, "connections", Integer.TYPE, Settings.get().getDefaultDatabaseConnections(), context);
                var properties = JsonUtils.getOrDefault(object, "properties", DatabaseSettings.STRING_STRING_MAP_TYPE, new HashMap<>(), context);
                DatabaseSettings settings;
                if (object.has("url")) {
                    settings = new URL(
                            connections,
                            object.get("url").getAsString(),
                            properties
                    );
                } else {
                    settings = new Explicit(
                            connections,
                            object.get("ip").getAsString(),
                            object.get("port").getAsInt(),
                            object.get("username").getAsString(),
                            object.get("password").getAsString(),
                            properties
                    );
                }
                settings.minIdle = JsonUtils.getOrDefault(object, "minIdle", Integer.TYPE, settings.minIdle, context);
                settings.connectionTimeout = JsonUtils.getOrDefault(object, "connectionTimeout", Integer.TYPE, settings.connectionTimeout, context);
                settings.idleTimeout = JsonUtils.getOrDefault(object, "idleTimeout", Integer.TYPE, settings.idleTimeout, context);
                settings.leakDetection = JsonUtils.getOrDefault(object, "leakDetection", Integer.TYPE, settings.leakDetection, context);
                return settings;
            }
        }

        @Override
        public JsonElement serialize(DatabaseSettings src, Type typeOfSrc, JsonSerializationContext context) {
            var object = new JsonObject();
            if (src instanceof URL url) {
                if (url.areExtraSettingsDefaults()) {
                    if (url.connections == Settings.get().getDefaultDatabaseConnections()) {
                        return new JsonPrimitive(url.url);
                    } else {
                        return new JsonPrimitive(url.connections + " " + url.url);
                    }
                } else {
                    object.addProperty("url", url.url);
                }
            } else if (src instanceof Explicit explicit) {
                object.addProperty("ip", explicit.ip);
                object.addProperty("port", explicit.port);
                object.addProperty("username", explicit.username);
                object.addProperty("password", explicit.password);
            }
            object.addProperty("connections", src.connections);
            if (src.minIdle != DatabaseSettings.MIN_IDLE_DEFAULT) {
                object.addProperty("minIdle", src.minIdle);
            }
            if (src.connectionTimeout != DatabaseSettings.CONNECTION_TIMEOUT_DEFAULT) {
                object.addProperty("connectionTimeout", src.connectionTimeout);
            }
            if (src.idleTimeout != DatabaseSettings.IDLE_TIMEOUT_DEFAULT) {
                object.addProperty("idleTimeout", src.idleTimeout);
            }
            if (src.leakDetection != DatabaseSettings.LEAK_DETECTION_DEFAULT) {
                object.addProperty("leakDetection", src.leakDetection);
            }
            if (!src.properties.isEmpty()) {
                object.add("properties", context.serialize(src.properties));
            }
            return object;
        }
    }
}