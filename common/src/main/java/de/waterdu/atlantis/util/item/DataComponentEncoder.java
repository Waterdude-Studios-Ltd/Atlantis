package de.waterdu.atlantis.util.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import de.waterdu.atlantis.util.java.interfaces.TriConsumer;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public record DataComponentEncoder<S, V>(
        DynamicOps<V> ops, Supplier<S> constructor,
        TriConsumer<ResourceLocation, DataResult<V>, S> encoder,
        Function<S, Set<String>> keys,
        BiFunction<S, String, V> decoder
) {

    public static final DataComponentEncoder<CompoundTag, Tag> TAG = new DataComponentEncoder<>(
            NbtOps.INSTANCE,
            CompoundTag::new,
            (key, value, store) -> store.put(key.toString(), value.getOrThrow()),
            CompoundTag::getAllKeys,
            CompoundTag::get
    );

    public static final DataComponentEncoder<JsonObject, JsonElement> JSON = new DataComponentEncoder<>(
            JsonOps.INSTANCE,
            JsonObject::new,
            (key, value, store) -> store.add(key.toString(), value.getOrThrow()),
            JsonObject::keySet,
            JsonObject::get
    );

    public S encode(DataComponentMap components) {
        var store = this.constructor.get();
        for (var component : components) {
            var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
            if (key == null) {
                continue;
            }
            try {
                this.encoder.accept(key, component.encodeValue(this.ops), store);
            } catch (Exception ignored) {
                // noop
            }
        }
        return store;
    }

    public DataComponentMap decode(S data) {
        var builder = DataComponentMap.builder();
        for (var key : this.keys.apply(data)) {
            var type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(key));
            if (type == null) {
                continue;
            }
            try {
                var decoded = type.codecOrThrow().decode(this.ops, this.decoder.apply(data, key)).getOrThrow().getFirst();
                setComponent(builder, type, decoded);
            } catch (Exception ignored) {
                // noop
            }
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> void setComponent(DataComponentMap.Builder builder, net.minecraft.core.component.DataComponentType<T> type, Object value) {
        builder.set(type, (T) value);
    }
}
