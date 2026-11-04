package de.waterdu.atlantis.file;

import com.google.common.annotations.VisibleForTesting;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.util.java.interfaces.ResettableLazy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Caches the configuration instance of one configuration type.
 *
 * @param <C> the configuration type
 */
public class ConfigurationHolder<C extends Configuration> {

    private static final Map<Class<?>, ConfigurationHolder<?>> HOLDERS = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final ResettableLazy<C> instance;

    /**
     * Creates a holder that gets the configuration from {@link AtlantisConfigProxy} on first use.
     *
     * @param modId the ID of the mod that owns the configuration
     * @param type the configuration class
     */
    private ConfigurationHolder(String modId, Class<C> type) {
        this.instance = ResettableLazy.concurrentOf(() -> AtlantisConfigProxy.of(modId).get(type));
    }

    /**
     * Removes all holders.
     */
    @VisibleForTesting
    public static void resetForTesting() {
        HOLDERS.clear();
    }

    /**
     * @param type must be registered. See {@link AtlantisConfig#findOwnerMod(Class)}.
     */
    public static <C extends Configuration> ConfigurationHolder<C> of(Class<C> type) {
        return ConfigurationHolder.of(AtlantisConfig.findOwnerMod(type), type);
    }

    /**
     * There is one holder for each type.
     *
     * @param modId used only when the first holder for the type is created
     */
    @SuppressWarnings("unchecked")
    public static <C extends Configuration> ConfigurationHolder<C> of(String modId, Class<C> type) {
        return (ConfigurationHolder<C>) HOLDERS.computeIfAbsent(type, c -> new ConfigurationHolder<>(modId, (Class<C>) c));
    }

    /**
     * Clears the cached configuration of the holder for the type, then gets it again. Does nothing if the type has no
     * holder.
     *
     * @param type the configuration class
     */
    protected static void invalidateOf(Class<?> type) {
        var holder = HOLDERS.get(type);
        if (holder != null) {
            holder.invalidate();
            holder.get();
        }
    }

    /**
     * @return the cached configuration. Each read of the config from storage refreshes it.
     */
    public C get() {
        this.lock.lock();
        this.lock.unlock();
        return this.instance.get();
    }

    /**
     * Saves the cached configuration. See {@link Configuration#save()}.
     */
    public void save() {
        this.get().save();
    }

    /**
     * Clears the cached configuration. The next {@link #get()} gets it again.
     */
    public void invalidate() {
        this.lock.lock();
        this.instance.reset();
        this.lock.unlock();
    }
}
