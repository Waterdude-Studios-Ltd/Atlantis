package de.waterdu.atlantis.file.storage;

import com.google.gson.Gson;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.file.AonCommenter;
import de.waterdu.atlantis.file.AtlantisConfig;
import de.waterdu.atlantis.file.ClassHolder;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.file.FileExtensions;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.NamedData;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Flatfile implements Storage {

    private static final String INVALID_EXT = ".invalid";
    private static final String BACKUP_EXT = ".backup";

    private File file;
    private File jsonFile = null;
    private Config<? extends Configuration> config;

    public Flatfile() {
        // noop
    }

    @Override
    public <T extends Configuration> void init(
            Config<T> config,
            String modID,
            String key,
            String path,
            Gson gson,
            T configuration,
            ClassHolder<T> classes
    ) {
        this.config = config;
        if (path.contains(".")) {
            this.file = new File(AtlantisConfig.dirOf(modID), path);
            if (config.isAON()) {
                this.jsonFile = new File(AtlantisConfig.dirOf(modID), path.split("\\.")[0] + FileExtensions.JSON);
            }
        } else {
            this.file = new File(AtlantisConfig.dirOf(modID) + File.separator + path);
            if (this.file.mkdir()) {
                AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Creating new directory for {}", config.name());
            }
        }
    }

    @Override
    public <T extends Configuration> void destruct(Config<T> config) {

    }

    @Override
    public <T extends Configuration> CompletableFuture<Boolean> write(Config<T> config) {
        var result = new CompletableFuture<Boolean>();
        if (!this.isServerAlive()) {
            result.complete(false);
            return result;
        }
        if (config.container().isData()) {
            this.writeAll(config, result);
        } else {
            Atlantis.THREAD_POOL.submit(() -> this.writeSingle(config, result));
        }
        return result;
    }

    private <T extends Configuration> void writeAll(Config<T> config, CompletableFuture<Boolean> result) {
        var delay = Settings.get().getDataSaveDelay();
        var writes = new AtomicInteger(0);
        for (var out : config.uuidDataMap().values()) {
            if (out != null) {
                writes.incrementAndGet();
                Atlantis.THREAD_POOL.schedule(() -> {
                    try {
                        this.writeSpecific(config, out);
                    } catch (InterruptedException e) {
                        AtlantisLogger.error("Failed to save instance of {} called {} with UUID {}", config.name(), out.identifier(), out.uuid());
                    }
                    if (writes.decrementAndGet() == 0) {
                        result.complete(true);
                        AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Successfully saved {}", config.name());
                    }
                }, delay);
                delay += Settings.get().getDataSaveDelay();
            }
        }

        if (writes.get() == 0) {
            result.complete(true);
        }
    }

    private <T extends Configuration> void writeSingle(Config<T> config, CompletableFuture<Boolean> result) {
        try {
            var newFile = this.file;
            if (newFile.createNewFile()) {
                AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Creating new file for {}", config.name());
                if (this.config.isAON() && this.jsonFile.exists()) {
                    newFile = this.jsonFile;
                }
            }
            var json = AonCommenter.comment(config, config.gson().toJson(config.container().get()), config.classes().getConfigurationClass());
            try (var fs = new FileOutputStream(newFile);
                 var ow = new OutputStreamWriter(fs, StandardCharsets.UTF_8);
                 var bw = new BufferedWriter(ow)) {
                bw.write(json);
            }
            if (this.config.isAON() && this.jsonFile.exists() && this.jsonFile.delete()) {
                AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Deleted old JSON file of {}", config.name());
            }
            AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Successfully saved {}", config.name());
            config.remapLang();
            result.complete(true);
        } catch (Exception e) {
            AtlantisLogger.error("Failed to save {}: {}", config.name(), e.getMessage());
            result.complete(false);
        }
    }

    @Override
    public <T extends Configuration> CompletableFuture<Boolean> writeSpecific(Config<T> config, T data) throws InterruptedException {
        var result = new CompletableFuture<Boolean>();
        if (!this.isServerAlive()) {
            result.complete(false);
            return result;
        }

        Atlantis.THREAD_POOL.submit(() -> {
            try {
                var output = this.resolveFile(config, data);
                if (!output.getParentFile().exists()) {
                    if (output.getParentFile().mkdirs()) {
                        AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Creating new directories for {}", config.name());
                    }
                }

                var temp = new File(output.getPath() + ".temp");

                if (!this.checkTempFile(config, data, temp)) {
                    return;
                }

                if (output.createNewFile()) {
                    AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Creating new instance of {} for file {}", config.name(), data.identifier());
                }

                if (temp.exists()) {
                    var json = config.gson().toJson(data);
                    try (var fs = new FileOutputStream(temp);
                         var ow = new OutputStreamWriter(fs, StandardCharsets.UTF_8);
                         var bw = new BufferedWriter(ow)) {
                        bw.write(json);
                    }
                    if (temp.exists()) {
                        if (output.exists()) {
                            if (output.delete()) {
                                AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Deleting output file of {} for file {} in order to swap temp file back in", config.name(), data.identifier());
                            }
                        }
                        if (!temp.renameTo(output)) {
                            AtlantisLogger.warn("Write probably failed for new instance of {} for file {}", config.name(), data.identifier());
                        }
                        result.complete(true);
                    } else {
                        Atlantis.THREAD_POOL.schedule(() -> {
                            try {
                                this.writeSpecific(config, data);
                            } catch (InterruptedException ignored) {}
                        }, Settings.get().getDuplicateSaveDelay(), TimeUnit.MILLISECONDS);
                        result.complete(false);
                    }
                } else {
                    Atlantis.THREAD_POOL.schedule(() -> {
                        try {
                            this.writeSpecific(config, data);
                        } catch (InterruptedException ignored) {}
                    }, Settings.get().getDuplicateSaveDelay(), TimeUnit.MILLISECONDS);
                    result.complete(false);
                }
            } catch (Exception e) {
                AtlantisLogger.error("Failed to save instance of {} for file {}: {}", config.name(), data.identifier(), e.getMessage());
                result.complete(false);
            }
        });

        return result;
    }

    private <T extends Configuration> boolean checkTempFile(Config<T> config, T data, File temp) throws IOException {
        if (!temp.createNewFile()) {
            AtlantisLogger.debug("Temp file already exists called {} for file {}, delaying save attempt", config.name(), data.identifier());
            Files.deleteIfExists(temp.toPath());
            Atlantis.THREAD_POOL.schedule(() -> {
                try {
                    this.writeSpecific(config, data);
                } catch (InterruptedException ignored) {
                    // noop
                }
            }, Settings.get().getDuplicateSaveDelay(), TimeUnit.MILLISECONDS);
            return false;
        }
        return true;
    }

    @Override
    public <T extends Configuration> CompletableFuture<Boolean> read(Config<T> config, boolean skipNulls) {
        var result = new CompletableFuture<Boolean>();
        if (!this.isServerAlive()) {
            result.complete(false);
            return result;
        }
        if (config.container().isData()) {
            var filenames = new HashSet<>(config.filenameDataMap().keySet());
            config.clearAll();
            Atlantis.THREAD_POOL.submit(() -> this.readAll(config, this.file, filenames).whenComplete((r, err) -> {
                this.cleanFolder(config, this.file);
                AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Successfully read {}", config.name());
                result.complete(true);
            }));
        } else {
            Atlantis.THREAD_POOL.submit(() -> {
                var written = false;
                var newFile = this.file;
                try {
                    if (newFile.createNewFile()) {
                        AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Creating new file for {}", config.name());
                        if (this.config.isAON() && this.jsonFile.exists()) {
                            newFile = this.jsonFile;
                        }
                    }
                    try (var fs = new FileInputStream(newFile);
                         var ir = new InputStreamReader(fs, StandardCharsets.UTF_8);
                         var br = new BufferedReader(ir)
                    ) {
                        var json = AonCommenter.uncomment(config, IOUtils.toString(br), config.classes().getConfigurationClass());
                        var configuration = config.gson().fromJson(json, config.classes().getConfigurationClass());
                        if (configuration != null || !skipNulls) {
                            config.container().set(config, configuration);
                            written = true;
                        }
                    }
                    AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Successfully read {}", config.name());
                    config.remapLang();
                    result.complete(written);
                } catch (Exception e) {
                    AtlantisLogger.error("Failed to read {}: {}", config.name(), e.getMessage());
                    this.markAsError(config, newFile);
                    result.complete(false);
                }
            });
        }
        return result;
    }

    @Override
    public <T extends Configuration> CompletableFuture<Boolean> readAll(Config<T> config, File dir, Set<String> names) {
        var result = new CompletableFuture<Boolean>();
        if (!this.isServerAlive()) {
            result.complete(false);
            return result;
        }
        var dirs = dir.listFiles();
        if (dirs != null) {
            for (final var toRead : dirs) {
                if (toRead != null) {
                    if (toRead.isFile() && !toRead.getName().startsWith(".") && !toRead.getName().endsWith(INVALID_EXT)) {
                        try {
                            if (this.isServerAlive()) {
                                try (var fs = new FileInputStream(toRead);
                                     var ir = new InputStreamReader(fs, StandardCharsets.UTF_8);
                                     var br = new BufferedReader(ir)) {
                                    var in = config.gson().fromJson(br, config.classes().getConfigurationClass());
                                    config.putUnsafe(in, false);
                                    if (!names.isEmpty() && !names.contains(in.identifier() + config.dataFileExtension())) {
                                        this.writeSpecific(config, in);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            AtlantisLogger.error("Failed to read {} for file {}: {}", config.name(), toRead.getName(), e.getMessage());
                        }
                    } else if (toRead.isDirectory()) {
                        this.readAll(config, toRead, names);
                    }
                }
            }
        }
        result.complete(true);
        return result;
    }

    @Override
    public <T extends Configuration> CompletableFuture<T> readFromUUID(Config<T> config, UUID uuid) {
        var result = new CompletableFuture<T>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        if (config.container().isData()) {
            var file = new File(this.file, uuid.toString() + config.dataFileExtension());
            if (file.exists() && !file.getName().startsWith(".") && !file.getName().endsWith(INVALID_EXT)) {
                try {
                    try (var fs = new FileInputStream(file);
                         var ir = new InputStreamReader(fs, StandardCharsets.UTF_8);
                         var br = new BufferedReader(ir)) {
                        var in = config.gson().fromJson(br, config.classes().getConfigurationClass());
                        config.putUnsafe(in, false);
                        result.complete(in);
                    }
                } catch (Exception e) {
                    AtlantisLogger.error("Failed to read {} for file {}: {}", config.name(), file.getName(), e.getMessage());
                    this.markAsError(config, file);
                    result.complete(null);
                }
            } else {
                result.complete(null);
            }
        } else {
            result.complete(null);
        }
        return result;
    }

    @Override
    public <T extends Configuration> CompletableFuture<T> readFromName(Config<T> config, String name) {
        var result = new CompletableFuture<T>();
        if (!this.isServerAlive()) {
            result.complete(null);
            return result;
        }
        if (config.container().isNamedData()) {
            var file = new File(this.file, name + config.dataFileExtension());
            if (file.exists() && !file.getName().startsWith(".") && !file.getName().endsWith(INVALID_EXT)) {
                try {
                    try (var fs = new FileInputStream(file);
                         var ir = new InputStreamReader(fs, StandardCharsets.UTF_8);
                         var br = new BufferedReader(ir)) {
                        var in = config.gson().fromJson(br, config.classes().getConfigurationClass());
                        config.putUnsafe(in, false);
                        result.complete(in);
                    }
                } catch (Exception e) {
                    AtlantisLogger.error("Failed to read {} for file {}: {}", config.name(), file.getName(), e.getMessage());
                    result.complete(null);
                }
            } else {
                result.complete(null);
            }
        } else {
            result.complete(null);
        }
        return result;
    }

    @Override
    public <T extends Configuration> CompletableFuture<T> delete(Config<T> config, T data) {
        var result = new CompletableFuture<T>();
        if (!this.isServerAlive() || data == null) {
            result.complete(null);
            return result;
        }
        Atlantis.THREAD_POOL.submit(() -> {
            try {
                var removed = config.uuidDataMap().remove(data.uuid());
                config.filenameDataMap().remove(data.identifier() + config.dataFileExtension());
                if (data instanceof NamedData namedData) {
                    config.nameDataMap().remove(namedData.name());
                }
                this.deleteFile(config, this.resolveFile(config, data));
                result.complete(removed);
            } catch (Exception e) {
                AtlantisLogger.error("Failed to delete {} for file {}: {}", config.name(), this.file.getName(), e.getMessage());
                result.complete(null);
            }
        });
        return result;
    }

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

    @SuppressWarnings("unchecked")
    public <T extends Configuration> Config<T> getConfig() {
        return (Config<T>) this.config;
    }

    private <T extends Configuration> File resolveFile(Config<T> config, Configuration data) {
        String directory = data.dir();
        if (directory.isEmpty()) {
            return new File(this.file, data.identifier() + config.dataFileExtension());
        } else {
            File file = this.file;
            for (String subdir : directory.split("/")) {
                file = new File(file, subdir);
                if (file.mkdir()) {
                    AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Creating new directory {} for {}", subdir, config.name());
                }
            }
            return new File(file, data.identifier() + config.dataFileExtension());
        }
    }

    private <T extends Configuration> void cleanFolder(Config<T> config, File file) {
        if (!this.isServerAlive()) {
            return;
        }
        var useBin = Settings.get().isUseErrorBin();
        var delay = Settings.get().getDataSaveDelay();
        var dir = file.listFiles();
        if (dir != null) {
            for (var f : dir) {
                if (f.isFile() && !f.getName().startsWith("") && !f.getName().endsWith(INVALID_EXT)) {
                    if (!config.filenameDataMap().containsKey(f.getName())) {
                        Atlantis.THREAD_POOL.schedule(() -> {
                            if (useBin ? this.moveToBin(config, f) : this.markAsError(config, f)) {
                                AtlantisLogger.debug("Deleted instance of {} {}", config.name(), f.getName());
                            } else {
                                AtlantisLogger.error("Failed to delete instance of {} {}", config.name(), f.getName());
                            }
                        }, delay);
                        delay += Settings.get().getDataSaveDelay();
                    }
                } else if (f.isDirectory()) {
                    this.cleanFolder(config, f);
                }
            }
        }
    }

    private <T extends Configuration> void deleteFile(Config<T> config, File file) {
        if (!this.isServerAlive()) {
            return;
        }
        if (file.isFile()) {
            Atlantis.THREAD_POOL.submit(() -> {
                if (this.moveToBin(config, file)) {
                    AtlantisLogger.debug("Deleted instance of {} {}", config.name(), file.getName());
                } else {
                    AtlantisLogger.error("Failed to delete instance of {} {}", config.name(), file.getName());
                }
            });
        }
    }

    private <T extends Configuration> boolean markAsError(Config<T> config, File file) {
        var i = 1;
        var parent = file.getParentFile();
        var dest = new File(parent, file.getName() + INVALID_EXT);
        while (dest.exists()) {
            dest = new File(parent, file.getName() + " (" + i++ + ")" + INVALID_EXT);
        }
        return file.renameTo(dest);
    }

    private static File errorBin() {
        return new File(AtlantisConfig.dirOf(Atlantis.ID), "bin");
    }

    private <T extends Configuration> boolean moveToBin(Config<T> config, File file) {
        var errorBin = Flatfile.errorBin();
        if (!errorBin.exists()) {
            if (errorBin.mkdirs()) {
                AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Creating new directory for error bin");
            }
        }
        var i = 1;
        var dest = new File(errorBin, file.getName() + "_" + config.owner() + "_" + config.key() + BACKUP_EXT);
        while (dest.exists()) {
            dest = new File(errorBin, file.getName() + "_" + config.owner() + "_" + config.key() + "(" + i++ + ")" + BACKUP_EXT);
        }
        return file.renameTo(dest);
    }
}
