package de.waterdu.atlantis.util.level;

import de.waterdu.atlantis.level.level.AtlantisLevel;
import de.waterdu.atlantis.level.level.InvalidAtlantisLevelException;

/**
 * Creates a value for an {@link AtlantisLevel} without a registry key, unlike {@link LevelElementFunction}.
 *
 * @param <R> the type of the value
 */
@FunctionalInterface
public interface AnonymousLevelElementFunction<R> {

    /**
     * Creates the value for the level.
     *
     * @param level the level that needs the value
     * @param args the arguments that the level gives, for example from its config. Can be empty.
     * @return the value
     * @throws InvalidAtlantisLevelException if the level is not valid for this function
     */
    R apply(AtlantisLevel level, String[] args) throws InvalidAtlantisLevelException;
}
