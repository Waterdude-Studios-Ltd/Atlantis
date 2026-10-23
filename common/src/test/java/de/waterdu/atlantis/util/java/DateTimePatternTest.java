package de.waterdu.atlantis.util.java;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimePatternTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(DateTimePattern.class, new DateTimePattern.TypeAdapter()).create();

    @Test
    void formats() {
        var pattern = DateTimePattern.of("yyyy-MM-dd HH:mm");

        assertThat(pattern.pattern()).isEqualTo("yyyy-MM-dd HH:mm");
        assertThat(pattern.format(LocalDateTime.of(2026, 10, 18, 9, 5))).isEqualTo("2026-10-18 09:05");
        assertThat(pattern.formatter()).isSameAs(pattern.formatter());
    }

    @Test
    void typeAdapterRoundTrips() {
        var pattern = this.gson.fromJson("\"dd/MM\"", DateTimePattern.class);

        assertThat(pattern.format(LocalDateTime.of(2026, 1, 2, 0, 0))).isEqualTo("02/01");
        assertThat(this.gson.toJson(pattern)).isEqualTo("\"dd/MM\"");
    }
}
