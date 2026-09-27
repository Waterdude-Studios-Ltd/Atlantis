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
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.meta.ConfigEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.file.storage.Storage;
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

    public Config(AtlantisConfig parent, String modID, String key, Gson gson, T configuration,
                  Class<T> clazz, Storage storage, boolean forceLoad, boolean lang, boolean aon, LoadOrder order) {
        this.parent = parent;
        this.owner = modID;
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

    protected void init(String path) throws SQLException, InvalidClassException, NoSuchFieldException {
        this.storage.init(this, this.owner, this.key, path, this.gson, this.cc.get(), this.classes);
    }

    public ConfigurationContainer<T> container() {
        return this.cc;
    }

    public Map<UUID, T> uuidDataMap() {
        return this.uuidDataMap;
    }

    public Map<String, T> filenameDataMap() {
        return this.filenameDataMap;
    }

    public String dataFileExtension() {
        return this.aon ? FileExtensions.AON : FileExtensions.JSON;
    }

    public boolean isAON() {
        return this.aon;
    }

    public LoadOrder order() {
        return this.order;
    }

    public Map<String, T> nameDataMap() {
        return this.nameDataMap;
    }

    public String key() {
        return this.key;
    }

    public AtlantisConfig parent() {
        return this.parent;
    }

    public String owner() {
        return this.owner;
    }

    public String name() {
        return this.owner + ":" + this.key;
    }

    public Gson gson() {
        return this.gson;
    }

    public String filename(Data data) {
        return data.identifier() + ".json";
    }

    public Storage storage() {
        return this.storage;
    }

    public boolean isData() {
        return this.cc.isData();
    }

    public boolean isPlayerData() {
        return this.cc.isPlayerData();
    }

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

    private void addScheduledTask(final Consumer<Collection<T>> consumer) {
        this.scheduledTasks.add(consumer);
    }

    public void addPutListener(final Predicate<T> listener) {
        this.putListeners.add(listener);
    }

    public void addAfterLoadTask(final Runnable task) {
        this.afterLoadTasks.add(task);
    }

    public boolean onPut(T data) {
        var result = false;
        for (var putListener : this.putListeners) {
            result |= putListener.test(data);
        }
        return result;
    }

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

    public void clearAll() {
        this.dataSet.clear();
        this.uuidDataMap.clear();
        this.nameDataMap.clear();
        this.filenameDataMap.clear();
    }

    public T get() {
        return this.cc.get();
    }

    public ClassHolder<T> classes() {
        return this.classes;
    }

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

    public Set<T> all() {
        this.lock.lock();
        var set = new HashSet<>(this.dataSet);
        this.lock.unlock();
        return set;
    }

    public T get(String name) {
        return this.nameDataMap.get(name);
    }

    public T get(Player player) {
        return this.uuidDataMap.get(player.getUUID());
    }

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

    public void evict(Player player) {
        this.lock.lock();
        try {
            if (player != null) {
                var uuid = AtlantisConfig.copyUUID(player.getUUID());
                this.dataSet.remove(this.uuidDataMap.remove(uuid));
                this.filenameDataMap.remove(uuid + ".json");
                this.nameDataMap.remove(player.getName().getString());
            }
        } finally {
            this.lock.unlock();
        }
    }

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

    public boolean put(T data) {
        if (data.getClass().equals(this.classes.dataClass().orElse(null))) {
            this.putUnsafe(data, true);
            return true;
        }
        return false;
    }

    public CompletableFuture<T> remove(UUID uuid) {
        return this.storage.deleteFromUUID(this, uuid).whenComplete((data, err) -> {
            Mods.modLoader().event(UEvent.DELETE_CONFIG).emit(new ConfigEventPayload.Delete(this, data));
            this.evict(data);
        });
    }

    public CompletableFuture<T> remove(Player player) {
        return this.storage.deleteFromPlayer(this, player).whenComplete((data, err) -> {
            Mods.modLoader().event(UEvent.DELETE_CONFIG).emit(new ConfigEventPayload.Delete(this, data));
            this.evict(data);
        });
    }

    public CompletableFuture<T> remove(String name) {
        return this.storage.deleteFromName(this, name).whenComplete((data, err) -> {
            Mods.modLoader().event(UEvent.DELETE_CONFIG).emit(new ConfigEventPayload.Delete(this, data));
            this.evict(data);
        });
    }

    public CompletableFuture<Boolean> read() {
        return this.read(false);
    }

    public CompletableFuture<Boolean> read(boolean skipNulls) {
        return this.storage.read(this, skipNulls).whenComplete((data, err) -> {
            this.afterLoadTasks.forEach(Runnable::run);
            Mods.modLoader().event(UEvent.READ_CONFIG).emit(new ConfigEventPayload.Read(this, this.cc.get()));
            ConfigurationHolder.invalidateOf(this.classes.getConfigurationClass());
        });
    }

    public CompletableFuture<T> readPlayerData(UUID uuid) {
        return this.storage.readFromUUID(this, uuid).whenComplete((data, err) -> Mods.modLoader().event(UEvent.READ_CONFIG).emit(new ConfigEventPayload.Read(this, data)));
    }

    public CompletableFuture<T> readPlayerData(Player player) {
        return this.storage.readFromPlayer(this, player).whenComplete((data, err) -> Mods.modLoader().event(UEvent.READ_CONFIG).emit(new ConfigEventPayload.Read(this, data)));
    }

    public CompletableFuture<T> readNamedData(String name) {
        return this.storage.readFromName(this, name).whenComplete((data, err) -> Mods.modLoader().event(UEvent.READ_CONFIG).emit(new ConfigEventPayload.Read(this, data)));
    }

    public CompletableFuture<Boolean> write() {
        return this.write(null);
    }

    public CompletableFuture<Boolean> write(T specific) {
        return this.write(specific, false);
    }

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

    public boolean shouldForceLoad() {
        return this.forceLoad;
    }

    public boolean isLang() {
        return this.lang;
    }

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

    public boolean markSaved(UUID uuid) {
        return this.filesToSave.remove(uuid);
    }

    public Message message(String key) {
        return this.langMap.computeIfAbsent(key.toLowerCase(), unused -> Message.empty());
    }

    public Text format(String key, Object... args) {
        return this.message(key).get(false, false, args);
    }

    protected void destruct() {
        this.storage.destruct(this);
    }

    @SuppressWarnings("unchecked")
    public void asyncSave() {
        var uuids = new HashSet<>(this.filesToSave);
        this.filesToSave.clear();
        this.classes().playerDataClass().ifPresent(clazz -> {
            var delay = Settings.get().getDataSaveDelay() * 2;
            for (var uuid : uuids) {
                Atlantis.THREAD_POOL.schedule(() -> {
                    var future = (CompletableFuture<T>) AtlantisConfig.getFor(this.owner(), clazz, uuid);
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
        });
    }
}
