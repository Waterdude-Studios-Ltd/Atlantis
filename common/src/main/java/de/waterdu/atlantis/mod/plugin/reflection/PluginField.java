package de.waterdu.atlantis.mod.plugin.reflection;

import de.waterdu.atlantis.AtlantisLogger;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Wraps a field of a plugin class and makes it accessible.
 *
 * @param <T> the type of the field
 */
public class PluginField<T> {

    private static final Map<String, PluginField<?>> CACHE = new ConcurrentHashMap<>();

    private final String name;
    private final Field field;

    /**
     * Creates a wrapper for the field and makes the field accessible.
     *
     * @param name the cache key of the field, also used in error messages
     * @param field the field
     */
    private PluginField(String name, Field field) {
        this.name = name;
        this.field = field;
        this.field.setAccessible(true);
    }

    /**
     * @param name the key of a global cache. Later calls with the same name return the cached field and do not call the
     *     supplier.
     */
    @SuppressWarnings("unchecked")
    public static <T> PluginField<T> of(String name, Supplier<Field> field) {
        return (PluginField<T>) CACHE.computeIfAbsent(name, it -> new PluginField<>(it, field.get()));
    }

    /**
     * Gets the value of the static field.
     *
     * @return the value, or {@code null} if the access fails. Logs the error.
     */
    public T getStatic() {
        return this.get(null);
    }

    /**
     * @param instance the object to read from, or {@code null} for a static field
     * @return the value, or {@code null} if the access fails. Logs the error.
     */
    @SuppressWarnings("unchecked")
    public @Nullable T get(@Nullable Object instance) {
        try {
            return (T) this.field.get(instance);
        } catch (Exception e) {
            AtlantisLogger.error("Failed to get field {}: {}", this.name, e.getMessage());
            return null;
        }
    }
}

