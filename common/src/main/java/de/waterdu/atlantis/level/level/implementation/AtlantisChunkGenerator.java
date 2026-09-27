package de.waterdu.atlantis.level.level.implementation;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.NonNull;

public class AtlantisChunkGenerator extends FlatLevelSource {

    public AtlantisChunkGenerator(FlatLevelGeneratorSettings settings) {
        super(settings);
    }

    @Override
    public void createStructures(@NonNull RegistryAccess registryAccess, @NonNull ChunkGeneratorStructureState structureState, @NonNull StructureManager structureManager, @NonNull ChunkAccess chunk, @NonNull StructureTemplateManager structureTemplateManager) {
        // noop
    }
}
