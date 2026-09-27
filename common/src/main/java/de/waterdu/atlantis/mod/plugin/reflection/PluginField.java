package de.waterdu.atlantis.mod.plugin.reflection;

import de.waterdu.atlantis.AtlantisLogger;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PluginField<T> {

    private static final Map<String, PluginField<?>> CACHE = new ConcurrentHashMap<>();

    private final String name;
    private final Field field;

    private PluginField(String name, Field field) {
        this.name = name;
        this.field = field;
        this.field.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    public static <T> PluginField<T> of(String name, Supplier<Field> field) {
        return (PluginField<T>) CACHE.computeIfAbsent(name, it -> new PluginField<>(it, field.get()));
    }

    public T getStatic() {
        return this.get(null);
    }

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

