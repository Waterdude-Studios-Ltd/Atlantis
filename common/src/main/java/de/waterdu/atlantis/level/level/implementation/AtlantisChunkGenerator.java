package de.waterdu.atlantis.level.level.implementation;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.NonNull;

/**
 * Generates flat chunks without structures.
 */
public class AtlantisChunkGenerator extends FlatLevelSource {

    /**
     * Creates a flat chunk generator.
     *
     * @param settings the flat level settings
     */
    public AtlantisChunkGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    /**
     * Does nothing, so no structures generate.
     *
     * @param registryAccess the registry access
     * @param structureState the structure state of the chunk generator
     * @param structureManager the structure manager
     * @param chunk the chunk
     * @param structureTemplateManager the structure template manager
     */
    @Override
    public void createStructures(@NonNull RegistryAccess registryAccess, @NonNull ChunkGeneratorStructureState structureState, @NonNull StructureManager structureManager, @NonNull ChunkAccess chunk, @NonNull StructureTemplateManager structureTemplateManager) {
        // noop
    }
}
