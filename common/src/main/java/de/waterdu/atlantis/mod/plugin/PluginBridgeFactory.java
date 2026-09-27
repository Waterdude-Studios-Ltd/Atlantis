package de.waterdu.atlantis.mod.plugin;

import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.java.ExceptionUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PluginBridgeFactory {

    private static final Map<Key<PluginBridge>, PluginBridge> BRIDGES = new ConcurrentHashMap<>();

    static {
        Key.registerGetter(PluginBridge.class, key -> make(key).orElse(null));
    }

    private PluginBridgeFactory() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    // TODO: Vault bridge.

    public static Optional<PluginBridge> make(String plugin) {
        var key = Key.<PluginBridge>of(plugin);
        return make(key, plugin);
    }

    public static Optional<PluginBridge> make(Key<PluginBridge> key) {
        return make(key, key.key().getPath());
    }

    private static Optional<PluginBridge> make(Key<PluginBridge> key, String plugin) {
        if (BRIDGES.containsKey(key)) {
            return Optional.ofNullable(BRIDGES.get(key));
        }

        try {
            var bridge = new PluginBridge(plugin);
            BRIDGES.put(key, bridge);
            return Optional.of(bridge);
        } catch (Exception e) {
            AtlantisLogger.error("Failed to create new plugin bridge for plugin {}: {}", plugin, e.getMessage());
            return Optional.empty();
        }
    }
}

