package de.waterdu.atlantis.mod.plugin.reflection;

import de.waterdu.atlantis.AtlantisLogger;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Wraps a method of a plugin class and makes it accessible.
 *
 * @param <T> the return type of the method
 */
public class PluginMethod<T> {

    private static final Map<String, PluginMethod<?>> CACHE = new ConcurrentHashMap<>();

    private final String name;
    private final Method method;

    /**
     * Creates a wrapper for the method and makes the method accessible.
     *
     * @param name the cache key of the method, also used in error messages
     * @param method the method
     */
    private PluginMethod(String name, Method method) {
        this.name = name;
        this.method = method;
        this.method.setAccessible(true);
    }

    /**
     * @param name the key of a global cache. Later calls with the same name return the cached method and do not call
     *     the supplier.
     */
    @SuppressWarnings("unchecked")
    public static <T> PluginMethod<T> of(String name, Supplier<Method> method) {
        return (PluginMethod<T>) CACHE.computeIfAbsent(name, it -> new PluginMethod<>(it, method.get()));
    }

    /**
     * Calls the static method.
     *
     * @param parameters the arguments to pass
     * @return the return value, or {@code null} if the call fails. Logs the error.
     */
    public T invokeStatic(Object... parameters) {
        return this.invoke(null, parameters);
    }

    /**
     * @param instance the object to call the method on, or {@code null} for a static method
     * @return the return value, or {@code null} if the call fails. Logs the error.
     */
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

