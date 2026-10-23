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

class DataTypeTest {

    private static final Map<String, Class<? extends Configuration>> KINDS = Map.of(
            "configuration", Configuration.class,
            "data", Data.class,
            "named", NamedData.class,
            "player", PlayerData.class
    );

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

    private static Config<?> config(String kind) {
        var config = mock(Config.class);
        doReturn(mock(KINDS.get(kind))).when(config).get();
        return config;
    }
}
