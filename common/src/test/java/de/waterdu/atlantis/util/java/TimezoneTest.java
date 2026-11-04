package de.waterdu.atlantis.util.java;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link Timezone} and its type adapter.
 */
class TimezoneTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(Timezone.class, new Timezone.TypeAdapter()).create();

    /**
     * Checks that a timezone keeps its ID and resolves it to the matching {@link ZoneId}.
     */
    @Test
    void resolvesZone() {
        var zone = Timezone.of("Europe/Berlin");

        assertThat(zone.id()).isEqualTo("Europe/Berlin");
        assertThat(zone.zone()).isEqualTo(ZoneId.of("Europe/Berlin"));
        assertThat(Timezone.UTC.zone()).isEqualTo(ZoneId.of("UTC"));
    }

    /**
     * Checks that {@link Timezone#now()} for UTC is within five seconds of the current UTC time.
     */
    @Test
    void nowIsInTheZone() {
        var now = Timezone.UTC.now();

        assertThat(Duration.between(now, LocalDateTime.now(ZoneOffset.UTC)).abs()).isLessThan(Duration.ofSeconds(5));
    }

    /**
     * Checks that an invalid ID creates a timezone, and that {@link Timezone#zone()} then throws a
     * {@link DateTimeException}.
     */
    @Test
    void invalidZoneFailsLazily() {
        var zone = Timezone.of("Not/AZone");

        assertThat(zone.id()).isEqualTo("Not/AZone");
        assertThatThrownBy(zone::zone).isInstanceOf(DateTimeException.class);
    }

    /**
     * Checks that the type adapter reads and writes a timezone as its ID.
     */
    @Test
    void typeAdapterRoundTrips() {
        var zone = this.gson.fromJson("\"America/New_York\"", Timezone.class);

        assertThat(zone.zone()).isEqualTo(ZoneId.of("America/New_York"));
        assertThat(this.gson.toJson(zone)).isEqualTo("\"America/New_York\"");
    }
}
