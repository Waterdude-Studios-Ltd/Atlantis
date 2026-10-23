package de.waterdu.atlantis.file.storage;

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

/**
 * The SQL that Database generates, checked without a database.
 */
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class DatabaseQueriesTest {

    private static final String DB = "testdb";

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

    @Test
    void createStatements() {
        var queries = queries(Record.class);

        assertThat(queries.prepare(queries.CREATE_DATABASE)).isEqualTo("CREATE DATABASE IF NOT EXISTS " + DB);
        var create = queries.prepare(queries.CREATE_TABLE);
        assertThat(create).startsWith("CREATE TABLE IF NOT EXISTS " + DB + ".mod_records (").endsWith(")");
        assertThat(create)
                .contains("uuid VARCHAR(36) NOT NULL PRIMARY KEY")
                .contains("name VARCHAR(30) NOT NULL UNIQUE")
                .contains("level INT NOT NULL")
                .contains("payload BLOB NOT NULL")
                .contains("tags JSON NOT NULL");
    }

    @Test
    void insertQuotesValuesAndUpdatesEveryNonKeyColumn() {
        var config = StorageTestConfigs.register(Record.class, "records", new Volatile());
        var queries = queries(Record.class);
        var record = record();

        var insert = queries.prepare(queries.INSERT_ELEMENT, "", config, record, schema(Record.class));

        assertThat(insert).startsWith("INSERT INTO " + DB + ".mod_records (");
        assertThat(insert).contains("(uuid, name, level, score, flag, note, payload, tags)");
        assertThat(insert).contains("'" + record.uuid() + "'", "'Steve'", "12", "2.5", "\"a\"", "\"b\"");
        assertThat(insert).contains("ON DUPLICATE KEY UPDATE ");
        var update = insert.substring(insert.indexOf("ON DUPLICATE KEY UPDATE "));
        assertThat(update).contains("name = 'Steve'", "level = 12", "flag = 1", "payload = 'binary'");
        assertThat(update).as("the primary key is never updated").doesNotContain("uuid =");
        assertThat(insert).doesNotContain("%");
    }

    @Test
    void singleQuotedValuesAreEscaped() {
        var config = StorageTestConfigs.register(Record.class, "records", new Volatile());
        var queries = queries(Record.class);
        var record = record();
        record.note = "O'Brien said \\' OR 1=1 -- \\";

        var insert = queries.prepare(queries.INSERT_ELEMENT, "", config, record, schema(Record.class));

        assertThat(insert).contains("'O''Brien said \\\\'' OR 1=1 -- \\\\'");
    }

    @Test
    void escapeDoublesBackslashesBeforeQuotes() {
        assertThat(Database.Queries.escape("plain")).isEqualTo("plain");
        assertThat(Database.Queries.escape("it's")).isEqualTo("it''s");
        assertThat(Database.Queries.escape("ends with \\")).isEqualTo("ends with \\\\");
        assertThat(Database.Queries.escape("\\'")).isEqualTo("\\\\''");
    }

    @Test
    void autoIncrementColumnsAreLeftToTheDatabase() {
        var config = StorageTestConfigs.register(AutoIncrementRecord.class, "auto", new Volatile());
        var schema = Database.Schema.of("mod", "auto", AutoIncrementRecord.class);
        var queries = new Database.Queries();
        queries.init(DB, schema);
        var record = new AutoIncrementRecord();

        assertThat(schema.autoIncrementElements()).isEqualTo(1);
        assertThat(queries.prepare(queries.CREATE_TABLE)).contains("id INT NOT NULL AUTO_INCREMENT PRIMARY KEY");
        var insert = queries.prepare(queries.INSERT_ELEMENT, "", config, record, schema);
        assertThat(insert).startsWith("INSERT INTO " + DB + ".mod_auto (uuid, name) VALUES ('" + record.uuid() + "', '')");
        assertThat(insert).endsWith("ON DUPLICATE KEY UPDATE uuid = '" + record.uuid() + "', name = ''");
    }

    @Test
    void lookupsByUuidAndName() {
        var queries = queries(Record.class);
        var uuid = UUID.randomUUID();

        assertThat(queries.prepare(queries.SELECT_ELEMENT_UUID, uuid.toString())).isEqualTo("SELECT * FROM " + DB + ".mod_records WHERE uuid = '" + uuid + "'");
        assertThat(queries.prepare(queries.DELETE_ELEMENT_UUID, uuid.toString())).isEqualTo("DELETE FROM " + DB + ".mod_records WHERE uuid = '" + uuid + "'");
        assertThat(queries.prepare(queries.SELECT_ELEMENT_NAME, "Steve")).isEqualTo("SELECT * FROM " + DB + ".mod_records WHERE name = 'Steve'");
        assertThat(queries.prepare(queries.SELECT_ALL)).isEqualTo("SELECT * FROM " + DB + ".mod_records");
    }

    private static Record record() {
        var record = new Record("Steve");
        record.level = 12;
        record.score = 2.5;
        record.flag = true;
        record.payload = "binary";
        record.tags = List.of("a", "b");
        return record;
    }

    private static Database.Schema schema(Class<?> type) {
        return Database.Schema.of("mod", "records", type);
    }

    private static Database.Queries queries(Class<?> type) {
        var queries = new Database.Queries();
        queries.init(DB, schema(type));
        return queries;
    }

    private static Database.Schema.Element element(Database.Schema schema, String name) {
        return schema.elements().stream().filter(element -> element.name().equals(name)).findFirst().orElseThrow();
    }
}
