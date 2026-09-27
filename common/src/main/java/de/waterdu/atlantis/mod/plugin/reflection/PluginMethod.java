package de.waterdu.atlantis.mod.plugin.reflection;

import de.waterdu.atlantis.AtlantisLogger;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PluginMethod<T> {

    private static final Map<String, PluginMethod<?>> CACHE = new ConcurrentHashMap<>();

    private final String name;
    private final Method method;

    private PluginMethod(String name, Method method) {
        this.name = name;
        this.method = method;
        this.method.setAccessible(true);
    }

    @SuppressWarnings("unchecked")
    public static <T> PluginMethod<T> of(String name, Supplier<Method> method) {
        return (PluginMethod<T>) CACHE.computeIfAbsent(name, it -> new PluginMethod<>(it, method.get()));
    }

    public T invokeStatic(Object... parameters) {
        return this.invoke(null, parameters);
    }

    @SuppressWarnings("unchecked")
    public @Nullable T invoke(@Nullable Object instance, Object... parameters) {
        try {
            return (T) this.method.invoke(instance, parameters);
        } catch (Exception e) {
            AtlantisLogger.error("Failed to invoke method {}: {}", this.name, e.getMessage());
            return null;
        }
    }
}

