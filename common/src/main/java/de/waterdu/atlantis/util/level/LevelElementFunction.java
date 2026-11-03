package de.waterdu.atlantis.util.level;

import de.waterdu.atlantis.level.level.AtlantisLevel;
import de.waterdu.atlantis.level.level.InvalidAtlantisLevelException;
import de.waterdu.atlantis.util.java.collection.Pair;
import net.minecraft.resources.ResourceKey;

/**
 * Creates a registry value for an {@link AtlantisLevel}, such as its dimension type, together with its key.
 *
 * @param <R> the type of the value
 */
@FunctionalInterface
public interface LevelElementFunction<R> {

    /**
     * @param args the arguments that the level gives, for example from its config. Can be empty.
     * @return the key to register the value with, and the value. null makes the level invalid.
     */
    Pair<ResourceKey<R>, R> apply(AtlantisLevel level, String[] args) throws InvalidAtlantisLevelException;
}
