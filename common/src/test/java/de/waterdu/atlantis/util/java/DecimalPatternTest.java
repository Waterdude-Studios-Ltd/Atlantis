package de.waterdu.atlantis.util.java;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class DecimalPatternTest {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(DecimalPattern.class, new DecimalPattern.TypeAdapter()).create();
    private Locale previous;

    @BeforeEach
    void useRootLocale() {
        this.previous = Locale.getDefault();
        Locale.setDefault(Locale.ROOT);
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(this.previous);
    }

    @Test
    void formatsLongsAndDoubles() {
        var pattern = DecimalPattern.of("#,##0.00");

        assertThat(pattern.format(1234567L)).isEqualTo("1,234,567.00");
        assertThat(pattern.format(3.14159)).isEqualTo("3.14");
        assertThat(pattern.pattern()).isEqualTo("#,##0.00");
        assertThat(pattern.formatter()).isSameAs(pattern.formatter());
    }

    @Test
    void typeAdapterRoundTrips() {
        var pattern = this.gson.fromJson("\"0.#\"", DecimalPattern.class);

        assertThat(pattern.format(2.25)).isEqualTo("2.2");
        assertThat(this.gson.toJson(pattern)).isEqualTo("\"0.#\"");
    }
}
