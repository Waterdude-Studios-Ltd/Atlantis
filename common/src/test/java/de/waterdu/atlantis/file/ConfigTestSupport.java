package de.waterdu.atlantis.file;

import de.waterdu.atlantis.file.auto.BasicConfiguration;
import de.waterdu.atlantis.file.auto.BasicData;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.auto.FileProperties;
import de.waterdu.atlantis.file.auto.I18nFile;
import de.waterdu.atlantis.file.auto.LoadOrder;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.storage.Storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Helpers for the config system tests. Most config work runs on Atlantis' thread pool, so tests wait for its results.
 */
final class ConfigTestSupport {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private ConfigTestSupport() {}

    static <T> T await(CompletableFuture<T> future) {
        try {
            return future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new AssertionError("Future did not complete", e);
        }
    }

    static void awaitUntil(String description, BooleanSupplier condition) {
        var deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Timed out waiting until " + description);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }
    }

    static String awaitContent(Path file, String expected) {
        awaitUntil(file + " contains " + expected, () -> read(file).contains(expected));
        return read(file);
    }

    static String read(Path file) {
        try {
            return Files.exists(file) ? Files.readString(file) : "";
        } catch (IOException e) {
            return "";
        }
    }

    /** Registers a config directly, like AtlantisConfigFinder does, and returns it. */
    static <T extends Configuration> Config<T> register(Class<T> clazz, Storage storage) {
        return register(clazz, storage, false, false);
    }

    static <T extends Configuration> Config<T> register(Class<T> clazz, Storage storage, boolean lang, boolean aon) {
        return register(clazz, path(clazz), storage, lang, aon);
    }

    static <T extends Configuration> Config<T> register(Class<T> clazz, String path, Storage storage, boolean lang, boolean aon) {
        try {
            AtlantisConfig.register(ConfigFixtures.MOD, path, clazz, clazz.getConstructor().newInstance(), storage, false, lang, aon, LoadOrder.NORMAL);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return AtlantisConfig.of(ConfigFixtures.MOD).config(clazz.getCanonicalName());
    }

    static String path(Class<?> clazz) {
        if (clazz.isAnnotationPresent(FileBackedObject.class)) {
            return clazz.getAnnotation(FileBackedObject.class).path();
        } else if (clazz.isAnnotationPresent(BasicConfiguration.class)) {
            return new FileProperties(clazz.getAnnotation(BasicConfiguration.class), clazz.asSubclass(Configuration.class)).path();
        } else if (clazz.isAnnotationPresent(BasicData.class)) {
            return new FileProperties(clazz.getAnnotation(BasicData.class), clazz.asSubclass(Configuration.class)).path();
        } else if (clazz.isAnnotationPresent(I18nFile.class)) {
            return new FileProperties(clazz.getAnnotation(I18nFile.class)).path();
        }
        throw new IllegalArgumentException(clazz + " has no config annotation");
    }

    /** The mod's config directory, resolved through the current test loader. */
    static Path modDir() {
        return AtlantisConfig.dirOf(ConfigFixtures.MOD).toPath();
    }
}
