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
 * Helpers for the config system tests. Most config work runs on Atlantis' thread pool. Thus, tests wait for its
 * results.
 */
final class ConfigTestSupport {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /**
     * Prevents instances of this class.
     */
    private ConfigTestSupport() {}

    /**
     * Waits up to 10 seconds for the future to complete.
     *
     * @param future the future to wait for
     * @param <T> the result type of the future
     * @return the result of the future
     * @throws AssertionError if the future fails or does not complete in time
     */
    static <T> T await(CompletableFuture<T> future) {
        try {
            return future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new AssertionError("Future did not complete", e);
        }
    }

    /**
     * Checks the condition every 10 milliseconds until it is {@code true}, for up to 10 seconds.
     *
     * @param description what the condition waits for, used in the failure message
     * @param condition the condition to wait for
     * @throws AssertionError if the time runs out or the thread is interrupted
     */
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

    /**
     * Waits until the file contains the expected text.
     *
     * @param file the file to read
     * @param expected the text that the file must contain
     * @return the content of the file
     * @throws AssertionError if the file does not contain the text in time
     */
    static String awaitContent(Path file, String expected) {
        awaitUntil(file + " contains " + expected, () -> read(file).contains(expected));
        return read(file);
    }

    /**
     * Reads the file as a string.
     *
     * @param file the file to read
     * @return the content of the file, or an empty string if the file does not exist or cannot be read
     */
    static String read(Path file) {
        try {
            return Files.exists(file) ? Files.readString(file) : "";
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * Registers a config directly, as AtlantisConfigFinder does, and returns it.
     */
    static <T extends Configuration> Config<T> register(Class<T> clazz, Storage storage) {
        return register(clazz, storage, false, false);
    }

    /**
     * Registers a config at the path from its annotation, and returns it.
     *
     * @param clazz the configuration class
     * @param storage the storage of the config
     * @param lang {@code true} if the config is a lang file
     * @param aon {@code true} if the config uses AON
     * @param <T> the type of the configuration
     * @return the registered config
     */
    static <T extends Configuration> Config<T> register(Class<T> clazz, Storage storage, boolean lang, boolean aon) {
        return register(clazz, path(clazz), storage, lang, aon);
    }

    /**
     * Registers a config at the given path under the test mod, and returns it.
     *
     * @param clazz the configuration class. It must have a public no-argument constructor.
     * @param path the path of the config, relative to the mod's config directory
     * @param storage the storage of the config
     * @param lang {@code true} if the config is a lang file
     * @param aon {@code true} if the config uses AON
     * @param <T> the type of the configuration
     * @return the registered config
     * @throws AssertionError if the class cannot be created
     */
    static <T extends Configuration> Config<T> register(Class<T> clazz, String path, Storage storage, boolean lang, boolean aon) {
        try {
            AtlantisConfig.register(ConfigFixtures.MOD, path, clazz, clazz.getConstructor().newInstance(), storage, false, lang, aon, LoadOrder.NORMAL);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return AtlantisConfig.of(ConfigFixtures.MOD).config(clazz.getCanonicalName());
    }

    /**
     * Gets the path of a configuration class from its config annotation.
     *
     * @param clazz the configuration class
     * @return the path of the config
     * @throws IllegalArgumentException if the class has no config annotation
     */
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

    /**
     * The mod's config directory, resolved through the current test loader.
     */
    static Path modDir() {
        return AtlantisConfig.dirOf(ConfigFixtures.MOD).toPath();
    }
}
