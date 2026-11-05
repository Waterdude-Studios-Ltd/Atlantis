package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.ConfigFixtures.SampleAon;
import de.waterdu.atlantis.file.ConfigFixtures.SampleConfiguration;
import de.waterdu.atlantis.file.ConfigFixtures.SampleData;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.storage.Storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public entry points to the package-private config fixtures, for the storage tests in file.storage.
 */
public final class StorageTestConfigs {

    public static final String MOD = ConfigFixtures.MOD;

    /**
     * Prevents instances of this class.
     */
    private StorageTestConfigs() {}

    /**
     * Registers {@link SampleConfiguration} with the given storage.
     *
     * @param storage the storage of the config
     * @return the registered config
     */
    public static Config<SampleConfiguration> single(Storage storage) {
        return ConfigTestSupport.register(SampleConfiguration.class, storage);
    }

    /**
     * Registers {@link SampleAon} as an AON config at {@code commented.aon}.
     *
     * @param storage the storage of the config
     * @return the registered config
     */
    public static Config<SampleAon> aon(Storage storage) {
        return ConfigTestSupport.register(SampleAon.class, "commented.aon", storage, false, true);
    }

    /**
     * Registers {@link SampleData} with the given storage.
     *
     * @param storage the storage of the config
     * @return the registered config
     */
    public static Config<SampleData> data(Storage storage) {
        return ConfigTestSupport.register(SampleData.class, storage);
    }

    /**
     * Registers {@link SampleData} as AON data with the given storage, at the path from its annotation.
     *
     * @param storage the storage of the config
     * @return the registered config
     */
    public static Config<SampleData> aonData(Storage storage) {
        return ConfigTestSupport.register(SampleData.class, storage, false, true);
    }

    /**
     * Registers any configuration class under the test mod, at the given path.
     */
    public static <T extends Configuration> Config<T> register(Class<T> clazz, String path, Storage storage) {
        return ConfigTestSupport.register(clazz, path, storage, false, false);
    }

    /**
     * Creates {@link SampleData} with a random UUID.
     *
     * @param value the value of the data
     * @return the new data
     */
    public static SampleData newData(int value) {
        return new SampleData(UUID.randomUUID(), value);
    }

    /**
     * Gets the value of {@link SampleData}.
     *
     * @param data the data, which must be a {@link SampleData}
     * @return the value of the data
     */
    public static int value(Object data) {
        return ((SampleData) data).value;
    }

    /**
     * Gets the number of {@link SampleConfiguration}.
     *
     * @param configuration the configuration, which must be a {@link SampleConfiguration}
     * @return the number of the configuration
     */
    public static int number(Object configuration) {
        return ((SampleConfiguration) configuration).number;
    }

    /**
     * Gets the count of {@link SampleAon}.
     *
     * @param configuration the configuration, which must be a {@link SampleAon}
     * @return the count of the configuration
     */
    public static int count(Object configuration) {
        return ((SampleAon) configuration).count;
    }

    /**
     * @return the config directory of the test mod
     */
    public static Path modDir() {
        return ConfigTestSupport.modDir();
    }

    /**
     * Gets every regular file in the config directory of the test mod, including subdirectories.
     *
     * @return the files
     * @throws UncheckedIOException if the directory cannot be read
     */
    public static List<Path> modDirFiles() {
        try (var files = Files.walk(modDir())) {
            return files.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Waits up to 10 seconds for the future to complete.
     *
     * @param future the future to wait for
     * @param <T> the result type of the future
     * @return the result of the future
     * @throws AssertionError if the future fails or does not complete in time
     */
    public static <T> T await(CompletableFuture<T> future) {
        return ConfigTestSupport.await(future);
    }

    /**
     * Waits up to 10 seconds until the condition is {@code true}.
     *
     * @param description what the condition waits for, used in the failure message
     * @param condition the condition to wait for
     * @throws AssertionError if the time runs out
     */
    public static void awaitUntil(String description, java.util.function.BooleanSupplier condition) {
        ConfigTestSupport.awaitUntil(description, condition);
    }

    /**
     * Reads the file as a string.
     *
     * @param file the file to read
     * @return the content of the file, or an empty string if the file does not exist or cannot be read
     */
    public static String read(Path file) {
        return ConfigTestSupport.read(file);
    }
}
