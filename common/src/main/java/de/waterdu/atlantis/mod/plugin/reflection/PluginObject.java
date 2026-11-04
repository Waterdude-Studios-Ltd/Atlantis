package de.waterdu.atlantis.mod.plugin.reflection;

/**
 * Holds an instance of a plugin class together with its {@link PluginClass}.
 *
 * @param <T> the type of the instance
 */
public class PluginObject<T> {

    private final PluginClass<T> clazz;
    private final T instance;

    /**
     * Creates an object and gets the cached {@link PluginClass} of the runtime class of the instance.
     *
     * @param instance the instance. If it is {@code null}, the class is also {@code null}.
     */
    @SuppressWarnings("unchecked")
    public PluginObject(T instance) {
        this.instance = instance;
        this.clazz = instance == null
                ? null
                : PluginClass.of(instance.getClass().getName(), () -> (Class<T>) instance.getClass());
    }

    /**
     * Creates an object with a known class.
     *
     * @param clazz the class of the instance
     * @param instance the instance
     */
    public PluginObject(PluginClass<T> clazz, T instance) {
        this.clazz = clazz;
        this.instance = instance;
    }

    /**
     * @return the class of the instance
     */
    public PluginClass<T> clazz() {
        return this.clazz;
    }

    /**
     * @return the instance
     */
    public T instance() {
        return this.instance;
    }
}
