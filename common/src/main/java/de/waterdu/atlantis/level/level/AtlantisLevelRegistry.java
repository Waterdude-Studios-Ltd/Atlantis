package de.waterdu.atlantis.level.level;

import com.google.common.collect.Lists;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.level.level.implementation.AtlantisChunkGenerator;
import de.waterdu.atlantis.level.level.implementation.VoidStructureGridChunkGenerator;
import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
import de.waterdu.atlantis.util.bootstrap.Entrypoint;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.java.collection.Pair;
import de.waterdu.atlantis.util.level.ChunkGeneratorFunction;
import de.waterdu.atlantis.util.level.LevelElementFunction;
import de.waterdu.atlantis.util.server.ServerUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

import org.jspecify.annotations.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Registers elements that an AtlantisLevel uses.
 */
@Bootstrapped
public final class AtlantisLevelRegistry {

    private static final ChunkGeneratorFunction NULL_CHUNK_GENERATOR_FUNCTION = (level, biomes, args) -> null;
    private static final LevelElementFunction<DimensionType> NULL_DIMENSION_TYPE_FUNCTION = (level, args) -> null;

    private static final DimensionType DEFAULT_DIMENSION_TYPE_IMPL = new DimensionType(OptionalLong.empty(), true, false, false, true, 1.0, false, false, 0, 256, 256, BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 16, new DimensionType.MonsterSettings(false, false, ConstantInt.ZERO, 0));

    private static final Map<ResourceLocation, ChunkGeneratorFunction> CHUNK_GENERATORS = new HashMap<>();
    private static final Map<ResourceLocation, LevelElementFunction<DimensionType>> DIMENSION_TYPES = new HashMap<>();

    /**
     * Five layers of one block in the void biome, without structures.
     * The optional argument is the block id. The default is stone.
     */
    public static final ResourceLocation SUPERFLAT_CHUNK_GENERATOR;
    /**
     * Void that places one structure in a grid.
     * The arguments are the distance between structures in chunks, then the structure id.
     */
    public static final ResourceLocation VOID_STRUCTURE_GRID_CHUNK_GENERATOR;
    /**
     * Uses the vanilla overworld dimension type. It takes no arguments.
     */
    public static final ResourceLocation OVERWORLD_DIMENSION_TYPE;
    /**
     * Registers a new dimension type with the id of the level. It uses overworld effects and builds from Y 0 to Y 255.
     * It takes no arguments.
     */
    public static final ResourceLocation UNIQUE_OVERWORLD_DIMENSION_TYPE;

    static {
        SUPERFLAT_CHUNK_GENERATOR = AtlantisLevelRegistry.registerChunkGenerator(Atlantis.ID, "superflat", (level, biomes, args) -> {
            Block block;
            try {
                block = Registries.BLOCK.registryFromRegistryKey().get(ResourceLocation.parse(args[0]));
            } catch (Exception e) {
                block = Blocks.STONE;
            }
            if (block == null) {
                block = Blocks.STONE;
            }
            var biome = biomes.getter().getOrThrow(Biomes.THE_VOID);
            return new AtlantisChunkGenerator(new FlatLevelGeneratorSettings(Optional.empty(), biome, List.of()).withBiomeAndLayers(Lists.newArrayList(new FlatLayerInfo(5, block)), Optional.empty(), biome));
        });
        VOID_STRUCTURE_GRID_CHUNK_GENERATOR = AtlantisLevelRegistry.registerChunkGenerator(Atlantis.ID, "structure_grid", (level, biomes,args) -> new VoidStructureGridChunkGenerator(Integer.parseInt(args[0]), args[1], biomes));
        OVERWORLD_DIMENSION_TYPE = AtlantisLevelRegistry.registerDimensionType("minecraft", "overworld", (level, args) -> Pair.of(BuiltinDimensionTypes.OVERWORLD, ServerUtils.registry(Registries.DIMENSION_TYPE).getOrThrow(BuiltinDimensionTypes.OVERWORLD)));
        UNIQUE_OVERWORLD_DIMENSION_TYPE = AtlantisLevelRegistry.registerDimensionType("atlantis", "unique", (level, args) -> Pair.of(ResourceKey.create(Registries.DIMENSION_TYPE, level.levelKey().location()), DEFAULT_DIMENSION_TYPE_IMPL));
    }

    private AtlantisLevelRegistry() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Creates a key from the namespace and the path, both in lower case.
     *
     * @param namespace the namespace of the key
     * @param path the path of the key
     * @return the new key
     */
    private static ResourceLocation makeKey(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace.toLowerCase(Locale.ROOT), path.toLowerCase(Locale.ROOT));
    }

    /**
     * Registers a new {@link ChunkGenerator}.
     * Do this during mod initialization.
     *
     * @param namespace the namespace of the {@link ResourceLocation} key of this {@link ChunkGenerator}
     * @param path the path of the {@link ResourceLocation} key of this {@link ChunkGenerator}
     * @param chunkGenerator function that converts {@link String} arguments to the {@link ChunkGenerator}
     * @return the {@link ResourceLocation} key of this {@link ChunkGenerator}
     */
    public static ResourceLocation registerChunkGenerator(String namespace, String path, ChunkGeneratorFunction chunkGenerator) {
        var key = AtlantisLevelRegistry.makeKey(namespace, path);
        AtlantisLevelRegistry.CHUNK_GENERATORS.put(key, chunkGenerator);
        return key;
    }

    /**
     * Gets a registered {@link ChunkGenerator} if present, or null if not.
     * You can supply arguments if the {@link ChunkGenerator} supports them.
     *
     * @param key the {@link ResourceLocation} key of a {@link ChunkGenerator}
     * @param level the {@link AtlantisLevel} to get it for
     * @param args the {@link String} arguments to pass to the {@link ChunkGenerator}
     * @return a new {@link ChunkGenerator}, if present
     */
    public static @Nullable ChunkGenerator getChunkGenerator(ResourceLocation key, AtlantisLevel level, RegistryOps.RegistryInfo<Biome> biomes, String... args) throws InvalidAtlantisLevelException {
        try {
            return AtlantisLevelRegistry.CHUNK_GENERATORS.getOrDefault(key, AtlantisLevelRegistry.NULL_CHUNK_GENERATOR_FUNCTION).apply(level, biomes, args);
        } catch (Exception e) {
            throw new InvalidAtlantisLevelException("ChunkGenerator", key, e);
        }
    }

    /**
     * Registers a new {@link DimensionType}.
     * Do this during mod initialization.
     *
     * @param namespace the namespace of the {@link ResourceLocation} key of this {@link DimensionType}
     * @param path the path of the {@link ResourceLocation} key of this {@link DimensionType}
     * @param dimensionType function that converts {@link String} arguments to the {@link DimensionType}
     * @return the {@link ResourceLocation} key of this {@link DimensionType}
     */
    public static ResourceLocation registerDimensionType(String namespace, String path, LevelElementFunction<DimensionType> dimensionType) {
        var key = AtlantisLevelRegistry.makeKey(namespace, path);
        AtlantisLevelRegistry.DIMENSION_TYPES.put(key, dimensionType);
        return key;
    }

    /**
     * Registers every level in {@link AtlantisLevelStorage} for registry injection.
     */
    @Entrypoint
    @SuppressWarnings("unused")
    private static void bootstrap() {
        AtlantisLevelStorage.instance().levels().values().forEach(AtlantisLevel::register);
    }

    /**
     * Gets a registered {@link DimensionType} if present, or null if not.
     * You can supply arguments if the {@link DimensionType} supports them.
     *
     * @param key the {@link ResourceLocation} key of a {@link DimensionType}
     * @param level the {@link AtlantisLevel} to get it for
     * @param args the {@link String} arguments to pass to the {@link DimensionType}
     * @return a new {@link DimensionType}, if present
     */
    public static @Nullable Pair<ResourceKey<DimensionType>, DimensionType> getDimensionType(ResourceLocation key, AtlantisLevel level, String... args) throws InvalidAtlantisLevelException {
        try {
            return AtlantisLevelRegistry.DIMENSION_TYPES.getOrDefault(key, AtlantisLevelRegistry.NULL_DIMENSION_TYPE_FUNCTION).apply(level, args);
        } catch (Exception e) {
            throw new InvalidAtlantisLevelException("DimensionType", key, e);
        }
    }
}
