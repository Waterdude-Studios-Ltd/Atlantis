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

/**
 * Holds the connection settings of a database, as a JDBC URL or as explicit host and login values.
 */
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

    /**
     * Creates settings with the default pool timeouts.
     *
     * @param connections the maximum number of connections in the pool
     * @param properties the extra connection properties
     */
    private DatabaseSettings(int connections, Map<String, String> properties) {
        this.connections = connections;
        this.properties = properties;
    }

    /**
     * Creates a database storage from these settings.
     *
     * @param modId the ID of the mod that owns the storage
     * @return the database storage
     */
    public abstract Database storage(String modId);

    /**
     * @return the maximum number of connections in the pool
     */
    public int connections() {
        return this.connections;
    }

    /**
     * @return the extra connection properties, each as {@code key=value}
     */
    protected String[] propertyArray() {
        return this.properties.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
    }

    /**
     * Checks if the pool timeouts and the minimum idle count have their default values and there are no extra
     * properties.
     *
     * @return {@code true} if all extra settings are defaults
     */
    public boolean areExtraSettingsDefaults() {
        return this.minIdle == DatabaseSettings.MIN_IDLE_DEFAULT &&
                this.connectionTimeout == DatabaseSettings.CONNECTION_TIMEOUT_DEFAULT &&
                this.idleTimeout == DatabaseSettings.IDLE_TIMEOUT_DEFAULT &&
                this.leakDetection == DatabaseSettings.LEAK_DETECTION_DEFAULT &&
                this.properties.isEmpty();
    }

    /**
     * Holds database settings that connect with a JDBC URL.
     */
    public static class URL extends DatabaseSettings {

        private final String url;

        /**
         * Creates settings with a placeholder MySQL URL.
         */
        public URL() {
            this("jdbc:mysql://user:password@ip:port/");
        }

        /**
         * Creates settings with the default number of connections from {@link Settings} and no extra properties.
         *
         * @param url the JDBC URL
         */
        public URL(String url) {
            this(Settings.get().getDefaultDatabaseConnections(), url, new HashMap<>());
        }

        /**
         * Creates settings.
         *
         * @param connections the maximum number of connections in the pool
         * @param url the JDBC URL
         * @param properties the extra connection properties
         */
        public URL(int connections, String url, Map<String, String> properties) {
            super(connections, properties);
            this.url = url;
        }

        /**
         * Creates a database storage that connects with the URL.
         *
         * @param modId the ID of the mod that owns the storage
         * @return the database storage
         */
        @Override
        public Database storage(String modId) {
            return new Database(this.url, modId, this.minIdle, this.connections(), this.connectionTimeout, this.idleTimeout, this.leakDetection, this.propertyArray());
        }
    }

    /**
     * @return new explicit settings with placeholder values for a local MySQL server
     */
    public static DatabaseSettings getDefault() {
        return new Explicit();
    }

    /**
     * Holds database settings that connect with an IP address, a port, a username and a password.
     */
    public static class Explicit extends DatabaseSettings {

        private final String ip;
        private final int port;
        private final String username;
        private final String password;

        /**
         * Creates settings for {@code 127.0.0.1:3306} with placeholder login values.
         */
        public Explicit() {
            this("127.0.0.1", 3306, "username", "password");
        }

        /**
         * Creates settings with the default number of connections from {@link Settings} and no extra properties.
         *
         * @param ip the IP address of the database server
         * @param port the port of the database server
         * @param username the username to log in with
         * @param password the password to log in with
         */
        public Explicit(String ip, int port, String username, String password) {
            this(Settings.get().getDefaultDatabaseConnections(), ip, port, username, password, new HashMap<>());
        }

        /**
         * Creates settings.
         *
         * @param connections the maximum number of connections in the pool
         * @param ip the IP address of the database server
         * @param port the port of the database server
         * @param username the username to log in with
         * @param password the password to log in with
         * @param properties the extra connection properties
         */
        public Explicit(int connections, String ip, int port, String username, String password, Map<String, String> properties) {
            super(connections, properties);
            this.ip = ip;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        /**
         * Creates a database storage that connects with the IP address, the port and the login values.
         *
         * @param modId the ID of the mod that owns the storage
         * @return the database storage
         */
        @Override
        public Database storage(String modId) {
            return new Database(modId, this.ip, this.port, this.username, this.password, this.minIdle, this.connections(), this.connectionTimeout, this.idleTimeout, this.leakDetection, this.propertyArray());
        }
    }

    /**
     * Converts {@link DatabaseSettings} to and from JSON.
     */
    public static class TypeAdapter implements JsonSerializer<DatabaseSettings>, JsonDeserializer<DatabaseSettings> {

        /**
         * Reads settings from JSON. A string is a URL, optionally after a connection count and a space. An object with
         * a {@code url} gives URL settings, and other objects give explicit settings.
         *
         * @param json the JSON of the settings
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context
         * @return the settings
         */
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

        /**
         * Writes the settings to JSON. URL settings with default extra settings become a string. Other settings become
         * an object that leaves out default values.
         *
         * @param src the settings to serialize
         * @param typeOfSrc the type of the settings
         * @param context the serialization context
         * @return the settings as JSON
         */
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