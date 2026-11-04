package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.PlayerData;

import java.util.function.Predicate;

/**
 * Selects configs by the type of their configuration.
 */
public enum DataType {

    ALL(config -> true),
    CONFIGURATION(config -> !(config instanceof Data)),
    ALL_DATA(Data.class::isInstance),
    DATA(config -> !(config instanceof PlayerData) && config instanceof Data),
    PLAYER_DATA(PlayerData.class::isInstance),
    ;

    private final Predicate<Configuration> predicate;

    /**
     * @param predicate tests the configuration instance of a config
     */
    DataType(Predicate<Configuration> predicate) {
        this.predicate = predicate;
    }

    /**
     * Checks if the configuration of the config is of this type.
     *
     * @param config the config to check
     * @return {@code true} if the configuration matches
     */
    public boolean isValid(Config<?> config) {
        return this.predicate.test(config.get());
    }
}
