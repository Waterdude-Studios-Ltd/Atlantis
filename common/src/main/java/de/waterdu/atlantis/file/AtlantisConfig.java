package de.waterdu.atlantis.file;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.auto.LoadOrder;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import de.waterdu.atlantis.file.storage.Storage;
import de.waterdu.atlantis.file.typeadapters.InterfaceTypeAdapter;
import de.waterdu.atlantis.file.typeadapters.NumberListTypeAdapter;
import de.waterdu.atlantis.file.typeadapters.ResourceLocationTypeAdapter;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.meta.ConfigEventPayload;
import de.waterdu.atlantis.mod.impl.common.event.meta.GatherTypeAdaptersEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.permission.AtlantisPermission;
import de.waterdu.atlantis.ui.api.Layout;
import de.waterdu.atlantis.ui.api.LayoutGroup;
import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
import de.waterdu.atlantis.util.bootstrap.Entrypoint;
import de.waterdu.atlantis.util.codec.ResourceOrTagKey;
import de.waterdu.atlantis.util.config.Configurable;
import de.waterdu.atlantis.util.config.DatabaseSettings;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.config.Leaderboard;
import de.waterdu.atlantis.util.config.RedisSettings;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.item.ConfigurableItemStack;
import de.waterdu.atlantis.util.java.DateTimePattern;
import de.waterdu.atlantis.util.java.DecimalPattern;
import de.waterdu.atlantis.util.java.Range;
import de.waterdu.atlantis.util.java.Timezone;
import de.waterdu.atlantis.util.java.math.Formula;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.level.LocalPosition;
import de.waterdu.atlantis.util.server.Command;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Holds the configs of one mod, its config directory and its {@link Gson}. The static methods find the instance of a
 * mod by its ID. Most code uses {@link Atlantis#CONFIG} instead.
 */
@Bootstrapped(Bootstrapped.Priority.HIGH)
public final class AtlantisConfig {

    private static final Map<String, AtlantisConfig> INSTANCES = new ConcurrentHashMap<>();
    private static final Map<Class<?>, String> CLASS_TO_MOD = new ConcurrentHashMap<>();
    private static final List<Config<? extends PlayerData>> PLAYER_CONFIGS = new ArrayList<>();
    private static final Map<Type, Object> TYPE_ADAPTERS = new HashMap<>();
    
    static {
        TYPE_ADAPTERS.put(Configuration.class, new InterfaceTypeAdapter<Configuration>());
        TYPE_ADAPTERS.put(Data.class, new InterfaceTypeAdapter<Data>());
        TYPE_ADAPTERS.put(NamedData.class, new InterfaceTypeAdapter<NamedData>());
        TYPE_ADAPTERS.put(PlayerData.class, new InterfaceTypeAdapter<PlayerData>());
        TYPE_ADAPTERS.put(ResourceLocation.class, new ResourceLocationTypeAdapter());
        TYPE_ADAPTERS.put(Text.class, new Text.TypeAdapter());
        TYPE_ADAPTERS.put(Text.Immutable.class, new Text.Immutable.TypeAdapter());
        TYPE_ADAPTERS.put(LevelPosition.class, new LevelPosition.TypeAdapter());
        TYPE_ADAPTERS.put(LocalPosition.class, new LocalPosition.TypeAdapter());
        TYPE_ADAPTERS.put(ConfigurableLevel.class, new ConfigurableLevel.TypeAdapter());
        TYPE_ADAPTERS.put(LayoutGroup.class, new LayoutGroup.TypeAdapter());
        TYPE_ADAPTERS.put(Layout.class, new Layout.TypeAdapter());
        TYPE_ADAPTERS.put(Layout.Data.class, new Layout.Data.TypeAdapter());
        TYPE_ADAPTERS.put(Formula.class, new Formula.TypeAdapter());
        TYPE_ADAPTERS.put(ConfigurableItemStack.class, new ConfigurableItemStack.TypeAdapter());
        TYPE_ADAPTERS.put(RedisSettings.class, new RedisSettings.TypeAdapter());
        TYPE_ADAPTERS.put(RedisSettings.URL.class, new RedisSettings.TypeAdapter());
        TYPE_ADAPTERS.put(RedisSettings.Explicit.class, new RedisSettings.TypeAdapter());
        TYPE_ADAPTERS.put(DatabaseSettings.class, new DatabaseSettings.TypeAdapter());
        TYPE_ADAPTERS.put(DatabaseSettings.URL.class, new DatabaseSettings.TypeAdapter());
        TYPE_ADAPTERS.put(DatabaseSettings.Explicit.class, new DatabaseSettings.TypeAdapter());
        TYPE_ADAPTERS.put(AtlantisPermission.class, new AtlantisPermission.TypeAdapter());
        TYPE_ADAPTERS.put(Message.class, new Message.TypeAdapter());
        TYPE_ADAPTERS.put(Timezone.class, new Timezone.TypeAdapter());
        TYPE_ADAPTERS.put(Leaderboard.class, new Leaderboard.TypeAdapter());
        TYPE_ADAPTERS.put(DateTimePattern.class, new DateTimePattern.TypeAdapter());
        TYPE_ADAPTERS.put(PlayerReference.class, new PlayerReference.TypeAdapter());
        TYPE_ADAPTERS.put(DecimalPattern.class, new DecimalPattern.TypeAdapter());
        TYPE_ADAPTERS.put(Command.class, new Command.TypeAdapter());
        TYPE_ADAPTERS.put(Key.class, new Key.TypeAdapter());
        TYPE_ADAPTERS.put(ResourceOrTagKey.class, new ResourceOrTagKey.TypeAdapter());
        for (var adapter : NumberListTypeAdapter.ADAPTERS) {
            TYPE_ADAPTERS.put(adapter.type(), adapter);
        }
        for (var adapter : Range.TypeAdapter.ADAPTERS) {
            TYPE_ADAPTERS.put(adapter.type(), adapter);
        }
    }

    private final GsonBuilder gsonBuilder;
    private Gson gson = null;

    private final Lock lock = new ReentrantLock(true);
    private final Lock readyLock = new ReentrantLock(true);

    private final List<Runnable> scheduledTasks = new ArrayList<>();

    AtomicBoolean ready = new AtomicBoolean(false);

    final Map<String, Config<? extends Configuration>> configs = new ConcurrentHashMap<>();
    private File dir;

    /**
     * Creates the {@link GsonBuilder} with the default settings, the Atlantis type adapters, the type adapters of the
     * mod proxies and the default {@link Configurable} contexts.
     */
    private AtlantisConfig() {
        this.gsonBuilder = new GsonBuilder().setPrettyPrinting()
                .disableHtmlEscaping()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .setLenient()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);

        for (var entry : TYPE_ADAPTERS.entrySet()) {
            this.gsonBuilder.registerTypeAdapter(entry.getKey(), entry.getValue());
        }

        Mods.forEachProxy(proxy -> proxy.registerTypeAdapters(this.gsonBuilder));
        Configurable.registerDefaultContexts(this.gsonBuilder);
    }

    /**
     * Collects the type adapters that listeners add with {@link GatherTypeAdaptersEventPayload}, and rebuilds the
     * {@link Gson} of each instance.
     */
    @Entrypoint
    @SuppressWarnings("unused")
    private static void bootstrap() {
        GatherTypeAdaptersEventPayload.retrieve(TYPE_ADAPTERS);
        for (var entry : INSTANCES.entrySet()) {
            entry.getValue().rebuildGson();
        }
    }

    /**
     * Changes the {@link GsonBuilder} and then rebuilds the {@link Gson}.
     *
     * @param scope changes the builder
     */
    private void gsonBuilder(Consumer<GsonBuilder> scope) {
        scope.accept(gsonBuilder);
        rebuildGson();
    }

    /**
     * Adds type adapters to the {@link Gson} of the mod.
     * Configs that are already registered keep their old {@link Gson}. Call this before the configs of the mod
     * register.
     */
    public static void registerTypeAdapters(String mod, Consumer<GsonBuilder> consumer) {
        AtlantisConfig.of(mod).registerTypeAdapters(consumer);
    }

    /**
     * Adds type adapters to the {@link Gson} of the mod.
     * Configs that are already registered keep their old {@link Gson}. Call this before the configs of the mod
     * register.
     */
    public void registerTypeAdapters(Consumer<GsonBuilder> consumer) {
        this.gsonBuilder(consumer);
    }

    /**
     * Builds the {@link Gson} if it is not built yet.
     */
    public void ensureGsonBuilt() {
        if (this.gson == null) {
            this.rebuildGson();
        }
    }

    /**
     * Builds a new {@link Gson} from the current {@link GsonBuilder}.
     */
    public void rebuildGson() {
        this.gson = this.gsonBuilder.create();
    }

    /**
     * @return the {@link Gson} of the mod. It is built on the first call.
     */
    public Gson gson() {
        this.ensureGsonBuilt();
        return this.gson;
    }

    /**
     * Creates a new instance for the mod and replaces the old instance.
     *
     * @param mod the mod ID
     */
    @SuppressWarnings("all")
    @ApiStatus.Internal
    static void preRegistration(String mod) {
        INSTANCES.put(mod, preRegistration0(mod));
    }

    /**
     * Creates a new instance for the mod and creates its config directory. The instance is not stored.
     *
     * @param mod the mod ID
     * @return the new instance
     */
    @SuppressWarnings("all")
    @ApiStatus.Internal
    static AtlantisConfig preRegistration0(String mod) {
        var instance = new AtlantisConfig();
        instance.dir = AtlantisConfig.resolveDir(mod);
        return instance;
    }

    /**
     * Creates the config directory of the loader and the directory of the mod in it, if they do not exist.
     *
     * @param mod the mod ID
     * @return the config directory of the mod
     */
    private static File resolveDir(String mod) {
        var all = Mods.modLoader().configDir().toFile();
        if (all.mkdir()) {
            AtlantisLogger.info("Created config directory");
        }
        var dir = new File(all, mod);
        dir.mkdir();
        return dir;
    }

    /**
     * Removes the configs, the scheduled tasks and the directory of each instance, and marks each instance as not
     * ready. The instances stay registered.
     */
    @VisibleForTesting
    public static void resetForTesting() {
        for (var instance : INSTANCES.values()) {
            instance.configs.clear();
            instance.scheduledTasks.clear();
            instance.ready.set(false);
            instance.dir = null;
        }
        CLASS_TO_MOD.clear();
        PLAYER_CONFIGS.clear();
    }

    /**
     * Reads the configuration from storage and blocks until the read completes. If storage has no value, writes the
     * default configuration.
     *
     * @param mod the mod ID
     * @param clazz the configuration class
     * @param <T> the configuration type
     * @return the configuration, or {@code null} if the read fails
     */
    @SuppressWarnings("unchecked")
    @ApiStatus.Internal
    static <T extends Configuration> T getDuringRegistration(String mod, Class<? extends T> clazz) {
        var config = AtlantisConfig.of(mod).configs.get(clazz.getCanonicalName());
        try {
            var result = config.read(true).get();
            if (!result) {
                config.write();
            }
            config.remapLang();
            return (T) config.get();
        } catch (ExecutionException | InterruptedException e) {
            AtlantisLogger.error("Failed immediate read of {}: {}", config.name(), e.getMessage());
            return null;
        }
    }

    /**
     * Starts to read each config of the mod in load order, and writes each config after its read. Player data is
     * read only if it is force loaded. Then marks the mod as ready.
     *
     * @param modId the mod ID
     */
    @ApiStatus.Internal
    static void postRegistration(String modId) {
        var instance = AtlantisConfig.of(modId);
        var configs = Lists.newArrayList(instance.configs.values());
        configs.sort(Comparator.comparingInt(config -> config.order().ordinal()));
        for (final var config : configs) {
            if (!config.isPlayerData() || config.shouldForceLoad()) {
                config.read(true).whenComplete((r, err) -> config.runTask(() -> config.write()));
            }
        }
        if (instance.readyLock.tryLock()) {
            instance.ready.set(true);
            instance.readyLock.unlock();
        }
    }

    /**
     * Marks the mod as not ready and calls {@link AtlantisConfigFinder#restoreFromBackup()}, so that the configs can
     * register again.
     *
     * @param mod the mod ID
     */
    @ApiStatus.Internal
    public static void markUnready(String mod) {
        var instance = AtlantisConfig.of(mod);
        if (instance.readyLock.tryLock()) {
            instance.ready.set(false);
            AtlantisConfigFinder.restoreFromBackup();
            instance.readyLock.unlock();
        }
    }

    /**
     * @return {@code true} after all configs of the mod register. The reads that start then can still be in progress.
     */
    public boolean ready() {
        return this.ready.get();
    }

    /**
     * Gets the instance of the mod, and creates it if it does not exist.
     *
     * @param mod the mod ID
     * @return the instance of the mod
     */
    static AtlantisConfig of(String mod) {
        return AtlantisConfig.INSTANCES.computeIfAbsent(mod, key -> AtlantisConfig.preRegistration0(mod));
    }

    /**
     * @return the instances of all mods
     */
    public static Collection<AtlantisConfig> allInstances() {
        return AtlantisConfig.INSTANCES.values();
    }

    /**
     * Gets the config directory of the mod, and creates it if it is not known yet.
     *
     * @param mod the mod ID
     * @return the config directory of the mod
     */
    public static File dirOf(String mod) {
        var instance = AtlantisConfig.of(mod);
        if (instance.dir == null) {
            instance.dir = AtlantisConfig.resolveDir(mod);
        }
        return instance.dir;
    }
    
    /**
     * Creates a {@link Config} for the configuration class and adds it to the mod. Fires
     * {@link UEvent#REGISTER_CONFIG}, whose listeners can change the path. Logs an error if the config fails to
     * initialize.
     *
     * @param mod the mod ID
     * @param path the path of the file or directory, relative to the config directory of the mod
     * @param clazz the configuration class
     * @param instance the default configuration instance
     * @param storage the storage that reads and writes the configuration
     * @param forceLoad reads player data from storage when the configs of the mod load
     * @param lang maps the fields of the configuration to messages
     * @param aon writes the files in the AON format instead of JSON
     * @param order the load order relative to the other configs of the mod
     * @param <T> the configuration type
     */
    @SuppressWarnings("unchecked")
    @ApiStatus.Internal
    static <T extends Configuration> void register(
            String mod,
            String path,
            Class<? extends Configuration> clazz,
            Configuration instance,
            Storage storage,
            boolean forceLoad,
            boolean lang,
            boolean aon,
            LoadOrder order
    ) {
        try {
            var config = AtlantisConfig.of(mod);
            var specific = new Config<>(config, mod, clazz.getCanonicalName(), config.gson(), (T) instance, (Class<T>) clazz, storage, forceLoad, lang, aon, order);
            var event = Mods.modLoader().event(UEvent.REGISTER_CONFIG).emit(new ConfigEventPayload.Register(specific, path));
            specific.init(event.path());
            config.configs.put(clazz.getCanonicalName(), specific);
            AtlantisConfig.CLASS_TO_MOD.put(clazz, mod);
            if (specific.isPlayerData()) {
                AtlantisConfig.PLAYER_CONFIGS.add((Config<? extends PlayerData>) specific);
            }
        } catch (Exception e) {
            AtlantisLogger.error("Failed to instantiate new {}:{} instance: {}", mod, clazz.getSimpleName(), e.getMessage());
        }
    }

    /**
     * Gets the configuration. If the mod is not ready, reads it from storage with
     * {@link #getDuringRegistration(String, Class)}.
     *
     * @param mod the mod ID
     * @param clazz the configuration class
     * @param <T> the configuration type
     * @return the configuration
     */
    @ApiStatus.Internal
    static <T extends Configuration> T get(String mod, Class<? extends T> clazz) {
        var instance = AtlantisConfig.of(mod);
        if (!instance.ready.get()) {
            return AtlantisConfig.getDuringRegistration(mod, clazz);
        }
        var config = instance.<T>config(clazz.getCanonicalName());
        return config.get();
    }

    /**
     * Gets the configuration if the mod is ready.
     *
     * @param mod the mod ID
     * @param clazz the configuration class
     * @param orElse the value to return if the mod is not ready or the configuration is {@code null}
     * @param <T> the configuration type
     * @return the configuration, or {@code orElse}
     */
    @ApiStatus.Internal
    static <T extends Configuration> T getOrElse(String mod, Class<? extends T> clazz, T orElse) {
        var instance = AtlantisConfig.of(mod);
        if (instance == null || !instance.ready.get()) {
            return orElse;
        }
        var result = instance.<T>config(clazz.getCanonicalName()).get();
        return result == null ? orElse : result;
    }

    /**
     * @param mod the mod ID
     * @param clazz the data class
     * @param <T> the data type
     * @return a copy of all instances in memory
     */
    @ApiStatus.Internal
    static <T extends Configuration> Collection<T> getAll(String mod, Class<? extends T> clazz) {
        return AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName()).all();
    }

    /**
     * Runs the task now if the configs of the mod are ready. If not, the task runs when they are ready.
     *
     * @param mod the mod ID
     * @param task the task to run
     */
    @ApiStatus.Internal
    static void enqueue(final String mod, final Runnable task) {
        AtlantisConfig.of(mod).enqueue(task);
    }

    /**
     * Runs the task with {@link Config#runTask(Consumer)}.
     *
     * @param mod the mod ID
     * @param clazz the configuration class
     * @param task receives a copy of all instances
     * @param <T> the configuration type
     */
    @ApiStatus.Internal
    static <T extends Configuration> void enqueue(String mod, Class<? extends T> clazz, Consumer<Collection<T>> task) {
        AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName()).runTask(task);
    }

    /**
     * Puts the instance in memory with {@link Config#put(Configuration)}.
     *
     * @param mod the mod ID
     * @param clazz the data class
     * @param instance the instance to put
     * @param <T> the data type
     */
    @ApiStatus.Internal
    static <T extends Configuration> void put(String mod, Class<? extends T> clazz, T instance) {
        AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName()).put(instance);
    }

    /**
     * Formats a message of an i18n config.
     *
     * @param mod the mod ID
     * @param clazz the i18n class
     * @param key the Java field name in the i18n class
     * @param args the values for {@code %1}, {@code %2} and so on
     * @param <T> the i18n type
     * @return the formatted message, or empty text if the config is not an i18n config
     */
    @ApiStatus.Internal
    static <T extends Configuration> Text format(String mod, Class<T> clazz, String key, Object... args) {
        var config = AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName());
        if (config.isLang()) {
            return config.format(key, args);
        }
        return Text.empty();
    }

    /**
     * Loads the player data from storage if it is not in memory.
     *
     * @param mod the mod ID
     * @param clazz the player data class
     * @param uuid the UUID of the player
     * @param <T> the player data type
     * @return a future of the instance, or of {@code null} if there is none
     */
    @ApiStatus.Internal
    static <T extends Configuration> CompletableFuture<T> getFor(String mod, Class<? extends T> clazz, UUID uuid) {
        return AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName()).get(AtlantisConfig.copyUUID(uuid));
    }

    /**
     * Loads the player data from storage if it is not in memory. If there is none, puts and saves the instance from
     * {@code orElse}.
     *
     * @param mod the mod ID
     * @param clazz the player data class
     * @param uuid the UUID of the player
     * @param orElse supplies a new instance
     * @param <T> the player data type
     * @return a future of the existing or the new instance
     */
    @ApiStatus.Internal
    static <T extends Configuration> CompletableFuture<T> getForOrPut(String mod, Class<? extends T> clazz, UUID uuid, Supplier<T> orElse) {
        var config = AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName());
        return config.get(AtlantisConfig.copyUUID(uuid)).thenApply((instance) -> {
            if (instance == null) {
                instance = orElse.get();
                config.put(instance);
                instance.save();
            }
            return instance;
        });
    }

    /**
     * Removes the player data with {@link Config#remove(UUID)}.
     *
     * @param mod the mod ID
     * @param clazz the player data class
     * @param uuid the UUID of the player
     * @param <T> the player data type
     * @return the future that {@link Config#remove(UUID)} returns
     */
    @ApiStatus.Internal
    static <T extends Configuration> CompletableFuture<T> remove(String mod, Class<? extends T> clazz, UUID uuid) {
        return AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName()).remove(AtlantisConfig.copyUUID(uuid));
    }

    /**
     * Removes the named data with {@link Config#remove(String)}.
     *
     * @param mod the mod ID
     * @param clazz the named data class
     * @param name the name of the instance
     * @param <T> the named data type
     * @return the future that {@link Config#remove(String)} returns
     */
    @ApiStatus.Internal
    static <T extends Configuration> CompletableFuture<T> remove(String mod, Class<? extends T> clazz, String name) {
        return AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName()).remove(name);
    }

    /**
     * Gets the named data from memory.
     *
     * @param mod the mod ID
     * @param clazz the named data class
     * @param name the name of the instance
     * @param <T> the named data type
     * @return the instance, or {@code null} if it is not in memory
     */
    @ApiStatus.Internal
    static <T extends Configuration> T getFor(String mod, Class<? extends T> clazz, String name) {
        return AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName()).get(name);
    }

    /**
     * Gets the player data from memory.
     *
     * @param mod the mod ID
     * @param clazz the player data class
     * @param player the player
     * @param <T> the player data type
     * @return the instance, or {@code null} if it is not in memory
     */
    @ApiStatus.Internal
    static <T extends Configuration> T getFor(String mod, Class<? extends T> clazz, Player player) {
        return AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName()).get(player);
    }

    /**
     * Starts to read the config from storage.
     *
     * @param mod the mod ID
     * @param clazz the configuration class
     * @param <T> the configuration type
     */
    @ApiStatus.Internal
    static <T extends Configuration> void load(String mod, Class<? extends T> clazz) {
        AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName()).read();
    }

    /**
     * Writes the whole config on {@link Atlantis#THREAD_POOL}.
     *
     * @param mod the mod ID
     * @param clazz the configuration class
     * @param <T> the configuration type
     */
    @ApiStatus.Internal
    static <T extends Configuration> void save(final String mod, final Class<? extends T> clazz) {
        Atlantis.THREAD_POOL.submit(() -> AtlantisConfig.of(mod).<T>config(clazz.getCanonicalName()).write());
    }

    /**
     * Writes the instance without {@code immediate}. Player data waits for the next async save.
     *
     * @param mod the mod ID
     * @param data the instance to write
     * @param <T> the configuration type
     */
    @ApiStatus.Internal
    static <T extends Configuration> void save(final String mod, final T data) {
        AtlantisConfig.save(mod, data, false);
    }

    /**
     * Writes the instance with {@link Config#write(Configuration, boolean)}. Player data is written on the current
     * thread, and other types on {@link Atlantis#THREAD_POOL}.
     *
     * @param mod the mod ID
     * @param data the instance to write
     * @param immediate writes player data now. If not set, player data waits for the next async save.
     * @param <T> the configuration type
     */
    @ApiStatus.Internal
    static <T extends Configuration> void save(final String mod, final T data, final boolean immediate) {
        Runnable writeTask = () -> AtlantisConfig.of(mod).<T>config(data.getClass().getName()).write(data, immediate);
        if (data instanceof PlayerData) {
            writeTask.run();
        } else {
            Atlantis.THREAD_POOL.submit(writeTask);
        }
    }

    /**
     * Starts to read each config of the mod that matches the type.
     *
     * @param mod the mod ID
     * @param type selects the configs to read
     */
    @ApiStatus.Internal
    static void loadAll(String mod, DataType type) {
        for (var config : AtlantisConfig.of(mod).configs.values()) {
            if (type.isValid(config)) {
                config.read();
            }
        }
    }

    /**
     * Writes each config of the mod that matches the type, on {@link Atlantis#THREAD_POOL}.
     *
     * @param mod the mod ID
     * @param type selects the configs to write
     */
    @ApiStatus.Internal
    static void saveAll(final String mod, final DataType type) {
        Atlantis.THREAD_POOL.submit(() -> {
            for (var config : AtlantisConfig.of(mod).configs.values()) {
                if (type.isValid(config)) {
                    config.write();
                }
            }
        });
    }

    /**
     * @param key the canonical name of the configuration class
     * @return the config. Throws {@link NullPointerException} if no config has this key.
     */
    @SuppressWarnings("unchecked")
    public <T extends Configuration> Config<T> config(String key) {
        return Objects.requireNonNull((Config<T>) this.configs.get(key), "Invalid configuration type " + key);
    }

    /**
     * Lets the storage of each config of the mod release its resources.
     */
    public void destruct() {
        for (var config : this.configs.values()) {
            config.destruct();
        }
    }

    /**
     * @param uuid the UUID to copy
     * @return a new UUID with the same value
     */
    static UUID copyUUID(UUID uuid) {
        return new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    /**
     * @return the mod ID, or {@code null} if the class is not registered
     */
    public static String findOwnerMod(Class<? extends Configuration> clazz) {
        return AtlantisConfig.CLASS_TO_MOD.get(clazz);
    }

    /**
     * Tells the storage of each player data config that the player joined.
     *
     * @param player the player that joined
     */
    public static void onPlayerJoin(ServerPlayer player) {
        for (var config : AtlantisConfig.PLAYER_CONFIGS) {
            config.storage().onPlayerJoin(config, player);
        }
    }

    /**
     * Tells the storage of each player data config that the player left.
     *
     * @param player the player that left
     */
    public static void onPlayerLeave(ServerPlayer player) {
        for (var config : AtlantisConfig.PLAYER_CONFIGS) {
            config.storage().onPlayerLeave(config, player);
        }
    }

    /**
     * Writes the queued player data of all mods with {@link #asyncSaveInner()}.
     */
    static void asyncSave() {
        for (var instance : AtlantisConfig.INSTANCES.values()) {
            instance.asyncSaveInner();
        }
    }

    /**
     * Writes the queued player data of each config of the mod with {@link Config#asyncSave()}.
     */
    public void asyncSaveInner() {
        for (var config : this.configs.values()) {
            config.asyncSave();
        }
    }

    /**
     * Runs the task now if the configs of the mod are ready. If not, schedules it. The scheduled tasks run on
     * {@link Atlantis#THREAD_POOL} when the configs are ready, in the order that they were added.
     *
     * @param task the task to run
     */
    void enqueue(final Runnable task) {
        if (this.ready.get() && this.lock.tryLock()) {
            this.lock.unlock();
            task.run();
        } else {
            if (this.scheduledTasks.isEmpty()) {
                var delay = Settings.get().getScheduledTaskDelay();
                this.scheduledTasks.add(task);
                Atlantis.THREAD_POOL.repeatUntilComplete(() -> {
                    if (this.ready.get() && this.lock.tryLock(delay, TimeUnit.MILLISECONDS)) {
                        this.lock.unlock();
                        for (var scheduledTask : this.scheduledTasks) {
                            scheduledTask.run();
                        }
                        this.scheduledTasks.clear();
                        return true;
                    } else {
                        return false;
                    }
                }, delay);
            } else {
                this.scheduledTasks.add(task);
            }
        }
    }
}
