package de.waterdu.atlantis.util.config;

import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import de.waterdu.atlantis.file.storage.Database;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DatabaseSettings.storage builds the pool from every setting. A Database does not connect when it is built.
 */
@ExtendWith(AtlantisTestEnvironment.class)
class DatabaseSettingsStorageTest {

    private static final String EXTRA_SETTINGS = """
            "connections": 7, "minIdle": 2, "connectionTimeout": 1234, "idleTimeout": 60000, "leakDetection": 30000,
            "properties": {"useSSL": "false", "allowPublicKeyRetrieval": "true"}""";

    /**
     * Checks that explicit settings give the JDBC URL, the pool name, the data source properties and the extra
     * settings.
     */
    @Test
    void explicitSettings() {
        var settings = parse("{\"ip\": \"10.0.0.5\", \"port\": 3307, \"username\": \"root\", \"password\": \"pw\", " + EXTRA_SETTINGS + "}");

        var hikari = hikari(settings.storage("mymod"));

        assertThat(hikari.getJdbcUrl()).isEqualTo("jdbc:mysql://root:pw@10.0.0.5:3307/");
        assertThat(hikari.getPoolName()).isEqualTo("mymod");
        assertThat(hikari.getDataSourceProperties()).containsEntry("serverName", "10.0.0.5").containsEntry("port", "3307").containsEntry("user", "root");
        assertExtraSettings(hikari);
    }

    /**
     * Checks that URL settings use the URL as it is and apply the extra settings.
     */
    @Test
    void urlSettings() {
        var settings = parse("{\"url\": \"jdbc:mysql://db.example:3306/\", " + EXTRA_SETTINGS + "}");

        var hikari = hikari(settings.storage("mymod"));

        assertThat(hikari.getJdbcUrl()).isEqualTo("jdbc:mysql://db.example:3306/");
        assertExtraSettings(hikari);
    }

    /**
     * Checks that the pool uses the default idle, timeout and leak detection values when no extra settings are given.
     */
    @Test
    void defaultsWithoutExtraSettings() {
        var hikari = hikari(parse("\"jdbc:mysql://db.example:3306/\"").storage("mymod"));

        assertThat(hikari.getMinimumIdle()).isEqualTo(5);
        assertThat(hikari.getConnectionTimeout()).isEqualTo(10000);
        assertThat(hikari.getIdleTimeout()).isEqualTo(600000);
        assertThat(hikari.getLeakDetectionThreshold()).isEqualTo(600000);
    }

    /**
     * Checks that the pool has the values from {@code EXTRA_SETTINGS}.
     *
     * @param hikari the pool configuration to check
     */
    private static void assertExtraSettings(HikariConfig hikari) {
        assertThat(hikari.getMaximumPoolSize()).isEqualTo(7);
        assertThat(hikari.getMinimumIdle()).isEqualTo(2);
        assertThat(hikari.getConnectionTimeout()).isEqualTo(1234);
        assertThat(hikari.getIdleTimeout()).isEqualTo(60000);
        assertThat(hikari.getLeakDetectionThreshold()).isEqualTo(30000);
        assertThat(hikari.getDataSourceProperties()).containsEntry("useSSL", "false").containsEntry("allowPublicKeyRetrieval", "true");
    }

    /**
     * Reads database settings from JSON with {@link DatabaseSettings.TypeAdapter}.
     *
     * @param json the JSON to read
     * @return the settings
     */
    private static DatabaseSettings parse(String json) {
        return new GsonBuilder().registerTypeAdapter(DatabaseSettings.class, new DatabaseSettings.TypeAdapter()).create().fromJson(json, DatabaseSettings.class);
    }

    /**
     * Gets the private {@code config} field of a database with reflection.
     *
     * @param database the database to read
     * @return the pool configuration of the database
     * @throws AssertionError if the field cannot be read
     */
    private static HikariConfig hikari(Database database) {
        try {
            var field = Database.class.getDeclaredField("config");
            field.setAccessible(true);
            return (HikariConfig) field.get(database);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
