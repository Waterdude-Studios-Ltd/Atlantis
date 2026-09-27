package de.waterdu.atlantis.level.chunk;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.util.java.UUIDUtils;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.Objects;
import java.util.UUID;

public final class AtlantisChunk {

    private String modid;
    private UUID owner;
    private ConfigurableLevel level;
    private int x;
    private int z;

    public AtlantisChunk() {
        this(Atlantis.ID, UUIDUtils.fromHashCode(Atlantis.ID), ConfigurableLevel.of("minecraft:overworld"), 0, 0);
    }

    public AtlantisChunk(String modid, ConfigurableLevel level, int x, int z) {
        this(modid, UUIDUtils.fromHashCode(modid), level, x, z);
    }

    public AtlantisChunk(String modid, Object owner, ConfigurableLevel level, int x, int z) {
        this(modid, UUIDUtils.fromHashCode(owner), level, x, z);
    }

    public AtlantisChunk(String modid, UUID owner, ConfigurableLevel level, int x, int z) {
        this.modid = modid;
        this.owner = owner;
        this.level = level;
        this.x = x;
        this.z = z;
    }

    public boolean load() {
        return this.level.get().setChunkForced(this.x, this.z, true);
    }

    public boolean unload() {
        boolean result = this.level.get().setChunkForced(this.x, this.z, false);
        AtlantisChunkStorage chunks = AtlantisChunkStorage.getInstance();
        boolean changed = chunks.transientChunks().remove(this);
        changed |= chunks.permanentChunks().remove(this);
        if (changed) {
            chunks.save();
        }
        return result;
    }

    public void update(Runnable task) {
        this.unload();
        task.run();
        this.load();
    }

    public String modID() {
        return this.modid;
    }

    public void setModID(String modid) {
        this.update(() -> this.modid = modid);
    }

    public UUID owner() {
        return this.owner;
    }

    public void setOwner(UUID owner) {
        this.update(() -> this.owner = owner);
    }

    public ConfigurableLevel level() {
        return this.level;
    }

    public void setLevel(ConfigurableLevel level) {
        this.update(() -> this.level = level);
    }

    public int x() {
        return this.x;
    }

    public void setX(int x) {
        this.update(() -> this.x = x);
    }

    public int z() {
        return this.z;
    }

    public void setZ(int z) {
        this.update(() -> this.z = z);
    }

    public ChunkPos pos() {
        return new ChunkPos(this.x, this.z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.modid, this.owner, this.level, this.x, this.z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AtlantisChunk other)) {
            return false;
        }
        return this.modid.equals(other.modid) && this.owner.equals(other.owner) &&  this.level.equals(other.level) && this.x == other.x && this.z == other.z;
    }

    @Override
    public String toString() {
        return "AtlantisChunk{" + this.modid + "," + this.owner + "," + this.level.key().location() + "@(" + this.x + "," + this.z + ")}";
    }
}
