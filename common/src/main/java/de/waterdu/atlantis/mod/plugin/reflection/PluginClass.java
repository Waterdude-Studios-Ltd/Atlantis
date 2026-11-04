package de.waterdu.atlantis.mod.plugin.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Wraps a class of a plugin and caches its methods and fields by name.
 *
 * @param <T> the type of the class
 */
public class PluginClass<T> {

    private static final Map<String, PluginClass<?>> CACHE = new ConcurrentHashMap<>();

    private final String name;
    private final Class<T> clazz;

    private final Map<String, PluginMethod<?>> methods = new ConcurrentHashMap<>();
    private final Map<String, PluginField<?>> fields = new ConcurrentHashMap<>();

    /**
     * Creates a wrapper for the class.
     *
     * @param name the cache key of the class
     * @param clazz the class
     */
    private PluginClass(String name, Class<T> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    /**
     * @param name the key of a global cache. Later calls with the same name return the cached class and do not call the
     *     supplier.
     */
    @SuppressWarnings("unchecked")
    public static <T> PluginClass<T> of(String name, Supplier<Class<T>> clazz) {
        return (PluginClass<T>) CACHE.computeIfAbsent(name, it -> new PluginClass<>(it, clazz.get()));
    }

    /**
     * @return the wrapped class
     */
    public Class<T> clazz() {
        return this.clazz;
    }

    /**
     * Gets the cached method with the name, or wraps the method from the supplier and caches it.
     *
     * @param method the cache key of the method in this class
     * @param supplier gives the method on the first call
     * @param <U> the return type of the method
     * @return the method
     */
    @SuppressWarnings("unchecked")
    public <U> PluginMethod<U> method(String method, Supplier<Method> supplier) {
        return (PluginMethod<U>) this.methods.computeIfAbsent(method, it -> PluginMethod.of(this.name + "#" + it, supplier));
    }

    /**
     * Gets the cached field with the name, or wraps the field from the supplier and caches it.
     *
     * @param field the cache key of the field in this class
     * @param supplier gives the field on the first call
     * @param <U> the type of the field
     * @return the field
     */
    @SuppressWarnings("unchecked")
    public <U> PluginField<U> field(String field, Supplier<Field> supplier) {
        return (PluginField<U>) this.fields.computeIfAbsent(field, it -> PluginField.of(this.name + "#" + it, supplier));
    }
}
