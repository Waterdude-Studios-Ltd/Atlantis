package de.waterdu.atlantis.file;

import de.waterdu.atlantis.Atlantis;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link AtlantisData}.
 */
class AtlantisDataTest {

    /**
     * Checks that the automatic thread count is two per Atlantis mod plus one, but never less than
     * {@link Atlantis#DEFAULT_THREADS}.
     *
     * @param atlantisMods the number of Atlantis mods
     * @param threads the expected thread count
     */
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
