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

/**
 * Reads and writes chosen fields of a type to and from a JSON object with reflection.
 *
 * @param <T> the type that the codec handles
 */
public class SimpleCodec<T> {

    private final Key<? extends T> key;
    private final Class<T> type;
    private final Supplier<T> supplier;
    private final Map<String, Type> types = new HashMap<>();
    private final Map<String, Field> fields = new HashMap<>();

    /**
     * Creates a codec without fields.
     *
     * @param key the ID of the codec
     * @param type the type that the codec handles
     * @param supplier creates new instances during deserialization
     */
    private SimpleCodec(Key<? extends T> key, Class<T> type, Supplier<T> supplier) {
        this.key = key;
        this.type = type;
        this.supplier = supplier;
    }

    /**
     * @param type an instance of the type. Only its runtime class is used.
     */
    @SuppressWarnings("unchecked")
    public static <T> SimpleCodec<T> of(Key<? extends T> key, T type) {
        return SimpleCodec.of(key, (Class<T>) type.getClass());
    }

    /**
     * The type must have an accessible no-argument constructor. If it does not, deserialization fails.
     */
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

    /**
     * Creates a codec that uses the supplier to create new instances.
     *
     * @param <T> the type that the codec handles
     * @param key the ID of the codec
     * @param type the type that the codec handles
     * @param supplier creates new instances during deserialization
     * @return the codec, without fields
     */
    public static <T> SimpleCodec<T> of(Key<? extends T> key, Class<T> type, Supplier<T> supplier) {
        return new SimpleCodec<>(key, type, supplier);
    }

    /**
     * Adds all non-static, non-transient fields that the type declares.
     * Fields of superclasses and fields of type {@link SimpleCodec} are not added.
     */
    public SimpleCodec<T> fromTypeFields() {
        for (var field : this.type.getDeclaredFields()) {
            if (!field.getType().equals(SimpleCodec.class) && !Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                this.with(field.getName(), field.getGenericType());
            }
        }
        return this;
    }

    /**
     * Adds a field of a plain class type.
     *
     * @param name the name of the field
     * @param type the type of the field
     * @return this codec
     */
    public SimpleCodec<T> withField(String name, Class<?> type) {
        this.types.put(name, type);
        this.fetchField(name);
        return this;
    }

    /**
     * Adds an array field.
     *
     * @param name the name of the field
     * @param type the component type of the array
     * @return this codec
     */
    public SimpleCodec<T> withArray(String name, Class<?> type) {
        this.types.put(name, type.arrayType());
        this.fetchField(name);
        return this;
    }

    /**
     * Adds a {@link List} field.
     *
     * @param name the name of the field
     * @param type the element type of the list
     * @return this codec
     */
    public SimpleCodec<T> withList(String name, Class<?> type) {
        this.types.put(name, TypeToken.getParameterized(List.class, type).getType());
        this.fetchField(name);
        return this;
    }

    /**
     * Adds a {@link Set} field.
     *
     * @param name the name of the field
     * @param type the element type of the set
     * @return this codec
     */
    public SimpleCodec<T> withSet(String name, Class<?> type) {
        this.types.put(name, TypeToken.getParameterized(Set.class, type).getType());
        this.fetchField(name);
        return this;
    }

    /**
     * Adds a {@link Map} field.
     *
     * @param name the name of the field
     * @param keyType the key type of the map
     * @param valueType the value type of the map
     * @return this codec
     */
    public SimpleCodec<T> withMap(String name, Class<?> keyType, Class<?> valueType) {
        this.types.put(name, TypeToken.getParameterized(Map.class, keyType, valueType).getType());
        this.fetchField(name);
        return this;
    }

    /**
     * The type itself must declare the field with exactly this generic type.
     * If it does not, the codec logs an error and ignores the field.
     */
    public SimpleCodec<T> with(String name, Type type) {
        this.types.put(name, type);
        this.fetchField(name);
        return this;
    }

    /**
     * Finds the declared field and makes it accessible. Logs an error if the field is missing or its generic type does
     * not match the added type.
     *
     * @param name the name of the field
     */
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

    /**
     * @return the ID of the codec
     */
    public Key<? extends T> id() {
        return this.key;
    }

    /**
     * Fields that are not in {@code object} keep the value that the supplier gives them.
     */
    public T deserialize(JsonObject object, JsonDeserializationContext context) {
        var newInstance = this.supplier.get();
        for (var entry : this.fields.entrySet()) {
            if (!object.has(entry.getKey())) {
                continue;
            }
            try {
                entry.getValue().set(newInstance, context.deserialize(object.get(entry.getKey()), this.types.get(entry.getKey())));
            } catch (IllegalAccessException ignored) {
                // Not possible
            }
        }
        return newInstance;
    }

    /**
     * Writes every added field of the instance to the JSON object.
     *
     * @param src the instance to serialize
     * @param object the JSON object that receives the fields
     * @param context the serialization context
     */
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
