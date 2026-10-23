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

    private StorageTestConfigs() {}

    public static Config<SampleConfiguration> single(Storage storage) {
        return ConfigTestSupport.register(SampleConfiguration.class, storage);
    }

    public static Config<SampleAon> aon(Storage storage) {
        return ConfigTestSupport.register(SampleAon.class, "commented.aon", storage, false, true);
    }

    public static Config<SampleData> data(Storage storage) {
        return ConfigTestSupport.register(SampleData.class, storage);
    }

    /** Registers any configuration class under the test mod, at the given path. */
    public static <T extends Configuration> Config<T> register(Class<T> clazz, String path, Storage storage) {
        return ConfigTestSupport.register(clazz, path, storage, false, false);
    }

    public static SampleData newData(int value) {
        return new SampleData(UUID.randomUUID(), value);
    }

    public static int value(Object data) {
        return ((SampleData) data).value;
    }

    public static int number(Object configuration) {
        return ((SampleConfiguration) configuration).number;
    }

    public static int count(Object configuration) {
        return ((SampleAon) configuration).count;
    }

    public static Path modDir() {
        return ConfigTestSupport.modDir();
    }

    public static List<Path> modDirFiles() {
        try (var files = Files.walk(modDir())) {
            return files.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T await(CompletableFuture<T> future) {
        return ConfigTestSupport.await(future);
    }

    public static void awaitUntil(String description, java.util.function.BooleanSupplier condition) {
        ConfigTestSupport.awaitUntil(description, condition);
    }

    public static String read(Path file) {
        return ConfigTestSupport.read(file);
    }
}
