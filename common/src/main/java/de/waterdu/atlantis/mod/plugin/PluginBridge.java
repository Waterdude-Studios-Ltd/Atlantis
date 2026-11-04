package de.waterdu.atlantis.mod.plugin;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.mod.plugin.reflection.PluginField;
import de.waterdu.atlantis.mod.plugin.reflection.PluginMethod;
import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gives reflective access to the classes, fields and methods of Bukkit plugins on a hybrid server. Create instances
 * with {@link PluginBridgeFactory}.
 */
public class PluginBridge {

    private final JavaPlugin plugin;
    private final List<?> loaders;
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, PluginField<?>>> fields = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, PluginMethod<?>>> methods = new ConcurrentHashMap<>();

    /**
     * Creates a bridge for the plugin and reads the class loaders of all plugins from its plugin loader.
     *
     * @param name the name of the plugin
     * @throws IllegalPluginAccessException if the plugin is not loaded and enabled
     */
    protected PluginBridge(String name) throws NoSuchMethodException, NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        if (!canMake(name)) {
            throw new IllegalPluginAccessException("No such enabled plugin: " + name);
        }

        this.plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin(name);
        var method = JavaPlugin.class.getDeclaredMethod("getPluginLoader");
        method.setAccessible(true);
        var pluginLoader = (JavaPluginLoader) method.invoke((Object) this.plugin, new Object[0]);
        var field = pluginLoader.getClass().getDeclaredField("loaders");
        field.setAccessible(true);
        this.loaders = (List<?>) field.get(pluginLoader);
    }

    /**
     * @return {@code true} if the plugin is loaded and enabled
     */
    public static boolean canMake(String name) {
        var plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Searches the class loaders of all plugins and caches the result.
     */
    public Optional<Class<?>> findClass(String classpath) {
        return Optional.ofNullable(this.classes.computeIfAbsent(classpath, s -> {
            for (var o : this.loaders) {
                try {
                    var loader = (URLClassLoader) o;
                    var c = loader.loadClass(classpath);
                    if (c == null) {
                        continue;
                    }
                    return c;
                } catch (Exception e) {
                    AtlantisLogger.error("Failed to find class {} in plugin {}: {}", classpath, this.plugin, e.getMessage());
                }
            }
            return null;
        }));
    }

    /**
     * Finds a field that the class declares itself. Inherited fields are not found.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<PluginField<T>> findField(Class<?> clazz, String field) {
        return Optional.ofNullable((PluginField<T>) this.fields.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).computeIfAbsent(field, s -> {
            try {
                return PluginField.of(clazz.getName() + "#" + s, () -> {
                    try {
                        return clazz.getDeclaredField(s);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                AtlantisLogger.error("Failed to find field {} in plugin {}: {}", field, this.plugin, e.getMessage());
                return null;
            }
        }));
    }

    /**
     * Uses the method name as the cache identifier. Use {@link #findMethod(Class, String, String, Class[])} for overloads.
     */
    public <T> Optional<PluginMethod<T>> findMethod(Class<?> clazz, String method, Class<?>... parameters) {
        return this.findMethod(clazz, method, method, parameters);
    }

    /**
     * Finds a method that the class declares itself. Inherited methods are not found.
     *
     * @param identifier the cache key for the method. Give each overload its own identifier.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<PluginMethod<T>> findMethod(Class<?> clazz, String identifier, String method, Class<?>... parameters) {
        return Optional.ofNullable((PluginMethod<T>) this.methods.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>()).computeIfAbsent(identifier, s -> {
            try {
                return PluginMethod.of(clazz.getName() + "#" + identifier, () -> {
                    try {
                        return clazz.getDeclaredMethod(method, parameters);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                AtlantisLogger.error("Failed to find method {} in plugin {}: {}", identifier, this.plugin, e.getMessage());
                return null;
            }
        }));
    }
}

