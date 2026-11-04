package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests which kinds of configuration each {@link DataType} accepts.
 */
class DataTypeTest {

    private static final Map<String, Class<? extends Configuration>> KINDS = Map.of(
            "configuration", Configuration.class,
            "data", Data.class,
            "named", NamedData.class,
            "player", PlayerData.class
    );

    /**
     * Checks that the data type accepts a configuration, data, named data and player data as expected.
     *
     * @param type the data type to check
     * @param configuration {@code true} if the type accepts a plain configuration
     * @param data {@code true} if the type accepts data
     * @param named {@code true} if the type accepts named data
     * @param player {@code true} if the type accepts player data
     */
    @ParameterizedTest
    @CsvSource({
            "ALL,           true,  true,  true,  true",
            "CONFIGURATION, true,  false, false, false",
            "ALL_DATA,      false, true,  true,  true",
            "DATA,          false, true,  true,  false",
            "PLAYER_DATA,   false, false, false, true",
    })
    void isValid(DataType type, boolean configuration, boolean data, boolean named, boolean player) {
        assertThat(type.isValid(config("configuration"))).as("configuration").isEqualTo(configuration);
        assertThat(type.isValid(config("data"))).as("data").isEqualTo(data);
        assertThat(type.isValid(config("named"))).as("named data").isEqualTo(named);
        assertThat(type.isValid(config("player"))).as("player data").isEqualTo(player);
    }

    /**
     * Creates a mock config whose instance is a mock of the given kind.
     *
     * @param kind {@code configuration}, {@code data}, {@code named} or {@code player}
     * @return the mock config
     */
    private static Config<?> config(String kind) {
        var config = mock(Config.class);
        doReturn(mock(KINDS.get(kind))).when(config).get();
        return config;
    }
}
