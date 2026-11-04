package de.waterdu.atlantis.file.storage;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.ClassHolder;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.DatabaseEncoders;
import de.waterdu.atlantis.file.DatabaseSerializable;
import de.waterdu.atlantis.file.FieldHolder;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.text.IncompleteString;
import de.waterdu.atlantis.util.text.TextUtils;

import java.io.File;
import java.io.InvalidClassException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Database {@link Storage} implementation for AquaConfig.
 * <p>
 * The details come from the {@link Config} instance where possible.
 * If you pass properties instead, include at least the following:
 * - serverName
 * - port
 * - databaseName
 * - user
 * - password
 */
public class Database implements Storage {

    private static final IncompleteString JDBC_URI = IncompleteString.builder()
            .append("jdbc:mysql://")
            .placeholder()
            .append(":")
            .placeholder()
            .append("@")
            .placeholder()
            .append(":")
            .placeholder()
            .append("/")
            .build();

    private final HikariConfig config;
    private HikariDataSource hikari;
    private Schema schema;
    private final Queries queries = new Queries();

    /**
     * @param properties data source properties in the form {@code key=value}
     */
    public Database(
            String poolName,
            String ip,
            int port,
            String username,
            String password,
            int maxPoolSize,
            String... properties
    ) {
        this(poolName, ip, port, username, password, 5, maxPoolSize, 10000, 600000, 600000, properties);
    }

    /**
     * @param properties data source properties in the form {@code key=value}
     */
    public Database(
            String poolName,
            String ip,
            int port,
            String username,
            String password,
            int minIdle,
            int maxPoolSize,
            int connectionTimeout,
            int idleTimeout,
            int leakDetection,
            String... properties
    ) {
        this(
                JDBC_URI.complete(username, password, ip, port),
                poolName,
                minIdle,
                maxPoolSize,
                connectionTimeout,
                idleTimeout,
                leakDetection,
                TextUtils.join(properties,
                        "serverName=" + ip,
                        "port=" + port,
                        "user=" + username,
                        "password=" + password
                )
        );
    }

    /**
     * @param properties data source properties in the form {@code key=value}
     */
    public Database(String jdbc, String poolName, int maxPoolSize, String... properties) {
        this(jdbc, poolName, 5, maxPoolSize, 10000, 600000, 600000, properties);
    }

    /**
     * @param properties data source properties in the form {@code key=value}
     */
    public Database(
            String jdbc,
            String poolName,
            int minIdle,
            int maxPoolSize,
            int connectionTimeout,
            int idleTimeout,
            int leakDetection,
            String... properties
    ) {
        this.config = new HikariConfig();
        this.config.setMaximumPoolSize(Math.max(1, maxPoolSize));
        this.config.setMinimumIdle(minIdle);
        this.config.setPoolName(poolName);
        this.config.setJdbcUrl(jdbc);
        for (var property : Settings.get().getHikariProperties().entrySet()) {
            this.config.addDataSourceProperty(property.getKey(), property.getValue());
        }
        for (var property : properties) {
            var kv = property.split("=");
            if (kv.length == 2) {
                this.config.addDataSourceProperty(kv[0], kv[1]);
            }
        }
        this.config.setConnectionTimeout(connectionTimeout);
        this.config.setIdleTimeout(idleTimeout);
        this.config.setLeakDetectionThreshold(leakDetection);
        this.config.setConnectionTestQuery("SELECT 1");
    }

    /**
     * Opens the connection pool, builds the {@link Schema} and the {@link Queries}, and creates the database and the
     * table if they do not exist.
     *
     * @param config the config that uses this storage
     * @param modId the ID of the mod that owns the config. The settings can map it to a different database name.
     * @param key the key of the config
     * @param path the table name, in lower case
     * @param gson the Gson instance of the config
     * @param configuration the default configuration instance
     * @param classes the classes of the config
     * @param <T> the configuration type
     * @throws InvalidClassException if the configuration is not {@link Data}
     * @throws SQLException if the test query fails
     * @throws NoSuchFieldException if the configuration type has no {@code uuid} or {@code name} field
     */
    @Override
    public <T extends Configuration> void init(
            Config<T> config,
            String modId,
            String key,
            String path,
            Gson gson,
            T configuration,
            ClassHolder<T> classes
    ) throws InvalidClassException, SQLException, NoSuchFieldException {
        var clazz = classes.getConfigurationClass();

        var originalModId = modId;
        modId = Settings.get().getDatabaseName(modId);

        if (!(configuration instanceof Data)) {
            throw new InvalidClassException("Only data configuration types can be written to database");
        }

        try {
            clazz.getDeclaredField("uuid");
        } catch (NoSuchFieldException e1) {
            try {
                clazz.getField("uuid");
            } catch (NoSuchFieldException e2) {
                throw new NoSuchFieldException("Database data requires a uuid field");
            }
        }

        try {
            clazz.getDeclaredField("name");
        } catch (NoSuchFieldException e) {
            try {
                clazz.getField("name");
            } catch (NoSuchFieldException e2) {
                throw new NoSuchFieldException("Database data requires a name field");
            }
        }

        this.config.addDataSourceProperty("databaseName", modId);
        this.hikari = new HikariDataSource(this.config);
        try (var connection = this.connection()) {
            try (var stmt = this.createStatement(connection)) {
                stmt.execute(this.hikari.getConnectionTestQuery());
            }
        }
        this.schema = Schema.of(originalModId, path.toLowerCase(), clazz);
        this.queries.init(modId, this.schema);
        try (var connection = this.connection()) {
            try (var stmt = this.createStatement(connection)) {
                stmt.execute(this.queries.prepare(this.queries.CREATE_DATABASE));
            } catch (SQLException e) {
                AtlantisLogger.debug("Database probably present already, skipping {}: {}", config.name(), e.getMessage());
            }
            try (var stmt = this.createStatement(connection)) {
                stmt.execute(this.queries.prepare(this.queries.CREATE_TABLE));
            } catch (SQLException e) {
                AtlantisLogger.debug("Table probably present already, skipping {}: {}", config.name(), e.getMessage());
            }
        } catch (SQLException e) {
            AtlantisLogger.error("SQL error when establishing connection, skipping {}: {}", config.name(), e.getMessage());
        }
    }

    /**
     * Closes the connection pool, but only on a dedicated server.
     *
     * @param config the config that uses this storage
     * @param <T> the configuration type
     */
    @Override
    public <T extends Configuration> void destruct(Config<T> config) {
        if (Mods.modLoader().isDedicatedServer()) {
            this.hikari.close();
        }
    }

    /**
     * Writes every data instance in memory with {@link #writeSpecific} on {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config to write
     * @param <T> the configuration type
     * @return a future of {@code true} after the writes start, or of {@code false} if the server is not alive or the
     *     config is not data
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> write(Config<T> config) {
        var result = new CompletableFuture<Boolean>();
        if (!this.isServerAlive()) {
            result.complete(false);
            return result;
        }
        if (config.container().isData()) {
            Atlantis.THREAD_POOL.submit(() -> {
                for (var out : config.uuidDataMap().values()) {
                    if (out != null) {
                        this.writeSpecific(config, out);
                    }
                }
                result.complete(true);
            });
        } else {
            result.complete(false);
        }
        return result;
    }

    /**
     * Inserts the data instance into the table, or updates the row if the key already exists. This runs on
     * {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config of the data
     * @param data the data instance to write
     * @param <T> the configuration type
     * @return a future of {@code true} if the query was successful
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> writeSpecific(Config<T> config, T data) {
        var result = new CompletableFuture<Boolean>();
        if (!this.isServerAlive()) {
            result.complete(false);
            return result;
        }
        Atlantis.THREAD_POOL.submit(() -> {
            var query = this.queries.prepare(this.queries.INSERT_ELEMENT, "", config, data, this.schema);
            try (var connection = this.connection()) {
                try (var stmt = this.createStatement(connection)) {
                    stmt.execute(query);
                    result.complete(true);
                }
            } catch (SQLException e) {
                AtlantisLogger.error("SQL error in writeSpecific on {}: {}", config.name(), e.getMessage());
                result.complete(false);
            }
        });
        return result;
    }

    /**
     * Clears the data in memory and reads all rows again with {@link #readAll}.
     *
     * @param config the config to read
     * @param skipNulls ignored
     * @param <T> the configuration type
     * @return a future of {@code true} if the read was successful, or of {@code false} if the config is not data
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> read(Config<T> config, boolean skipNulls) {
        var result = new CompletableFuture<Boolean>();
        if (!this.isServerAlive()) {
            result.complete(false);
            return result;
        }
        if (config.container().isData()) {
            config.clearAll();
            return this.readAll(config, null, null);
        } else {
            result.complete(false);
        }
        return result;
    }

    /**
     * Reads every row of the table and puts the decoded instances in memory. This runs on
     * {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config to read
     * @param dir ignored
     * @param names ignored
     * @param <T> the configuration type
     * @return a future of {@code true} if the query was successful
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> readAll(Config<T> config, File dir, Set<String> names) {
        var result = new CompletableFuture<Boolean>();
        if (!this.isServerAlive()) {
            result.complete(false);
            return result;
        }
        Atlantis.THREAD_POOL.submit(() -> {
            var query = this.queries.prepare(this.queries.SELECT_ALL);
            try (var connection = this.connection()) {
                try (var stmt = this.createStatement(connection)) {
                    try (var rs = stmt.executeQuery(query)) {
                        if (rs.first()) {
                            do {
                                try {
                                    var data = config.classes().getConfigurationClass().getConstructor().newInstance();
                                    for (var element : this.schema.elements) {
                                        element.field.field().set(data, element.decoder.decode(config, element.field, element.decoder.extract(rs, element.name)));
                                    }
                                    config.putUnsafe(data, false);
                                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                    AtlantisLogger.error("Failed to instantiate object with zero-args constructor on {}: {}", config.name(), e.getMessage());
                                }
                            } while (rs.next());
                        }
                    }
                    AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Successfully read {}", config.name());
                    result.complete(true);
                }
            } catch (SQLException e) {
                AtlantisLogger.error("SQL error in readAll on {}: {}", config.name(), e.getMessage());
                result.complete(false);
            }
        });
        return result;
    }

    /**
     * Reads the row with the given UUID and puts the decoded instance in memory. This runs on
     * {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config of the data
     * @param uuid the UUID of the instance
     * @param <T> the configuration type
     * @return a future of the instance, or of {@code null} if there is none
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> readFromUUID(Config<T> config, UUID uuid) {
        var result = new CompletableFuture<T>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        Atlantis.THREAD_POOL.submit(() -> {
            var query = this.queries.prepare(this.queries.SELECT_ELEMENT_UUID, uuid.toString());
            try (var connection = this.connection()) {
                try (var stmt = this.createStatement(connection)) {
                    try (var rs = stmt.executeQuery(query)) {
                        if (rs.first()) {
                            do {
                                try {
                                    var data = config.classes().getConfigurationClass().getConstructor().newInstance();
                                    for (var element : this.schema.elements) {
                                        try {
                                            element.field.field().set(data, element.decoder.decode(config, element.field, element.decoder.extract(rs, element.name)));
                                        } catch (Exception e) {
                                            AtlantisLogger.error("Failed to decode field of type {}, field name {}, data type {}", element.field.type().getTypeName(), element.field, config.name());
                                        }
                                    }
                                    config.putUnsafe(data, false);
                                    result.complete(data);
                                    AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Successfully read specific {} for UUID {}", config.name(), uuid);
                                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                    AtlantisLogger.error("Failed to instantiate object with zero-args constructor on {}: {}", config.name(), e.getMessage());
                                }
                            } while (rs.next());
                        }
                    }
                    if (!result.isDone()) {
                        AtlantisLogger.debug("Failed to read specific {} for UUID {}", config.name(), uuid);
                        result.complete(null);
                    }
                }
            } catch (SQLException e) {
                AtlantisLogger.error("SQL error in readAll on {}: {}", config.name(), e.getMessage());
                result.complete(null);
            }
        });
        return result;
    }

    /**
     * Reads the row with the given name and puts the decoded instance in memory. This runs on
     * {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config of the data
     * @param name the name of the instance
     * @param <T> the configuration type
     * @return a future of the instance, or of {@code null} if there is none
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> readFromName(Config<T> config, String name) {
        var result = new CompletableFuture<T>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        Atlantis.THREAD_POOL.submit(() -> {
            var query = this.queries.prepare(this.queries.SELECT_ELEMENT_NAME, Queries.escape(name));
            try (var connection = this.connection()) {
                try (var stmt = this.createStatement(connection)) {
                    try (var rs = stmt.executeQuery(query)) {
                        if (rs.first()) {
                            do {
                                try {
                                    var data = config.classes().getConfigurationClass().getConstructor().newInstance();
                                    for (var element : this.schema.elements) {
                                        Object value = element.decoder.decode(config, element.field, element.decoder.extract(rs, element.name));
                                        element.field.field().set(data, value);
                                    }
                                    config.putUnsafe(data, false);
                                    result.complete(data);
                                    AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Successfully read specific {} for name {}", config.name(), name);
                                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                    AtlantisLogger.error("Failed to instantiate object with zero-args constructor on {}: {}", config.name(), e.getMessage());
                                }
                            } while (rs.next());
                        }
                    }
                    if (!result.isDone()) {
                        AtlantisLogger.debug("Failed to read specific {} for name {}", config.name(), name);
                        result.complete(null);
                    }
                }
            } catch (SQLException e) {
                AtlantisLogger.error("SQL error in readAll on {}: {}", config.name(), e.getMessage());
                result.complete(null);
            }
        });
        return result;
    }

    /**
     * Deletes the data instance with {@link #deleteFromUUID}.
     *
     * @param config the config of the data
     * @param data the data instance to delete, or {@code null}
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if nothing was removed
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> delete(Config<T> config, T data) {
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }
        return this.deleteFromUUID(config, data.uuid());
    }

    /**
     * Deletes the row with the given UUID and removes the instance from memory. This runs on
     * {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config of the data
     * @param uuid the UUID of the instance
     * @param <T> the configuration type
     * @return a future of the instance removed from memory, or of {@code null} if there was none
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> deleteFromUUID(Config<T> config, UUID uuid) {
        var result = new CompletableFuture<T>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        Atlantis.THREAD_POOL.submit(() -> {
            var query = this.queries.prepare(this.queries.DELETE_ELEMENT_UUID, uuid.toString());
            try (var connection = this.connection()) {
                try (var stmt = this.createStatement(connection)) {
                    stmt.execute(query);
                    T removed = config.uuidDataMap().remove(uuid);
                    if (removed != null) {
                        config.filenameDataMap().remove(removed.identifier());
                        if (removed instanceof NamedData namedData) {
                            config.nameDataMap().remove(namedData.name());
                        }
                    }
                    result.complete(removed);
                }
            } catch (SQLException e) {
                AtlantisLogger.error("SQL error in deleteFromUUID on {}: {}", config.name(), e.getMessage());
                result.complete(null);
            }
        });
        return result;
    }

    /**
     * Deletes the named data instance with the given name with {@link #delete}. The instance must be in memory.
     *
     * @param config the config of the data
     * @param name the name of the instance
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if nothing was removed
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> deleteFromName(Config<T> config, String name) {
        var data = config.get(name);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }
        return this.delete(config, data);
    }

    /**
     * Borrows a connection from the pool. Close it after use. Use it only after the storage initializes.
     */
    public Connection connection() throws SQLException {
        return this.hikari.getConnection();
    }

    /**
     * Creates a scroll-insensitive, read-only statement on the connection.
     *
     * @param connection the connection to use
     * @return the statement
     * @throws SQLException if the connection cannot create the statement
     */
    public Statement createStatement(Connection connection) throws SQLException {
        return connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    }

    /**
     * @return the queries of this storage, completed for its database and table after {@link #init}
     */
    public Queries queries() {
        return this.queries;
    }

    /**
     * Describes the table of a data type: the table name and one {@link Element} per
     * {@link DatabaseSerializable} field.
     *
     * @param table the table name
     * @param type the data type
     * @param elements the columns of the table
     * @param autoIncrementElements the number of auto-increment columns
     */
    public record Schema(
            String table, 
            Class<?> type, 
            List<Element> elements, 
            int autoIncrementElements
    ) {

        /**
         * Creates the schema from the declared and public fields of the type that have {@link DatabaseSerializable}.
         * The table name gets the mod ID as a prefix if the settings enable it.
         *
         * @param originalModId the ID of the mod that owns the config
         * @param table the table name without the prefix
         * @param type the data type
         * @return the schema
         */
        public static Schema of(String originalModId, String table, Class<?> type) {
            var autoIncrementElements = 0;
            var elements = Lists.<Element>newArrayList();
            var parsedFields = Sets.<Field>newHashSet();

            for (var field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(DatabaseSerializable.class)) {
                    field.setAccessible(true);
                    var annotation = field.getAnnotation(DatabaseSerializable.class);
                    if (annotation != null) {
                        if (!parsedFields.contains(field)) {
                            parsedFields.add(field);
                            var element = Element.of(field, annotation);
                            elements.add(element);
                            if (element.autoIncrement) {
                                autoIncrementElements++;
                            }
                        }
                    }
                }
            }

            for (var field : type.getFields()) {
                if (field.isAnnotationPresent(DatabaseSerializable.class)) {
                    field.setAccessible(true);
                    var annotation = field.getAnnotation(DatabaseSerializable.class);
                    if (annotation != null) {
                        if (!parsedFields.contains(field)) {
                            parsedFields.add(field);
                            var element = Element.of(field, annotation);
                            elements.add(element);
                            if (element.autoIncrement) {
                                autoIncrementElements++;
                            }
                        }
                    }
                }
            }

            return new Schema((Settings.get().isModIdPrefixOnTableNames() ? originalModId + "_" : "") + table, type, elements, autoIncrementElements);
        }

        /**
         * Describes one column of a {@link Schema}.
         *
         * @param field the field of the data type
         * @param name the column name, in lower case
         * @param type the SQL type, in upper case
         * @param surround the quote mark around values of this column
         * @param encoder converts the field value to its SQL value
         * @param decoder converts the SQL value to the field value
         * @param primaryKey {@code true} if the column is the primary key
         * @param autoIncrement {@code true} if the database sets the value
         * @param uniqueKey {@code true} if the column is unique or the primary key
         * @param notNull {@code true} if the column cannot be {@code NULL}
         */
        public record Element(
                FieldHolder field,
                String name, 
                String type, 
                String surround, 
                DatabaseEncoders.Encoder<?> encoder,
                DatabaseEncoders.Decoder<?> decoder,
                boolean primaryKey,
                boolean autoIncrement,
                boolean uniqueKey,
                boolean notNull
        ) {

            /**
             * Creates the column from the field and its annotation.
             *
             * @param field the field of the data type
             * @param annotation the annotation of the field
             * @return the column
             * @throws NullPointerException if the annotation names an encoder that does not exist
             */
            public static Element of(Field field, DatabaseSerializable annotation) {
                return new Element(
                        FieldHolder.of(field),
                        field.getName().toLowerCase(),
                        annotation.type().toUpperCase(),
                        annotation.quoteMark(),
                        DatabaseEncoders.getEncoder(annotation.encoder()).orElseThrow(() -> new NullPointerException("Invalid encoder " + annotation.encoder())),
                        DatabaseEncoders.getDecoder(annotation.encoder()).orElseThrow(() -> new NullPointerException("Invalid decoder " + annotation.encoder())),
                        annotation.primaryKey(),
                        annotation.autoIncrement(),
                        annotation.primaryKey() || annotation.uniqueKey(),
                        annotation.notNull()
                );
            }
        }
    }

    /**
     * Holds the SQL queries of a {@link Database}. The {@code %} tokens are replaced in {@link #init}, and the
     * placeholders are filled in by {@link #prepare}.
     */
    public static final class Queries {

        public IncompleteString CREATE_DATABASE = IncompleteString.of("CREATE DATABASE IF NOT EXISTS %d");
        public IncompleteString CREATE_TABLE = IncompleteString.of("CREATE TABLE IF NOT EXISTS %d.%t (%c)");
        public IncompleteString INSERT_ELEMENT = IncompleteString.builder().string("INSERT INTO %d.%t (%n) VALUES (%v) ON DUPLICATE KEY UPDATE %u").build();
        public IncompleteString UPDATE_ELEMENT_UUID = IncompleteString.builder().string("UPDATE %d.%t SET %u WHERE uuid = '").placeholder().string("'").build();
        public IncompleteString SELECT_ELEMENT_UUID = IncompleteString.builder().string("SELECT * FROM %d.%t WHERE uuid = '").placeholder().string("'").build();
        public IncompleteString DELETE_ELEMENT_UUID = IncompleteString.builder().string("DELETE FROM %d.%t WHERE uuid = '").placeholder().string("'").build();
        public IncompleteString UPDATE_ELEMENT_NAME = IncompleteString.builder().string("UPDATE %d.%t SET %u WHERE name = '").placeholder().string("'").build();
        public IncompleteString SELECT_ELEMENT_NAME = IncompleteString.builder().string("SELECT * FROM %d.%t WHERE name = '").placeholder().string("'").build();
        public IncompleteString DELETE_ELEMENT_NAME = IncompleteString.builder().string("DELETE FROM %d.%t WHERE name = '").placeholder().string("'").build();
        public IncompleteString SELECT_ALL = IncompleteString.of("SELECT * FROM %d.%t");

        /**
         * Replaces the {@code %d}, {@code %t}, {@code %c}, {@code %u}, {@code %n} and {@code %v} tokens of every
         * query with the database name, the table name and the column lists of the schema.
         *
         * @param database the database name
         * @param schema the schema of the table
         */
        public void init(String database, Schema schema) {
            var create = IncompleteString.builder();
            var update = IncompleteString.builder();
            var names = IncompleteString.builder();
            var values = IncompleteString.builder();

            for (var element : schema.elements) {
                if (!create.isEmpty()) {
                    create.append(", ");
                }
                create.append(element.name).append(" ").append(element.type);
                if (element.notNull) {
                    create.append(" ").append(Keywords.NOT_NULL);
                }
                if (element.autoIncrement) {
                    create.append(" ").append(Keywords.AUTO_INCREMENT);
                }
                if (element.primaryKey) {
                    create.append(" ").append(Keywords.PRIMARY_KEY);
                } else if (element.uniqueKey) {
                    create.append(" ").append(Keywords.UNIQUE_KEY);
                }

                if (!element.autoIncrement) {
                    if (!element.primaryKey) {
                        if (!update.isEmpty()) {
                            update.append(", ");
                        }
                        update.append(element.name).append(" = ").append(element.surround).placeholder().append(element.surround);
                    }

                    if (!names.isEmpty()) {
                        names.append(", ");
                    }
                    names.append(element.name);

                    if (!values.isEmpty()) {
                        values.append(", ");
                    }
                    values.append(element.surround).placeholder().append(element.surround);
                }
            }

            try {
                var createBuilt = create.build();
                var updateBuilt = update.build();
                var namesBuilt = names.build();
                var valuesBuilt = values.build();
                for (var field : this.getClass().getDeclaredFields()) {
                    var value = (IncompleteString) field.get(this);
                    field.set(this, value.replace("%d", database)
                            .replace("%t", schema.table)
                            .replace("%c", createBuilt)
                            .replace("%u", updateBuilt)
                            .replace("%n", namesBuilt)
                            .replace("%v", valuesBuilt)
                    );
                }
            } catch (IllegalAccessException ignored) {}
        }

        /**
         * Fills in the placeholders of the query with the arguments, in order.
         *
         * @param query the query to complete
         * @param args the values of the placeholders
         * @return the complete query
         */
        public String prepare(IncompleteString query, String... args) {
            return query.complete(args);
        }

        /**
         * Escapes backslashes and single quotes, so that the value is safe inside single quotes.
         *
         * @param value the value to escape
         * @return the escaped value
         */
        static String escape(String value) {
            return value.replace("\\", "\\\\").replace("'", "''");
        }

        /**
         * Fills in the placeholders of the query with the encoded field values of the data. Auto-increment columns
         * are skipped, and values in single quotes are escaped.
         *
         * @param query the query to complete
         * @param key the value of the last placeholder, or an empty string to fill in the update values of the
         *     non-key columns instead
         * @param config the config of the data
         * @param data the data instance
         * @param schema the schema of the table
         * @param <T> the configuration type
         * @return the complete query
         */
        public <T extends Configuration> String prepare(IncompleteString query, String key, Config<T> config, Object data, Schema schema) {
            var args = new String[schema.elements.size() + (key.isEmpty() ? schema.elements.size() - 1 : 1) - schema.autoIncrementElements];
            var i = 0;
            for (var element : schema.elements) {
                if (!element.autoIncrement) {
                    try {
                        args[i] = element.encoder.encode(config, element.field, element.field.field().get(data));
                        if (element.surround.equalsIgnoreCase(DatabaseEncoders.QuoteMarks.SINGLE)) {
                            args[i] = Queries.escape(args[i]);
                        }
                    } catch (IllegalAccessException e) {
                        AtlantisLogger.error("Failed to get field when preparing query on {}", config.name());
                    }
                    i++;
                }
            }
            if (!key.isEmpty()) {
                args[i] = key;
            } else {
                for (var element : schema.elements) {
                    if (!element.primaryKey && !element.autoIncrement) {
                        try {
                            args[i] = element.encoder.encode(config, element.field, element.field.field().get(data));
                            if (element.surround.equalsIgnoreCase(DatabaseEncoders.QuoteMarks.SINGLE)) {
                                args[i] = Queries.escape(args[i]);
                            }
                        } catch (IllegalAccessException e) {
                            AtlantisLogger.error("Failed to get field when preparing query on {}", config.name());
                        }
                        i++;
                    }
                }
            }
            return query.complete(args);
        }
    }

    /**
     * Holds the SQL keywords for column definitions.
     */
    public static final class Keywords {

        public static String PRIMARY_KEY = "PRIMARY KEY";
        public static String UNIQUE_KEY = "UNIQUE";
        public static String NOT_NULL = "NOT NULL";
        public static String AUTO_INCREMENT = "AUTO_INCREMENT";

        private Keywords() {
            ExceptionUtils.throwCannotInstantiateClassException();
        }
    }
}
