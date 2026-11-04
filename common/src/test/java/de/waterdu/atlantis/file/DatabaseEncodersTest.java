package de.waterdu.atlantis.file;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the encoders and decoders in {@link DatabaseEncoders}.
 */
class DatabaseEncodersTest {

    private final Config<?> config = mock(Config.class);
    private final ResultSet resultSet = mock(ResultSet.class);

    @SuppressWarnings("unused")
    private List<String> listField;
    @SuppressWarnings("unused")
    private Object plainField;

    /**
     * Gets a field of this test class as a {@link FieldHolder}.
     *
     * @param name the name of the field
     * @return the field holder
     * @throws NoSuchFieldException if this class has no field with that name
     */
    private FieldHolder field(String name) throws NoSuchFieldException {
        return FieldHolder.of(DatabaseEncodersTest.class.getDeclaredField(name));
    }

    /**
     * Checks that each default type name has an encoder and a decoder.
     *
     * @param name the type name
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "json", "string", "int", "integer", "float", "short", "double", "long", "bigint", "byte", "uuid", "bool",
            "boolean", "tinytext", "text", "mediumtext", "longtext", "tinyblob", "blob", "mediumblob", "longblob", "varchar"
    })
    void everyDefaultHasAnEncoderAndDecoder(String name) {
        assertThat(DatabaseEncoders.getEncoder(name)).isPresent();
        assertThat(DatabaseEncoders.getDecoder(name)).isPresent();
    }

    /**
     * Checks that the encoder lookup ignores case.
     */
    @Test
    void lookupIgnoresCase() {
        assertThat(DatabaseEncoders.getEncoder("VarChar")).containsSame(DatabaseEncoders.getEncoder("varchar").orElseThrow());
    }

    /**
     * Checks that the encoder lookup still finds {@code INT} when the default locale is Turkish.
     */
    @Test
    void lookupIgnoresDefaultLocale() {
        var original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(DatabaseEncoders.getEncoder("INT")).isPresent();
        } finally {
            Locale.setDefault(original);
        }
    }

    /**
     * Checks that an unknown type name has no encoder and no decoder.
     */
    @Test
    void unknownNamesAreEmpty() {
        assertThat(DatabaseEncoders.getEncoder("no_such_encoder")).isEmpty();
        assertThat(DatabaseEncoders.getDecoder("no_such_encoder")).isEmpty();
    }

    /**
     * Checks that {@code register} adds the encoder and decoder under each name, without regard to case.
     */
    @Test
    void registerReplacesAndAddsEncoders() {
        var encoder = new DatabaseEncoders.Encoder<String>((config, field, data) -> "custom:" + data);
        var decoder = new DatabaseEncoders.Decoder<>(ResultSet::getString);
        DatabaseEncoders.register(encoder, decoder, "Test_Custom_A", "test_custom_b");

        assertThat(DatabaseEncoders.getEncoder("test_custom_a")).containsSame(encoder);
        assertThat(DatabaseEncoders.getDecoder("TEST_CUSTOM_B")).containsSame(decoder);
    }

    /**
     * Checks that the {@code int} encoder gives the string form of the value.
     *
     * @throws NoSuchFieldException if the test field is missing
     */
    @Test
    void defaultEncoderUsesStringValueOf() throws NoSuchFieldException {
        var encoder = DatabaseEncoders.getEncoder("int").orElseThrow();

        assertThat(encoder.encode(this.config, this.field("plainField"), 42)).isEqualTo("42");
    }

    /**
     * Checks that booleans encode as {@code 1} and {@code 0}, and decode with {@link ResultSet#getBoolean}.
     *
     * @throws Exception if the test field is missing or the result set fails
     */
    @Test
    void booleansEncodeAsBits() throws Exception {
        var encoder = DatabaseEncoders.getEncoder("boolean").orElseThrow();
        var decoder = DatabaseEncoders.getDecoder("bool").orElseThrow();
        when(this.resultSet.getBoolean("flag")).thenReturn(true);

        assertThat(encoder.encode(this.config, this.field("plainField"), true)).isEqualTo("1");
        assertThat(encoder.encode(this.config, this.field("plainField"), false)).isEqualTo("0");
        assertThat(decoder.decode(this.config, this.field("plainField"), decoder.extract(this.resultSet, "flag"))).isEqualTo(true);
    }

    /**
     * Checks that the {@code json} encoder and decoder use the Gson of the config, and that a list survives the
     * round trip.
     *
     * @throws Exception if the test field is missing or the result set fails
     */
    @Test
    void jsonRoundTripsThroughTheConfigGson() throws Exception {
        when(this.config.gson()).thenReturn(new Gson());
        var field = this.field("listField");
        var encoder = DatabaseEncoders.getEncoder("json").orElseThrow();
        var decoder = DatabaseEncoders.getDecoder("json").orElseThrow();

        var encoded = encoder.encode(this.config, field, List.of("a", "b"));
        when(this.resultSet.getString("list")).thenReturn(encoded);

        assertThat(encoded).isEqualTo("[\"a\",\"b\"]");
        assertThat(decoder.decode(this.config, field, decoder.extract(this.resultSet, "list"))).isEqualTo(List.of("a", "b"));
    }

    /**
     * Checks that a UUID encodes as its string and decodes from a string.
     *
     * @throws Exception if the test field is missing or the result set fails
     */
    @Test
    void uuidDecodesFromString() throws Exception {
        var uuid = UUID.randomUUID();
        var decoder = DatabaseEncoders.getDecoder("uuid").orElseThrow();
        when(this.resultSet.getString("id")).thenReturn(uuid.toString());

        assertThat(DatabaseEncoders.getEncoder("uuid").orElseThrow().encode(this.config, this.field("plainField"), uuid)).isEqualTo(uuid.toString());
        assertThat(decoder.decode(this.config, this.field("plainField"), decoder.extract(this.resultSet, "id"))).isEqualTo(uuid);
    }

    /**
     * Checks that the {@code bigint} decoder reads a long and gives a {@link BigInteger}.
     *
     * @throws Exception if the test field is missing or the result set fails
     */
    @Test
    void bigintDecodesFromLong() throws Exception {
        var decoder = DatabaseEncoders.getDecoder("bigint").orElseThrow();
        when(this.resultSet.getLong("n")).thenReturn(Long.MAX_VALUE);

        assertThat(decoder.decode(this.config, this.field("plainField"), decoder.extract(this.resultSet, "n"))).isEqualTo(BigInteger.valueOf(Long.MAX_VALUE));
    }

    /**
     * Checks that a blob type keeps non-ASCII text through an encode and decode as UTF-8 bytes.
     *
     * @param type the blob type name
     * @throws Exception if the test field is missing or the result set fails
     */
    @ParameterizedTest
    @ValueSource(strings = {"tinyblob", "blob", "mediumblob", "longblob"})
    void blobsRoundTripTheirText(String type) throws Exception {
        var encoder = DatabaseEncoders.getEncoder(type).orElseThrow();
        var decoder = DatabaseEncoders.getDecoder(type).orElseThrow();
        var written = encoder.encode(this.config, this.field("plainField"), "héllo wörld");
        when(this.resultSet.getBytes("data")).thenReturn(written.getBytes(StandardCharsets.UTF_8));

        assertThat(decoder.decode(this.config, this.field("plainField"), decoder.extract(this.resultSet, "data"))).isEqualTo("héllo wörld");
    }

    /**
     * Checks that a {@code null} blob decodes to {@code null}.
     *
     * @throws Exception if the test field is missing or the result set fails
     */
    @Test
    void nullBlobDecodesToNull() throws Exception {
        var decoder = DatabaseEncoders.getDecoder("blob").orElseThrow();
        when(this.resultSet.getBytes("data")).thenReturn(null);

        assertThat(decoder.decode(this.config, this.field("plainField"), decoder.extract(this.resultSet, "data"))).isNull();
    }

    /**
     * Checks that each primitive and string decoder reads with the matching {@link ResultSet} getter.
     *
     * @throws SQLException if the mock result set fails
     */
    @Test
    void primitiveDecodersUseTheMatchingResultSetGetter() throws SQLException {
        when(this.resultSet.getInt("c")).thenReturn(1);
        when(this.resultSet.getFloat("c")).thenReturn(2f);
        when(this.resultSet.getShort("c")).thenReturn((short) 3);
        when(this.resultSet.getDouble("c")).thenReturn(4d);
        when(this.resultSet.getLong("c")).thenReturn(5L);
        when(this.resultSet.getByte("c")).thenReturn((byte) 6);
        when(this.resultSet.getString("c")).thenReturn("seven");

        var expected = Map.<String, Object>of(
                "int", 1, "integer", 1, "float", 2f, "short", (short) 3, "double", 4d, "long", 5L, "byte", (byte) 6,
                "string", "seven", "text", "seven", "varchar", "seven"
        );
        for (var entry : expected.entrySet()) {
            assertThat((Object) DatabaseEncoders.getDecoder(entry.getKey()).orElseThrow().extract(this.resultSet, "c")).as(entry.getKey()).isEqualTo(entry.getValue());
        }
    }

    /**
     * Checks that {@code varchar} gives the same SQL types as the UUID, username and maximum constants.
     */
    @Test
    void varcharBuildsSqlType() {
        assertThat(DatabaseEncoders.Defaults.varchar(36)).isEqualTo(DatabaseEncoders.Defaults.VARCHAR_UUID);
        assertThat(DatabaseEncoders.Defaults.varchar(30)).isEqualTo(DatabaseEncoders.Defaults.VARCHAR_USERNAME);
        assertThat(DatabaseEncoders.Defaults.varchar(65535)).isEqualTo(DatabaseEncoders.Defaults.VARCHAR_MAX);
    }

    /**
     * Checks the characters of each quote mark constant.
     */
    @Test
    void quoteMarks() {
        assertThat(List.of(DatabaseEncoders.QuoteMarks.NONE, DatabaseEncoders.QuoteMarks.SINGLE, DatabaseEncoders.QuoteMarks.DOUBLE, DatabaseEncoders.QuoteMarks.BACKTICK))
                .containsExactly("", "'", "\"", "`");
    }
}
