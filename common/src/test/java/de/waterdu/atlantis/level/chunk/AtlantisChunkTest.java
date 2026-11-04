package de.waterdu.atlantis.level.chunk;

import de.waterdu.atlantis.test.MinecraftBootstrapped;
import de.waterdu.atlantis.util.java.UUIDUtils;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link AtlantisChunk}.
 */
@MinecraftBootstrapped
class AtlantisChunkTest {

    private static final ConfigurableLevel OVERWORLD = ConfigurableLevel.of("minecraft:overworld");
    private static final ConfigurableLevel NETHER = ConfigurableLevel.of("minecraft:the_nether");

    /**
     * Checks that the default chunk belongs to Atlantis and is at chunk 0, 0 in the overworld.
     */
    @Test
    void defaultsToAtlantisInTheOverworldOrigin() {
        var chunk = new AtlantisChunk();

        assertThat(chunk.modId()).isEqualTo("atlantis");
        assertThat(chunk.owner()).isEqualTo(UUIDUtils.fromHashCode("atlantis"));
        assertThat(chunk.level()).isEqualTo(OVERWORLD);
        assertThat(chunk.pos()).isEqualTo(new ChunkPos(0, 0));
    }

    /**
     * Checks that the owner UUID comes from the hash code of the mod ID or of the owner object, and is the same
     * each time.
     */
    @Test
    void ownerIsDerivedDeterministically() {
        var byModId = new AtlantisChunk("mod", OVERWORLD, 1, 2);
        var byObject = new AtlantisChunk("mod", (Object) "owner", OVERWORLD, 1, 2);

        assertThat(byModId.owner()).isEqualTo(UUIDUtils.fromHashCode("mod"));
        assertThat(byObject.owner()).isEqualTo(UUIDUtils.fromHashCode("owner"));
        assertThat(new AtlantisChunk("mod", (Object) "owner", OVERWORLD, 1, 2).owner()).isEqualTo(byObject.owner());
    }

    /**
     * Checks that the chunk gives its coordinates, position and level.
     */
    @Test
    void exposesItsPosition() {
        var chunk = new AtlantisChunk("mod", UUID.randomUUID(), NETHER, -3, 7);

        assertThat(chunk.x()).isEqualTo(-3);
        assertThat(chunk.z()).isEqualTo(7);
        assertThat(chunk.pos()).isEqualTo(new ChunkPos(-3, 7));
        assertThat(chunk.level()).isEqualTo(NETHER);
    }

    /**
     * Checks that equality and the hash code use the mod ID, owner, level and coordinates.
     */
    @Test
    void equalityCoversEveryField() {
        var owner = UUID.randomUUID();
        var chunk = new AtlantisChunk("mod", owner, OVERWORLD, 1, 2);

        assertThat(chunk)
                .isEqualTo(new AtlantisChunk("mod", owner, ConfigurableLevel.of("minecraft:overworld"), 1, 2))
                .hasSameHashCodeAs(new AtlantisChunk("mod", owner, ConfigurableLevel.of("minecraft:overworld"), 1, 2))
                .isNotEqualTo(new AtlantisChunk("other", owner, OVERWORLD, 1, 2))
                .isNotEqualTo(new AtlantisChunk("mod", UUID.randomUUID(), OVERWORLD, 1, 2))
                .isNotEqualTo(new AtlantisChunk("mod", owner, NETHER, 1, 2))
                .isNotEqualTo(new AtlantisChunk("mod", owner, OVERWORLD, 2, 2))
                .isNotEqualTo(new AtlantisChunk("mod", owner, OVERWORLD, 1, 3))
                .isNotEqualTo("chunk");
    }

    /**
     * Checks that an equal chunk is found in and removed from a {@link HashSet}.
     */
    @Test
    void worksInHashSets() {
        var owner = UUID.randomUUID();
        var set = new HashSet<AtlantisChunk>();
        set.add(new AtlantisChunk("mod", owner, OVERWORLD, 1, 2));

        assertThat(set.contains(new AtlantisChunk("mod", owner, OVERWORLD, 1, 2))).isTrue();
        assertThat(set.remove(new AtlantisChunk("mod", owner, OVERWORLD, 1, 2))).isTrue();
    }
}
