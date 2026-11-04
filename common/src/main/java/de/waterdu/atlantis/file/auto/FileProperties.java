package de.waterdu.atlantis.file.auto;

import de.waterdu.atlantis.file.FileExtensions;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.storage.Flatfile;
import de.waterdu.atlantis.file.storage.Storage;
import de.waterdu.atlantis.util.java.ClassNameCache;

import java.util.Locale;

/**
 * Holds the registration settings of a configuration class, taken from its config annotation.
 *
 * @param mod the ID of the mod that owns the file
 * @param path the path of the file or directory, relative to the config directory of the mod
 * @param storage the storage class
 * @param order the load order relative to the other configs of the mod
 * @param force reads player data from storage when the configs of the mod load
 * @param i18n {@code true} if the file is an i18n file
 * @param providerClass the class that provides the storage, or {@link NoConfiguration} for none
 * @param providerMethod the name of the method that provides the storage, or empty for none
 * @param aon {@code true} if the file uses the AON format
 */
public record FileProperties(
    String mod,
    String path,
    Class<? extends Storage> storage,
    LoadOrder order,
    boolean force,
    boolean i18n,
    Class<? extends Configuration> providerClass,
    String providerMethod,
    boolean aon
) {

    /**
     * Creates the properties of a flatfile config. The path is the lower-case simple class name with {@code .json}.
     *
     * @param configuration the annotation of the class
     * @param clazz the configuration class
     */
    public FileProperties(BasicConfiguration configuration, Class<? extends Configuration> clazz) {
        this(configuration.mod(), ClassNameCache.simpleName(clazz).toLowerCase(Locale.ROOT) + FileExtensions.JSON, Flatfile.class, LoadOrder.NORMAL, false, false, NoConfiguration.class, "", false);
    }

    /**
     * Creates the properties of a flatfile data config that force loads. The path is the lower-case simple class name.
     *
     * @param data the annotation of the class
     * @param clazz the configuration class
     */
    public FileProperties(BasicData data, Class<? extends Configuration> clazz) {
        this(data.mod(), ClassNameCache.simpleName(clazz).toLowerCase(Locale.ROOT), Flatfile.class, LoadOrder.NORMAL, true, false, NoConfiguration.class, "", false);
    }

    /**
     * Creates the properties of a flatfile i18n config at {@code lang.json}.
     *
     * @param i18n the annotation of the class
     */
    public FileProperties(I18nFile i18n) {
        this(i18n.mod(), "lang.json", Flatfile.class, LoadOrder.NORMAL, false, true, NoConfiguration.class, "", false);
    }

    /**
     * Creates the properties from the values of the annotation. The file uses the AON format if the annotation sets
     * {@code aon} or the path ends with {@code .aon}.
     *
     * @param fbo the annotation of the class
     */
    public FileProperties(FileBackedObject fbo) {
        this(fbo.mod(), fbo.path(), fbo.storage(), fbo.order(), fbo.force(), fbo.i18n(), fbo.providerClass(), fbo.providerMethod(), fbo.aon() || fbo.path().endsWith(".aon"));
    }
}
