package de.waterdu.atlantis.file.auto;

import de.waterdu.atlantis.file.FileExtensions;
import de.waterdu.atlantis.file.datatypes.Configuration;
import de.waterdu.atlantis.file.storage.Flatfile;
import de.waterdu.atlantis.file.storage.Storage;
import de.waterdu.atlantis.util.java.ClassNameCache;

import java.util.Locale;

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

    public FileProperties(BasicConfiguration configuration, Class<? extends Configuration> clazz) {
        this(configuration.mod(), ClassNameCache.simpleName(clazz).toLowerCase(Locale.ROOT) + FileExtensions.JSON, Flatfile.class, LoadOrder.NORMAL, false, false, NoConfiguration.class, "", false);
    }

    public FileProperties(BasicData data, Class<? extends Configuration> clazz) {
        this(data.mod(), ClassNameCache.simpleName(clazz).toLowerCase(Locale.ROOT), Flatfile.class, LoadOrder.NORMAL, true, false, NoConfiguration.class, "", false);
    }

    public FileProperties(I18nFile i18n) {
        this(i18n.mod(), "lang.json", Flatfile.class, LoadOrder.NORMAL, false, true, NoConfiguration.class, "", false);
    }

    public FileProperties(FileBackedObject fbo) {
        this(fbo.mod(), fbo.path(), fbo.storage(), fbo.order(), fbo.force(), fbo.i18n(), fbo.providerClass(), fbo.providerMethod(), fbo.aon() || fbo.path().endsWith(".aon"));
    }
}
