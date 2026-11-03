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

/**
 * Identifies a value of a type by a resource location. Getters, extensions and a global cache are registered per type.
 *
 * @param key the resource location of the value
 * @param <T> the type of the value
 */
public record Key<T>(ResourceLocation key) {

    private static final Map<Class<?>, Function<Key<?>, Object>> GETTERS = new HashMap<>();
    private static final Map<Class<?>, Function<Key<?>, Extension<?>>> EXTENSIONS = new HashMap<>();
    private static final Map<Class<?>, Map<Key<?>, Object>> CACHES = new HashMap<>();

    /**
     * Registers the getter that {@link #get(Class)} uses for the type. It replaces the previous getter of the type.
     *
     * @param type the type of the values
     * @param getter gets the value of a key
     * @param <T> the type of the values
     */
    @SuppressWarnings("unchecked")
    public static <T> void registerGetter(Class<T> type, Function<Key<T>, T> getter) {
        GETTERS.put(type, (Function<Key<?>, Object>) (Object) getter);
    }

    /**
     * Registers the extension factory that {@link #ext(Class)} uses for the type. It replaces the previous factory of
     * the type.
     *
     * @param type the type of the values
     * @param extension creates the extension for a key
     * @param <T> the type of the values
     */
    @SuppressWarnings("unchecked")
    public static <T> void registerExtension(Class<T> type, Function<Key<T>, Extension<T>> extension) {
        EXTENSIONS.put(type, (Function<Key<?>, Extension<?>>) (Object) extension);
    }

    /**
     * @param key a resource location. If it has no namespace, the namespace is {@code minecraft}.
     */
    public static <T> Key<T> of(String key) {
        return new Key<>(ResourceLocation.parse(key));
    }

    /**
     * Creates a key from a namespace and a path.
     *
     * @param namespace the namespace of the resource location
     * @param path the path of the resource location
     * @param <T> the type of the value
     * @return the key
     */
    public static <T> Key<T> of(String namespace, String path) {
        return new Key<>(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    /**
     * Also puts the value in the cache. The cache entry uses the runtime class of the value as its type.
     */
    @SuppressWarnings("unchecked")
    public static <T> Key<T> of(String key, T value) {
        var actualKey = Key.<T>of(key);
        actualKey.cacheMap((Class<T>) value.getClass()).put(actualKey, value);
        return actualKey;
    }

    /**
     * Also puts the value in the cache. The cache entry uses the runtime class of the value as its type.
     */
    @SuppressWarnings("unchecked")
    public static <T> Key<T> of(String namespace, String path, T value) {
        var actualKey = Key.<T>of(namespace, path);
        actualKey.cacheMap((Class<T>) value.getClass()).put(actualKey, value);
        return actualKey;
    }

    /**
     * Calls the getter that is registered for the type. It does not use the cache. Throws
     * {@link UnsupportedOperationException} if the type has no getter.
     */
    @SuppressWarnings("unchecked")
    public T get(Class<T> type) {
        var getter = GETTERS.get(type);
        if (getter == null) {
            throw new UnsupportedOperationException("No getter for type " + type.getName());
        }
        return (T) ((Function<Key<?>, ?>) getter).apply(this);
    }

    /**
     * Gets the cached value. If there is none, calls {@link #get(Class)} and caches the result. The cache is global and
     * only {@link #uncache(Class)} removes values from it.
     */
    public T cache(Class<T> type) {
        return this.cacheMap(type).computeIfAbsent(this, ___ -> this.get(type));
    }

    /**
     * @return the removed value, or {@code null} if no value was cached
     */
    public T uncache(Class<T> type) {
        return this.cacheMap(type).remove(this);
    }

    /**
     * Gets the cache map of the type, and creates it if it does not exist.
     *
     * @param type the type of the values
     * @return the cache map of the type
     */
    @SuppressWarnings("unchecked")
    private Map<Key<T>, T> cacheMap(Class<T> type) {
        return (Map<Key<T>, T>) (Object) CACHES.computeIfAbsent(type, ___ -> new HashMap<>());
    }

    /**
     * Creates the extension that is registered for the type. Throws {@link UnsupportedOperationException} if the type
     * has no extension. The return type is not checked.
     */
    @SuppressWarnings("unchecked")
    public <E> E ext(Class<T> type) {
        var extension = EXTENSIONS.get(type);
        if (extension == null) {
            throw new UnsupportedOperationException("No extension for type " + type.getName());
        }
        return (E) extension.apply(this);
    }

    /**
     * Checks if the other object is a key with the same resource location. The type of the value is ignored.
     *
     * @param o the object to compare with
     * @return {@code true} if the resource locations are equal
     */
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

    /**
     * @return the hash code of the resource location
     */
    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    /**
     * @return the resource location as a string
     */
    @Override
    public @NonNull String toString() {
        return this.key.toString();
    }

    /**
     * Serializes a key to a JSON string and deserializes it from one.
     */
    public static class TypeAdapter implements JsonSerializer<Key<?>>, JsonDeserializer<Key<?>> {

        /**
         * Creates a key from the string in the JSON element.
         *
         * @param json the JSON string
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context
         * @return the key
         */
        @Override
        public Key<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Key.of(json.getAsString());
        }

        /**
         * Writes the key as a JSON string.
         *
         * @param src the key to serialize
         * @param typeOfSrc the type of the key
         * @param context the serialization context
         * @return the resource location of the key as a JSON string
         */
        @Override
        public JsonElement serialize(Key<?> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    /**
     * Adds type-specific operations to a key. Subclasses are created by the factory given to
     * {@link Key#registerExtension}.
     *
     * @param <T> the type of the value
     */
    public static class Extension<T> {

        private final Key<T> key;

        /**
         * @param key the key to extend
         */
        public Extension(Key<T> key) {
            this.key = key;
        }

        /**
         * @return the key that this extension extends
         */
        public Key<T> key() {
            return this.key;
        }

        /**
         * Calls {@link Key#get(Class)} on the key.
         *
         * @param type the type of the value
         * @return the value from the registered getter
         */
        public T get(Class<T> type) {
            return this.key.get(type);
        }

        /**
         * Calls {@link Key#cache(Class)} on the key.
         *
         * @param type the type of the value
         * @return the cached value
         */
        public T cache(Class<T> type) {
            return this.key.cache(type);
        }

        /**
         * Calls {@link Key#uncache(Class)} on the key.
         *
         * @param type the type of the value
         * @return the removed value, or {@code null} if no value was cached
         */
        public T uncache(Class<T> type) {
            return this.key.uncache(type);
        }
    }
}
