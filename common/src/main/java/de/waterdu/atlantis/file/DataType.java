package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.PlayerData;

import java.util.function.Predicate;

public enum DataType {

    ALL(config -> true),
    CONFIGURATION(config -> !(config instanceof Data)),
    ALL_DATA(Data.class::isInstance),
    DATA(config -> !(config instanceof PlayerData) && config instanceof Data),
    PLAYER_DATA(PlayerData.class::isInstance),
    ;

    private final Predicate<Configuration> predicate;

    DataType(Predicate<Configuration> predicate) {
        this.predicate = predicate;
    }

    public boolean isValid(Config<?> config) {
        return this.predicate.test(config.get());
    }
}
