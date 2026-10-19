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

public final class ResourceOrTagKey<T> {

    private final String key;
    private ResourceKey<? extends Registry<T>> registry;
    private ResourceKey<T> resourceKey;
    private TagKey<T> tagKey;
    private Either<ResourceKey<T>, TagKey<T>> either;

    private ResourceOrTagKey(String key) {
        this.key = key;
    }

    private ResourceOrTagKey(ResourceKey<T> key) {
        this(key.location().toString());
        this.registry = ResourceKey.createRegistryKey(key.registry());
        this.resourceKey = key;
        this.createEither();
    }

    private ResourceOrTagKey(TagKey<T> key) {
        this(key.location().toString());
        this.registry = key.registry();
        this.tagKey = key;
        this.createEither();
    }

    public static <T> ResourceOrTagKey<T> create(String key) {
        return new ResourceOrTagKey<>(key);
    }

    @SuppressWarnings("unchecked")
    public static <T> ResourceOrTagKey<T> create(String key, ResourceKey<? extends Registry<T>> registry) {
        return (ResourceOrTagKey<T>) ResourceOrTagKey.create(key).provideContext((ResourceKey<? extends Registry<Object>>) registry);
    }

    public static <T> ResourceOrTagKey<T> create(ResourceKey<T> key) {
        return new ResourceOrTagKey<>(key);
    }

    public static <T> ResourceOrTagKey<T> create(TagKey<T> key) {
        return new ResourceOrTagKey<>(key);
    }

    public ResourceOrTagKey<T> provideContext(ResourceKey<? extends Registry<T>> registry) {
        if (this.registry == null) {
            this.registry = registry;
            this.parseKey();
        }
        return this;
    }

    private void parseKey() {
        if (this.key.startsWith("#")) {
            this.tagKey = TagKey.create(this.registry, ResourceLocation.tryParse(this.key.substring(1).toLowerCase(Locale.ROOT)));
        } else {
            this.resourceKey = ResourceKey.create(this.registry, ResourceLocation.tryParse(this.key.toLowerCase(Locale.ROOT)));
        }
    }

    private void checkContextProvided() {
        if (this.registry == null) {
            AtlantisLogger.error("Attempting to access contextless ResourceOrTagKey");
        }
    }

    public ResourceKey<T> asResourceKey() {
        this.checkContextProvided();
        return this.resourceKey;
    }

    public TagKey<T> asTagKey() {
        this.checkContextProvided();
        return this.tagKey;
    }

    public Either<ResourceKey<T>, TagKey<T>> asEither() {
        this.checkContextProvided();
        this.createEither();
        return this.either;
    }

    public Either<ResourceKey<T>, TagKey<T>> asEitherUnsafe() {
        this.createEither();
        return this.either;
    }

    private void createEither() {
        if (this.either == null) {
            this.either = this.resourceKey != null ? Either.left(this.resourceKey) : Either.right(this.tagKey);
        }
    }

    public boolean is(T instance) {
        return this.is(Holder.direct(instance));
    }

    public boolean is(Holder<T> holder) {
        return this.asEither().map(holder::is, holder::is);
    }

    public static class TypeAdapter implements JsonSerializer<ResourceOrTagKey<?>>, JsonDeserializer<ResourceOrTagKey<?>> {

        @Override
        public ResourceOrTagKey<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return ResourceOrTagKey.create(json.getAsString());
        }

        @Override
        public JsonElement serialize(ResourceOrTagKey<?> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.key);
        }
    }
}

