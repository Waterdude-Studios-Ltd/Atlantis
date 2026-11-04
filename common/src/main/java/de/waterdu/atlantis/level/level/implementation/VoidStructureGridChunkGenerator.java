package de.waterdu.atlantis.level.level.implementation;

import de.waterdu.atlantis.AtlantisLogger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Generates a void level with one structure in a grid of chunks.
 */
public class VoidStructureGridChunkGenerator extends AtlantisChunkGenerator {

    private final ResourceLocation structureKey;
    private Structure structure = null;
    private final int chunkOffset;

    /**
     * Creates a void chunk generator with the structure in every chunk whose coordinates are multiples of the offset.
     *
     * @param chunkOffset the distance in chunks between two structures
     * @param structure the key of the structure, parsed as a {@link ResourceLocation}
     * @param biomes the biome registry info, used to get the void biome
     */
    public VoidStructureGridChunkGenerator(int chunkOffset, String structure, RegistryOps.RegistryInfo<Biome> biomes) {
        super(new FlatLevelGeneratorSettings(Optional.empty(), biomes.getter().getOrThrow(Biomes.THE_VOID), new ArrayList<>()));
        this.structureKey = ResourceLocation.parse(structure);
        this.chunkOffset = chunkOffset;
    }

    /**
     * Starts the structure in the chunk if both chunk coordinates are multiples of the offset. It gets the structure
     * from the registry the first time. If the structure cannot start, it logs an error.
     *
     * @param registryAccess the registry access, used to get the structure
     * @param structureState the structure state of the chunk generator
     * @param structureManager the structure manager
     * @param chunk the chunk
     * @param structureTemplateManager the structure template manager
     */
    @Override
    public void createStructures(@NonNull RegistryAccess registryAccess, @NonNull ChunkGeneratorStructureState structureState, @NonNull StructureManager structureManager, @NonNull ChunkAccess chunk, @NonNull StructureTemplateManager structureTemplateManager) {
        if (this.structure == null) {
            this.structure = registryAccess.registryOrThrow(Registries.STRUCTURE).get(this.structureKey);
        }

        var pos = chunk.getPos();
        if (pos.x % this.chunkOffset == 0 && pos.z % this.chunkOffset == 0) {
            var sectionPos = SectionPos.of(pos.getWorldPosition());
            var start = structureManager.getStartForStructure(sectionPos, this.structure, chunk);
            if (start == null) {
                AtlantisLogger.error("Failed to place structure {} in AtlantisLevel", this.structureKey.toString());
                return;
            }
            structureManager.setStartForStructure(sectionPos, this.structure, start, chunk);
        }
    }
}
