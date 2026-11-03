package de.waterdu.atlantis.util.codec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.datafixers.util.Either;
import de.waterdu.atlantis.AtlantisLogger;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * Holds a key that is either a resource key or a tag key. A string key is parsed only when a registry is known.
 *
 * @param <T> the type of the registry entries
 */
public final class ResourceOrTagKey<T> {

    private final String key;
    private ResourceKey<? extends Registry<T>> registry;
    private ResourceKey<T> resourceKey;
    private TagKey<T> tagKey;
    private Either<ResourceKey<T>, TagKey<T>> either;

    /**
     * Creates a key without a registry.
     *
     * @param key a resource location, or a tag location with a {@code #} prefix
     */
    private ResourceOrTagKey(String key) {
        this.key = key;
    }

    /**
     * Creates a key from a resource key. The registry comes from the resource key.
     *
     * @param key the resource key
     */
    private ResourceOrTagKey(ResourceKey<T> key) {
        this(key.location().toString());
        this.registry = ResourceKey.createRegistryKey(key.registry());
        this.resourceKey = key;
        this.createEither();
    }

    /**
     * Creates a key from a tag key. The registry comes from the tag key.
     *
     * @param key the tag key
     */
    private ResourceOrTagKey(TagKey<T> key) {
        this("#" + key.location());
        this.registry = key.registry();
        this.tagKey = key;
        this.createEither();
    }

    /**
     * Call {@link #provideContext} before you read the key.
     *
     * @param key a resource location, or a tag location with a {@code #} prefix
     */
    public static <T> ResourceOrTagKey<T> create(String key) {
        return new ResourceOrTagKey<>(key);
    }

    /**
     * Creates a key from a string and parses it for the registry.
     *
     * @param <T> the type of the registry entries
     * @param key a resource location, or a tag location with a {@code #} prefix
     * @param registry the registry of the key
     * @return the key
     */
    @SuppressWarnings("unchecked")
    public static <T> ResourceOrTagKey<T> create(String key, ResourceKey<? extends Registry<T>> registry) {
        return (ResourceOrTagKey<T>) ResourceOrTagKey.create(key).provideContext((ResourceKey<? extends Registry<Object>>) registry);
    }

    /**
     * Creates a key from a resource key.
     *
     * @param <T> the type of the registry entries
     * @param key the resource key
     * @return the key
     */
    public static <T> ResourceOrTagKey<T> create(ResourceKey<T> key) {
        return new ResourceOrTagKey<>(key);
    }

    /**
     * Creates a key from a tag key.
     *
     * @param <T> the type of the registry entries
     * @param key the tag key
     * @return the key
     */
    public static <T> ResourceOrTagKey<T> create(TagKey<T> key) {
        return new ResourceOrTagKey<>(key);
    }

    /**
     * Does nothing if the key already has a registry.
     */
    public ResourceOrTagKey<T> provideContext(ResourceKey<? extends Registry<T>> registry) {
        if (this.registry == null) {
            this.registry = registry;
            this.parseKey();
        }
        return this;
    }

    /**
     * Parses the string key, in lower case, into a tag key if it starts with {@code #}, or into a resource key if not.
     */
    private void parseKey() {
        if (this.key.startsWith("#")) {
            this.tagKey = TagKey.create(this.registry, ResourceLocation.tryParse(this.key.substring(1).toLowerCase(Locale.ROOT)));
        } else {
            this.resourceKey = ResourceKey.create(this.registry, ResourceLocation.tryParse(this.key.toLowerCase(Locale.ROOT)));
        }
    }

    /**
     * Logs an error if the key has no registry.
     */
    private void checkContextProvided() {
        if (this.registry == null) {
            AtlantisLogger.error("Attempting to access contextless ResourceOrTagKey");
        }
    }

    /**
     * @return null if this is a tag key or if no registry is provided
     */
    public ResourceKey<T> asResourceKey() {
        this.checkContextProvided();
        return this.resourceKey;
    }

    /**
     * @return null if this is a resource key or if no registry is provided
     */
    public TagKey<T> asTagKey() {
        this.checkContextProvided();
        return this.tagKey;
    }

    /**
     * Gets the key as an either. Logs an error if the key has no registry.
     *
     * @return the resource key on the left, or the tag key on the right
     */
    public Either<ResourceKey<T>, TagKey<T>> asEither() {
        this.checkContextProvided();
        this.createEither();
        return this.either;
    }

    /**
     * {@link #asEither()} without the error log when no registry is provided.
     */
    public Either<ResourceKey<T>, TagKey<T>> asEitherUnsafe() {
        this.createEither();
        return this.either;
    }

    /**
     * Creates the either from the resource key or the tag key, if it does not exist yet.
     */
    private void createEither() {
        if (this.either == null) {
            this.either = this.resourceKey != null ? Either.left(this.resourceKey) : Either.right(this.tagKey);
        }
    }

    /**
     * Checks if the instance matches this key.
     *
     * @param instance the registry entry to check
     * @return {@code true} if the instance has the resource key or is in the tag
     */
    public boolean is(T instance) {
        return this.is(Holder.direct(instance));
    }

    /**
     * Checks if the holder matches this key.
     *
     * @param holder the holder to check
     * @return {@code true} if the holder has the resource key or is in the tag
     */
    public boolean is(Holder<T> holder) {
        return this.asEither().map(holder::is, holder::is);
    }

    /**
     * Converts a {@link ResourceOrTagKey} to and from a JSON string.
     */
    public static class TypeAdapter implements JsonSerializer<ResourceOrTagKey<?>>, JsonDeserializer<ResourceOrTagKey<?>> {

        /**
         * Creates a key without a registry from the JSON string.
         *
         * @param json the JSON string
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context
         * @return the key
         */
        @Override
        public ResourceOrTagKey<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return ResourceOrTagKey.create(json.getAsString());
        }

        /**
         * Writes the string form of the key, with a {@code #} prefix for a tag key.
         *
         * @param src the key to serialize
         * @param typeOfSrc the type of the key
         * @param context the serialization context
         * @return the key as a JSON string
         */
        @Override
        public JsonElement serialize(ResourceOrTagKey<?> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.key);
        }
    }
}

