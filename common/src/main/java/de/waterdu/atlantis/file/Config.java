package de.waterdu.atlantis.file;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.auto.LoadOrder;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import de.waterdu.atlantis.file.storage.Storage;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.meta.ConfigEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.playerdata.UUIDNameCache;
import de.waterdu.atlantis.util.text.Text;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Player;

import java.io.InvalidClassException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Holds one registered configuration type of a mod and the instances of it in memory. It reads and writes them
 * through its {@link Storage}.
 *
 * @param <T> the configuration type
 */
public class Config<T extends Configuration> {

    private final Lock lock = new ReentrantLock(true);

    private final AtlantisConfig parent;

    private final ClassHolder<T> classes;

    private final Gson gson;
    private final Storage storage;

    private final String owner;
    private final String key;
    private final ConfigurationContainer<T> cc = new ConfigurationContainer<>();

    private final Set<T> dataSet = Sets.newLinkedHashSet();
    private final Map<UUID, T> uuidDataMap = new ConcurrentHashMap<>();
    private final Map<String, T> nameDataMap = new ConcurrentHashMap<>();
    private final Map<String, T> filenameDataMap = new ConcurrentHashMap<>();

    private final boolean forceLoad;
    private final boolean lang;
    private final Map<String, Message> langMap = new HashMap<>();
    private final boolean aon;

    private final LoadOrder order;

    private final List<Consumer<Collection<T>>> scheduledTasks = new ArrayList<>();

    private final Set<UUID> filesToSave = Sets.newConcurrentHashSet();

    private final List<Predicate<T>> putListeners = new ArrayList<>();
    private final List<Runnable> afterLoadTasks = new ArrayList<>();

    /**
     * Creates a config and sets its configuration.
     *
     * @param parent the config registry of the mod
     * @param modId the ID of the mod that owns the config
     * @param key the canonical name of the configuration class
     * @param gson the Gson instance that serializes the configuration
     * @param configuration the default configuration instance
     * @param clazz the configuration class
     * @param storage the storage that reads and writes the configuration
     * @param forceLoad reads player data from storage when the configs of the mod load
     * @param lang maps the fields of the configuration to messages. See {@link #remapLang()}.
     * @param aon writes data files in the AON format instead of JSON
     * @param order the load order relative to the other configs of the mod
     */
    public Config(
            AtlantisConfig parent,
            String modId,
            String key,
            Gson gson,
            T configuration,
            Class<T> clazz,
            Storage storage,
            boolean forceLoad,
            boolean lang,
            boolean aon,
            LoadOrder order
    ) {
        this.parent = parent;
        this.owner = modId;
        this.gson = gson;
        this.key = key;
        this.cc.set(this, configuration);
        this.classes = new ClassHolder<>(clazz);
        this.forceLoad = forceLoad;
        this.lang = lang;
        this.storage = storage;
        this.aon = aon;
        this.order = order;
    }

    /**
     * Initializes the storage of this config.
     *
     * @param path the path of the file or directory, relative to the config directory of the mod
     * @throws SQLException if the database storage fails to connect or to set up its table
     * @throws InvalidClassException if the storage does not support the configuration type
     * @throws NoSuchFieldException if the storage needs a field that the configuration class does not have
     */
    protected void init(String path) throws SQLException, InvalidClassException, NoSuchFieldException {
        this.storage.init(this, this.owner, this.key, path, this.gson, this.cc.get(), this.classes);
    }

    /**
     * @return the container that holds the configuration instance
     */
    public ConfigurationContainer<T> container() {
        return this.cc;
    }

    /**
     * @return the live map of the instances in memory, by UUID
     */
    public Map<UUID, T> uuidDataMap() {
        return this.uuidDataMap;
    }

    /**
     * @return the live map of the instances in memory, by file name
     */
    public Map<String, T> filenameDataMap() {
        return this.filenameDataMap;
    }

    /**
     * @return {@link FileExtensions#AON} if the config uses the AON format, or {@link FileExtensions#JSON} if not
     */
    public String dataFileExtension() {
        return this.aon ? FileExtensions.AON : FileExtensions.JSON;
    }

    /**
     * @return {@code true} if the config uses the AON format
     */
    public boolean isAON() {
        return this.aon;
    }

    /**
     * @return the load order of this config, relative to the other configs of the mod
     */
    public LoadOrder order() {
        return this.order;
    }

    /**
     * @return the live map of the named instances in memory, by name
     */
    public Map<String, T> nameDataMap() {
        return this.nameDataMap;
    }

    /**
     * @return the canonical name of the configuration class
     */
    public String key() {
        return this.key;
    }

    /**
     * @return the config registry of the mod that owns this config
     */
    public AtlantisConfig parent() {
        return this.parent;
    }

    /**
     * @return the ID of the mod that owns this config
     */
    public String owner() {
        return this.owner;
    }

    /**
     * @return the mod ID and the {@link #key()}, separated by {@code :}
     */
    public String name() {
        return this.owner + ":" + this.key;
    }

    /**
     * @return the Gson instance that serializes the configuration
     */
    public Gson gson() {
        return this.gson;
    }

    /**
     * Gets the file name of the instance, with the data file extension of this config.
     *
     * @param data the instance
     * @return the identifier of the instance followed by {@link #dataFileExtension()}
     */
    public String filename(Data data) {
        return data.identifier() + this.dataFileExtension();
    }

    /**
     * @return the storage that reads and writes this config
     */
    public Storage storage() {
        return this.storage;
    }

    /**
     * @return {@code true} if the configuration implements {@link Data}
     */
    public boolean isData() {
        return this.cc.isData();
    }

    /**
     * @return {@code true} if the configuration implements {@link PlayerData}
     */
    public boolean isPlayerData() {
        return this.cc.isPlayerData();
    }

    /**
     * Runs the task now if the configs of the mod are ready. If not, the task runs on {@link Atlantis#THREAD_POOL} when
     * they are ready.
     */
    public void runTask(final Runnable task) {
        Consumer<Collection<T>> consumer = all -> task.run();
        if (this.parent.ready.get() && this.lock.tryLock()) {
            this.lock.unlock();
            task.run();
        } else {
            if (this.scheduledTasks.isEmpty()) {
                var delay = Settings.get().getScheduledTaskDelay();
                this.scheduledTasks.add(consumer);
                Atlantis.THREAD_POOL.repeatUntilComplete(() -> {
                    if (this.parent.ready.get() && this.lock.tryLock(delay, TimeUnit.MILLISECONDS)) {
                        this.lock.unlock();
                        final var all = this.all();
                        for (var scheduledTask : this.scheduledTasks) {
                            scheduledTask.accept(all);
                        }
                        this.scheduledTasks.clear();
                        return true;
                    } else {
                        return false;
                    }
                }, delay);
            } else {
                this.scheduledTasks.add(consumer);
            }
        }
    }

    /**
     * Adds a task that runs when the configs of the mod are ready. This does not start the scheduler.
     *
     * @param consumer receives a copy of all instances
     */
    private void addScheduledTask(final Consumer<Collection<T>> consumer) {
        this.scheduledTasks.add(consumer);
    }

    /**
     * Every listener runs before an instance is put and before {@link ConfigurationContainer#set} replaces the
     * configuration.
     *
     * @param listener returns {@code true} to stop the change. The other listeners still run.
     */
    public void addPutListener(final Predicate<T> listener) {
        this.putListeners.add(listener);
    }

    /**
     * Adds a task that runs each time {@link #read(boolean)} completes.
     *
     * @param task the task to run
     */
    public void addAfterLoadTask(final Runnable task) {
        this.afterLoadTasks.add(task);
    }

    /**
     * @return {@code true} if a put listener stops the change
     */
    public boolean onPut(T data) {
        var result = false;
        for (var putListener : this.putListeners) {
            result |= putListener.test(data);
        }
        return result;
    }

    /**
     * Runs the task now if the configs of the mod are ready. If not, the task runs on {@link Atlantis#THREAD_POOL} when
     * they are ready.
     *
     * @param task receives a copy of all instances
     */
    public void runTask(final Consumer<Collection<T>> task) {
        if (this.parent.ready.get() && this.lock.tryLock()) {
            this.lock.unlock();
            task.accept(this.all());
        } else {
            if (this.scheduledTasks.isEmpty()) {
                var delay = Settings.get().getScheduledTaskDelay();
                this.addScheduledTask(task);
                Atlantis.THREAD_POOL.repeatUntilComplete(() -> {
                    if (this.parent.ready.get() && this.lock.tryLock(delay, TimeUnit.MILLISECONDS)) {
                        this.lock.unlock();
                        final var all = this.all();
                        for (var scheduledTask : this.scheduledTasks) {
                            scheduledTask.accept(all);
                        }
                        this.scheduledTasks.clear();
                        return true;
                    } else {
                        return false;
                    }
                }, delay);
            } else {
                this.addScheduledTask(task);
            }
        }
    }

    /**
     * Removes all instances from memory. This does not change storage.
     */
    public void clearAll() {
        this.dataSet.clear();
        this.uuidDataMap.clear();
        this.nameDataMap.clear();
        this.filenameDataMap.clear();
    }

    /**
     * @return the configuration instance
     */
    public T get() {
        return this.cc.get();
    }

    /**
     * @return the holder of the configuration class and its related classes
     */
    public ClassHolder<T> classes() {
        return this.classes;
    }

    /**
     * Loads player data from storage if it is not in memory.
     *
     * @param uuid can be null
     * @return a future of the instance, or of {@code null} if there is none
     */
    public CompletableFuture<T> get(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(null);
        }
        var data = this.uuidDataMap.get(uuid);
        if (data == null && this.cc.isPlayerData()) {
            return this.readPlayerData(uuid);
        }
        return CompletableFuture.completedFuture(data);
    }

    /**
     * Loads player data from storage if it is not in memory, and blocks until storage returns it.
     *
     * @param uuid can be null
     * @return the instance, or {@code null} if there is none
     */
    public T now(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        var data = this.uuidDataMap.get(uuid);
        if (data == null && this.cc.isPlayerData()) {
            return this.readPlayerData(uuid).join();
        }
        return data;
    }

    /**
     * Gets a copy of all instances in memory. This waits for the lock of the config.
     *
     * @return a new set of the instances
     */
    public Set<T> all() {
        this.lock.lock();
        var set = new HashSet<>(this.dataSet);
        this.lock.unlock();
        return set;
    }

    /**
     * Checks only the instances in memory.
     */
    public T get(String name) {
        return this.nameDataMap.get(name);
    }

    /**
     * Checks only the instances in memory. Use {@link #get(UUID)} to load player data from storage.
     */
    public T get(Player player) {
        return this.uuidDataMap.get(player.getUUID());
    }

    /**
     * Puts the instance in memory without a type check. A put listener can stop it.
     * For player data, also adds the name to {@link UUIDNameCache}.
     *
     * @param data must implement {@link Data}
     * @param force replaces an instance that has the same UUID. If not set, the existing instance stays.
     */
    public void putUnsafe(T data, boolean force) {
        this.lock.lock();
        try {
            var cast = (Data) data;
            var uuid = AtlantisConfig.copyUUID(cast.uuid());
            if ((force || !this.uuidDataMap.containsKey(uuid)) && !this.onPut(data)) {
                this.dataSet.add(data);
                this.uuidDataMap.put(uuid, data);
                this.filenameDataMap.put(this.filename(cast), data);
                if (data instanceof NamedData namedData) {
                    this.nameDataMap.put(namedData.name(), data);
                    if (data instanceof PlayerData playerData) {
                        if (!playerData.uuid().equals(Util.NIL_UUID) && !playerData.name().isEmpty()) {
                            UUIDNameCache.add(playerData.name(), playerData.uuid());
                        }
                    }
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Removes the instance from memory. This does not change storage.
     *
     * @param data can be null
     */
    public void evict(T data) {
        this.lock.lock();
        try {
            if (data != null) {
                var cast = (Data) data;
                var uuid = AtlantisConfig.copyUUID(cast.uuid());
                this.dataSet.remove(data);
                this.uuidDataMap.remove(uuid);
                this.filenameDataMap.remove(this.filename(cast));
                if (data instanceof NamedData namedData) {
                    this.nameDataMap.remove(namedData.name());
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Removes the data of the player from memory. This does not change storage.
     *
     * @param player can be null
     */
    public void evict(Player player) {
        this.lock.lock();
        try {
            if (player != null) {
                var uuid = AtlantisConfig.copyUUID(player.getUUID());
                var removed = this.uuidDataMap.remove(uuid);
                if (removed != null) {
                    this.dataSet.remove(removed);
                    this.filenameDataMap.remove(this.filename((Data) removed));
                }
                this.nameDataMap.remove(player.getName().getString());
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Moves the instance to its new name and file name in memory. {@link NamedData#setNameAndUpdate(String)} calls
     * this.
     *
     * @param oldFilename the value of {@link #filename(Data)} before the change
     */
    public void updateName(T data, String oldName, String oldFilename) {
        this.lock.lock();
        try {
            if (data instanceof NamedData namedData) {
                this.nameDataMap.remove(oldName);
                this.nameDataMap.put(namedData.name(), data);
                this.filenameDataMap.remove(oldFilename);
                this.filenameDataMap.put(this.filename(namedData), data);
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Puts the instance in memory and replaces an instance that has the same UUID. A put listener can stop it.
     *
     * @return {@code false} if the class of the instance is not exactly the data class
     */
    public boolean put(T data) {
        if (data.getClass().equals(this.classes.dataClass())) {
            this.putUnsafe(data, true);
            return true;
        }
        return false;
    }

    /**
     * Deletes the instance from storage, then fires {@link UEvent#DELETE_CONFIG} and removes it from memory.
     *
     * @param uuid the UUID of the instance
     * @return a future of the deleted instance
     */
    public CompletableFuture<T> remove(UUID uuid) {
        return this.storage.deleteFromUUID(this, uuid).whenComplete((data, err) -> {
            Mods.modLoader().event(UEvent.DELETE_CONFIG).emit(new ConfigEventPayload.Delete(this, data));
            this.evict(data);
        });
    }

    /**
     * Deletes the data of the player from storage, then fires {@link UEvent#DELETE_CONFIG} and removes it from memory.
     *
     * @param player the player
     * @return a future of the deleted instance
     */
    public CompletableFuture<T> remove(Player player) {
        return this.storage.deleteFromPlayer(this, player).whenComplete((data, err) -> {
            Mods.modLoader().event(UEvent.DELETE_CONFIG).emit(new ConfigEventPayload.Delete(this, data));
            this.evict(data);
        });
    }

    /**
     * Deletes the named instance from storage, then fires {@link UEvent#DELETE_CONFIG} and removes it from memory.
     *
     * @param name the name of the instance
     * @return a future of the deleted instance
     */
    public CompletableFuture<T> remove(String name) {
        return this.storage.deleteFromName(this, name).whenComplete((data, err) -> {
            Mods.modLoader().event(UEvent.DELETE_CONFIG).emit(new ConfigEventPayload.Delete(this, data));
            this.evict(data);
        });
    }

    /**
     * Reads from storage. A stored {@code null} replaces the current configuration.
     *
     * @return a future of {@code false} if the storage did not read a value
     */
    public CompletableFuture<Boolean> read() {
        return this.read(false);
    }

    /**
     * Reads from storage. For data types, the storage clears the memory and reads all instances again.
     * Unsaved changes in memory can be lost.
     *
     * @param skipNulls keeps the current configuration if the stored value is {@code null}
     * @return a future of {@code false} if the storage did not read a value
     */
    public CompletableFuture<Boolean> read(boolean skipNulls) {
        return this.storage.read(this, skipNulls).whenComplete((data, err) -> {
            this.afterLoadTasks.forEach(Runnable::run);
            Mods.modLoader().event(UEvent.READ_CONFIG).emit(new ConfigEventPayload.Read(this, this.cc.get()));
            ConfigurationHolder.invalidateOf(this.classes.getConfigurationClass());
        });
    }

    /**
     * Reads the player data from storage, then fires {@link UEvent#READ_CONFIG}.
     *
     * @param uuid the UUID of the player
     * @return a future of the instance
     */
    public CompletableFuture<T> readPlayerData(UUID uuid) {
        return this.storage.readFromUUID(this, uuid).whenComplete((data, err) -> Mods.modLoader().event(UEvent.READ_CONFIG).emit(new ConfigEventPayload.Read(this, data)));
    }

    /**
     * Reads the data of the player from storage, then fires {@link UEvent#READ_CONFIG}.
     *
     * @param player the player
     * @return a future of the instance
     */
    public CompletableFuture<T> readPlayerData(Player player) {
        return this.storage.readFromPlayer(this, player).whenComplete((data, err) -> Mods.modLoader().event(UEvent.READ_CONFIG).emit(new ConfigEventPayload.Read(this, data)));
    }

    /**
     * Reads the named instance from storage, then fires {@link UEvent#READ_CONFIG}.
     *
     * @param name the name of the instance
     * @return a future of the instance
     */
    public CompletableFuture<T> readNamedData(String name) {
        return this.storage.readFromName(this, name).whenComplete((data, err) -> Mods.modLoader().event(UEvent.READ_CONFIG).emit(new ConfigEventPayload.Read(this, data)));
    }

    /**
     * Writes the whole config to storage, then fires {@link UEvent#WRITE_CONFIG}.
     *
     * @return a future of the result of the storage write
     */
    public CompletableFuture<Boolean> write() {
        return this.write(null);
    }

    /**
     * {@link #write(Configuration, boolean)} without {@code immediate}. Player data waits for {@link #asyncSave()}.
     */
    public CompletableFuture<Boolean> write(T specific) {
        return this.write(specific, false);
    }

    /**
     * @param specific the instance to write. If {@code null}, writes the whole config.
     * @param immediate writes player data now. If not set, player data waits for {@link #asyncSave()} or for logout.
     * @return a future of {@code false} if the class of {@code specific} is not the configuration class
     */
    public CompletableFuture<Boolean> write(T specific, boolean immediate) {
        if (specific == null) {
            return this.storage.write(this).whenComplete((b, err) -> Mods.modLoader().event(UEvent.WRITE_CONFIG).emit(new ConfigEventPayload.Write(this, this.cc.get())));
        } else if (specific.getClass().equals(this.classes.getConfigurationClass())) {
            try {
                if (this.isPlayerData() && !immediate) {
                    this.filesToSave.add(specific.uuid());
                    return CompletableFuture.completedFuture(true);
                } else {
                    return this.storage.writeSpecific(this, specific).whenComplete((b, err) -> Mods.modLoader().event(UEvent.WRITE_CONFIG).emit(new ConfigEventPayload.Write(this, specific)));
                }
            } catch (InterruptedException ignored) {}
        }
        return CompletableFuture.completedFuture(false);
    }

    /**
     * @return {@code true} if player data reads from storage when the configs of the mod load
     */
    public boolean shouldForceLoad() {
        return this.forceLoad;
    }

    /**
     * @return {@code true} if the config is an i18n file
     */
    public boolean isLang() {
        return this.lang;
    }

    /**
     * Updates the shared messages from the {@link String} and {@link Message} fields of the configuration. Does
     * nothing if the config is not an i18n file. Logs an error for each field that fails.
     */
    public void remapLang() {
        if (this.lang) {
            for (var field : this.cc.get().getClass().getDeclaredFields()) {
                try {
                    if (field.getType() == String.class) {
                        var value = Text.of((String) field.get(this.cc.get()));
                        this.langMap.computeIfAbsent(field.getName().toLowerCase(), unused -> Message.of(value)).update(value);
                    } else if (field.getType() == Message.class) {
                        var value = (Message) field.get(this.cc.get());
                        this.langMap.computeIfAbsent(field.getName().toLowerCase(), unused -> value).update(value);
                    }
                } catch (Exception e) {
                    AtlantisLogger.error("Failed to map i18n on {}: {}", this.owner, this.classes.simpleName());
                    AtlantisLogger.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Removes the UUID from the player data that waits for {@link #asyncSave()}. This does not write the data.
     *
     * @return {@code true} if the UUID was waiting
     */
    public boolean markSaved(UUID uuid) {
        return this.filesToSave.remove(uuid);
    }

    /**
     * @param key the Java field name in the i18n class, not the name in the file. Case does not matter.
     * @return the shared message. It changes when the file reloads. An empty message if the key is not known.
     */
    public Message message(String key) {
        return this.langMap.computeIfAbsent(key.toLowerCase(), unused -> Message.empty());
    }

    /**
     * @param key the Java field name in the i18n class, not the name in the file. Case does not matter.
     * @param args the values for {@code %1}, {@code %2} and so on
     */
    public Text format(String key, Object... args) {
        return this.message(key).get(false, false, args);
    }

    /**
     * Releases the resources of the storage, for example the database connection pool.
     */
    protected void destruct() {
        this.storage.destruct(this);
    }

    /**
     * Writes the player data that {@link #write(Configuration, boolean)} queued.
     * Each write starts {@link Settings#getDataSaveDelay()} milliseconds after the previous one.
     */
    @SuppressWarnings("unchecked")
    public void asyncSave() {
        var uuids = new HashSet<>(this.filesToSave);
        this.filesToSave.clear();
        var pdc = this.classes().playerDataClass();
        if (pdc != null) {
            var delay = Settings.get().getDataSaveDelay() * 2;
            for (var uuid : uuids) {
                Atlantis.THREAD_POOL.schedule(() -> {
                    var future = (CompletableFuture<T>) AtlantisConfig.getFor(this.owner(), pdc, uuid);
                    future.whenComplete((data, err) -> {
                        if (data != null) {
                            try {
                                this.storage.writeSpecific(this, data);
                            } catch (InterruptedException e) {
                                AtlantisLogger.error("Failed to save instance of {} called {} with UUID {}", this.name(), data.identifier(), data.uuid());
                            }
                        }
                    });
                }, delay);
                delay += Settings.get().getDataSaveDelay();
            }
        }
    }
}
