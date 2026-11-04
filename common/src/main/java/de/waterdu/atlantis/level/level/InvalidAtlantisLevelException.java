package de.waterdu.atlantis.level.level;

import net.minecraft.resources.ResourceLocation;

/**
 * Signals that the chunk generator or the dimension type of an {@link AtlantisLevel} is not valid.
 */
public class InvalidAtlantisLevelException extends Exception {

    /**
     * Creates an exception for a property of a level that is not valid.
     *
     * @param level the level with the property
     * @param type the name of the property type, for example {@code ChunkGenerator}
     * @param key the key of the property
     */
    protected InvalidAtlantisLevelException(AtlantisLevel level, String type, String key) {
        super("Invalid property: " + type + " " + key + " (" + level.toString() + ")");
    }

    /**
     * Creates an exception for a property function that failed.
     *
     * @param type the name of the property type, for example {@code ChunkGenerator}
     * @param key the key of the property function
     * @param e the exception that the function threw
     */
    protected InvalidAtlantisLevelException(String type, ResourceLocation key, Exception e) {
        super("Construction error: " + type + " " + key.toString() + ": " + e.getMessage());
    }
}
