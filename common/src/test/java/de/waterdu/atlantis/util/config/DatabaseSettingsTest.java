package de.waterdu.atlantis.util.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.test.AtlantisTestEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests how {@link DatabaseSettings.TypeAdapter} reads and writes database settings.
 */
@ExtendWith(AtlantisTestEnvironment.class)
class DatabaseSettingsTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(DatabaseSettings.class, new DatabaseSettings.TypeAdapter()).create();

    /**
     * @return the default number of database connections from {@link Settings}
     */
    private static int defaultConnections() {
        return Settings.get().getDefaultDatabaseConnections();
    }

    /**
     * Checks that a JSON string reads as URL settings with the default connections, and writes back as the same
     * string.
     */
    @Test
    void stringIsAUrlWithDefaultConnections() {
        var settings = this.gson.fromJson("\"jdbc:mysql://host/db\"", DatabaseSettings.class);

        assertThat(settings).isInstanceOf(DatabaseSettings.URL.class);
        assertThat(settings.connections()).isEqualTo(defaultConnections());
        assertThat(settings.areExtraSettingsDefaults()).isTrue();
        assertThat(this.gson.toJson(settings, DatabaseSettings.class)).isEqualTo("\"jdbc:mysql://host/db\"");
    }

    /**
     * Checks that a number before the URL in the string sets the connections, and that it writes back the same way.
     */
    @Test
    void stringMayPrefixConnections() {
        var connections = defaultConnections() + 7;
        var json = "\"" + connections + " jdbc:mysql://host/db\"";

        var settings = this.gson.fromJson(json, DatabaseSettings.class);

        assertThat(settings).isInstanceOf(DatabaseSettings.URL.class);
        assertThat(settings.connections()).isEqualTo(connections);
        assertThat(this.gson.toJson(settings, DatabaseSettings.class)).isEqualTo(json);
    }

    /**
     * Checks that URL settings with extra settings read all values and write back as an object.
     */
    @Test
    void urlWithExtrasSerializesAsObject() {
        var json = "{\"url\":\"jdbc:mysql://host/db\",\"connections\":3,\"minIdle\":1,\"connectionTimeout\":2,\"idleTimeout\":4,\"leakDetection\":5,\"properties\":{\"useSSL\":\"false\"}}";

        var settings = this.gson.fromJson(json, DatabaseSettings.class);

        assertThat(settings).isInstanceOf(DatabaseSettings.URL.class);
        assertThat(settings.areExtraSettingsDefaults()).isFalse();
        assertThat(settings.minIdle).isEqualTo(1);
        assertThat(settings.connectionTimeout).isEqualTo(2);
        assertThat(settings.idleTimeout).isEqualTo(4);
        assertThat(settings.leakDetection).isEqualTo(5);
        assertThat(settings.properties).containsExactly(Map.entry("useSSL", "false"));
        assertThat(JsonParser.parseString(this.gson.toJson(settings, DatabaseSettings.class))).isEqualTo(JsonParser.parseString(json));
    }

    /**
     * Checks that explicit settings write back the same JSON, without the extra settings that have default values.
     */
    @Test
    void explicitRoundTripsAndOmitsDefaultExtras() {
        var json = "{\"ip\":\"10.0.0.1\",\"port\":3307,\"username\":\"u\",\"password\":\"p\",\"connections\":4}";

        var settings = this.gson.fromJson(json, DatabaseSettings.class);

        assertThat(settings).isInstanceOf(DatabaseSettings.Explicit.class);
        assertThat(settings.connections()).isEqualTo(4);
        assertThat(settings.areExtraSettingsDefaults()).isTrue();
        assertThat(JsonParser.parseString(this.gson.toJson(settings, DatabaseSettings.class))).isEqualTo(JsonParser.parseString(json));
    }

    /**
     * Checks that explicit settings without {@code connections} use the default connections.
     */
    @Test
    void explicitWithoutConnectionsUsesDefault() {
        var settings = this.gson.fromJson("{\"ip\":\"h\",\"port\":1,\"username\":\"u\",\"password\":\"p\"}", DatabaseSettings.class);

        assertThat(settings.connections()).isEqualTo(defaultConnections());
    }

    /**
     * Checks that the default settings point to {@code 127.0.0.1:3306} with the default connections.
     */
    @Test
    void defaultIsLocalExplicit() {
        var json = JsonParser.parseString(this.gson.toJson(DatabaseSettings.getDefault(), DatabaseSettings.class)).getAsJsonObject();

        assertThat(json.get("ip").getAsString()).isEqualTo("127.0.0.1");
        assertThat(json.get("port").getAsInt()).isEqualTo(3306);
        assertThat(json.get("connections").getAsInt()).isEqualTo(defaultConnections());
    }
}
