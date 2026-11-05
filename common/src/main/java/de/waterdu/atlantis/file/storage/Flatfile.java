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
import de.waterdu.atlantis.file.datatypes.Data;
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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores a configuration in one file, or data in one file per instance inside a directory.
 */
public class Flatfile implements Storage {

    private static final String INVALID_EXT = ".invalid";
    private static final String BACKUP_EXT = ".backup";

    private File file;
    private File jsonFile = null;
    private Config<? extends Configuration> config;

    /**
     * Creates a file storage. {@link #init} sets the file or directory.
     */
    public Flatfile() {
        // noop
    }

    /**
     * Sets the file of the config if {@code path} has a file extension. If not, it sets and creates the directory of
     * the data. For AON configs, it also keeps the old JSON file so that a new AON file can take its content.
     *
     * @param config the config that uses this storage
     * @param modId the ID of the mod that owns the config
     * @param key the key of the config
     * @param path the path of the file or directory, relative to the directory of the mod
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
    ) {
        this.config = config;
        if (path.contains(".")) {
            this.file = new File(AtlantisConfig.dirOf(modId), path);
            if (config.isAON()) {
                this.jsonFile = new File(AtlantisConfig.dirOf(modId), path.split("\\.")[0] + FileExtensions.JSON);
            }
        } else {
            this.file = new File(AtlantisConfig.dirOf(modId) + File.separator + path);
            if (this.file.mkdir()) {
                AtlantisLogger.log(Settings.get().getLogLoadMessages(), "Creating new directory for {}", config.name());
            }
        }
    }

    /**
     * Does nothing, because this storage holds no resources.
     *
     * @param config the config that uses this storage
     * @param <T> the configuration type
     */
    @Override
    public <T extends Configuration> void destruct(Config<T> config) {

    }

    /**
     * Writes every data instance in memory with {@link #writeAll}, or writes the configuration to its file on
     * {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config to write
     * @param <T> the configuration type
     * @return a future of {@code true} if the write was successful, or of {@code false} if the server is not alive
     */
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

    /**
     * Schedules a {@link #writeSpecific} for every data instance in memory. Each write starts one data save delay
     * after the one before it.
     *
     * @param config the config to write
     * @param result completes with {@code true} after the last write
     * @param <T> the configuration type
     */
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

    /**
     * Writes the configuration to its file, with comments for AON configs. If the AON file is new and an old JSON
     * file exists, it writes to the JSON file instead. It deletes the old JSON file after the write.
     *
     * @param config the config to write
     * @param result completes with {@code true} if the write was successful
     * @param <T> the configuration type
     */
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

    /**
     * Writes the data instance to a temporary file on {@link Atlantis#THREAD_POOL}, then moves it over the file of the
     * instance. If another write of the same instance is in progress, it tries again after the duplicate save delay.
     *
     * @param config the config of the data
     * @param data the data instance to write
     * @param <T> the configuration type
     * @return a future of {@code true} if the write was successful, or of {@code false} if it failed or must try
     *     again
     */
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
                        if (!Flatfile.replace(temp, output)) {
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

    /**
     * Moves the source file over the target file. It uses an atomic move if the file system supports it.
     *
     * @param source the file to move
     * @param target the file to replace
     * @return {@code true} if the move was successful
     */
    static boolean replace(File source, File target) {
        try {
            try {
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Creates the temporary file. If it already exists, it deletes it and schedules a new {@link #writeSpecific}
     * after the duplicate save delay.
     *
     * @param config the config of the data
     * @param data the data instance to write
     * @param temp the temporary file
     * @param <T> the configuration type
     * @return {@code true} if the temporary file was created and the write can continue
     * @throws IOException if the file system cannot create or delete the file
     */
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

    /**
     * Clears the data in memory and reads every file of the directory again, or reads the configuration from its
     * file. A configuration file that fails to read is marked as invalid. The read runs on
     * {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config to read
     * @param skipNulls keeps the current configuration if the file holds {@code null}
     * @param <T> the configuration type
     * @return a future of {@code true} if the read was successful and set a value
     */
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

    /**
     * Reads every data file in the directory and its subdirectories and puts the instances in memory. It skips hidden
     * and invalid files. If {@code names} is not empty, it writes again each instance whose file name is not in it.
     *
     * @param config the config to read
     * @param dir the directory to read
     * @param names the file names of the instances that were in memory before the read
     * @param <T> the configuration type
     * @return a completed future of {@code true}, or of {@code false} if the server is not alive
     */
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
                                    if (!names.isEmpty() && !names.contains(config.filename((Data) in))) {
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

    /**
     * Reads the file named after the UUID in the data directory and puts the instance in memory. A file that fails
     * to read is marked as invalid.
     *
     * @param config the config of the data
     * @param uuid the UUID of the instance
     * @param <T> the configuration type
     * @return a future of the instance, or of {@code null} if there is none or the config is not data
     */
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

    /**
     * Reads the file named after the name in the data directory and puts the instance in memory.
     *
     * @param config the config of the data
     * @param name the name of the instance
     * @param <T> the configuration type
     * @return a future of the instance, or of {@code null} if there is none or the config is not named data
     */
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

    /**
     * Removes the data instance from memory and moves its file to the error bin. This runs on
     * {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config of the data
     * @param data the data instance to delete, or {@code null}
     * @param <T> the configuration type
     * @return a future of the instance removed from the UUID map, or of {@code null}
     */
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
                config.filenameDataMap().remove(config.filename((Data) data));
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

    /**
     * Deletes the data instance with the given UUID with {@link #delete}. The instance must already be loaded.
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
     * Deletes the named data instance with the given name with {@link #delete}.
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

    /**
     * @param <T> the configuration type
     * @return the config that this storage was initialized with
     */
    @SuppressWarnings("unchecked")
    public <T extends Configuration> Config<T> getConfig() {
        return (Config<T>) this.config;
    }

    /**
     * Gets the file of the data instance. It creates the subdirectories from {@link Configuration#dir()} if they do
     * not exist.
     *
     * @param config the config of the data
     * @param data the data instance
     * @param <T> the configuration type
     * @return the file of the instance
     */
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

    /**
     * Goes through the directory and its subdirectories to remove the data files of instances that are not in
     * memory. It moves them to the error bin or marks them as invalid. Only files with the data file extension of
     * the config are removed. Hidden files and temporary files stay. Each removal checks again that no instance in
     * memory uses the file name.
     *
     * @param config the config of the data
     * @param file the directory to clean
     * @param <T> the configuration type
     */
    private <T extends Configuration> void cleanFolder(Config<T> config, File file) {
        this.cleanFolder(config, file, Flatfile.loadedFileNames(config), new AtomicLong(Settings.get().getDataSaveDelay()));
    }

    /**
     * Goes through the directory and its subdirectories to remove the data files that are not in the set of loaded
     * file names.
     *
     * @param config the config of the data
     * @param file the directory to clean
     * @param loaded the file names of the instances in memory
     * @param delay the delay of the next removal in milliseconds. Each removal adds the data save delay to it.
     * @param <T> the configuration type
     */
    private <T extends Configuration> void cleanFolder(Config<T> config, File file, Set<String> loaded, AtomicLong delay) {
        if (!this.isServerAlive()) {
            return;
        }
        var useBin = Settings.get().isUseErrorBin();
        var extension = config.dataFileExtension();
        var dir = file.listFiles();
        if (dir != null) {
            for (var f : dir) {
                var name = f.getName();
                if (f.isFile() && !name.startsWith(".") && name.endsWith(extension) && !loaded.contains(name)) {
                    Atlantis.THREAD_POOL.schedule(() -> {
                        if (!this.isServerAlive() || !f.isFile() || Flatfile.loadedFileNames(config).contains(name)) {
                            return;
                        }
                        if (useBin ? this.moveToBin(config, f) : this.markAsError(config, f)) {
                            AtlantisLogger.debug("Deleted instance of {} {}", config.name(), name);
                        } else {
                            AtlantisLogger.error("Failed to delete instance of {} {}", config.name(), name);
                        }
                    }, delay.getAndAdd(Settings.get().getDataSaveDelay()));
                } else if (f.isDirectory()) {
                    this.cleanFolder(config, f, loaded, delay);
                }
            }
        }
    }

    /**
     * Gets the file names of the data instances in memory. The names use the data file extension of the config.
     *
     * @param config the config of the data
     * @param <T> the configuration type
     * @return the file names
     */
    private static <T extends Configuration> Set<String> loadedFileNames(Config<T> config) {
        var names = new HashSet<String>();
        for (var data : config.uuidDataMap().values()) {
            if (data != null) {
                names.add(config.filename((Data) data));
            }
        }
        return names;
    }

    /**
     * Moves the file to the error bin on {@link Atlantis#THREAD_POOL}.
     *
     * @param config the config of the data
     * @param file the file to delete
     * @param <T> the configuration type
     */
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

    /**
     * Renames the file with the {@code .invalid} extension, so that reads skip it. It adds a number if the name is
     * already used.
     *
     * @param config the config of the data
     * @param file the file to mark
     * @param <T> the configuration type
     * @return {@code true} if the rename was successful
     */
    private <T extends Configuration> boolean markAsError(Config<T> config, File file) {
        var i = 1;
        var parent = file.getParentFile();
        var dest = new File(parent, file.getName() + INVALID_EXT);
        while (dest.exists()) {
            dest = new File(parent, file.getName() + " (" + i++ + ")" + INVALID_EXT);
        }
        return file.renameTo(dest);
    }

    /**
     * @return the {@code bin} directory in the config directory of Atlantis
     */
    private static File errorBin() {
        return new File(AtlantisConfig.dirOf(Atlantis.ID), "bin");
    }

    /**
     * Moves the file to the error bin with the {@code .backup} extension. The new name holds the owner and key of the
     * config, and a number if the name is already used.
     *
     * @param config the config of the data
     * @param file the file to move
     * @param <T> the configuration type
     * @return {@code true} if the move was successful
     */
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
