package de.waterdu.atlantis.util.level;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.util.java.RandomUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import org.joml.Vector3fc;
import org.joml.Vector3ic;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds a position and a rotation without a level. Methods that change a value give a new position.
 */
public class LocalPosition extends Vec3 {

    private static final Set<RelativeMovement> RELATIVE_MOVEMENTS = new HashSet<>();
    public static final LocalPosition ZERO = new LocalPosition();

    private final float[] rotation = new float[]{0F, 0F};

    /**
     * Creates a position at the origin with no rotation.
     */
    public LocalPosition() {
        this(0, 0, 0);
    }

    /**
     * Creates a position with a copy of the rotation.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param rotation the x rotation (pitch) and the y rotation (yaw)
     */
    protected LocalPosition(double x, double y, double z, float[] rotation) {
        super(x, y, z);
        this.rotation[0] = rotation[0];
        this.rotation[1] = rotation[1];
    }

    /**
     * Creates a position with a rotation.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param xRot the x rotation (pitch)
     * @param yRot the y rotation (yaw)
     */
    public LocalPosition(double x, double y, double z, float xRot, float yRot) {
        super(x, y, z);
        this.rotation[0] = xRot;
        this.rotation[1] = yRot;
    }

    /**
     * Creates a position at the corner of the block, with no rotation.
     *
     * @param pos the block position
     */
    public LocalPosition(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
    }

    /**
     * Creates a position at the corner of the block, with a rotation.
     *
     * @param pos the block position
     * @param xRot the x rotation (pitch)
     * @param yRot the y rotation (yaw)
     */
    public LocalPosition(BlockPos pos, float xRot, float yRot) {
        this(pos.getX(), pos.getY(), pos.getZ(), xRot, yRot);
    }

    /**
     * Creates a position with no rotation.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     */
    public LocalPosition(double x, double y, double z) {
        this(x, y, z, 0, 0);
    }

    /**
     * Creates a position from the current position and rotation of the entity.
     *
     * @param entity the entity
     */
    public LocalPosition(Entity entity) {
        this(entity.getX(), entity.getY(), entity.getZ(), entity.getXRot(), entity.getYRot());
    }

    /**
     * @return the x rotation (pitch)
     */
    public float xRot() {
        return this.rotation[0];
    }

    /**
     * @return the y rotation (yaw)
     */
    public float yRot() {
        return this.rotation[1];
    }

    /**
     * @return a copy with the new value. This position does not change.
     */
    public LocalPosition setX(double x) {
        return new LocalPosition(x, this.y, this.z, this.rotation);
    }

    /**
     * @return a copy with the new value. This position does not change.
     */
    public LocalPosition setY(double y) {
        return new LocalPosition(this.x, y, this.z, this.rotation);
    }

    /**
     * @return a copy with the new value. This position does not change.
     */
    public LocalPosition setZ(double z) {
        return new LocalPosition(this.x, this.y, z, this.rotation);
    }

    /**
     * @return a copy with the new value. This position does not change.
     */
    public LocalPosition setXRot(float xRot) {
        return new LocalPosition(this.x, this.y, this.z, xRot, this.rotation[1]);
    }

    /**
     * @return a copy with the new value. This position does not change.
     */
    public LocalPosition setYRot(float yRot) {
        return new LocalPosition(this.x, this.y, this.z, this.rotation[0], yRot);
    }

    /**
     * Checks if the x rotation or the y rotation is not zero.
     *
     * @return {@code true} if the position has a rotation
     */
    public boolean hasRotation() {
        return this.rotation[0] != 0 || this.rotation[1] != 0;
    }

    /**
     * Creates a random position with a factor of 1. Each axis is a random value from minus this axis to this axis.
     *
     * @return a new position with the same rotation
     */
    public LocalPosition randomize() {
        return this.randomize(1);
    }

    /**
     * @return a new position. Each axis is a random value from {@code -factor} to {@code factor} times this axis. The
     *     rotation stays the same.
     */
    public LocalPosition randomize(double factor) {
        double doubleFactor = factor + factor;
        return new LocalPosition(
                (RandomUtils.getRandom().nextDouble() * doubleFactor * this.x) - (this.x * factor),
                (RandomUtils.getRandom().nextDouble() * doubleFactor * this.y) - (this.y * factor),
                (RandomUtils.getRandom().nextDouble() * doubleFactor * this.z) - (this.z * factor),
                this.xRot(), this.yRot()
        );
    }

    /**
     * @return the position of the block that contains this position
     */
    public BlockPos getBlockPos() {
        return BlockPos.containing(this.x, this.y, this.z);
    }

    /**
     * Teleports the entity to this position and rotation in its current level.
     *
     * @param entity the entity to teleport
     */
    public void teleport(Entity entity) {
        this.teleport(entity, entity.level());
    }

    /**
     * Teleports the entity to this position in its current level.
     *
     * @param entity the entity to teleport
     * @param ignoreRotation keeps the current rotation of the entity
     */
    public void teleport(Entity entity, boolean ignoreRotation) {
        this.teleport(entity, entity.level(), ignoreRotation);
    }

    /**
     * Teleports the entity to this position and rotation in the level. Does nothing when the level is not loaded.
     *
     * @param entity the entity to teleport
     * @param level the target level
     */
    public void teleport(Entity entity, ConfigurableLevel level) {
        this.teleport(entity, level, false);
    }

    /**
     * Teleports the entity to this position in the level. Does nothing when the level is not loaded.
     *
     * @param entity the entity to teleport
     * @param level the target level
     * @param ignoreRotation keeps the current rotation of the entity
     */
    public void teleport(Entity entity, ConfigurableLevel level, boolean ignoreRotation) {
        this.teleport(entity, level.get(), ignoreRotation);
    }

    /**
     * Teleports the entity to this position and rotation in the level.
     *
     * @param entity the entity to teleport
     * @param level the target level
     */
    public void teleport(Entity entity, Level level) {
        this.teleport(entity, level, false);
    }

    /**
     * Does nothing when the entity is null or the level is not a server level.
     *
     * @param ignoreRotation keeps the current rotation of the entity
     */
    public void teleport(Entity entity, Level level, boolean ignoreRotation) {
        if (entity == null) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            entity.teleportTo(serverLevel, this.x, this.y, this.z, RELATIVE_MOVEMENTS, ignoreRotation ? entity.getYRot() : this.yRot(), ignoreRotation ? entity.getXRot() : this.xRot());
        }
    }

    /**
     * Adds the values to the coordinates.
     *
     * @param x the value to add to the x coordinate
     * @param y the value to add to the y coordinate
     * @param z the value to add to the z coordinate
     * @return a new position with the same rotation
     */
    @Override
    public LocalPosition add(double x, double y, double z) {
        return new LocalPosition(this.x + x, this.y + y, this.z + z, this.rotation);
    }

    /**
     * Adds the coordinates of the other position. The rotation of the other position is ignored.
     *
     * @param position the position to add
     * @return a new position with the same rotation
     */
    public LocalPosition add(LocalPosition position) {
        return this.add(position.x, position.y, position.z);
    }

    /**
     * Moves the position one block in the direction.
     *
     * @param direction the direction to move in
     * @return a new position with the same rotation
     */
    public LocalPosition move(Direction direction) {
        return this.move(direction, 1);
    }

    /**
     * Moves the position the distance in the direction.
     *
     * @param direction the direction to move in
     * @param distance the distance to move
     * @return a new position with the same rotation
     */
    public LocalPosition move(Direction direction, double distance) {
        return this.add(direction.getStepX() * distance, direction.getStepY() * distance, direction.getStepZ() * distance);
    }

    /**
     * @return a copy with the angles added to the rotation
     */
    public LocalPosition rotate(float xRot, float yRot) {
        return new LocalPosition(this.x, this.y, this.z, this.rotation[0] + xRot, this.rotation[1] + yRot);
    }

    /**
     * Gets the distance on the x axis from this position to the coordinate.
     *
     * @param x the x coordinate
     * @return {@code x} minus the x coordinate of this position
     */
    public double getDX(double x) {
        return x - this.x;
    }

    /**
     * Gets the distance on the y axis from this position to the coordinate.
     *
     * @param y the y coordinate
     * @return {@code y} minus the y coordinate of this position
     */
    public double getDY(double y) {
        return y - this.y;
    }

    /**
     * Gets the distance on the z axis from this position to the coordinate.
     *
     * @param z the z coordinate
     * @return {@code z} minus the z coordinate of this position
     */
    public double getDZ(double z) {
        return z - this.z;
    }

    /**
     * Compares only the coordinates, not the rotation.
     *
     * @param object a JOML vector, a {@link Position}, or a numeric array with three values. Other objects never match.
     */
    public boolean matches(Object object) {
        return switch (object) {
            case Vector3ic vec -> this.x() == vec.x() && this.y() == vec.y() && this.z() == vec.z();
            case Vector3fc vec -> this.x() == vec.x() && this.y() == vec.y() && this.z() == vec.z();
            case Vector3dc vec -> this.x() == vec.x() && this.y() == vec.y() && this.z() == vec.z();
            case Position vec -> this.x() == vec.x() && this.y() == vec.y() && this.z() == vec.z();
            case int[] array -> array.length == 3 && this.x() == array[0] && this.y() == array[1] && this.z() == array[2];
            case float[] array -> array.length == 3 && this.x() == array[0] && this.y() == array[1] && this.z() == array[2];
            case double[] array -> array.length == 3 && this.x() == array[0] && this.y() == array[1] && this.z() == array[2];
            case long[] array -> array.length == 3 && this.x() == array[0] && this.y() == array[1] && this.z() == array[2];
            case short[] array -> array.length == 3 && this.x() == array[0] && this.y() == array[1] && this.z() == array[2];
            case byte[] array -> array.length == 3 && this.x() == array[0] && this.y() == array[1] && this.z() == array[2];
            default -> false;
        };
    }

    /**
     * Checks if the object is a {@link LocalPosition} with the same coordinates and rotation.
     *
     * @param object the object to compare with
     * @return {@code true} if the positions are equal
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof LocalPosition pos)) {
            return false;
        }
        return this.x() == pos.x() && this.y() == pos.y() && this.z() == pos.z() && this.xRot() == pos.xRot() && this.yRot() == pos.yRot();
    }

    /**
     * Gets the hash code of the coordinates from {@link Vec3}. The rotation is not part of it.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * @return the position as {@code (x, y, z, [xRot, yRot])}
     */
    @Override
    public @NonNull String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ", [" + this.rotation[0] + ", " + this.rotation[1] + "])";
    }

    /**
     * Converts a {@link LocalPosition} to and from a JSON object with {@code x}, {@code y}, {@code z} and optional
     * {@code xRot} and {@code yRot} fields.
     */
    public static class TypeAdapter implements JsonSerializer<LocalPosition>, JsonDeserializer<LocalPosition> {

        /**
         * Creates a position from a JSON object. A missing rotation field is 0.
         *
         * @param json the JSON object
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context
         * @return the position
         */
        @Override
        public LocalPosition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var object = json.getAsJsonObject();
            var position = new LocalPosition(object.get("x").getAsDouble(), object.get("y").getAsDouble(), object.get("z").getAsDouble());
            if (object.has("xRot")) {
                position.rotation[0] = object.get("xRot").getAsFloat();
            }
            if (object.has("yRot")) {
                position.rotation[1] = object.get("yRot").getAsFloat();
            }
            return position;
        }

        /**
         * Writes the position as a JSON object. The rotation fields are only written when the position has a
         * rotation.
         *
         * @param src the position
         * @param typeOfSrc the type of the position
         * @param context the serialization context
         * @return the JSON object
         */
        @Override
        public JsonElement serialize(LocalPosition src, Type typeOfSrc, JsonSerializationContext context) {
            var object = new JsonObject();
            object.addProperty("x", src.x);
            object.addProperty("y", src.y);
            object.addProperty("z", src.z);
            if (src.hasRotation()) {
                object.addProperty("xRot", src.rotation[0]);
                object.addProperty("yRot", src.rotation[1]);
            }
            return object;
        }
    }
}
