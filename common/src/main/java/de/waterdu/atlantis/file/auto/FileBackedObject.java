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
 * Annotation for designating a file accessible via an {@link AtlantisConfig} instance created via
 * {@link AtlantisConfigProxy}.
 * <p>
 * Allows the {@link Storage} wanted to be specified at this point as well, by referencing a {@link Configuration} class
 * and a method within it, either with zero-args or one {@link String} arg (the modid), or via the storage class
 * directly if that class has a zero-args constructor available.
 * <p>
 * Changing the load order will make it so specific registered data types will always load before or after the rest.
 * This should be used when the above is used, ensuring the {@link Configuration} needed to provide the {@link Storage}
 * is available for use.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface FileBackedObject {

    /**
     * The {@link AtlantisConfig} this will be accessible from.
     *
     * @return The owner of this configuration's modid.
     */
    String mod();

    /**
     * If {@link Configuration} is implemented, this should contain a file extension, eg "file.json".
     * <p>
     * If {@link Data}, {@link NamedData}, or {@link PlayerData} are implemented instead, this should not contain a file
     * extension, and instead, this will be the directory path these data files go, eg "dir".
     *
     * @return The path of this configuration.
     */
    String path();

    /**
     * The {@link Storage} implementation this will use. The storage class provided must have a zero-args constructor.
     * <p>
     * If {@link FileBackedObject#providerMethod()} and
     * {@link FileBackedObject#providerClass()} are set, this will be ignored.
     *
     * @return The class of the {@link Storage} type of this configuration.
     */
    Class<? extends Storage> storage() default Flatfile.class;

    /**
     * The order this should be loaded in respect to other configurations registered with the same modid.
     * Where multiple instances share the same ordering, they will load in the order they are located.
     *
     * @return The {@link LoadOrder} of this configuration in respect to others of the same modid.
     */
    LoadOrder order() default LoadOrder.NORMAL;

    /**
     * Whether this data type should be forced to load. This only applies to player data, and will cause it to load all
     * player data slowly over time into memory.
     *
     * @return Whether this configuration should be forced to load on start.
     */
    boolean force() default false;

    /**
     * Whether this data type is a i18n file, and should be mapped as such. Allows use of i18n file access methods.
     *
     * @return Whether this configuration is a i18n file.
     */
    boolean i18n() default false;

    /**
     * This is used in conjunction with {@link FileBackedObject#providerMethod()}.
     * If this is used, it should point to a {@link Configuration} class registered with the same modid at an earlier
     * {@link LoadOrder}, containing a method with args () or (String), which returns an instance of {@link Storage}.
     * This configuration instance will then use that {@link Storage} as opposed to trying to instantiate it via
     * {@link FileBackedObject#storage()}.
     *
     * @return The {@link Configuration} class containing a method which provides the {@link Storage} for this.
     */
    Class<? extends Configuration> providerClass() default NoConfiguration.class;

    /**
     * This is used in conjunction with {@link FileBackedObject#providerClass()}.
     * If this is used, it should point to a method in a {@link Configuration} class registered with the same modid at
     * an earlier {@link LoadOrder}, with args () or ({@link String}), which returns an instance of {@link Storage}.
     * If a method with a {@link String} arg is used, it will be passed the modid when called.
     * This configuration instance will then use that {@link Storage} as opposed to trying to instantiate it via
     * {@link FileBackedObject#storage()}.
     *
     * @return The method name which provides the {@link Storage} for this in the class specified by
     * {@link FileBackedObject#providerClass()}.
     */
    String providerMethod() default "";

    /**
     * If this data type is AON, Atlantis Object Notation. If so, it will also have an {@link AonComment} on the type,
     * and on its fields.
     * This also controls if this should be converted from JSON to AON, if the JSON exists.
     * If the path of this configuration instance ends in .aon, this will automatically be considered as true.
     *
     * @return Whether this configuration is an Atlantis Object Notation file or not.
     */
    boolean aon() default false;
}
