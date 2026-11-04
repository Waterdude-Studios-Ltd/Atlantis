package de.waterdu.atlantis.level.level;

import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link AtlantisLevel}.
 */
@MinecraftBootstrapped
class AtlantisLevelTest {

    /**
     * Checks that a level without a namespace uses the {@code atlantis} namespace, and that the default path is
     * {@code example}.
     */
    @Test
    void defaultsToAnAtlantisLevel() {
        assertThat(new AtlantisLevel().id()).isEqualTo("atlantis:example");
        assertThat(new AtlantisLevel("arena").id()).isEqualTo("atlantis:arena");
    }

    /**
     * Checks the level key, the configurable level, {@code toString}, equality and the hash code of a level.
     */
    @Test
    void identity() {
        var level = new AtlantisLevel("atlantis_test", "arena");

        assertThat(level.levelKey()).isEqualTo(ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("atlantis_test", "arena")));
        assertThat(level.asConfigurableLevel()).isEqualTo(ConfigurableLevel.of("atlantis_test:arena"));
        assertThat(level.toString()).isEqualTo("AtlantisLevel[atlantis_test:arena]");
        assertThat(level).isEqualTo(new AtlantisLevel("atlantis_test", "arena")).hasSameHashCodeAs(new AtlantisLevel("atlantis_test", "arena"));
        assertThat(level).isNotEqualTo(new AtlantisLevel("atlantis_test", "other"));
    }

    /**
     * Checks that {@link AtlantisLevel#setLevel} changes the ID of the level.
     */
    @Test
    void setLevelChangesIdentity() {
        var level = new AtlantisLevel("atlantis_test", "arena");

        level.setLevel(ConfigurableLevel.of("atlantis_test:moved"));

        assertThat(level.id()).isEqualTo("atlantis_test:moved");
    }

    /**
     * Checks that a level matches equal levels, configurable levels and level keys, but not other levels or strings.
     */
    @Test
    void matches() {
        var level = new AtlantisLevel("atlantis_test", "arena");

        assertThat(level.matches(level)).isTrue();
        assertThat(level.matches(new AtlantisLevel("atlantis_test", "arena"))).isTrue();
        assertThat(level.matches(ConfigurableLevel.of("atlantis_test:arena"))).isTrue();
        assertThat(level.matches(ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("atlantis_test", "arena")))).isTrue();
        assertThat(level.matches(Level.OVERWORLD)).isFalse();
        assertThat(level.matches("atlantis_test:arena")).isFalse();
    }

    /**
     * Checks that a builder that does not instantiate the level sets the chunk generator and dimension type, but
     * creates no level stem.
     */
    @Test
    void builderWithoutInstantiatingSetsGeneratorAndDimensionType() throws InvalidAtlantisLevelException {
        var level = AtlantisLevel.builder("atlantis_test", "built")
                .chunkGenerator(AtlantisLevelRegistry.SUPERFLAT_CHUNK_GENERATOR, "minecraft:grass_block")
                .dimensionType("atlantis:unique")
                .build(false);

        assertThat(level.id()).isEqualTo("atlantis_test:built");
        var dimensionType = level.makeDimensionType();
        assertThat(dimensionType).isNotNull();
        assertThat(dimensionType.getA().location()).isEqualTo(ResourceLocation.fromNamespaceAndPath("atlantis_test", "built"));
        assertThat(level.levelStem()).isNull();
        assertThat(level.dimensionType()).isNull();
    }

    /**
     * Checks that a level built without a dimension type or chunk generator makes neither.
     */
    @Test
    void levelWithoutDimensionTypeHasNone() throws InvalidAtlantisLevelException {
        var level = AtlantisLevel.builder("atlantis_test", "incomplete").build(false);

        assertThat(level.makeDimensionType()).isNull();
        assertThat(level.makeChunkGenerator(null)).isNull();
    }
}
