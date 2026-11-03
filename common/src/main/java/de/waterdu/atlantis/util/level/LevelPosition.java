package de.waterdu.atlantis.util.level;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.util.entity.PlayerReference;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds a {@link LocalPosition} together with the level that it is in.
 */
public class LevelPosition {

    private final ConfigurableLevel level;
    private final LocalPosition position;

    /**
     * Creates a position in a level.
     *
     * @param level the level
     * @param position the position and rotation in the level
     */
    private LevelPosition(ConfigurableLevel level, LocalPosition position) {
        this.level = level;
        this.position = position;
    }

    /**
     * Creates a position from the level, position and rotation of the entity.
     *
     * @param entity the entity
     */
    private LevelPosition(Entity entity) {
        this(ConfigurableLevel.of(entity.level()), new LocalPosition(entity));
    }

    /**
     * Creates a position in a level.
     *
     * @param level the level
     * @param position the position and rotation in the level
     * @return the position
     */
    public static LevelPosition of(ConfigurableLevel level, LocalPosition position) {
        return new LevelPosition(level, position);
    }

    /**
     * Creates a position from the current level, position and rotation of the entity.
     *
     * @param entity the entity
     * @return the position
     */
    public static LevelPosition of(Entity entity) {
        return new LevelPosition(entity);
    }

    /**
     * Creates a position with no rotation.
     *
     * @param level the level id
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the position
     */
    public static LevelPosition of(String level, double x, double y, double z) {
        return LevelPosition.of(level, x, y, z, 0F, 0F);
    }

    /**
     * Creates a position with a rotation.
     *
     * @param level the level id
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param xRot the x rotation (pitch)
     * @param yRot the y rotation (yaw)
     * @return the position
     */
    public static LevelPosition of(String level, double x, double y, double z, float xRot, float yRot) {
        return LevelPosition.of(ConfigurableLevel.of(level), new LocalPosition(x, y, z, xRot, yRot));
    }

    /**
     * @return null when the level does not exist
     */
    public ServerLevel level() {
        return this.level.get();
    }

    /**
     * @return the key of the level
     */
    public ResourceKey<Level> levelKey() {
        return this.level.key();
    }

    /**
     * @return the position and rotation in the level
     */
    public LocalPosition position() {
        return this.position;
    }

    /**
     * @return the x coordinate
     */
    public double x() {
        return this.position.x();
    }

    /**
     * @return the y coordinate
     */
    public double y() {
        return this.position.y();
    }

    /**
     * @return the z coordinate
     */
    public double z() {
        return this.position.z();
    }

    /**
     * @return the x rotation (pitch)
     */
    public float xRot() {
        return this.position.xRot();
    }

    /**
     * @return the y rotation (yaw)
     */
    public float yRot() {
        return this.position.yRot();
    }

    /**
     * Creates the entity at this position, but does not add it to the level.
     */
    public <E extends Entity> @Nullable E createEntity(EntityType<E> type) {
        E entity = type.create(this.level());
        if (entity == null) {
            return null;
        }
        entity.absMoveTo(this.x(), this.y(), this.z(), this.yRot(), this.xRot());
        return entity;
    }

    /**
     * Plays the sound at this position in the {@link SoundSource#MASTER} category, at full volume and normal pitch.
     *
     * @param sound the sound to play
     */
    public void sound(SoundEvent sound) {
        this.sound(sound, SoundSource.MASTER);
    }

    /**
     * Plays the sound at this position in the {@link SoundSource#MASTER} category.
     *
     * @param sound the sound to play
     * @param volume the volume
     * @param pitch the pitch
     */
    public void sound(SoundEvent sound, float volume, float pitch) {
        this.sound(sound, SoundSource.MASTER, volume, pitch);
    }

    /**
     * Plays the sound at this position at full volume and normal pitch.
     *
     * @param sound the sound to play
     * @param category the sound category
     */
    public void sound(SoundEvent sound, SoundSource category) {
        this.sound(sound, category, 1F, 1F);
    }

    /**
     * All players near the position hear the sound.
     */
    public void sound(SoundEvent sound, SoundSource category, float volume, float pitch) {
        this.level().playSound(null, this.position().x(), this.position().y(), this.position().z(), sound, category, volume, pitch);
    }

    /**
     * Does nothing when the player is offline.
     */
    public void teleport(PlayerReference reference) {
        if (reference == null) {
            return;
        }
        this.teleport(reference.entity());
    }

    /**
     * Teleports the entity to this position if it is present.
     *
     * @param entity the entity to teleport
     */
    public void teleport(Optional<? extends Entity> entity) {
        entity.ifPresent(this::teleport);
    }

    /**
     * Teleports the entity to this position and rotation in the level. Does nothing when the entity is null or the
     * level is not loaded.
     *
     * @param entity the entity to teleport
     */
    public void teleport(Entity entity) {
        this.position.teleport(entity, this.level);
    }

    /**
     * Gets the hash code of the level and the position.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.level, this.position);
    }

    /**
     * Checks if the object is a {@link LevelPosition} with the same level, position and rotation.
     *
     * @param obj the object to compare with
     * @return {@code true} if the positions are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LevelPosition other)) {
            return false;
        }
        return this.level.equals(other.level) && this.position.equals(other.position);
    }

    /**
     * Converts a {@link LevelPosition} to and from a JSON object with a {@code level} and a {@code position} field.
     */
    public static class TypeAdapter implements JsonSerializer<LevelPosition>, JsonDeserializer<LevelPosition> {

        /**
         * Creates a position from a JSON object with a {@code level} and a {@code position} field.
         *
         * @param json the JSON object
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context, which reads the level and the position
         * @return the position
         */
        @Override
        public LevelPosition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            return LevelPosition.of(context.deserialize(object.get("level"), ConfigurableLevel.class), context.deserialize(object.get("position"), LocalPosition.class));
        }

        /**
         * Writes the position as a JSON object with a {@code level} and a {@code position} field.
         *
         * @param src the position
         * @param typeOfSrc the type of the position
         * @param context the serialization context, which writes the level and the position
         * @return the JSON object
         */
        @Override
        public JsonElement serialize(LevelPosition src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            object.add("level", context.serialize(src.level));
            object.add("position", context.serialize(src.position));
            return object;
        }
    }
}
