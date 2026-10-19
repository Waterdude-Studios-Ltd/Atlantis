package de.waterdu.atlantis.file.storage;

import com.google.gson.Gson;
import de.waterdu.atlantis.file.ClassHolder;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.NamedData;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Volatile implements Storage {

    public Volatile() {}

    @Override
    public <T extends Configuration> void init(Config<T> config, String modID, String key, String path, Gson gson,
                                               T configuration, ClassHolder<T> classes) {

    }

    @Override
    public <T extends Configuration> void destruct(Config<T> config) {

    }

    @Override
    public <T extends Configuration> CompletableFuture<Boolean> write(Config<T> config) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public <T extends Configuration> CompletableFuture<Boolean> writeSpecific(Config<T> config, T data) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public <T extends Configuration> CompletableFuture<Boolean> read(Config<T> config, boolean skipNulls) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public <T extends Configuration> CompletableFuture<Boolean> readAll(Config<T> config, File dir, Set<String> names) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public <T extends Configuration> CompletableFuture<T> readFromUUID(Config<T> config, UUID uuid) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <T extends Configuration> CompletableFuture<T> readFromName(Config<T> config, String name) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public <T extends Configuration> CompletableFuture<T> delete(Config<T> config, T data) {
        CompletableFuture<T> result = new CompletableFuture<>();
        if (data == null) {
            result.complete(null);
            return result;
        }
        T removed = config.uuidDataMap().remove(data.uuid());
        config.filenameDataMap().remove(data.identifier() + config.dataFileExtension());
        if (data instanceof NamedData) {
            NamedData namedData = (NamedData) data;
            config.nameDataMap().remove(namedData.name());
        }
        result.complete(removed);
        return result;
    }

    @Override
    public <T extends Configuration> CompletableFuture<T> deleteFromUUID(Config<T> config, UUID uuid) {
        CompletableFuture<T> result = new CompletableFuture<>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        CompletableFuture<T> data = config.get(uuid);
        if (data != null && data.isDone()) {
            try {
                return this.delete(config, data.get());
            } catch (ExecutionException | InterruptedException ignored) {}
        }
        result.complete(null);
        return result;
    }

    @Override
    public <T extends Configuration> CompletableFuture<T> deleteFromName(Config<T> config, String name) {
        CompletableFuture<T> result = new CompletableFuture<>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        T data = config.get(name);
        if (data != null) {
            return this.delete(config, data);
        }
        result.complete(null);
        return result;
    }
}
