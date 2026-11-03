package de.waterdu.atlantis.util.level;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.util.java.interfaces.Lazy;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.reflect.Type;

/**
 * Refers to a level by its id, so that configs can store it as a string.
 */
public class ConfigurableLevel {

    private final ResourceKey<Level> key;
    private final Lazy<ServerLevel> level = Lazy.of(() -> LevelUtils.level(this.key()));

    /**
     * Creates a reference to the level with the given id.
     *
     * @param key the level id. The namespace defaults to {@code minecraft}.
     */
    public ConfigurableLevel(String key) {
        this.key = LevelUtils.dimension(key);
    }

    /**
     * Creates a reference to the level with the given id.
     *
     * @param key the level id. The namespace defaults to {@code minecraft}.
     * @return the reference
     */
    public static ConfigurableLevel of(String key) {
        return new ConfigurableLevel(key);
    }

    /**
     * Creates a reference to the level with the id {@code mod:key}.
     *
     * @param mod the namespace of the level id
     * @param key the path of the level id
     * @return the reference
     */
    public static ConfigurableLevel of(String mod, String key) {
        return new ConfigurableLevel(mod + ":" + key);
    }

    /**
     * Creates a reference to the level with the given id.
     *
     * @param location the level id
     * @return the reference
     */
    public static ConfigurableLevel of(ResourceLocation location) {
        return ConfigurableLevel.of(location.toString());
    }

    /**
     * Creates a reference to the level with the given key.
     *
     * @param level the level key
     * @return the reference
     */
    public static ConfigurableLevel of(ResourceKey<Level> level) {
        return ConfigurableLevel.of(level.location());
    }

    /**
     * Creates a reference to the dimension of the given level.
     *
     * @param level the level
     * @return the reference
     */
    public static ConfigurableLevel of(Level level) {
        return ConfigurableLevel.of(level.dimension());
    }

    /**
     * @return the key of the level
     */
    public ResourceKey<Level> key() {
        return this.key;
    }

    /**
     * After the first lookup that finds the level, the level is cached.
     *
     * @return null when the level does not exist
     */
    public ServerLevel get() {
        return this.level.get();
    }

    /**
     * Checks if the entity is in this level.
     *
     * @param entity the entity to check
     * @return {@code true} if the level of the entity matches
     */
    public boolean contains(Entity entity) {
        return this.matches(entity.level());
    }

    /**
     * @param level a {@link Level}, a {@link ResourceKey}, a {@link ResourceLocation}, a level id string or a
     *     {@link ConfigurableLevel}. Other objects never match.
     */
    public boolean matches(Object level) {
        if (level instanceof Level other) {
            return other.dimension().equals(this.key);
        } else if (level instanceof ResourceKey<?> other) {
            return other.equals(this.key);
        } else if (level instanceof ResourceLocation other) {
            return this.key.location().equals(other);
        } else if (level instanceof String other) {
            var dim = LevelUtils.dimension(other);
            return dim != null && dim.equals(this.key);
        } else if (level instanceof ConfigurableLevel other) {
            return other.key().equals(this.key);
        }
        return false;
    }

    /**
     * Gets the hash code of the level id.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * Checks if the object is a {@link ConfigurableLevel} with the same level key.
     *
     * @param obj the object to compare with
     * @return {@code true} if the keys are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConfigurableLevel other)) {
            return false;
        }
        return this.key.equals(other.key);
    }

    /**
     * @return the level id, for example {@code minecraft:overworld}
     */
    @Override
    public String toString() {
        return this.key.location().toString();
    }

    /**
     * Converts a {@link ConfigurableLevel} to and from a JSON string of its level id.
     */
    public static class TypeAdapter implements JsonSerializer<ConfigurableLevel>, JsonDeserializer<ConfigurableLevel> {

        /**
         * Creates a level reference from a JSON string of the level id.
         *
         * @param json the JSON string
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context
         * @return the level reference
         */
        @Override
        public ConfigurableLevel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new ConfigurableLevel(json.getAsString());
        }

        /**
         * Writes the level id as a JSON string.
         *
         * @param src the level reference
         * @param typeOfSrc the type of the level reference
         * @param context the serialization context
         * @return the JSON string
         */
        @Override
        public JsonElement serialize(ConfigurableLevel src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.key().location().toString());
        }
    }
}
