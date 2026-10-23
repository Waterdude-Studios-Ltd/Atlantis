package de.waterdu.atlantis.util.config;

import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import de.waterdu.atlantis.file.storage.Database;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DatabaseSettings.storage builds the pool from every setting. Building a Database doesn't connect.
 */
@ExtendWith(AtlantisTestEnvironment.class)
class DatabaseSettingsStorageTest {

    private static final String EXTRA_SETTINGS = """
            "connections": 7, "minIdle": 2, "connectionTimeout": 1234, "idleTimeout": 60000, "leakDetection": 30000,
            "properties": {"useSSL": "false", "allowPublicKeyRetrieval": "true"}""";

    @Test
    void explicitSettings() {
        var settings = parse("{\"ip\": \"10.0.0.5\", \"port\": 3307, \"username\": \"root\", \"password\": \"pw\", " + EXTRA_SETTINGS + "}");

        var hikari = hikari(settings.storage("mymod"));

        assertThat(hikari.getJdbcUrl()).isEqualTo("jdbc:mysql://root:pw@10.0.0.5:3307/");
        assertThat(hikari.getPoolName()).isEqualTo("mymod");
        assertThat(hikari.getDataSourceProperties()).containsEntry("serverName", "10.0.0.5").containsEntry("port", "3307").containsEntry("user", "root");
        assertExtraSettings(hikari);
    }

    @Test
    void urlSettings() {
        var settings = parse("{\"url\": \"jdbc:mysql://db.example:3306/\", " + EXTRA_SETTINGS + "}");

        var hikari = hikari(settings.storage("mymod"));

        assertThat(hikari.getJdbcUrl()).isEqualTo("jdbc:mysql://db.example:3306/");
        assertExtraSettings(hikari);
    }

    @Test
    void defaultsWithoutExtraSettings() {
        var hikari = hikari(parse("\"jdbc:mysql://db.example:3306/\"").storage("mymod"));

        assertThat(hikari.getMinimumIdle()).isEqualTo(5);
        assertThat(hikari.getConnectionTimeout()).isEqualTo(10000);
        assertThat(hikari.getIdleTimeout()).isEqualTo(600000);
        assertThat(hikari.getLeakDetectionThreshold()).isEqualTo(600000);
    }

    private static void assertExtraSettings(HikariConfig hikari) {
        assertThat(hikari.getMaximumPoolSize()).isEqualTo(7);
        assertThat(hikari.getMinimumIdle()).isEqualTo(2);
        assertThat(hikari.getConnectionTimeout()).isEqualTo(1234);
        assertThat(hikari.getIdleTimeout()).isEqualTo(60000);
        assertThat(hikari.getLeakDetectionThreshold()).isEqualTo(30000);
        assertThat(hikari.getDataSourceProperties()).containsEntry("useSSL", "false").containsEntry("allowPublicKeyRetrieval", "true");
    }

    private static DatabaseSettings parse(String json) {
        return new GsonBuilder().registerTypeAdapter(DatabaseSettings.class, new DatabaseSettings.TypeAdapter()).create().fromJson(json, DatabaseSettings.class);
    }

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
