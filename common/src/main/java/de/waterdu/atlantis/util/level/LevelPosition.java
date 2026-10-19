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

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

public class LevelPosition {

    private final ConfigurableLevel level;
    private final LocalPosition position;

    private LevelPosition(ConfigurableLevel level, LocalPosition position) {
        this.level = level;
        this.position = position;
    }

    private LevelPosition(Entity entity) {
        this(ConfigurableLevel.of(entity.level()), new LocalPosition(entity));
    }

    public static LevelPosition of(ConfigurableLevel level, LocalPosition position) {
        return new LevelPosition(level, position);
    }

    public static LevelPosition of(Entity entity) {
        return new LevelPosition(entity);
    }

    public static LevelPosition of(String level, double x, double y, double z) {
        return LevelPosition.of(level, x, y, z, 0F, 0F);
    }

    public static LevelPosition of(String level, double x, double y, double z, float xRot, float yRot) {
        return LevelPosition.of(ConfigurableLevel.of(level), new LocalPosition(x, y, z, xRot, yRot));
    }

    public ServerLevel level() {
        return this.level.get();
    }

    public ResourceKey<Level> levelKey() {
        return this.level.key();
    }

    public LocalPosition position() {
        return this.position;
    }

    public double x() {
        return this.position.x();
    }

    public double y() {
        return this.position.y();
    }

    public double z() {
        return this.position.z();
    }

    public float xRot() {
        return this.position.xRot();
    }

    public float yRot() {
        return this.position.yRot();
    }

    public <E extends Entity> Optional<E> createEntity(EntityType<E> type) {
        E entity = type.create(this.level());
        if (entity == null) {
            return Optional.empty();
        }
        entity.absMoveTo(this.x(), this.y(), this.z(), this.yRot(), this.xRot());
        return Optional.of(entity);
    }

    public void sound(SoundEvent sound) {
        this.sound(sound, SoundSource.MASTER);
    }

    public void sound(SoundEvent sound, float volume, float pitch) {
        this.sound(sound, SoundSource.MASTER, volume, pitch);
    }

    public void sound(SoundEvent sound, SoundSource category) {
        this.sound(sound, category, 1F, 1F);
    }

    public void sound(SoundEvent sound, SoundSource category, float volume, float pitch) {
        this.level().playSound(null, this.position().x(), this.position().y(), this.position().z(), sound, category, volume, pitch);
    }

    public void teleport(PlayerReference reference) {
        if (reference == null) {
            return;
        }
        this.teleport(reference.entity());
    }

    public void teleport(Optional<? extends Entity> entity) {
        entity.ifPresent(this::teleport);
    }

    public void teleport(Entity entity) {
        this.position.teleport(entity, this.level);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.level, this.position);
    }

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

    public static class TypeAdapter implements JsonSerializer<LevelPosition>, JsonDeserializer<LevelPosition> {

        @Override
        public LevelPosition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            return LevelPosition.of(context.deserialize(object.get("level"), ConfigurableLevel.class), context.deserialize(object.get("position"), LocalPosition.class));
        }

        @Override
        public JsonElement serialize(LevelPosition src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();
            object.add("level", context.serialize(src.level));
            object.add("position", context.serialize(src.position));
            return object;
        }
    }
}
