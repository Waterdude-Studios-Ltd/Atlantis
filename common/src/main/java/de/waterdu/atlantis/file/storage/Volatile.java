package de.waterdu.atlantis.file.storage;

import com.google.gson.Gson;
import de.waterdu.atlantis.file.ClassHolder;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Keeps instances only in memory. Reads and writes do nothing, and deletes only remove the instance from memory.
 */
public class Volatile implements Storage {

    /**
     * Creates a storage that keeps instances only in memory.
     */
    public Volatile() {}

    /**
     * Does nothing, because this storage has nothing to prepare.
     *
     * @param config the config that uses this storage
     * @param modId the ID of the mod that owns the config
     * @param key the key of the config
     * @param path the path of the config
     * @param gson the Gson instance of the config
     * @param configuration the default configuration instance
     * @param classes the classes of the config
     * @param <T> the configuration type
     */
    @Override
    public <T extends Configuration> void init(
            Config<T> config,
            String modId,
            String key,
            String path,
            Gson gson,
            T configuration,
            ClassHolder<T> classes
    ) {}

    /**
     * Does nothing, because this storage holds no resources.
     *
     * @param config the config that uses this storage
     * @param <T> the configuration type
     */
    @Override
    public <T extends Configuration> void destruct(Config<T> config) {}

    /**
     * Writes nothing.
     *
     * @param config the config to write
     * @param <T> the configuration type
     * @return a completed future of {@code true}
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> write(Config<T> config) {
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Writes nothing.
     *
     * @param config the config of the data
     * @param data the data instance
     * @param <T> the configuration type
     * @return a completed future of {@code true}
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> writeSpecific(Config<T> config, T data) {
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Reads nothing.
     *
     * @param config the config to read
     * @param skipNulls ignored
     * @param <T> the configuration type
     * @return a completed future of {@code true}
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> read(Config<T> config, boolean skipNulls) {
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Reads nothing.
     *
     * @param config the config to read
     * @param dir ignored
     * @param names ignored
     * @param <T> the configuration type
     * @return a completed future of {@code true}
     */
    @Override
    public <T extends Configuration> CompletableFuture<Boolean> readAll(Config<T> config, File dir, Set<String> names) {
        return CompletableFuture.completedFuture(true);
    }

    /**
     * Reads nothing.
     *
     * @param config the config of the data
     * @param uuid the UUID of the instance
     * @param <T> the configuration type
     * @return a completed future of {@code null}
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> readFromUUID(Config<T> config, UUID uuid) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Reads nothing.
     *
     * @param config the config of the data
     * @param name the name of the instance
     * @param <T> the configuration type
     * @return a completed future of {@code null}
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> readFromName(Config<T> config, String name) {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Removes the data instance from the UUID, file name and name maps of the config.
     *
     * @param config the config of the data
     * @param data the data instance to remove, or {@code null}
     * @param <T> the configuration type
     * @return a completed future of the instance removed from the UUID map, or of {@code null}
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> delete(Config<T> config, T data) {
        var result = new CompletableFuture<T>();
        if (data == null) {
            result.complete(null);
            return result;
        }
        var removed = config.uuidDataMap().remove(data.uuid());
        config.filenameDataMap().remove(config.filename((Data) data));
        if (data instanceof NamedData namedData) {
            config.nameDataMap().remove(namedData.name());
        }
        result.complete(removed);
        return result;
    }

    /**
     * Removes the data instance with the given UUID from memory with {@link #delete}. The instance must already be
     * loaded.
     *
     * @param config the config of the data
     * @param uuid the UUID of the instance
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if the server is not alive or nothing was removed
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> deleteFromUUID(Config<T> config, UUID uuid) {
        var result = new CompletableFuture<T>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        var data = config.get(uuid);
        if (data != null && data.isDone()) {
            try {
                return this.delete(config, data.get());
            } catch (ExecutionException | InterruptedException ignored) {}
        }
        result.complete(null);
        return result;
    }

    /**
     * Removes the named data instance with the given name from memory with {@link #delete}.
     *
     * @param config the config of the data
     * @param name the name of the instance
     * @param <T> the configuration type
     * @return a future of the removed instance, or of {@code null} if the server is not alive or nothing was removed
     */
    @Override
    public <T extends Configuration> CompletableFuture<T> deleteFromName(Config<T> config, String name) {
        var result = new CompletableFuture<T>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        var data = config.get(name);
        if (data != null) {
            return this.delete(config, data);
        }
        result.complete(null);
        return result;
    }
}
