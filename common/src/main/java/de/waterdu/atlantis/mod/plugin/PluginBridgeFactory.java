package de.waterdu.atlantis.mod.plugin;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates and caches a {@link PluginBridge} for each plugin. Also registers a {@link Key} getter for plugin bridges.
 */
public class PluginBridgeFactory {

    private static final Map<Key<PluginBridge>, PluginBridge> BRIDGES = new ConcurrentHashMap<>();

    static {
        Key.registerGetter(PluginBridge.class, PluginBridgeFactory::make);
    }

    private PluginBridgeFactory() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * @return the cached bridge for the plugin, or {@code null} if the plugin is not enabled
     */
    public static @Nullable PluginBridge make(String plugin) {
        var key = Key.<PluginBridge>of(plugin.toLowerCase());
        return make(key, plugin);
    }

    /**
     * Gets the bridge for the plugin that the path of the key names.
     *
     * @param key the key of the bridge. Its path is the plugin name.
     * @return the cached bridge for the plugin, or {@code null} if the plugin is not enabled
     */
    public static @Nullable PluginBridge make(Key<PluginBridge> key) {
        return make(key, key.key().getPath());
    }

    /**
     * Gets the cached bridge for the key, or creates and caches a new bridge for the plugin. Logs the error if the
     * bridge cannot be created.
     *
     * @param key the cache key of the bridge
     * @param plugin the name of the plugin
     * @return the bridge, or {@code null} if it cannot be created
     */
    private static @Nullable PluginBridge make(Key<PluginBridge> key, String plugin) {
        if (BRIDGES.containsKey(key)) {
            return BRIDGES.get(key);
        }

        try {
            var bridge = new PluginBridge(plugin);
            BRIDGES.put(key, bridge);
            return bridge;
        } catch (Exception e) {
            AtlantisLogger.error("Failed to create new plugin bridge for plugin {}: {}", plugin, e.getMessage());
            return null;
        }
    }
}

