package de.waterdu.atlantis.file.storage;

import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.StorageTestConfigs;
import de.waterdu.atlantis.file.storage.DatabaseFixtures.Record;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.util.config.DatabaseSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static de.waterdu.atlantis.file.StorageTestConfigs.await;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database storage against a real MySQL server. Needs Docker; skipped without it.
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@MinecraftBootstrapped
@ExtendWith(AtlantisTestEnvironment.class)
class DatabaseIntegrationTest {

    private static final String PASSWORD = "atlantis";
    private static final AtomicInteger TABLES = new AtomicInteger();

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0").withUsername("root").withPassword(PASSWORD);

    private final List<Config<?>> configs = new ArrayList<>();

    @AfterEach
    void closePools() {
        this.configs.forEach(config -> config.storage().destruct(config));
    }

    @Test
    void writesAndReadsEveryColumnType() throws Exception {
        var config = this.register(explicit());
        var record = record("Steve");

        assertThat(await(config.storage().writeSpecific(config, record))).isTrue();
        config.clearAll();
        var read = (Record) await(config.storage().readFromUUID(config, record.uuid()));

        assertSameColumns(read, record);
    }

    @Test
    void readsByName() throws Exception {
        var config = this.register(explicit());
        var record = record("Alex");
        await(config.storage().writeSpecific(config, record));
        config.clearAll();

        assertThat(((Record) await(config.storage().readFromName(config, "Alex"))).uuid()).isEqualTo(record.uuid());
        assertThat(await(config.storage().readFromName(config, "Nobody"))).isNull();
    }

    @Test
    void namesAreEscapedInLookups() throws Exception {
        var config = this.register(explicit());
        await(config.storage().writeSpecific(config, record("Steve")));

        assertThat(await(config.storage().readFromName(config, "x' OR '1'='1"))).isNull();
    }

    @Test
    void writingAgainUpdatesTheRow() throws Exception {
        var config = this.register(explicit());
        var record = record("Steve");
        await(config.storage().writeSpecific(config, record));
        record.level = 99;
        record.tags = List.of("changed");

        await(config.storage().writeSpecific(config, record));
        config.clearAll();
        var read = (Record) await(config.storage().readFromUUID(config, record.uuid()));

        assertThat(read.level).isEqualTo(99);
        assertThat(read.tags).containsExactly("changed");
    }

    @Test
    void readLoadsEveryRow() throws Exception {
        var config = this.register(explicit());
        for (var name : List.of("a", "b", "c")) {
            var record = record(name);
            config.put(record);
        }
        assertThat(await(config.storage().write(config))).isTrue();
        StorageTestConfigs.awaitUntil("all rows are written", () -> {
            config.clearAll();
            await(config.storage().read(config, false));
            return config.all().size() == 3;
        });

        assertThat(config.all()).extracting(record -> ((Record) record).name()).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    void deleteRemovesTheRow() throws Exception {
        var config = this.register(explicit());
        var record = record("Steve");
        config.put(record);
        await(config.storage().writeSpecific(config, record));

        assertThat(await(config.storage().deleteFromUUID(config, record.uuid()))).isSameAs(record);
        assertThat(config.uuidDataMap()).isEmpty();
        assertThat(await(config.storage().readFromUUID(config, record.uuid()))).isNull();
    }

    @Test
    void urlSettingsConnect() throws Exception {
        var url = "jdbc:mysql://root:" + PASSWORD + "@" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT) + "/";
        var config = this.register(settings("{\"url\": \"" + url + "\", " + PROPERTIES + "}"));
        var record = record("Steve");

        assertThat(await(config.storage().writeSpecific(config, record))).isTrue();
        config.clearAll();
        assertThat(await(config.storage().readFromUUID(config, record.uuid()))).isNotNull();
    }

    private static final String PROPERTIES = "\"connections\": 2, \"properties\": {\"useSSL\": \"false\", \"allowPublicKeyRetrieval\": \"true\"}";

    private static DatabaseSettings explicit() {
        return settings("{\"ip\": \"" + MYSQL.getHost() + "\", \"port\": " + MYSQL.getMappedPort(MySQLContainer.MYSQL_PORT)
                + ", \"username\": \"root\", \"password\": \"" + PASSWORD + "\", " + PROPERTIES + "}");
    }

    private static DatabaseSettings settings(String json) {
        return new GsonBuilder().registerTypeAdapter(DatabaseSettings.class, new DatabaseSettings.TypeAdapter()).create().fromJson(json, DatabaseSettings.class);
    }

    private Config<Record> register(DatabaseSettings settings) {
        var table = "records_" + TABLES.incrementAndGet();
        var config = StorageTestConfigs.register(Record.class, table, settings.storage("atlantis_test_" + table));
        assertThat(config).as("config registered (see the log for the database error)").isNotNull();
        this.configs.add(config);
        return config;
    }

    private static Record record(String name) {
        var record = new Record(name);
        record.level = 12;
        record.score = 2.5;
        record.flag = true;
        record.note = "O'Brien \\' \\";
        record.payload = "binary \\ 'data'";
        record.tags = List.of("a", "b");
        return record;
    }

    private static void assertSameColumns(Record read, Record expected) {
        assertThat(read).isNotNull();
        assertThat(read.uuid()).isEqualTo(expected.uuid());
        assertThat(read.name()).isEqualTo(expected.name());
        assertThat(read.level).isEqualTo(expected.level);
        assertThat(read.score).isEqualTo(expected.score);
        assertThat(read.flag).isEqualTo(expected.flag);
        assertThat(read.note).isEqualTo(expected.note);
        assertThat(read.payload).isEqualTo(expected.payload);
        assertThat(read.tags).isEqualTo(expected.tags);
    }
}
