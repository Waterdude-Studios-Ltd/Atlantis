package de.waterdu.atlantis.gametest.level;

import de.waterdu.atlantis.gametest.AtlantisGameTest;
import de.waterdu.atlantis.level.chunk.AtlantisChunk;
import de.waterdu.atlantis.level.chunk.AtlantisChunkLoader;
import de.waterdu.atlantis.level.chunk.AtlantisChunkStorage;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;

/**
 * Chunk forcing through AtlantisChunkLoader. Each test uses its own chunks, far from the test structures.
 */
public final class ChunkLoaderGameTest {

    private static final String BATCH = "atlantis_chunks";
    private static final String SETUP_BATCH = "atlantis_chunks_setup";
    private static final int BASE = 2000;

    private ChunkLoaderGameTest() {}

    @AtlantisGameTest(batch = BATCH)
    public static void loadForcesATransientChunk(GameTestHelper helper) {
        var chunk = chunk(helper, 0, 0);

        AtlantisChunkLoader.load(chunk);

        try {
            assertForced(helper, chunk.pos(), true);
            helper.assertTrue(storage().transientChunks().contains(chunk), "chunk not stored as transient");
            helper.assertFalse(storage().permanentChunks().contains(chunk), "chunk stored as permanent");
        } finally {
            AtlantisChunkLoader.unload(chunk);
        }
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void loadForcesAPermanentChunk(GameTestHelper helper) {
        var chunk = chunk(helper, 1, 0);

        AtlantisChunkLoader.load(chunk, true);

        try {
            assertForced(helper, chunk.pos(), true);
            helper.assertTrue(storage().permanentChunks().contains(chunk), "chunk not stored as permanent");
            helper.assertFalse(storage().transientChunks().contains(chunk), "chunk stored as transient");
        } finally {
            AtlantisChunkLoader.unload(chunk);
        }
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void unloadReleasesAndForgetsTheChunk(GameTestHelper helper) {
        var chunk = chunk(helper, 2, 0);
        AtlantisChunkLoader.load(chunk, true);

        AtlantisChunkLoader.unload(chunk);

        assertForced(helper, chunk.pos(), false);
        helper.assertFalse(storage().permanentChunks().contains(chunk), "chunk still stored");
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void settersMoveTheForcedChunkAndKeepItStored(GameTestHelper helper) {
        var chunk = chunk(helper, 3, 0);
        var before = chunk.pos();
        AtlantisChunkLoader.load(chunk, true);

        chunk.setX(chunk.x() + 1);
        chunk.setZ(chunk.z() + 1);

        try {
            assertForced(helper, before, false);
            assertForced(helper, chunk.pos(), true);
            helper.assertTrue(storage().permanentChunks().contains(chunk), "chunk is no longer stored as permanent");
            helper.assertFalse(storage().transientChunks().contains(chunk), "chunk became transient");
        } finally {
            AtlantisChunkLoader.unload(chunk);
        }
        helper.succeed();
    }

    @AtlantisGameTest(batch = BATCH)
    public static void settersOfAnUnstoredChunkDoNotStoreIt(GameTestHelper helper) {
        var chunk = chunk(helper, 5, 0);

        chunk.setX(chunk.x() + 1);

        try {
            helper.assertFalse(storage().permanentChunks().contains(chunk), "stored as permanent");
            helper.assertFalse(storage().transientChunks().contains(chunk), "stored as transient");
        } finally {
            AtlantisChunkLoader.unload(chunk);
        }
        helper.succeed();
    }

    @AtlantisGameTest(batch = SETUP_BATCH)
    public static void setupDropsTransientChunksAndKeepsPermanentOnes(GameTestHelper helper) {
        var first = chunk(helper, 0, 1);
        var second = chunk(helper, 1, 1);
        var permanent = chunk(helper, 2, 1);
        AtlantisChunkLoader.load(first);
        AtlantisChunkLoader.load(second);
        AtlantisChunkLoader.load(permanent, true);

        AtlantisChunkLoader.setup(Runnable::run);

        try {
            assertForced(helper, first.pos(), false);
            assertForced(helper, second.pos(), false);
            assertForced(helper, permanent.pos(), true);
            helper.assertTrue(storage().transientChunks().isEmpty(), "transient chunks were kept");
            helper.assertTrue(storage().permanentChunks().contains(permanent), "permanent chunk was dropped");
        } finally {
            AtlantisChunkLoader.unload(permanent);
        }
        helper.succeed();
    }

    private static AtlantisChunk chunk(GameTestHelper helper, int x, int z) {
        return new AtlantisChunk("atlantis_test", ConfigurableLevel.of(helper.getLevel()), BASE + x, BASE + z);
    }

    private static AtlantisChunkStorage storage() {
        return AtlantisChunkStorage.getInstance();
    }

    private static void assertForced(GameTestHelper helper, ChunkPos pos, boolean forced) {
        helper.assertValueEqual(helper.getLevel().getForcedChunks().contains(pos.toLong()), forced, "forced state of " + pos);
    }
}
