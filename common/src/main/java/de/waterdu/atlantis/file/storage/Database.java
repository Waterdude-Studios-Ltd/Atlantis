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
 * Details will be gotten from the {@link Config} instance where possible.
 * Alternatively, when passing through properties, make sure to include the following at a minimum:
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

    public Database(String jdbc, String poolName, int maxPoolSize, String... properties) {
        this(jdbc, poolName, 5, maxPoolSize, 10000, 600000, 600000, properties);
    }

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

    @Override
    public <T extends Configuration> void init(
            Config<T> config,
            String modID,
            String key,
            String path,
            Gson gson,
            T configuration,
            ClassHolder<T> classes
    ) throws InvalidClassException, SQLException, NoSuchFieldException {
        var clazz = classes.getConfigurationClass();

        var originalModID = modID;
        modID = Settings.get().getDatabaseName(modID);

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

        this.config.addDataSourceProperty("databaseName", modID);
        this.hikari = new HikariDataSource(this.config);
        try (var connection = this.connection()) {
            try (var stmt = this.createStatement(connection)) {
                stmt.execute(this.hikari.getConnectionTestQuery());
            }
        }
        this.schema = Schema.of(originalModID, path.toLowerCase(), clazz);
        this.queries.init(modID, this.schema);
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

    @Override
    public <T extends Configuration> void destruct(Config<T> config) {
        if (Mods.modLoader().isDedicatedServer()) {
            this.hikari.close();
        }
    }

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

    @Override
    public <T extends Configuration> CompletableFuture<T> delete(Config<T> config, T data) {
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }
        return this.deleteFromUUID(config, data.uuid());
    }

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

    @Override
    public <T extends Configuration> CompletableFuture<T> deleteFromName(Config<T> config, String name) {
        var data = config.get(name);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }
        return this.delete(config, data);
    }

    public Connection connection() throws SQLException {
        return this.hikari.getConnection();
    }

    public Statement createStatement(Connection connection) throws SQLException {
        return connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    }

    public Queries queries() {
        return this.queries;
    }

    public record Schema(
            String table, 
            Class<?> type, 
            List<Element> elements, 
            int autoIncrementElements
    ) {

        public static Schema of(String originalModID, String table, Class<?> type) {
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

            return new Schema((Settings.get().isModIDPrefixOnTableNames() ? originalModID + "_" : "") + table, type, elements, autoIncrementElements);
        }

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

        public String prepare(IncompleteString query, String... args) {
            return query.complete(args);
        }

        static String escape(String value) {
            return value.replace("\\", "\\\\").replace("'", "''");
        }

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
