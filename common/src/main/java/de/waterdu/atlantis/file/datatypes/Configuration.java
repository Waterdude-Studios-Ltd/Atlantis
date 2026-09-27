package de.waterdu.atlantis.file.datatypes;

import de.waterdu.atlantis.file.AtlantisConfig;
import de.waterdu.atlantis.file.AtlantisConfigProxy;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.util.java.ClassNameCache;
import de.waterdu.atlantis.util.java.UUIDUtils;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

public interface Configuration {

    default void save() {
        this.proxy().save(this.getClass());
    }

    default void load() {
       this.proxy().load(this.getClass());
    }

    default AtlantisConfigProxy.Impl proxy() {
        return AtlantisConfigProxy.of(AtlantisConfig.findOwnerMod(this.getClass()));
    }

    @SuppressWarnings("unchecked")
    default <T extends Configuration> Config<T> config() {
        return (Config<T>) this.proxy().config(this.getClass());
    }

    @ApiStatus.Internal
    default UUID uuid() {
        return UUIDUtils.ZEROED_UUID;
    }

    @ApiStatus.Internal
    default String identifier() {
        return ClassNameCache.simpleName(this.getClass());
    }

    @ApiStatus.Internal
    default String dir() {
        return "";
    }
}
