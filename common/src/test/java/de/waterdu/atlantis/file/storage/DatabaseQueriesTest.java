package de.waterdu.atlantis.file.storage;

import de.waterdu.atlantis.file.DatabaseEncoders.QuoteMarks;
import de.waterdu.atlantis.file.StorageTestConfigs;
import de.waterdu.atlantis.file.storage.DatabaseFixtures.AutoIncrementRecord;
import de.waterdu.atlantis.file.storage.DatabaseFixtures.Record;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the SQL that Database generates, without a database.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class DatabaseQueriesTest {

    private static final String DB = "testdb";

    /**
     * Checks that the schema has one element per annotated field, with the correct types and keys.
     */
    @Test
    void schemaHasOneElementPerAnnotatedField() {
        var schema = Database.Schema.of("mod", "records", Record.class);

        assertThat(schema.table()).isEqualTo("mod_records");
        assertThat(schema.elements()).extracting(Database.Schema.Element::name)
                .containsExactlyInAnyOrder("uuid", "name", "level", "score", "flag", "note", "payload", "tags");
        assertThat(schema.autoIncrementElements()).isZero();
        var uuid = element(schema, "uuid");
        assertThat(uuid.type()).isEqualTo("VARCHAR(36)");
        assertThat(uuid.primaryKey()).isTrue();
        assertThat(uuid.uniqueKey()).isTrue();
        assertThat(element(schema, "name").uniqueKey()).isTrue();
        assertThat(element(schema, "level").surround()).isEmpty();
        assertThat(element(schema, "tags").type()).isEqualTo("JSON");
    }

    /**
     * Checks the {@code CREATE DATABASE} and {@code CREATE TABLE} statements.
     */
    @Test
    void createStatements() {
        var queries = queries(Record.class);

        assertThat(queries.prepare(queries.createDatabase)).isEqualTo("CREATE DATABASE IF NOT EXISTS " + DB);
        var create = queries.prepare(queries.createTable);
        assertThat(create).startsWith("CREATE TABLE IF NOT EXISTS " + DB + ".mod_records (").endsWith(")");
        assertThat(create)
                .contains("uuid VARCHAR(36) NOT NULL PRIMARY KEY")
                .contains("name VARCHAR(30) NOT NULL UNIQUE")
                .contains("level INT NOT NULL")
                .contains("payload BLOB NOT NULL")
                .contains("tags JSON NOT NULL");
    }

    /**
     * Checks that the insert statement quotes the values and updates every column except the primary key.
     */
    @Test
    void insertQuotesValuesAndUpdatesEveryNonKeyColumn() {
        var config = StorageTestConfigs.register(Record.class, "records", new Volatile());
        var queries = queries(Record.class);
        var record = record();

        var insert = queries.prepare(queries.insertElement, "", config, record, schema(Record.class));

        assertThat(insert).startsWith("INSERT INTO " + DB + ".mod_records (");
        assertThat(insert).contains("(uuid, name, level, score, flag, note, payload, tags)");
        assertThat(insert).contains("'" + record.uuid() + "'", "'Steve'", "12", "2.5", "\"a\"", "\"b\"");
        assertThat(insert).contains("ON DUPLICATE KEY UPDATE ");
        var update = insert.substring(insert.indexOf("ON DUPLICATE KEY UPDATE "));
        assertThat(update).contains("name = 'Steve'", "level = 12", "flag = 1", "payload = 'binary'");
        assertThat(update).as("the primary key is never updated").doesNotContain("uuid =");
        assertThat(insert).doesNotContain("%");
    }

    /**
     * Checks that quotes and backslashes in a single-quoted value are escaped in the insert statement.
     */
    @Test
    void singleQuotedValuesAreEscaped() {
        var config = StorageTestConfigs.register(Record.class, "records", new Volatile());
        var queries = queries(Record.class);
        var record = record();
        record.note = "O'Brien said \\' OR 1=1 -- \\";

        var insert = queries.prepare(queries.insertElement, "", config, record, schema(Record.class));

        assertThat(insert).contains("'O''Brien said \\\\'' OR 1=1 -- \\\\'");
    }

    /**
     * Checks that {@link Database.Queries#escape} doubles backslashes and single quotes.
     */
    @Test
    void escapeDoublesBackslashesBeforeQuotes() {
        assertThat(Database.Queries.escape("plain")).isEqualTo("plain");
        assertThat(Database.Queries.escape("it's")).isEqualTo("it''s");
        assertThat(Database.Queries.escape("ends with \\")).isEqualTo("ends with \\\\");
        assertThat(Database.Queries.escape("\\'")).isEqualTo("\\\\''");
    }

    /**
     * Checks that values in double quotes and backticks are escaped for their quote mark.
     */
    @Test
    void doubleQuotedAndBacktickValuesAreEscaped() {
        assertThat(Database.Queries.escape(QuoteMarks.SINGLE, "it's")).isEqualTo("it''s");
        assertThat(Database.Queries.escape(QuoteMarks.DOUBLE, "say \"hi\" \\")).isEqualTo("say \"\"hi\"\" \\\\");
        assertThat(Database.Queries.escape(QuoteMarks.BACKTICK, "a`b")).isEqualTo("a``b");
    }

    /**
     * Checks that a value without quote marks must be a number, {@code NULL}, {@code TRUE} or {@code FALSE}.
     */
    @Test
    void unquotedValuesMustBeLiterals() {
        assertThat(Database.Queries.escape(QuoteMarks.NONE, "12")).isEqualTo("12");
        assertThat(Database.Queries.escape(QuoteMarks.NONE, "-2.5E-3")).isEqualTo("-2.5E-3");
        assertThat(Database.Queries.escape(QuoteMarks.NONE, "null")).isEqualTo("null");
        assertThat(Database.Queries.escape(QuoteMarks.NONE, "TRUE")).isEqualTo("TRUE");
        assertThatThrownBy(() -> Database.Queries.escape(QuoteMarks.NONE, "1; DROP TABLE x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Database.Queries.escape(QuoteMarks.NONE, "NaN")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Database.Queries.escape(QuoteMarks.NONE, "")).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Checks that a quote mark that is not one of {@link QuoteMarks} is rejected.
     */
    @Test
    void unknownQuoteMarksAreRejected() {
        assertThatThrownBy(() -> Database.Queries.escape("$$", "value")).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Checks that an auto-increment column is declared in the table but left out of the insert statement.
     */
    @Test
    void autoIncrementColumnsAreLeftToTheDatabase() {
        var config = StorageTestConfigs.register(AutoIncrementRecord.class, "auto", new Volatile());
        var schema = Database.Schema.of("mod", "auto", AutoIncrementRecord.class);
        var queries = Database.Queries.of(DB, schema);
        var record = new AutoIncrementRecord();

        assertThat(schema.autoIncrementElements()).isEqualTo(1);
        assertThat(queries.prepare(queries.createTable)).contains("id INT NOT NULL AUTO_INCREMENT PRIMARY KEY");
        var insert = queries.prepare(queries.insertElement, "", config, record, schema);
        assertThat(insert).startsWith("INSERT INTO " + DB + ".mod_auto (uuid, name) VALUES ('" + record.uuid() + "', '')");
        assertThat(insert).endsWith("ON DUPLICATE KEY UPDATE uuid = '" + record.uuid() + "', name = ''");
    }

    /**
     * Checks the select and delete statements by UUID and by name, and the select-all statement.
     */
    @Test
    void lookupsByUuidAndName() {
        var queries = queries(Record.class);
        var uuid = UUID.randomUUID();

        assertThat(queries.prepare(queries.selectElementUUID, uuid.toString())).isEqualTo("SELECT * FROM " + DB + ".mod_records WHERE uuid = '" + uuid + "'");
        assertThat(queries.prepare(queries.deleteElementUUID, uuid.toString())).isEqualTo("DELETE FROM " + DB + ".mod_records WHERE uuid = '" + uuid + "'");
        assertThat(queries.prepare(queries.selectElementName, "Steve")).isEqualTo("SELECT * FROM " + DB + ".mod_records WHERE name = 'Steve'");
        assertThat(queries.prepare(queries.selectAll)).isEqualTo("SELECT * FROM " + DB + ".mod_records");
    }

    /**
     * Creates a record named {@code Steve} with fixed column values.
     *
     * @return the record
     */
    private static Record record() {
        var record = new Record("Steve");
        record.level = 12;
        record.score = 2.5;
        record.flag = true;
        record.payload = "binary";
        record.tags = List.of("a", "b");
        return record;
    }

    /**
     * Creates the schema of a class for the {@code records} table of the mod {@code mod}.
     *
     * @param type the data class
     * @return the schema
     */
    private static Database.Schema schema(Class<?> type) {
        return Database.Schema.of("mod", "records", type);
    }

    /**
     * Creates queries for the test database and the schema of a class.
     *
     * @param type the data class
     * @return the initialized queries
     */
    private static Database.Queries queries(Class<?> type) {
        return Database.Queries.of(DB, schema(type));
    }

    /**
     * Finds an element of a schema by its name.
     *
     * @param schema the schema to search
     * @param name the name of the element
     * @return the element
     * @throws java.util.NoSuchElementException if the schema has no element with that name
     */
    private static Database.Schema.Element element(Database.Schema schema, String name) {
        return schema.elements().stream().filter(element -> element.name().equals(name)).findFirst().orElseThrow();
    }
}
