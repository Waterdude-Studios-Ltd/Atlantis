package de.waterdu.atlantis.util.level;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.reflect.Type;

public class ConfigurableLevel {

    private final ResourceKey<Level> key;
    private ServerLevel level = null;

    public ConfigurableLevel(String key) {
        this.key = LevelUtils.dimension(key);
    }

    public static ConfigurableLevel of(String key) {
        return new ConfigurableLevel(key);
    }

    public static ConfigurableLevel of(String mod, String key) {
        return new ConfigurableLevel(mod + ":" + key);
    }

    public static ConfigurableLevel of(ResourceLocation location) {
        return ConfigurableLevel.of(location.toString());
    }

    public static ConfigurableLevel of(ResourceKey<Level> level) {
        return ConfigurableLevel.of(level.location());
    }

    public static ConfigurableLevel of(Level level) {
        return ConfigurableLevel.of(level.dimension());
    }

    public ResourceKey<Level> key() {
        return this.key;
    }

    public ServerLevel get() {
        if (this.level == null) {
            this.level = LevelUtils.level(this.key).orElse(null);
        }
        return this.level;
    }

    public boolean contains(Entity entity) {
        return this.matches(entity.level());
    }

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

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

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

    @Override
    public String toString() {
        return this.key.location().toString();
    }

    public static class TypeAdapter implements JsonSerializer<ConfigurableLevel>, JsonDeserializer<ConfigurableLevel> {

        @Override
        public ConfigurableLevel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new ConfigurableLevel(json.getAsString());
        }

        @Override
        public JsonElement serialize(ConfigurableLevel src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.key().location().toString());
        }
    }
}
