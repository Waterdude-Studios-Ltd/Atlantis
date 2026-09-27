package de.waterdu.atlantis.util.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public record Key<T>(ResourceLocation key) {

    private static final Map<Class<?>, Function<Key<?>, Object>> GETTERS = new HashMap<>();
    private static final Map<Class<?>, Function<Key<?>, Extension<?>>> EXTENSIONS = new HashMap<>();
    private static final Map<Class<?>, Map<Key<?>, Object>> CACHES = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> void registerGetter(Class<T> type, Function<Key<T>, T> getter) {
        GETTERS.put(type, (Function<Key<?>, Object>) (Object) getter);
    }

    @SuppressWarnings("unchecked")
    public static <T> void registerExtension(Class<T> type, Function<Key<T>, Extension<T>> extension) {
        EXTENSIONS.put(type, (Function<Key<?>, Extension<?>>) (Object) extension);
    }

    public static <T> Key<T> of(String key) {
        return new Key<>(ResourceLocation.parse(key));
    }

    public static <T> Key<T> of(String namespace, String path) {
        return new Key<>(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    @SuppressWarnings("unchecked")
    public static <T> Key<T> of(String key, T value) {
        var actualKey = Key.<T>of(key);
        actualKey.cacheMap((Class<T>) value.getClass()).put(actualKey, value);
        return actualKey;
    }

    @SuppressWarnings("unchecked")
    public static <T> Key<T> of(String namespace, String path, T value) {
        var actualKey = Key.<T>of(namespace, path);
        actualKey.cacheMap((Class<T>) value.getClass()).put(actualKey, value);
        return actualKey;
    }

    @SuppressWarnings("unchecked")
    public T get(Class<T> type) {
        var getter = GETTERS.get(type);
        if (getter == null) {
            throw new UnsupportedOperationException("No getter for type " + type.getName());
        }
        return (T) ((Function<Key<?>, ?>) getter).apply(this);
    }

    public T cache(Class<T> type) {
        return this.cacheMap(type).computeIfAbsent(this, ___ -> this.get(type));
    }

    public T uncache(Class<T> type) {
        return this.cacheMap(type).remove(this);
    }

    @SuppressWarnings("unchecked")
    private Map<Key<T>, T> cacheMap(Class<T> type) {
        return (Map<Key<T>, T>) (Object) CACHES.computeIfAbsent(type, ___ -> new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public <E> E ext(Class<T> type) {
        var extension = EXTENSIONS.get(type);
        if (extension == null) {
            throw new UnsupportedOperationException("No extension for type " + type.getName());
        }
        return (E) extension.apply(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Key(ResourceLocation otherKey))) {
            return false;
        }
        return this.key.equals(otherKey);
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public @NonNull String toString() {
        return this.key.toString();
    }

    public static class TypeAdapter implements JsonSerializer<Key<?>>, JsonDeserializer<Key<?>> {

        @Override
        public Key<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Key.of(json.getAsString());
        }

        @Override
        public JsonElement serialize(Key<?> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    public static class Extension<T> {

        private final Key<T> key;

        public Extension(Key<T> key) {
            this.key = key;
        }

        public Key<T> key() {
            return this.key;
        }

        public T get(Class<T> type) {
            return this.key.get(type);
        }

        public T cache(Class<T> type) {
            return this.key.cache(type);
        }

        public T uncache(Class<T> type) {
            return this.key.uncache(type);
        }
    }
}
