package de.waterdu.atlantis.file;

import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.java.interfaces.SQLBiFunction;
import de.waterdu.atlantis.util.java.interfaces.TriFunction;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Registers the encoders and decoders that convert fields to and from SQL values for database storage.
 */
public final class DatabaseEncoders {

    /**
     * Holds the names of the built-in SQL types and encoders. {@link AllowedTypes} on each name lists the Java types
     * that {@link DatabaseSerializableValidator} accepts for it.
     */
    public static class Defaults {

        @AllowedTypes({})
        public static final String JSON = "json";

        @AllowedTypes(String.class)
        public static final String STRING = "string";

        @AllowedTypes({int.class, Integer.class})
        public static final String INT = "int";

        @AllowedTypes({int.class, Integer.class})
        public static final String INTEGER = "integer";

        @AllowedTypes({float.class, Float.class})
        public static final String FLOAT = "float";

        @AllowedTypes({short.class, Short.class})
        public static final String SHORT = "short";

        @AllowedTypes({double.class, Double.class})
        public static final String DOUBLE = "double";

        @AllowedTypes({long.class, Long.class})
        public static final String LONG = "long";

        @AllowedTypes(BigInteger.class)
        public static final String BIGINT = "bigint";

        @AllowedTypes({byte.class, Byte.class})
        public static final String BYTE = "byte";

        @AllowedTypes(UUID.class)
        public static final String UUID = "uuid";

        @AllowedTypes({boolean.class, Boolean.class})
        public static final String BOOL = "bool";

        @AllowedTypes({boolean.class, Boolean.class})
        public static final String BOOLEAN = "boolean";

        @AllowedTypes(String.class)
        public static final String TINYTEXT = "tinytext";

        @AllowedTypes(String.class)
        public static final String TEXT = "text";

        @AllowedTypes(String.class)
        public static final String MEDIUMTEXT = "mediumtext";

        @AllowedTypes({})
        public static final String LONGTEXT = "longtext";

        @AllowedTypes(String.class)
        public static final String TINYBLOB = "tinyblob";

        @AllowedTypes(String.class)
        public static final String BLOB = "blob";

        @AllowedTypes(String.class)
        public static final String MEDIUMBLOB = "mediumblob";

        @AllowedTypes({})
        public static final String LONGBLOB = "longblob";

        public static final String VARCHAR = "varchar";

        @AllowedTypes({String.class, UUID.class})
        public static final String VARCHAR_UUID = "varchar(36)";

        @AllowedTypes(String.class)
        public static final String VARCHAR_USERNAME = "varchar(30)";

        @AllowedTypes(String.class)
        public static final String VARCHAR_MAX = "varchar(65535)";

        private Defaults() {
            ExceptionUtils.throwCannotInstantiateClassException();
        }

        /**
         * Gets the name of a {@code varchar} type with the length.
         *
         * @param len the maximum length
         * @return {@code varchar(len)}
         */
        public static String varchar(int len) {
            return VARCHAR + "(" + len + ")";
        }
    }

    /**
     * Holds the marks that can go around an encoded value in SQL queries. See
     * {@link DatabaseSerializable#quoteMark()}.
     */
    public static class QuoteMarks {

        public static final String NONE = "";
        public static final String SINGLE = "'";
        public static final String DOUBLE = "\"";
        public static final String BACKTICK = "`";

        private QuoteMarks() {
            ExceptionUtils.throwCannotInstantiateClassException();
        }
    }

    private static final Map<String, Encoder<?>> ENCODERS = new HashMap<>();
    private static final Map<String, Decoder<?>> DECODERS = new HashMap<>();

    static {
        DatabaseEncoders.register(Defaults.JSON,
                new Encoder<>((config, field, data) -> config.gson().toJson(data)),
                new Decoder<>(ResultSet::getString, (config, field, data) -> config.gson().fromJson(data, field.type()))
        );

        DatabaseEncoders.register(
                new Encoder<String>(),
                new Decoder<>(ResultSet::getBytes, (config, field, data) -> data == null ? null : new String(data, StandardCharsets.UTF_8))
        , Defaults.TINYBLOB, Defaults.BLOB, Defaults.MEDIUMBLOB, Defaults.LONGBLOB);

        DatabaseEncoders.register(
                new Encoder<String>(),
                new Decoder<>(ResultSet::getString)
        , Defaults.STRING, Defaults.TINYTEXT, Defaults.TEXT, Defaults.MEDIUMTEXT, Defaults.LONGTEXT, Defaults.VARCHAR);

        DatabaseEncoders.register(
                new Encoder<Integer>(),
                new Decoder<>(ResultSet::getInt)
        , Defaults.INT, Defaults.INTEGER);

        DatabaseEncoders.register(Defaults.FLOAT,
                new Encoder<Float>(),
                new Decoder<>(ResultSet::getFloat)
        );

        DatabaseEncoders.register(Defaults.SHORT,
                new Encoder<Short>(),
                new Decoder<>(ResultSet::getShort)
        );

        DatabaseEncoders.register(Defaults.DOUBLE,
                new Encoder<Double>(),
                new Decoder<>(ResultSet::getDouble)
        );

        DatabaseEncoders.register(Defaults.LONG,
                new Encoder<Long>(),
                new Decoder<>(ResultSet::getLong)
        );

        DatabaseEncoders.register(Defaults.BIGINT,
                new Encoder<Long>(),
                new Decoder<>(ResultSet::getLong, (config, field, data) -> BigInteger.valueOf(data))
        );

        DatabaseEncoders.register(Defaults.BYTE,
                new Encoder<Byte>(),
                new Decoder<>(ResultSet::getByte)
        );

        DatabaseEncoders.register(Defaults.UUID,
                new Encoder<>(),
                new Decoder<>(ResultSet::getString, (config, field, data) -> UUID.fromString(data))
        );

        DatabaseEncoders.register(
                new Encoder<>((config, field, data) -> (boolean) data ? "1" : "0"),
                new Decoder<>(ResultSet::getBoolean, (config, field, data) -> data)
        ,Defaults.BOOL, Defaults.BOOLEAN);
    }

    private DatabaseEncoders() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Replaces an encoder that has the same name. Register it before the database storage that uses it initializes.
     *
     * @param name the name for {@link DatabaseSerializable#encoder()}. Case does not matter.
     */
    public static void register(String name, Encoder<?> encoder, Decoder<?> decoder) {
        DatabaseEncoders.ENCODERS.put(name.toLowerCase(Locale.ROOT), encoder);
        DatabaseEncoders.DECODERS.put(name.toLowerCase(Locale.ROOT), decoder);
    }

    /**
     * Registers the same encoder and decoder under each name. See {@link #register(String, Encoder, Decoder)}.
     *
     * @param encoder the encoder
     * @param decoder the decoder
     * @param names the names for {@link DatabaseSerializable#encoder()}. Case does not matter.
     */
    public static void register(Encoder<?> encoder, Decoder<?> decoder, String... names) {
        for (var name : names) {
            DatabaseEncoders.register(name, encoder, decoder);
        }
    }

    /**
     * Gets the encoder that has the name.
     *
     * @param name the name of the encoder. Case does not matter.
     * @return the encoder, or an empty optional if no encoder has the name
     */
    public static Optional<Encoder<?>> getEncoder(String name) {
        return Optional.ofNullable(DatabaseEncoders.ENCODERS.get(name.toLowerCase(Locale.ROOT)));
    }

    /**
     * Gets the decoder that has the name.
     *
     * @param name the name of the decoder. Case does not matter.
     * @return the decoder, or an empty optional if no decoder has the name
     */
    public static Optional<Decoder<?>> getDecoder(String name) {
        return Optional.ofNullable(DatabaseEncoders.DECODERS.get(name.toLowerCase(Locale.ROOT)));
    }

    /**
     * Converts a field value to the string that goes in an SQL query.
     *
     * @param <T> the type of the field value
     * @param encoder converts the config, the field and the value to the string
     */
    public record Encoder<T>(TriFunction<Config<?>, FieldHolder, T, String> encoder) {

        /**
         * Creates an encoder that uses {@link String#valueOf(Object)}.
         */
        public Encoder() {
            this((config, field, data) -> String.valueOf(data));
        }

        /**
         * Encodes the value without a type check.
         *
         * @param config the config that owns the field
         * @param field the field of the value
         * @param data the value. It must be of type {@code T}.
         * @return the encoded string
         */
        @SuppressWarnings("unchecked")
        public String encode(Config<?> config, FieldHolder field, Object data) {
            return this.encoder.apply(config, field, (T) data);
        }
    }

    /**
     * Reads a column from an SQL result and converts it to a field value.
     *
     * @param <T> the type that the extractor reads from the result
     * @param extractor reads the value of a column from the result
     * @param decoder converts the config, the field and the read value to the field value
     */
    public record Decoder<T>(SQLBiFunction<ResultSet, String, T> extractor, TriFunction<Config<?>, FieldHolder, T, ?> decoder) {

        /**
         * Creates a decoder that uses the read value as the field value.
         *
         * @param extractor reads the value of a column from the result
         */
        public Decoder(SQLBiFunction<ResultSet, String, T> extractor) {
            this(extractor, (config, field, data) -> data);
        }

        /**
         * Reads the value of the column from the current row of the result.
         *
         * @param rs the SQL result
         * @param column the name of the column
         * @return the read value
         * @throws SQLException if the result cannot read the column
         */
        public T extract(ResultSet rs, String column) throws SQLException {
            return this.extractor.apply(rs, column);
        }

        /**
         * Decodes the read value without a type check.
         *
         * @param config the config that owns the field
         * @param field the field to decode for
         * @param data the read value. It must be of type {@code T}.
         * @return the field value
         */
        @SuppressWarnings("unchecked")
        public Object decode(Config<?> config, FieldHolder field, Object data) {
            return this.decoder.apply(config, field, (T) data);
        }
    }
}
