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

public class LocalPosition extends Vec3 {

    private static final Set<RelativeMovement> RELATIVE_MOVEMENTS = new HashSet<>();
    public static final LocalPosition ZERO = new LocalPosition();

    private final float[] rotation = new float[]{0F, 0F};

    public LocalPosition() {
        this(0, 0, 0);
    }

    protected LocalPosition(double x, double y, double z, float[] rotation) {
        super(x, y, z);
        this.rotation[0] = rotation[0];
        this.rotation[1] = rotation[1];
    }

    public LocalPosition(double x, double y, double z, float xRot, float yRot) {
        super(x, y, z);
        this.rotation[0] = xRot;
        this.rotation[1] = yRot;
    }

    public LocalPosition(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ(), 0, 0);
    }

    public LocalPosition(BlockPos pos, float xRot, float yRot) {
        this(pos.getX(), pos.getY(), pos.getZ(), xRot, yRot);
    }

    public LocalPosition(double x, double y, double z) {
        this(x, y, z, 0, 0);
    }

    public LocalPosition(Entity entity) {
        this(entity.getX(), entity.getY(), entity.getZ(), entity.getXRot(), entity.getYRot());
    }

    public float xRot() {
        return this.rotation[0];
    }

    public float yRot() {
        return this.rotation[1];
    }

    public LocalPosition setX(double x) {
        return new LocalPosition(x, this.y, this.z, this.rotation);
    }

    public LocalPosition setY(double y) {
        return new LocalPosition(this.x, y, this.z, this.rotation);
    }

    public LocalPosition setZ(double z) {
        return new LocalPosition(this.x, this.y, z, this.rotation);
    }

    public LocalPosition setXRot(float xRot) {
        return new LocalPosition(this.x, this.y, this.z, xRot, this.rotation[1]);
    }

    public LocalPosition setYRot(float yRot) {
        return new LocalPosition(this.x, this.y, this.z, this.rotation[0], yRot);
    }

    public boolean hasRotation() {
        return this.rotation[0] != 0 || this.rotation[1] != 0;
    }

    public LocalPosition randomize() {
        return this.randomize(1);
    }

    public LocalPosition randomize(double factor) {
        double doubleFactor = factor + factor;
        return new LocalPosition(
                (RandomUtils.getRandom().nextDouble() * doubleFactor * this.x) - (this.x * factor),
                (RandomUtils.getRandom().nextDouble() * doubleFactor * this.y) - (this.y * factor),
                (RandomUtils.getRandom().nextDouble() * doubleFactor * this.z) - (this.z * factor),
                this.xRot(), this.yRot()
        );
    }

    public BlockPos getBlockPos() {
        return new BlockPos((int) this.x, (int) this.y, (int) this.z);
    }

    public void teleport(Entity entity) {
        this.teleport(entity, entity.level());
    }

    public void teleport(Entity entity, boolean ignoreRotation) {
        this.teleport(entity, entity.level(), ignoreRotation);
    }

    public void teleport(Entity entity, ConfigurableLevel level) {
        this.teleport(entity, level, false);
    }

    public void teleport(Entity entity, ConfigurableLevel level, boolean ignoreRotation) {
        this.teleport(entity, level.get(), ignoreRotation);
    }

    public void teleport(Entity entity, Level level) {
        this.teleport(entity, level, false);
    }

    public void teleport(Entity entity, Level level, boolean ignoreRotation) {
        if (entity == null) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            entity.teleportTo(serverLevel, this.x, this.y, this.z, RELATIVE_MOVEMENTS, ignoreRotation ? entity.getYRot() : this.yRot(), ignoreRotation ? entity.getXRot() : this.xRot());
        }
    }

    @Override
    public LocalPosition add(double x, double y, double z) {
        return new LocalPosition(this.x + x, this.y + y, this.z + z, this.rotation);
    }

    public LocalPosition add(LocalPosition position) {
        return this.add(position.x, position.y, position.z);
    }

    public LocalPosition move(Direction direction) {
        return this.move(direction, 1);
    }

    public LocalPosition move(Direction direction, double distance) {
        return this.add(direction.getStepX() * distance, direction.getStepY() * distance, direction.getStepZ() * distance);
    }

    public LocalPosition rotate(float xRot, float yRot) {
        return new LocalPosition(this.x, this.y, this.z, this.rotation[0] + xRot, this.rotation[1] + yRot);
    }

    public double getDX(double x) {
        return x - this.x;
    }

    public double getDY(double y) {
        return y - this.y;
    }

    public double getDZ(double z) {
        return z - this.z;
    }

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

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public @NonNull String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ", [" + this.rotation[0] + ", " + this.rotation[1] + "])";
    }

    public static class TypeAdapter implements JsonSerializer<LocalPosition>, JsonDeserializer<LocalPosition> {

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
