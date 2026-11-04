package de.waterdu.atlantis.level.chunk;

import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.util.java.UUIDUtils;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies a chunk that a mod force-loads, with its owner, level and chunk coordinates.
 */
public final class AtlantisChunk {

    private String modId;
    private UUID owner;
    private ConfigurableLevel level;
    private int x;
    private int z;

    /**
     * Creates the chunk at {@code 0, 0} in the overworld, owned by Atlantis.
     */
    public AtlantisChunk() {
        this(Atlantis.ID, UUIDUtils.fromHashCode(Atlantis.ID), ConfigurableLevel.of("minecraft:overworld"), 0, 0);
    }

    /**
     * The owner UUID comes from the hash code of {@code modId}.
     */
    public AtlantisChunk(String modId, ConfigurableLevel level, int x, int z) {
        this(modId, UUIDUtils.fromHashCode(modId), level, x, z);
    }

    /**
     * @param owner any object. The owner UUID comes from its hash code.
     */
    public AtlantisChunk(String modId, Object owner, ConfigurableLevel level, int x, int z) {
        this(modId, UUIDUtils.fromHashCode(owner), level, x, z);
    }

    /**
     * Creates the chunk.
     *
     * @param modId the ID of the mod that loads the chunk
     * @param owner the UUID of the owner
     * @param level the level of the chunk
     * @param x the chunk X coordinate
     * @param z the chunk Z coordinate
     */
    public AtlantisChunk(String modId, UUID owner, ConfigurableLevel level, int x, int z) {
        this.modId = modId;
        this.owner = owner;
        this.level = level;
        this.x = x;
        this.z = z;
    }

    /**
     * Force-loads the chunk. Does not add it to {@link AtlantisChunkStorage}, so it does not stay loaded after a restart.
     * Use {@link AtlantisChunkLoader#load(AtlantisChunk, boolean)} to store it.
     *
     * @return {@code true} if the chunk was not force-loaded before
     */
    public boolean load() {
        return this.level.get().setChunkForced(this.x, this.z, true);
    }

    /**
     * Stops force-loading the chunk and removes it from {@link AtlantisChunkStorage}.
     *
     * @return {@code true} if the chunk was force-loaded before
     */
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

    /**
     * Unloads the chunk, runs {@code task}, then loads the chunk again and stores it again if it was stored.
     * The setters use this, so the chunk is force-loaded after a setter even if it was not before.
     */
    public void update(Runnable task) {
        var chunks = AtlantisChunkStorage.getInstance();
        var permanent = chunks.permanentChunks().contains(this);
        var stored = permanent || chunks.transientChunks().contains(this);
        this.unload();
        task.run();
        this.load();
        if (stored) {
            chunks.addChunk(this, permanent);
        }
    }

    /**
     * @return the ID of the mod that loads the chunk
     */
    public String modId() {
        return this.modId;
    }

    /**
     * Sets the mod ID with {@link #update}.
     *
     * @param modId the ID of the mod that loads the chunk
     */
    public void setModId(String modId) {
        this.update(() -> this.modId = modId);
    }

    /**
     * @return the UUID of the owner
     */
    public UUID owner() {
        return this.owner;
    }

    /**
     * Sets the owner with {@link #update}.
     *
     * @param owner the UUID of the owner
     */
    public void setOwner(UUID owner) {
        this.update(() -> this.owner = owner);
    }

    /**
     * @return the level of the chunk
     */
    public ConfigurableLevel level() {
        return this.level;
    }

    /**
     * Moves the chunk to another level with {@link #update}.
     *
     * @param level the level of the chunk
     */
    public void setLevel(ConfigurableLevel level) {
        this.update(() -> this.level = level);
    }

    /**
     * @return the chunk X coordinate
     */
    public int x() {
        return this.x;
    }

    /**
     * Sets the chunk X coordinate with {@link #update}.
     *
     * @param x the chunk X coordinate
     */
    public void setX(int x) {
        this.update(() -> this.x = x);
    }

    /**
     * @return the chunk Z coordinate
     */
    public int z() {
        return this.z;
    }

    /**
     * Sets the chunk Z coordinate with {@link #update}.
     *
     * @param z the chunk Z coordinate
     */
    public void setZ(int z) {
        this.update(() -> this.z = z);
    }

    /**
     * @return a new {@link ChunkPos} with the coordinates of the chunk
     */
    public ChunkPos pos() {
        return new ChunkPos(this.x, this.z);
    }

    /**
     * Gets the hash code of the mod ID, owner, level and coordinates.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.modId, this.owner, this.level, this.x, this.z);
    }

    /**
     * Checks if the object is a chunk with the same mod ID, owner, level and coordinates.
     *
     * @param obj the object to compare
     * @return {@code true} if all values are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AtlantisChunk other)) {
            return false;
        }
        return this.modId.equals(other.modId) && this.owner.equals(other.owner) &&  this.level.equals(other.level) && this.x == other.x && this.z == other.z;
    }

    /**
     * @return the mod ID, owner, level ID and coordinates, in the form
     *     {@code AtlantisChunk{modId,owner,level@(x,z)}}
     */
    @Override
    public String toString() {
        return "AtlantisChunk{" + this.modId + "," + this.owner + "," + this.level.key().location() + "@(" + this.x + "," + this.z + ")}";
    }
}
