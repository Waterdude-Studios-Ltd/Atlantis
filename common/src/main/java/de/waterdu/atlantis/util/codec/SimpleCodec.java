package de.waterdu.atlantis.util.codec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.config.Key;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class SimpleCodec<T> {

    private final Key<? extends T> key;
    private final Class<T> type;
    private final Supplier<T> supplier;
    private final Map<String, Type> types = new HashMap<>();
    private final Map<String, Field> fields = new HashMap<>();

    private SimpleCodec(Key<? extends T> key, Class<T> type, Supplier<T> supplier) {
        this.key = key;
        this.type = type;
        this.supplier = supplier;
    }

    @SuppressWarnings("unchecked")
    public static <T> SimpleCodec<T> of(Key<? extends T> key, T type) {
        return SimpleCodec.of(key, (Class<T>) type.getClass());
    }

    public static <T> SimpleCodec<T> of(Key<? extends T> key, Class<T> type) {
        try {
            final var constructor = type.getDeclaredConstructor();
            return new SimpleCodec<>(key, type, () -> {
                try {
                    return constructor.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    AtlantisLogger.error("SimpleCodec: Failed to create new instance of type {}", type);
                    return null;
                }
            });
        } catch (NoSuchMethodException e) {
            AtlantisLogger.error("SimpleCodec: Failed to find constructor in type {}", type);
            return new SimpleCodec<>(key, type, () -> null);
        }
    }

    public static <T> SimpleCodec<T> of(Key<? extends T> key, Class<T> type, Supplier<T> supplier) {
        return new SimpleCodec<>(key, type, supplier);
    }

    public SimpleCodec<T> fromTypeFields() {
        for (var field : this.type.getDeclaredFields()) {
            if (!field.getType().equals(SimpleCodec.class) && !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                this.with(field.getName(), field.getGenericType());
            }
        }
        return this;
    }

    public SimpleCodec<T> withField(String name, Class<?> type) {
        this.types.put(name, type);
        this.fetchField(name);
        return this;
    }

    public SimpleCodec<T> withArray(String name, Class<?> type) {
        this.types.put(name, TypeToken.getArray(type).getType());
        this.fetchField(name);
        return this;
    }

    public SimpleCodec<T> withList(String name, Class<?> type) {
        this.types.put(name, TypeToken.getParameterized(List.class, type).getType());
        this.fetchField(name);
        return this;
    }

    public SimpleCodec<T> withSet(String name, Class<?> type) {
        this.types.put(name, TypeToken.getParameterized(Set.class, type).getType());
        this.fetchField(name);
        return this;
    }

    public SimpleCodec<T> withMap(String name, Class<?> keyType, Class<?> valueType) {
        this.types.put(name, TypeToken.getParameterized(Map.class, keyType, valueType).getType());
        this.fetchField(name);
        return this;
    }

    public SimpleCodec<T> with(String name, Type type) {
        this.types.put(name, type);
        this.fetchField(name);
        return this;
    }

    private void fetchField(String name) {
        try {
            var field = this.type.getDeclaredField(name);
            if (field.getGenericType().equals(this.types.get(name))) {
                field.setAccessible(true);
                this.fields.put(name, field);
            } else {
                AtlantisLogger.error("SimpleCodec: Failed to find matching field {} in type {}", name, this.type);
            }
        } catch (NoSuchFieldException e) {
            AtlantisLogger.error("SimpleCodec: Failed to find field {} in type {}", name, this.type);
        }
    }

    public Key<? extends T> id() {
        return this.key;
    }

    public T deserialize(JsonObject object, JsonDeserializationContext context) {
        var newInstance = this.supplier.get();
        for (var entry : this.fields.entrySet()) {
            try {
                entry.getValue().set(newInstance, context.deserialize(object.get(entry.getKey()), this.types.get(entry.getKey())));
            } catch (IllegalAccessException ignored) {
                // Not possible
            }
        }
        return newInstance;
    }

    public void serialize(T src, JsonObject object, JsonSerializationContext context) {
        for (var entry : this.fields.entrySet()) {
            try {
                object.add(entry.getKey(), context.serialize(entry.getValue().get(src), this.types.get(entry.getKey())));
            } catch (IllegalAccessException ignored) {
                // Not possible
            }
        }
    }
}
