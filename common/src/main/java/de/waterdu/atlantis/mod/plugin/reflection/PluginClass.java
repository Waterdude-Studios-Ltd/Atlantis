package de.waterdu.atlantis.mod.plugin.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PluginClass<T> {

    private static final Map<String, PluginClass<?>> CACHE = new ConcurrentHashMap<>();

    private final String name;
    private final Class<T> clazz;

    private final Map<String, PluginMethod<?>> methods = new ConcurrentHashMap<>();
    private final Map<String, PluginField<?>> fields = new ConcurrentHashMap<>();

    private PluginClass(String name, Class<T> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    @SuppressWarnings("unchecked")
    public static <T> PluginClass<T> of(String name, Supplier<Class<T>> clazz) {
        return (PluginClass<T>) CACHE.computeIfAbsent(name, it -> new PluginClass<>(it, clazz.get()));
    }

    public Class<T> clazz() {
        return this.clazz;
    }

    @SuppressWarnings("unchecked")
    public <U> PluginMethod<U> method(String method, Supplier<Method> supplier) {
        return (PluginMethod<U>) this.methods.computeIfAbsent(method, it -> PluginMethod.of(this.name + "#" + it, supplier));
    }

    @SuppressWarnings("unchecked")
    public <U> PluginField<U> field(String field, Supplier<Field> supplier) {
        return (PluginField<U>) this.fields.computeIfAbsent(field, it -> PluginField.of(this.name + "#" + it, supplier));
    }
}
