package de.waterdu.atlantis.util.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests how {@link RedisSettings.TypeAdapter} reads and writes Redis settings.
 */
class RedisSettingsTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(RedisSettings.class, new RedisSettings.TypeAdapter()).create();

    /**
     * Checks that a JSON string reads as URL settings and writes back as an object with a {@code url}.
     */
    @Test
    void stringIsAUrl() {
        var settings = this.gson.fromJson("\"redis://user:pw@host:6379/\"", RedisSettings.class);

        assertThat(settings).isInstanceOf(RedisSettings.URL.class);
        assertThat(this.gson.toJson(settings, RedisSettings.class)).isEqualTo("{\"url\":\"redis://user:pw@host:6379/\"}");
    }

    /**
     * Checks that an object with a {@code url} keeps its properties when it is read and written.
     */
    @Test
    void objectWithUrlKeepsProperties() {
        var json = "{\"url\":\"redis://host/\",\"properties\":{\"timeout\":\"5\"}}";

        var settings = this.gson.fromJson(json, RedisSettings.class);

        assertThat(settings).isInstanceOf(RedisSettings.URL.class);
        assertThat(settings.properties).containsExactly(Map.entry("timeout", "5"));
        assertThat(JsonParser.parseString(this.gson.toJson(settings, RedisSettings.class))).isEqualTo(JsonParser.parseString(json));
    }

    /**
     * Checks that explicit settings have no properties and write back the same JSON.
     */
    @Test
    void explicitRoundTrips() {
        var json = "{\"ip\":\"10.0.0.1\",\"port\":6380,\"username\":\"u\",\"password\":\"p\"}";

        var settings = this.gson.fromJson(json, RedisSettings.class);

        assertThat(settings).isInstanceOf(RedisSettings.Explicit.class);
        assertThat(settings.properties).isEmpty();
        assertThat(JsonParser.parseString(this.gson.toJson(settings, RedisSettings.class))).isEqualTo(JsonParser.parseString(json));
    }

    /**
     * Checks that the default settings are explicit settings for {@code 127.0.0.1:6379}.
     */
    @Test
    void defaultIsLocalExplicit() {
        var json = JsonParser.parseString(this.gson.toJson(RedisSettings.getDefault(), RedisSettings.class));

        assertThat(json).isEqualTo(JsonParser.parseString("{\"ip\":\"127.0.0.1\",\"port\":6379,\"username\":\"username\",\"password\":\"password\"}"));
    }
}
