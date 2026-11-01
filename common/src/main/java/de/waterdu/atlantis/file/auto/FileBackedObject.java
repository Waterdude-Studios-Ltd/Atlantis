package de.waterdu.atlantis.file.auto;

import de.waterdu.atlantis.file.AonComment;
import de.waterdu.atlantis.file.AtlantisConfig;
import de.waterdu.atlantis.file.AtlantisConfigProxy;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.datatypes.Data;
import de.waterdu.atlantis.file.datatypes.NamedData;
import de.waterdu.atlantis.file.datatypes.PlayerData;
import de.waterdu.atlantis.file.storage.Flatfile;
import de.waterdu.atlantis.file.storage.Storage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a file that an {@link AtlantisConfig} instance can access. {@link AtlantisConfigProxy} creates that instance.
 * <p>
 * This annotation can also specify the {@link Storage} to use. To do this, reference a {@link Configuration} class and
 * a method in it. The method has no arguments or one {@link String} argument (the mod ID). Alternatively, reference the
 * storage class directly if that class has a constructor with no arguments.
 * <p>
 * A change to the load order makes specific registered data types always load before or after the others. Use it when
 * a {@link Configuration} method provides the {@link Storage}. This makes sure that this {@link Configuration} is
 * available first.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface FileBackedObject {

    /**
     * The {@link AtlantisConfig} that this file is accessible from.
     *
     * @return The owner of this configuration's mod ID.
     */
    String mod();

    /**
     * If {@link Configuration} is implemented, this should contain a file extension, for example "file.json".
     * <p>
     * If {@link Data}, {@link NamedData}, or {@link PlayerData} are implemented instead, this should not contain a file
     * extension. Instead, this is the path of the directory for these data files, for example "dir".
     *
     * @return The path of this configuration.
     */
    String path();

    /**
     * The {@link Storage} implementation that this file uses. The storage class must have a constructor with no
     * arguments.
     * <p>
     * If {@link FileBackedObject#providerMethod()} and
     * {@link FileBackedObject#providerClass()} are set, this value is ignored.
     *
     * @return The class of the {@link Storage} type of this configuration.
     */
    Class<? extends Storage> storage() default Flatfile.class;

    /**
     * The load order of this file, relative to other configurations registered with the same mod ID.
     * If multiple instances have the same order, they load in the order in which they are found.
     *
     * @return The {@link LoadOrder} of this configuration, relative to others of the same mod ID.
     */
    LoadOrder order() default LoadOrder.NORMAL;

    /**
     * Whether to force this data type to load. This applies only to player data. It loads all player data into memory
     * slowly over time.
     *
     * @return Whether to force this configuration to load on start.
     */
    boolean force() default false;

    /**
     * Whether this data type is an i18n file. If true, it is mapped as an i18n file, and the i18n file access methods
     * become available.
     *
     * @return Whether this configuration is an i18n file.
     */
    boolean i18n() default false;

    /**
     * Use this together with {@link FileBackedObject#providerMethod()}.
     * If set, it should point to a {@link Configuration} class registered with the same mod ID at an earlier
     * {@link LoadOrder}. That class contains a method with arguments () or (String) that returns a {@link Storage}.
     * This configuration instance then uses that {@link Storage}. It does not try to instantiate the storage through
     * {@link FileBackedObject#storage()}.
     *
     * @return The {@link Configuration} class that contains a method that provides the {@link Storage} for this.
     */
    Class<? extends Configuration> providerClass() default NoConfiguration.class;

    /**
     * Use this together with {@link FileBackedObject#providerClass()}.
     * If set, it should point to a method in a {@link Configuration} class registered with the same mod ID at an
     * earlier {@link LoadOrder}. The method has arguments () or ({@link String}) and returns a {@link Storage}.
     * If the method has a {@link String} argument, it receives the mod ID when it is called.
     * This configuration instance then uses that {@link Storage}. It does not try to instantiate the storage through
     * {@link FileBackedObject#storage()}.
     *
     * @return The name of the method that provides the {@link Storage} for this, in the class that
     * {@link FileBackedObject#providerClass()} specifies.
     */
    String providerMethod() default "";

    /**
     * Whether this data type is AON (Atlantis Object Notation). If true, the type and its fields also have an
     * {@link AonComment}.
     * This also controls whether to convert this file from JSON to AON if the JSON file exists.
     * If the path of this configuration instance ends in .aon, this value is automatically true.
     *
     * @return Whether this configuration is an Atlantis Object Notation file.
     */
    boolean aon() default false;
}
