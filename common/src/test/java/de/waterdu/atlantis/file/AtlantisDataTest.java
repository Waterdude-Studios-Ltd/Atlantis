package de.waterdu.atlantis.file;

import de.waterdu.atlantis.Atlantis;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class AtlantisDataTest {

    @ParameterizedTest
    @CsvSource({
            "0, 4",
            "1, 4",
            "2, 5",
            "5, 11",
    })
    void automaticThreadCountNeverDropsBelowTheDefault(int atlantisMods, int threads) {
        assertThat(AtlantisData.automaticThreadCount(atlantisMods)).isEqualTo(threads).isGreaterThanOrEqualTo(Atlantis.DEFAULT_THREADS);
    }
}
