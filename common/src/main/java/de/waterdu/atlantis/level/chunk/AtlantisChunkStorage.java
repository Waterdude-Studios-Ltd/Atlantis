package de.waterdu.atlantis.level.chunk;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.datatypes.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * Stores the permanent and transient chunks of Atlantis in {@code chunks.json}.
 */
@FileBackedObject(mod = Atlantis.ID, path = "chunks.json")
public final class AtlantisChunkStorage implements Configuration {

    private Set<AtlantisChunk> permanentChunks = new HashSet<>();
    private Set<AtlantisChunk> transientChunks = new HashSet<>();

    /**
     * @return the permanent chunks
     */
    public Set<AtlantisChunk> permanentChunks() {
        return this.permanentChunks;
    }

    /**
     * Sets the permanent chunks and saves the file.
     *
     * @param permanentChunks the new permanent chunks
     */
    public void setPermanentChunks(Set<AtlantisChunk> permanentChunks) {
        this.permanentChunks = permanentChunks;
        this.save();
    }

    /**
     * @return the transient chunks
     */
    public Set<AtlantisChunk> transientChunks() {
        return this.transientChunks;
    }

    /**
     * Sets the transient chunks and saves the file.
     *
     * @param transientChunks the new transient chunks
     */
    public void setTransientChunks(Set<AtlantisChunk> transientChunks) {
        this.transientChunks = transientChunks;
        this.save();
    }

    /**
     * Adds a chunk to the permanent or transient chunks and saves the file.
     *
     * @param chunk the chunk to add
     * @param permanent {@code true} to add the chunk to the permanent chunks, {@code false} for the transient chunks
     */
    public void addChunk(AtlantisChunk chunk, boolean permanent) {
        if (permanent) {
            this.permanentChunks.add(chunk);
        } else {
            this.transientChunks.add(chunk);
        }
        this.save();
    }

    /**
     * @return the loaded instance from {@link Atlantis#CONFIG}
     */
    public static AtlantisChunkStorage getInstance() {
        return Atlantis.CONFIG.get(AtlantisChunkStorage.class);
    }
}
