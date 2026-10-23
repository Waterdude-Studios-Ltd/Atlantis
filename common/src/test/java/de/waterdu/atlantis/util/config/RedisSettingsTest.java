package de.waterdu.atlantis.util.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RedisSettingsTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(RedisSettings.class, new RedisSettings.TypeAdapter()).create();

    @Test
    void stringIsAUrl() {
        var settings = this.gson.fromJson("\"redis://user:pw@host:6379/\"", RedisSettings.class);

        assertThat(settings).isInstanceOf(RedisSettings.URL.class);
        assertThat(this.gson.toJson(settings, RedisSettings.class)).isEqualTo("{\"url\":\"redis://user:pw@host:6379/\"}");
    }

    @Test
    void objectWithUrlKeepsProperties() {
        var json = "{\"url\":\"redis://host/\",\"properties\":{\"timeout\":\"5\"}}";

        var settings = this.gson.fromJson(json, RedisSettings.class);

        assertThat(settings).isInstanceOf(RedisSettings.URL.class);
        assertThat(settings.properties).containsExactly(Map.entry("timeout", "5"));
        assertThat(JsonParser.parseString(this.gson.toJson(settings, RedisSettings.class))).isEqualTo(JsonParser.parseString(json));
    }

    @Test
    void explicitRoundTrips() {
        var json = "{\"ip\":\"10.0.0.1\",\"port\":6380,\"username\":\"u\",\"password\":\"p\"}";

        var settings = this.gson.fromJson(json, RedisSettings.class);

        assertThat(settings).isInstanceOf(RedisSettings.Explicit.class);
        assertThat(settings.properties).isEmpty();
        assertThat(JsonParser.parseString(this.gson.toJson(settings, RedisSettings.class))).isEqualTo(JsonParser.parseString(json));
    }

    @Test
    void defaultIsLocalExplicit() {
        var json = JsonParser.parseString(this.gson.toJson(RedisSettings.getDefault(), RedisSettings.class));

        assertThat(json).isEqualTo(JsonParser.parseString("{\"ip\":\"127.0.0.1\",\"port\":6379,\"username\":\"username\",\"password\":\"password\"}"));
    }
}
