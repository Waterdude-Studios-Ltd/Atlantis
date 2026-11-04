package de.waterdu.atlantis.file;

import com.google.common.annotations.VisibleForTesting;
import de.waterdu.atlantis.Atlantis;
import de.waterdu.atlantis.AtlantisLogger;
import de.waterdu.atlantis.file.auto.BasicConfiguration;
import de.waterdu.atlantis.file.auto.BasicData;
import de.waterdu.atlantis.file.auto.FileBackedObject;
import de.waterdu.atlantis.file.auto.FileProperties;
import de.waterdu.atlantis.file.auto.I18nFile;
import de.waterdu.atlantis.file.auto.LoadOrder;
import de.waterdu.atlantis.file.auto.NoConfiguration;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.storage.Flatfile;
import de.waterdu.atlantis.file.storage.Storage;
import de.waterdu.atlantis.util.bootstrap.Bootstrapped;
import de.waterdu.atlantis.util.bootstrap.Bootstrapper;
import de.waterdu.atlantis.util.bootstrap.Entrypoint;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Finds the classes with {@link BasicConfiguration}, {@link BasicData}, {@link I18nFile} or {@link FileBackedObject}
 * in the bootstrap scan, and registers their configs with {@link AtlantisConfig}.
 */
@Bootstrapped
public final class AtlantisConfigFinder {

    private static final List<Class<? extends Annotation>> ANNOTATIONS = List.of(
            BasicConfiguration.class,
            BasicData.class,
            I18nFile.class,
            FileBackedObject.class
    );

    private static final Supplier<Storage> DEFAULT_STORAGE = Flatfile::new;
    private static final Map<String, List<ConfigurationInstance<? extends Configuration>>> INSTANCES = new HashMap<>();
    private static final Map<String, List<ConfigurationInstance<? extends Configuration>>> BACKUP_INSTANCES = new HashMap<>();
    private static final AtomicBoolean FOUND = new AtomicBoolean(false);

    private AtlantisConfigFinder() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Finds the annotated configuration classes and creates their default instances. Runs only once. Keeps a backup
     * of the found classes for {@link #restoreFromBackup()}.
     */
    @Entrypoint
    public static void find() {
        if (!AtlantisConfigFinder.FOUND.getAndSet(true)) {
            for (var annotation : ANNOTATIONS) {
                Bootstrapper.forEachAnnotatedClass(annotation, annotated -> {
                    if (Configuration.class.isAssignableFrom(annotated.clazz())) {
                        AtlantisConfigFinder.tryRegister(annotated.annotation(), annotated.clazz().asSubclass(Configuration.class));
                    }
                });
            }

            BACKUP_INSTANCES.clear();
            BACKUP_INSTANCES.putAll(INSTANCES);
        }
    }

    /**
     * Removes the found classes and the backup, so that {@link #find()} can run again.
     */
    @VisibleForTesting
    public static void resetForTesting() {
        INSTANCES.clear();
        BACKUP_INSTANCES.clear();
        FOUND.set(false);
    }

    /**
     * Restores the list of found configuration classes that {@link #init(boolean)} removes, so that they can register
     * again.
     * This does not change files.
     */
    public static void restoreFromBackup() {
        INSTANCES.clear();
        INSTANCES.putAll(BACKUP_INSTANCES);
    }

    /**
     * Adds the configuration class to the found classes if the annotation is one of the config annotations. Does
     * nothing for other annotations.
     *
     * @param annotation the annotation on the class
     * @param clazz the configuration class
     */
    static void tryRegister(Annotation annotation, Class<? extends Configuration> clazz) {
        if (annotation instanceof BasicConfiguration configuration) {
            AtlantisConfigFinder.register(configuration, clazz);
        } else if (annotation instanceof BasicData data) {
            AtlantisConfigFinder.register(data, clazz);
        } else if (annotation instanceof I18nFile i18n) {
            AtlantisConfigFinder.register(i18n, clazz);
        } else if (annotation instanceof FileBackedObject fbo) {
            AtlantisConfigFinder.register(fbo, clazz);
        }
    }

    /**
     * Adds a configuration class with {@link BasicConfiguration} to the found classes.
     *
     * @param configuration the annotation on the class
     * @param clazz the configuration class
     */
    private static void register(BasicConfiguration configuration, Class<? extends Configuration> clazz) {
        AtlantisConfigFinder.register(new FileProperties(configuration, clazz), clazz);
    }

    /**
     * Adds a data class with {@link BasicData} to the found classes.
     *
     * @param data the annotation on the class
     * @param clazz the data class
     */
    private static void register(BasicData data, Class<? extends Configuration> clazz) {
        AtlantisConfigFinder.register(new FileProperties(data, clazz), clazz);
    }

    /**
     * Adds an i18n class with {@link I18nFile} to the found classes.
     *
     * @param i18n the annotation on the class
     * @param clazz the i18n class
     */
    private static void register(I18nFile i18n, Class<? extends Configuration> clazz) {
        AtlantisConfigFinder.register(new FileProperties(i18n), clazz);
    }

    /**
     * Adds a class with {@link FileBackedObject} to the found classes.
     *
     * @param fbo the annotation on the class
     * @param clazz the configuration class
     */
    private static void register(FileBackedObject fbo, Class<? extends Configuration> clazz) {
        AtlantisConfigFinder.register(new FileProperties(fbo), clazz);
    }

    /**
     * Creates the default instance of the class and adds it to the found classes of its mod. The storage comes from
     * the provider method in the properties if one is set, or else from the storage class. If the storage fails to
     * create, {@link Flatfile} is used. Logs an error if the class has no public zero-argument constructor.
     *
     * @param properties the file properties from the annotation
     * @param clazz the configuration class
     */
    private static void register(final FileProperties properties, final Class<? extends Configuration> clazz) {
        Supplier<Storage> storageSupplier;
        if (!properties.providerClass().equals(NoConfiguration.class) && !properties.providerMethod().isEmpty()) {
            storageSupplier = () -> {
                try {
                    final var storageProvider = AtlantisConfig.get(properties.mod(), properties.providerClass());
                    Method method;
                    try {
                        method = storageProvider.getClass().getDeclaredMethod(properties.providerMethod());
                        return (Storage) method.invoke(storageProvider);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        method = storageProvider.getClass().getDeclaredMethod(properties.providerMethod(), String.class);
                        return (Storage) method.invoke(storageProvider, properties.mod());
                    }
                } catch (ClassCastException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    AtlantisLogger.error("Failed to supply storage from {}#{}, using default", properties.providerClass().getSimpleName(), properties.providerMethod());
                    return AtlantisConfigFinder.DEFAULT_STORAGE.get();
                }
            };
        } else {
            storageSupplier = () -> {
                var storageClass = properties.storage();
                try {
                    return storageClass.getConstructor().newInstance();
                } catch (ExceptionInInitializerError | InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    AtlantisLogger.error("Failed to instantiate new storage {}, using default", storageClass.getSimpleName());
                    return AtlantisConfigFinder.DEFAULT_STORAGE.get();
                }
            };
        }

        try {
            AtlantisConfigFinder.INSTANCES.computeIfAbsent(properties.mod(), ___ -> new ArrayList<>()).add(
                    new ConfigurationInstance<>(properties.order(), properties.path(), clazz, storageSupplier, properties.force(), properties.i18n(), properties.aon())
            );
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            AtlantisLogger.error("Failed to instantiate new {}:{} with zero-args constructor", properties.mod(), properties.path());
        }
    }

    /**
     * Registers the found configs of Atlantis first, and then of the other mods if {@code all} is set. Removes the
     * registered classes from the found classes.
     *
     * @param all also registers the configs of the other mods
     */
    @ApiStatus.Internal
    public static void init(boolean all) {
        var atlantis = AtlantisConfigFinder.INSTANCES.remove(Atlantis.ID);
        if (atlantis != null) {
            AtlantisConfigFinder.init0(Atlantis.ID, atlantis);
        }
        if (all) {
            for (var entry : AtlantisConfigFinder.INSTANCES.entrySet()) {
                AtlantisConfigFinder.init0(entry.getKey(), entry.getValue());
            }
            AtlantisConfigFinder.gc();
        }
    }

    /**
     * Registers the configs of the mod in load order, and then calls {@link AtlantisConfig#postRegistration(String)}.
     *
     * @param modId the mod ID
     * @param instances the found configs of the mod
     */
    private static void init0(String modId, List<ConfigurationInstance<? extends Configuration>> instances) {
        instances.sort(Comparator.comparingInt(instance -> instance.order.ordinal()));
        for (var instance : instances) {
            AtlantisConfig.register(modId, instance.path, instance.clazz, instance.instance, instance.storage.get(), instance.force, instance.i18n, instance.aon, instance.order);
        }
        AtlantisConfig.postRegistration(modId);
    }

    /**
     * Removes all found classes. The backup stays.
     */
    private static void gc() {
        AtlantisConfigFinder.INSTANCES.clear();
    }

    /**
     * Holds a found config until it registers.
     *
     * @param order the load order relative to the other configs of the mod
     * @param path the path of the file or directory, relative to the config directory of the mod
     * @param clazz the configuration class
     * @param instance the default configuration instance
     * @param storage supplies the storage of the config
     * @param force reads player data from storage when the configs of the mod load
     * @param i18n maps the fields of the configuration to messages
     * @param aon writes the files in the AON format instead of JSON
     * @param <T> the configuration type
     */
    private record ConfigurationInstance<T extends Configuration>(
            LoadOrder order,
            String path,
            Class<T> clazz,
            T instance,
            Supplier<Storage> storage,
            boolean force,
            boolean i18n,
            boolean aon
    ) {

        /**
         * Creates the default instance with the public zero-argument constructor of the class.
         *
         * @param order the load order relative to the other configs of the mod
         * @param path the path of the file or directory, relative to the config directory of the mod
         * @param clazz the configuration class
         * @param storage supplies the storage of the config
         * @param force reads player data from storage when the configs of the mod load
         * @param i18n maps the fields of the configuration to messages
         * @param aon writes the files in the AON format instead of JSON
         * @throws InstantiationException if the class is abstract
         * @throws IllegalAccessException if the constructor is not accessible
         * @throws NoSuchMethodException if the class has no public zero-argument constructor
         * @throws InvocationTargetException if the constructor throws
         */
        public ConfigurationInstance(
                LoadOrder order,
                String path,
                Class<T> clazz,
                Supplier<Storage> storage,
                boolean force,
                boolean i18n,
                boolean aon
        ) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
            this(order, path, clazz, clazz.getConstructor().newInstance(), storage, force, i18n, aon);
        }
    }
}
