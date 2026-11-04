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

/**
 * Gives access to the configs of a mod by configuration class. {@link Atlantis#CONFIG} is the instance for Atlantis.
 */
public final class AtlantisConfigProxy {

    private static final Map<String, Impl> IMPLEMENTATIONS = new ConcurrentHashMap<>();

    private AtlantisConfigProxy() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Gets the proxy of the mod, and creates it if it does not exist.
     *
     * @param modId the mod ID
     * @return the proxy of the mod
     */
    public static Impl of(String modId) {
        return AtlantisConfigProxy.IMPLEMENTATIONS.computeIfAbsent(modId, Impl::new);
    }

    /**
     * Reads, writes and changes the configs of one mod.
     *
     * @param internal the config registry of the mod
     * @param canonicalNameCache the canonical names of the configuration classes, keyed by class
     */
    public record Impl(AtlantisConfig internal, Map<Class<?>, String> canonicalNameCache) {

        /**
         * Creates the proxy for the config registry of the mod, with an empty name cache.
         *
         * @param modId the mod ID
         */
        private Impl(String modId) {
            this(AtlantisConfig.of(modId), new ConcurrentHashMap<>());
        }

        /**
         * @param clazz the configuration class
         * @return the canonical name of the class, from the cache
         */
        private String canonicalName(Class<?> clazz) {
            return this.canonicalNameCache.computeIfAbsent(clazz, Class::getCanonicalName);
        }
        
        /**
         * @return the config. Throws {@link NullPointerException} if the class is not registered for this mod.
         */
        public <T extends Configuration> Config<T> config(Class<T> clazz) {
            return this.internal.config(this.canonicalName(clazz));
        }

        /**
         * Adds type adapters to the Gson of the mod.
         * Configs that are already registered keep their old Gson. Call this before the configs of the mod register.
         */
        public void registerTypeAdapters(Consumer<GsonBuilder> consumer) {
            this.internal.registerTypeAdapters(consumer);
        }

        /**
         * @param listener returns {@code true} to stop the change. The other listeners still run.
         */
        @SuppressWarnings("unchecked")
        public <T extends Configuration> void addPutListener(Class<? extends T> clazz, final Predicate<T> listener) {
            ((Config<T>) this.config(clazz)).addPutListener(listener);
        }

        /**
         * Checks only the instances in memory.
         */
        public <T extends Configuration> boolean contains(Class<? extends T> clazz, UUID uuid) {
            return this.config(clazz).uuidDataMap().containsKey(uuid);
        }

        /**
         * Checks only the instances in memory.
         */
        public <T extends Configuration> boolean contains(Class<? extends T> clazz, String name) {
            return this.config(clazz).nameDataMap().containsKey(name);
        }

        /**
         * Checks if the instance is in memory.
         *
         * @param clazz the data class
         * @param data the instance to find
         * @param <T> the data type
         * @return {@code true} if the config holds an equal instance
         */
        public <T extends Configuration> boolean contains(Class<? extends T> clazz, T data) {
            return this.config(clazz).all().contains(data);
        }

        /**
         * If the configs of the mod are not ready, reads from storage and blocks. See {@link #immediate(Class)}.
         */
        public <T extends Configuration> T get(Class<? extends T> clazz) {
            if (!this.internal.ready.get()) {
                return this.immediate(clazz);
            }
            return this.config(clazz).get();
        }

        /**
         * Reads from storage and blocks until the read completes. If storage has no value, writes the defaults.
         *
         * @return the configuration, or {@code null} if the read fails with an exception
         */
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

        /**
         * Reads a configuration before the configs of Atlantis register. If they are not ready, registers the class and
         * the found configs of Atlantis, reads the class with {@link #immediate(Class)} and applies the defaults. Then
         * marks Atlantis as not ready again, so that the configs register as usual later. If they are ready, only
         * calls {@link #immediate(Class)}.
         *
         * @param clazz the configuration class
         * @param annotation the config annotation on the class
         * @param applyDefaults changes the configuration after the read. It does not run if the configs are ready.
         * @param <T> the configuration type
         * @return the configuration, or {@code null} if the read fails
         */
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

        /**
         * @return {@code orElse} if the configs of the mod are not ready or the configuration is {@code null}
         */
        public <T extends Configuration> T orElse(Class<? extends T> clazz, T orElse) {
            if (this.internal == null || !this.internal.ready.get()) {
                return orElse;
            }
            var result = this.config(clazz).get();
            return result == null ? orElse : result;
        }

        /**
         * Gets the configuration of the class of {@code orElse}.
         *
         * @param orElse the value to return if the configs of the mod are not ready or the configuration is
         *     {@code null}
         * @param <T> the configuration type
         * @return the configuration, or {@code orElse}
         */
        @SuppressWarnings("unchecked")
        public <T extends Configuration> T orElse(T orElse) {
            return (T) this.orElse(orElse.getClass(), orElse);
        }

        /**
         * @param clazz the data class
         * @param <T> the data type
         * @return a copy of all instances in memory
         */
        public <T extends Configuration> Collection<T> all(Class<? extends T> clazz) {
            return this.internal.<T>config(this.canonicalName(clazz)).all();
        }

        /**
         * Checks only the instances in memory.
         *
         * @param clazz the data class
         * @param <T> the data type
         * @return {@code true} if the config holds no instances
         */
        public <T extends Configuration> boolean isEmpty(Class<T> clazz) {
            return this.all(clazz).isEmpty();
        }

        /**
         * Runs the task now if the configs of the mod are ready. If not, the task runs on {@link Atlantis#THREAD_POOL}
         * when they are ready.
         */
        public void enqueue(final Runnable task) {
            this.internal.enqueue(task);
        }

        /**
         * Runs the task now if the configs of the mod are ready. If not, the task runs on {@link Atlantis#THREAD_POOL}
         * when they are ready.
         *
         * @param task receives a copy of all instances
         */
        public <T extends Configuration> void enqueue(Class<T> clazz, Consumer<Collection<T>> task) {
            this.config(clazz).runTask(task);
        }

        /**
         * Replaces an instance that has the same UUID.
         * Does nothing if the class of the instance is not exactly the data class of the config.
         */
        public <T extends Configuration> void put(Class<? extends T> clazz, T instance) {
            this.internal.config(this.canonicalName(clazz)).put(instance);
        }

        /**
         * Puts the instance in the config of its own class. Replaces an instance that has the same UUID.
         *
         * @param instance the instance to put
         * @param <T> the data type
         */
        public <T extends Configuration> void put(T instance) {
            this.put(instance.getClass(), instance);
        }

        /**
         * @param key the Java field name in the i18n class, not the name in the file. Case does not matter.
         * @return the shared message. An empty message if the config is not an i18n file.
         */
        public <T extends Configuration> Message message(Class<T> clazz, String key) {
            var config = this.config(clazz);
            if (config.isLang()) {
                return config.message(key);
            }
            return Message.empty();
        }

        /**
         * @param key the Java field name in the i18n class, not the name in the file. Case does not matter.
         * @param args the values for {@code %1}, {@code %2} and so on
         * @return empty text if the config is not an i18n file
         */
        public <T extends Configuration> Text format(Class<T> clazz, String key, Object... args) {
            var config = this.config(clazz);
            if (config.isLang()) {
                return config.format(key, args);
            }
            return Text.empty();
        }

        /**
         * Same as {@link #format(Class, String, Object...)}, with the {@code prefixPositive} message in front.
         */
        public <T extends Configuration> Text formatPositive(Class<T> clazz, String key, Object... args) {
            var config = this.config(clazz);
            if (config.isLang()) {
                return config.format("prefixPositive").append(config.format(key, args));
            }
            return Text.empty();
        }

        /**
         * Same as {@link #format(Class, String, Object...)}, with the {@code prefixNeutral} message in front.
         */
        public <T extends Configuration> Text formatNeutral(Class<T> clazz, String key, Object... args) {
            var config = this.config(clazz);
            if (config.isLang()) {
                return config.format("prefixNeutral").append(config.format(key, args));
            }
            return Text.empty();
        }

        /**
         * Same as {@link #format(Class, String, Object...)}, with the {@code prefixNegative} message in front.
         */
        public <T extends Configuration> Text formatNegative(Class<T> clazz, String key, Object... args) {
            var config = this.config(clazz);
            if (config.isLang()) {
                return config.format("prefixNegative").append(config.format(key, args));
            }
            return Text.empty();
        }

        /**
         * Loads player data from storage if it is not in memory.
         *
         * @return a future of the instance, or of {@code null} if there is none
         */
        public <T extends Configuration> CompletableFuture<T> get(Class<T> clazz, UUID uuid) {
            return this.config(clazz).get(uuid);
        }

        /**
         * Loads player data from storage if it is not in memory, and blocks until storage returns it.
         *
         * @return the instance, or {@code null} if there is none
         */
        public <T extends Configuration> T now(Class<T> clazz, UUID uuid) {
            return this.config(clazz).now(uuid);
        }

        /**
         * Loads player data from storage if it is not in memory. If there is none, puts and saves the instance from
         * {@code orElse}.
         *
         * @param clazz the player data class
         * @param uuid the UUID of the player
         * @param orElse supplies a new instance
         * @param <T> the player data type
         * @return a future of the existing or the new instance
         */
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

        /**
         * Removes the player data of the player with {@link #remove(Class, UUID)}.
         *
         * @param clazz the player data class
         * @param player the player
         * @param <T> the player data type
         * @return the future that {@link Config#remove(UUID)} returns
         */
        public <T extends Configuration> CompletableFuture<T> remove(Class<T> clazz, PlayerReference player) {
            return this.remove(clazz, player.uuid());
        }

        /**
         * Removes the player data of the player with {@link #remove(Class, UUID)}.
         *
         * @param clazz the player data class
         * @param player the player
         * @param <T> the player data type
         * @return the future that {@link Config#remove(UUID)} returns
         */
        public <T extends Configuration> CompletableFuture<T> remove(Class<T> clazz, ServerPlayer player) {
            return this.remove(clazz, player.getUUID());
        }

        /**
         * Removes the player data with {@link Config#remove(UUID)}.
         *
         * @param clazz the player data class
         * @param uuid the UUID of the player
         * @param <T> the player data type
         * @return the future that {@link Config#remove(UUID)} returns
         */
        public <T extends Configuration> CompletableFuture<T> remove(Class<T> clazz, UUID uuid) {
            return this.config(clazz).remove(AtlantisConfig.copyUUID(uuid));
        }

        /**
         * Removes the named data with {@link Config#remove(String)}.
         *
         * @param clazz the named data class
         * @param name the name of the instance
         * @param <T> the named data type
         * @return the future that {@link Config#remove(String)} returns
         */
        public <T extends Configuration> CompletableFuture<T> remove(Class<T> clazz, String name) {
            return this.config(clazz).remove(name);
        }

        /**
         * For player data, gets the UUID from {@link UUIDNameCache} and loads the data from storage if necessary.
         * For other data, checks only the instances in memory.
         */
        public <T extends Configuration> CompletableFuture<T> get(Class<T> clazz, String name) {
            var config = this.config(clazz);
            if (config.isPlayerData()) {
                return config.get(UUIDNameCache.get(name));
            } else {
                return CompletableFuture.completedFuture(config.nameDataMap().get(name));
            }
        }

        /**
         * Same as {@link #get(Class, String)}, but blocks until storage returns the player data.
         */
        public <T extends Configuration> T now(Class<T> clazz, String name) {
            var config = this.config(clazz);
            if (config.isPlayerData()) {
                return config.now(UUIDNameCache.get(name));
            } else {
                return config.nameDataMap().get(name);
            }
        }

        /**
         * Loads the player data from storage if it is not in memory.
         *
         * @param clazz the player data class
         * @param player the player
         * @param <T> the player data type
         * @return a future of the instance, or of {@code null} if there is none
         */
        public <T extends Configuration> CompletableFuture<T> get(Class<T> clazz, PlayerReference player) {
            return this.config(clazz).get(player.uuid());
        }

        /**
         * Loads the player data from storage if it is not in memory, and blocks until storage returns it.
         *
         * @param clazz the player data class
         * @param player the player
         * @param <T> the player data type
         * @return the instance, or {@code null} if there is none
         */
        public <T extends Configuration> T now(Class<T> clazz, PlayerReference player) {
            return this.now(clazz, player.uuid());
        }

        /**
         * Loads the player data from storage if it is not in memory.
         *
         * @param clazz the player data class
         * @param player the player
         * @param <T> the player data type
         * @return a future of the instance, or of {@code null} if there is none
         */
        public <T extends Configuration> CompletableFuture<T> get(Class<T> clazz, ServerPlayer player) {
            return this.config(clazz).get(player.getUUID());
        }

        /**
         * Loads the player data from storage if it is not in memory, and blocks until storage returns it.
         *
         * @param clazz the player data class
         * @param player the player
         * @param <T> the player data type
         * @return the instance, or {@code null} if there is none
         */
        public <T extends Configuration> T now(Class<T> clazz, ServerPlayer player) {
            return this.now(clazz, player.getUUID());
        }

        /**
         * Starts a read from storage. Returns before the read completes.
         */
        public  <T extends Configuration> void load(Class<T> clazz) {
            this.internal.config(this.canonicalName(clazz)).read();
        }

        /**
         * Writes the whole config on {@link Atlantis#THREAD_POOL}.
         *
         * @param clazz the configuration class
         * @param <T> the configuration type
         */
        public  <T extends Configuration> void save(final Class<T> clazz) {
            Atlantis.THREAD_POOL.submit(() -> this.internal.config(this.canonicalName(clazz)).write());
        }

        /**
         * Writes the instance without {@code immediate}. Player data waits for the async autosave or for the player to
         * log out.
         *
         * @param data the instance to write
         * @param <T> the configuration type
         */
        public <T extends Configuration> void save(final T data) {
            this.save(data, false);
        }

        /**
         * Other data always writes on {@link Atlantis#THREAD_POOL}.
         *
         * @param immediate writes player data now. If not set, player data waits for the async autosave or for the
         *     player to log out.
         */
        public <T extends Configuration> void save(final T data, final boolean immediate) {
            Runnable writeTask = () -> this.internal.config(this.canonicalName(data.getClass())).write(data, immediate);
            if (data instanceof PlayerData) {
                writeTask.run();
            } else {
                Atlantis.THREAD_POOL.submit(writeTask);
            }
        }

        /**
         * Starts to read each config of the mod that matches the type.
         *
         * @param type selects the configs to read
         */
        public void loadAll(DataType type) {
            this.forEachConfig(type, Config::read);
        }

        /**
         * Writes each config of the mod that matches the type, on {@link Atlantis#THREAD_POOL}.
         *
         * @param type selects the configs to write
         */
        public void saveAll(final DataType type) {
            Atlantis.THREAD_POOL.submit(() -> this.forEachConfig(type, Config::write));
        }

        /**
         * Runs the action for each config of the mod that matches the type.
         *
         * @param type selects the configs
         * @param consumer the action to run for each config
         */
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
