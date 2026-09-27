package de.waterdu.atlantis.mod.plugin.reflection;

public class PluginObject<T> {

    private final PluginClass<T> clazz;
    private final T instance;

    @SuppressWarnings("unchecked")
    public PluginObject(T instance) {
        this.instance = instance;
        this.clazz = instance == null
                ? null
                : PluginClass.of(instance.getClass().getName(), () -> (Class<T>) instance.getClass());
    }

    public PluginObject(PluginClass<T> clazz, T instance) {
        this.clazz = clazz;
        this.instance = instance;
    }

    public PluginClass<T> clazz() {
        return this.clazz;
    }

    public T instance() {
        return this.instance;
    }
}
