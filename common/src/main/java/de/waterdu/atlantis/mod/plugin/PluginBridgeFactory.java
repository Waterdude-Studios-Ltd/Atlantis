package de.waterdu.atlantis.mod.plugin;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PluginBridgeFactory {

    private static final Map<Key<PluginBridge>, PluginBridge> BRIDGES = new ConcurrentHashMap<>();

    static {
        Key.registerGetter(PluginBridge.class, PluginBridgeFactory::make);
    }

    private PluginBridgeFactory() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static @Nullable PluginBridge make(String plugin) {
        var key = Key.<PluginBridge>of(plugin.toLowerCase());
        return make(key, plugin);
    }

    public static @Nullable PluginBridge make(Key<PluginBridge> key) {
        return make(key, key.key().getPath());
    }

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

