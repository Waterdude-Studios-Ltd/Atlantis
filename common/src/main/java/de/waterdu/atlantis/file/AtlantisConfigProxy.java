package de.waterdu.atlantis.file;

import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import de.waterdu.atlantis.playerdata.UUIDNameCache;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class AtlantisConfigProxy {

    private static final Map<String, Impl> IMPLEMENTATIONS = new ConcurrentHashMap<>();

    private AtlantisConfigProxy() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static Impl of(String modID) {
        return AtlantisConfigProxy.IMPLEMENTATIONS.computeIfAbsent(modID, Impl::new);
    }

    public record Impl(AtlantisConfig internal, Map<Class<?>, String> canonicalNameCache) {

        private Impl(String modID) {
            this(AtlantisConfig.of(modID), new ConcurrentHashMap<>());
        }

        private String canonicalName(Class<?> clazz) {
            return this.canonicalNameCache.computeIfAbsent(clazz, Class::getCanonicalName);
        }
        
        public <T extends Configuration> Config<T> config(Class<T> clazz) {
            return this.internal.config(this.canonicalName(clazz));
        }

        public void registerTypeAdapters(Consumer<GsonBuilder> consumer) {
            this.internal.registerTypeAdapters(consumer);
        }

        @SuppressWarnings("unchecked")
        public <T extends Configuration> void addPutListener(Class<? extends T> clazz, final Predicate<T> listener) {
            ((Config<T>) this.config(clazz)).addPutListener(listener);
        }

        public <T extends Configuration> boolean contains(Class<? extends T> clazz, UUID uuid) {
            return this.config(clazz).uuidDataMap().containsKey(uuid);
        }

        public <T extends Configuration> boolean contains(Class<? extends T> clazz, String name) {
            return this.config(clazz).nameDataMap().containsKey(name);
        }

        public <T extends Configuration> boolean contains(Class<? extends T> clazz, T data) {
            return this.config(clazz).all().contains(data);
        }

        public <T extends Configuration> T get(Class<? extends T> clazz) {
            if (!this.internal.ready.get()) {
                return this.immediate(clazz);
            }
            return this.config(clazz).get();
        }

        @SuppressWarnings("unchecked")
        public <T extends Configuration> T immediate(Class<? extends T> clazz) {
            var config = (Config<T>) this.config(clazz);
            try {
                var result = config.read(true).get();
                if (!result) {
                    config.write();
                }
                config.remapLang();
                return config.get();
            } catch (ExecutionException | InterruptedException e) {
                AtlantisLogger.error("Failed immediate read of {}: {}", config.name(), e.getMessage());
                return null;
            }
        }

        @ApiStatus.Internal
        public <T extends Configuration> T forcedImmediate(Class<? extends T> clazz, Class<? extends Annotation> annotation, Consumer<T> applyDefaults) {
            if (this.internal.ready.get()) {
                return this.immediate(clazz);
            }

            AtlantisConfigFinder.tryRegister(clazz.getAnnotation(annotation), clazz);
            AtlantisConfigFinder.init(false);
            var config = this.immediate(clazz);
            applyDefaults.accept(config);
            AtlantisConfig.markUnready(Atlantis.ID);
            return config;
        }

        public <T extends Configuration> T orElse(Class<? extends T> clazz, T orElse) {
            if (this.internal == null || !this.internal.ready.get()) {
                return orElse;
            }
            var result = this.config(clazz).get();
            return result == null ? orElse : result;
        }

        @SuppressWarnings("unchecked")
        public <T extends Configuration> T orElse(T orElse) {
            return (T) this.orElse(orElse.getClass(), orElse);
        }

        public <T extends Configuration> Collection<T> all(Class<? extends T> clazz) {
            return this.internal.<T>config(this.canonicalName(clazz)).all();
        }

        public <T extends Configuration> boolean isEmpty(Class<T> clazz) {
            return this.all(clazz).isEmpty();
        }

        public void enqueue(final Runnable task) {
            this.internal.enqueue(task);
        }

        public <T extends Configuration> void enqueue(Class<T> clazz, Consumer<Collection<T>> task) {
            this.config(clazz).runTask(task);
        }

        public <T extends Configuration> void put(Class<? extends T> clazz, T instance) {
            this.internal.config(this.canonicalName(clazz)).put(instance);
        }

        public <T extends Configuration> void put(T instance) {
            this.put(instance.getClass(), instance);
        }

        public <T extends Configuration> Message message(Class<T> clazz, String key) {
            var config = this.config(clazz);
            if (config.isLang()) {
                return config.message(key);
            }
            return Message.empty();
        }

        public <T extends Configuration> Text format(Class<T> clazz, String key, Object... args) {
            var config = this.config(clazz);
            if (config.isLang()) {
                return config.format(key, args);
            }
            return Text.empty();
        }

        public <T extends Configuration> Text formatPositive(Class<T> clazz, String key, Object... args) {
            var config = this.config(clazz);
            if (config.isLang()) {
                return config.format("prefixPositive").append(config.format(key, args));
            }
            return Text.empty();
        }

        public <T extends Configuration> Text formatNeutral(Class<T> clazz, String key, Object... args) {
            var config = this.config(clazz);
            if (config.isLang()) {
                return config.format("prefixNeutral").append(config.format(key, args));
            }
            return Text.empty();
        }

        public <T extends Configuration> Text formatNegative(Class<T> clazz, String key, Object... args) {
            var config = this.config(clazz);
            if (config.isLang()) {
                return config.format("prefixNegative").append(config.format(key, args));
            }
            return Text.empty();
        }

        public <T extends Configuration> CompletableFuture<T> get(Class<T> clazz, UUID uuid) {
            return this.config(clazz).get(uuid);
        }

        public <T extends Configuration> T now(Class<T> clazz, UUID uuid) {
            return this.config(clazz).now(uuid);
        }

        public <T extends Configuration> CompletableFuture<T> getForOrPut(Class<T> clazz, UUID uuid, Supplier<T> orElse) {
            var config = this.config(clazz);
            return this.config(clazz).get(AtlantisConfig.copyUUID(uuid)).thenApply(instance -> {
                if (instance == null) {
                    instance = orElse.get();
                    config.put(instance);
                    instance.save();
                }
                return instance;
            });
        }

        public <T extends Configuration> CompletableFuture<T> remove(Class<T> clazz, PlayerReference player) {
            return this.remove(clazz, player.uuid());
        }

        public <T extends Configuration> CompletableFuture<T> remove(Class<T> clazz, ServerPlayer player) {
            return this.remove(clazz, player.getUUID());
        }

        public <T extends Configuration> CompletableFuture<T> remove(Class<T> clazz, UUID uuid) {
            return this.config(clazz).remove(AtlantisConfig.copyUUID(uuid));
        }

        public <T extends Configuration> CompletableFuture<T> remove(Class<T> clazz, String name) {
            return this.config(clazz).remove(name);
        }

        public <T extends Configuration> CompletableFuture<T> get(Class<T> clazz, String name) {
            var config = this.config(clazz);
            if (config.isPlayerData()) {
                return config.get(UUIDNameCache.get(name));
            } else {
                return CompletableFuture.completedFuture(config.nameDataMap().get(name));
            }
        }

        public <T extends Configuration> T now(Class<T> clazz, String name) {
            var config = this.config(clazz);
            if (config.isPlayerData()) {
                return config.now(UUIDNameCache.get(name));
            } else {
                return config.nameDataMap().get(name);
            }
        }

        public <T extends Configuration> CompletableFuture<T> get(Class<T> clazz, PlayerReference player) {
            return this.config(clazz).get(player.uuid());
        }

        public <T extends Configuration> T now(Class<T> clazz, PlayerReference player) {
            return this.now(clazz, player.uuid());
        }

        public <T extends Configuration> CompletableFuture<T> get(Class<T> clazz, ServerPlayer player) {
            return this.config(clazz).get(player.getUUID());
        }

        public <T extends Configuration> T now(Class<T> clazz, ServerPlayer player) {
            return this.now(clazz, player.getUUID());
        }

        public  <T extends Configuration> void load(Class<T> clazz) {
            this.internal.config(this.canonicalName(clazz)).read();
        }

        public  <T extends Configuration> void save(final Class<T> clazz) {
            Atlantis.THREAD_POOL.submit(() -> this.internal.config(this.canonicalName(clazz)).write());
        }

        public <T extends Configuration> void save(final T data) {
            this.save(data, false);
        }

        public <T extends Configuration> void save(final T data, final boolean immediate) {
            Runnable writeTask = () -> this.internal.config(this.canonicalName(data.getClass())).write(data, immediate);
            if (data instanceof PlayerData) {
                writeTask.run();
            } else {
                Atlantis.THREAD_POOL.submit(writeTask);
            }
        }

        public void loadAll(DataType type) {
            this.forEachConfig(type, Config::read);
        }

        public void saveAll(final DataType type) {
            Atlantis.THREAD_POOL.submit(() -> this.forEachConfig(type, Config::write));
        }

        private void forEachConfig(DataType type, Consumer<Config<?>> consumer) {
            var configuration = type == DataType.ALL || type == DataType.CONFIGURATION;
            var data = type == DataType.ALL || type == DataType.ALL_DATA || type == DataType.DATA;
            var playerData = type == DataType.ALL || type == DataType.ALL_DATA || type == DataType.PLAYER_DATA;
            for (var config : this.internal.configs.values()) {
                if (
                        (!(config.get() instanceof Data) && configuration) ||
                        (!(config.get() instanceof PlayerData) && config.get() instanceof Data && data) ||
                        (config.get() instanceof PlayerData && playerData)
                ) {
                    consumer.accept(config);
                }
            }
        }
    }
}
