package de.waterdu.atlantis.level.level;

import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.util.java.collection.Pair;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.DimensionType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MinecraftBootstrapped
class AtlantisLevelRegistryTest {

    private static String unique() {
        return "test_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static AtlantisLevel level() {
        return AtlantisLevel.builder("atlantis_test", "level").build(false);
    }

    @Test
    void builtInKeys() {
        assertThat(AtlantisLevelRegistry.SUPERFLAT_CHUNK_GENERATOR).isEqualTo(ResourceLocation.fromNamespaceAndPath("atlantis", "superflat"));
        assertThat(AtlantisLevelRegistry.VOID_STRUCTURE_GRID_CHUNK_GENERATOR).isEqualTo(ResourceLocation.fromNamespaceAndPath("atlantis", "structure_grid"));
        assertThat(AtlantisLevelRegistry.OVERWORLD_DIMENSION_TYPE).isEqualTo(ResourceLocation.withDefaultNamespace("overworld"));
        assertThat(AtlantisLevelRegistry.UNIQUE_OVERWORLD_DIMENSION_TYPE).isEqualTo(ResourceLocation.fromNamespaceAndPath("atlantis", "unique"));
    }

    @Test
    void registrationLowercasesTheKey() {
        var path = unique();

        var key = AtlantisLevelRegistry.registerDimensionType("Atlantis_Test", path.toUpperCase(), (level, args) -> null);

        assertThat(key).isEqualTo(ResourceLocation.fromNamespaceAndPath("atlantis_test", path));
    }

    @Test
    void unknownKeysAreEmpty() throws InvalidAtlantisLevelException {
        var unknown = ResourceLocation.fromNamespaceAndPath("atlantis_test", unique());

        assertThat(AtlantisLevelRegistry.getChunkGenerator(unknown, level(), null)).isEmpty();
        assertThat(AtlantisLevelRegistry.getDimensionType(unknown, level())).isEmpty();
    }

    @Test
    void registeredDimensionTypeReceivesLevelAndArgs() throws InvalidAtlantisLevelException {
        var dimensionKey = ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath("atlantis_test", "custom"));
        var seen = new Object[2];
        var key = AtlantisLevelRegistry.registerDimensionType("atlantis_test", unique(), (level, args) -> {
            seen[0] = level;
            seen[1] = Arrays.asList(args);
            return Pair.of(dimensionKey, null);
        });
        var level = level();

        var result = AtlantisLevelRegistry.getDimensionType(key, level, "a", "b");

        assertThat(result).isPresent();
        assertThat(result.get().getA()).isEqualTo(dimensionKey);
        assertThat(seen[0]).isSameAs(level);
        assertThat(seen[1]).isEqualTo(Arrays.asList("a", "b"));
    }

    @Test
    void registeredChunkGeneratorReceivesArgs() throws InvalidAtlantisLevelException {
        var seen = new Object[1];
        var key = AtlantisLevelRegistry.registerChunkGenerator("atlantis_test", unique(), (level, biomes, args) -> {
            seen[0] = Arrays.asList(args);
            return null;
        });

        assertThat(AtlantisLevelRegistry.getChunkGenerator(key, level(), null, "x")).isEmpty();
        assertThat(seen[0]).isEqualTo(Arrays.asList("x"));
    }

    @Test
    void uniqueOverworldIsNamedAfterTheLevel() throws InvalidAtlantisLevelException {
        var result = AtlantisLevelRegistry.getDimensionType(AtlantisLevelRegistry.UNIQUE_OVERWORLD_DIMENSION_TYPE, level()).orElseThrow();

        assertThat(result.getA()).isEqualTo(ResourceKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath("atlantis_test", "level")));
        DimensionType type = result.getB();
        assertThat(type.hasSkyLight()).isTrue();
        assertThat(type.minY()).isZero();
        assertThat(type.height()).isEqualTo(256);
    }

    @Test
    void failuresAreWrapped() {
        var dimensionKey = AtlantisLevelRegistry.registerDimensionType("atlantis_test", unique(), (level, args) -> {
            throw new IllegalStateException("boom");
        });
        var generatorKey = AtlantisLevelRegistry.registerChunkGenerator("atlantis_test", unique(), (level, biomes, args) -> {
            throw new IllegalStateException("bang");
        });

        assertThatThrownBy(() -> AtlantisLevelRegistry.getDimensionType(dimensionKey, level()))
                .isInstanceOf(InvalidAtlantisLevelException.class)
                .hasMessageContaining("DimensionType").hasMessageContaining(dimensionKey.toString()).hasMessageContaining("boom");
        assertThatThrownBy(() -> AtlantisLevelRegistry.getChunkGenerator(generatorKey, level(), null))
                .isInstanceOf(InvalidAtlantisLevelException.class)
                .hasMessageContaining("ChunkGenerator").hasMessageContaining("bang");
    }
}
