package de.waterdu.atlantis.level.chunk;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.datatypes.Configuration;

import java.util.HashSet;
import java.util.Set;

@FileBackedObject(mod = Atlantis.ID, path = "chunks.json")
public final class AtlantisChunkStorage implements Configuration {

    private Set<AtlantisChunk> permanentChunks = new HashSet<>();
    private Set<AtlantisChunk> transientChunks = new HashSet<>();

    public Set<AtlantisChunk> permanentChunks() {
        return this.permanentChunks;
    }

    public void setPermanentChunks(Set<AtlantisChunk> permanentChunks) {
        this.permanentChunks = permanentChunks;
        this.save();
    }

    public Set<AtlantisChunk> transientChunks() {
        return this.transientChunks;
    }

    public void setTransientChunks(Set<AtlantisChunk> transientChunks) {
        this.transientChunks = transientChunks;
        this.save();
    }

    public void addChunk(AtlantisChunk chunk, boolean permanent) {
        if (permanent) {
            this.permanentChunks.add(chunk);
        } else {
            this.transientChunks.add(chunk);
        }
        this.save();
    }

    public static AtlantisChunkStorage getInstance() {
        return Atlantis.CONFIG.get(AtlantisChunkStorage.class);
    }
}
