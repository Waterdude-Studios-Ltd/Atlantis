package de.waterdu.atlantis.util.level;

import de.waterdu.atlantis.level.level.AtlantisLevel;
import de.waterdu.atlantis.level.level.InvalidAtlantisLevelException;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;

@FunctionalInterface
public interface ChunkGeneratorFunction {

    ChunkGenerator apply(AtlantisLevel level, RegistryOps.RegistryInfo<Biome> biomes, String[] args) throws InvalidAtlantisLevelException;
}
