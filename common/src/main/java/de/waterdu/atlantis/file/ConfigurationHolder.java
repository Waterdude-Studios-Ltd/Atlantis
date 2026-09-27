package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.util.java.interfaces.ResettableLazy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ConfigurationHolder<C extends Configuration> {

    private static final Map<Class<?>, ConfigurationHolder<?>> HOLDERS = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final ResettableLazy<C> instance;

    private ConfigurationHolder(String modid, Class<C> type) {
        this.instance = ResettableLazy.concurrentOf(() -> AtlantisConfigProxy.of(modid).get(type));
    }

    public static <C extends Configuration> ConfigurationHolder<C> of(Class<C> type) {
        return ConfigurationHolder.of(AtlantisConfig.findOwnerMod(type), type);
    }

    @SuppressWarnings("unchecked")
    public static <C extends Configuration> ConfigurationHolder<C> of(String modid, Class<C> type) {
        return (ConfigurationHolder<C>) HOLDERS.computeIfAbsent(type, c -> new ConfigurationHolder<>(modid, (Class<C>) c));
    }

    protected static void invalidateOf(Class<?> type) {
        var holder = HOLDERS.get(type);
        if (holder != null) {
            holder.invalidate();
            holder.get();
        }
    }

    public C get() {
        this.lock.lock();
        this.lock.unlock();
        return this.instance.get();
    }

    public void save() {
        this.get().save();
    }

    public void invalidate() {
        this.lock.lock();
        this.instance.reset();
        this.lock.unlock();
    }
}
