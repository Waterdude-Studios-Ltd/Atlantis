package de.waterdu.atlantis.file.datatypes;

import de.waterdu.atlantis.file.AtlantisConfig;
import de.waterdu.atlantis.file.AtlantisConfigProxy;
import de.waterdu.atlantis.file.Config;
import de.waterdu.atlantis.util.java.ClassNameCache;
import de.waterdu.atlantis.util.java.UUIDUtils;
import org.jetbrains.annotations.ApiStatus;

import java.util.UUID;

/**
 * Marks a class as a configuration that Atlantis reads from and writes to storage. A plain configuration has one
 * instance. See {@link Data} for types with many instances.
 */
public interface Configuration {

    /**
     * Writes on {@link de.waterdu.atlantis.Atlantis#THREAD_POOL}. Returns before the write completes.
     */
    default void save() {
        this.proxy().save(this.getClass());
    }

    /**
     * Starts a read from storage. Returns before the read completes.
     */
    default void load() {
       this.proxy().load(this.getClass());
    }

    /**
     * @return the config proxy of the mod that owns this type
     */
    default AtlantisConfigProxy.Impl proxy() {
        return AtlantisConfigProxy.of(AtlantisConfig.findOwnerMod(this.getClass()));
    }

    /**
     * Gets the config of this type, cast to the type that the caller expects.
     *
     * @param <T> the configuration type
     * @return the config
     */
    @SuppressWarnings("unchecked")
    default <T extends Configuration> Config<T> config() {
        return (Config<T>) this.proxy().config(this.getClass());
    }

    /**
     * @return {@link UUIDUtils#ZEROED_UUID}, because a configuration has only one instance
     */
    @ApiStatus.Internal
    default UUID uuid() {
        return UUIDUtils.ZEROED_UUID;
    }

    /**
     * @return the simple name of the class
     */
    @ApiStatus.Internal
    default String identifier() {
        return ClassNameCache.simpleName(this.getClass());
    }

    /**
     * @return an empty string, because a configuration has no subdirectory
     */
    @ApiStatus.Internal
    default String dir() {
        return "";
    }
}
