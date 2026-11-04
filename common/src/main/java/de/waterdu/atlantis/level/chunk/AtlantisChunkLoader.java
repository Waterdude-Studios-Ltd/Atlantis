package de.waterdu.atlantis.level.chunk;

import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * Force-loads chunks and stores them in {@link AtlantisChunkStorage}.
 */
public final class AtlantisChunkLoader {

    private AtlantisChunkLoader() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Unloads the stored transient chunks and loads the stored permanent chunks. Atlantis calls this when the server starts.
     */
    public static void setup(Consumer<Runnable> enqueue) {
        enqueue.accept(() -> {
            var chunks = AtlantisChunkStorage.getInstance();
            for (var chunk : List.copyOf(chunks.transientChunks())) {
                chunk.unload();
            }
            chunks.transientChunks().clear();
            for (var chunk : chunks.permanentChunks()) {
                chunk.load();
            }
            chunks.save();
        });
    }

    /**
     * Force-loads the chunk and stores it as a transient chunk, so it unloads when the server starts again.
     *
     * @param chunk the chunk to load
     * @return the same chunk
     */
    public static AtlantisChunk load(AtlantisChunk chunk) {
        return AtlantisChunkLoader.load(chunk, false);
    }

    /**
     * Force-loads the chunk and adds it to {@link AtlantisChunkStorage}.
     *
     * @param permanent loads the chunk again after a restart. Otherwise, the chunk unloads when the server starts
     *     again.
     */
    public static AtlantisChunk load(AtlantisChunk chunk, boolean permanent) {
        chunk.load();
        AtlantisChunkStorage.getInstance().addChunk(chunk, permanent);
        return chunk;
    }

    /**
     * Stops force-loading the chunk and removes it from {@link AtlantisChunkStorage}.
     *
     * @param chunk the chunk to unload
     */
    public static void unload(AtlantisChunk chunk) {
        chunk.unload();
    }
}
