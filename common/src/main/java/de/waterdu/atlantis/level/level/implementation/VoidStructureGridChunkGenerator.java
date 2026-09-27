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

public class VoidStructureGridChunkGenerator extends AtlantisChunkGenerator {

    private final ResourceLocation structureKey;
    private Structure structure = null;
    private final int chunkOffset;

    public VoidStructureGridChunkGenerator(int chunkOffset, String structure, RegistryOps.RegistryInfo<Biome> biomes) {
        super(new FlatLevelGeneratorSettings(Optional.empty(), biomes.getter().getOrThrow(Biomes.THE_VOID), new ArrayList<>()));
        this.structureKey = ResourceLocation.parse(structure);
        this.chunkOffset = chunkOffset;
    }

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
