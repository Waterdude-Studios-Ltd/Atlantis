package de.waterdu.atlantis.util.level;

import de.waterdu.atlantis.level.level.AtlantisLevel;
import de.waterdu.atlantis.level.level.InvalidAtlantisLevelException;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Creates the chunk generator of an {@link AtlantisLevel}. Register one with
 * {@code AtlantisLevelRegistry.registerChunkGenerator}.
 */
@FunctionalInterface
public interface ChunkGeneratorFunction {

    /**
     * @param args the arguments that the level gives, for example from its config. Can be empty.
     * @return the chunk generator. null makes the level invalid.
     */
    ChunkGenerator apply(AtlantisLevel level, RegistryOps.RegistryInfo<Biome> biomes, String[] args) throws InvalidAtlantisLevelException;
}
