package de.waterdu.atlantis.level.chunk;

import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.util.function.Consumer;

public final class AtlantisChunkLoader {

    private AtlantisChunkLoader() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void setup(Consumer<Runnable> enqueue) {
        enqueue.accept(() -> {
            var chunks = AtlantisChunkStorage.getInstance();
            for (var chunk : chunks.transientChunks()) {
                chunk.unload();
            }
            chunks.transientChunks().clear();
            for (var chunk : chunks.permanentChunks()) {
                chunk.load();
            }
            chunks.save();
        });
    }

    public static AtlantisChunk load(AtlantisChunk chunk) {
        return AtlantisChunkLoader.load(chunk, false);
    }

    public static AtlantisChunk load(AtlantisChunk chunk, boolean permanent) {
        chunk.load();
        AtlantisChunkStorage.getInstance().addChunk(chunk, permanent);
        return chunk;
    }

    public static void unload(AtlantisChunk chunk) {
        chunk.unload();
    }
}
