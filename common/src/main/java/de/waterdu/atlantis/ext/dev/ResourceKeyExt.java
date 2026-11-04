package de.waterdu.atlantis.ext.dev;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;

/**
 * Adds registry lookups to {@link net.minecraft.resources.ResourceKey} through interface injection.
 *
 * @param <T> the type of the value that the key names
 */
public interface ResourceKeyExt<T> {

    /**
     * Throws an exception when no server runs.
     *
     * @return the registry that holds the value of this key
     */
    default Registry<T> registryAccess() {
        return null;
    }

    /**
     * Throws an exception when no server runs.
     *
     * @return the registered value. Can be null, or the registry default, when the key is not registered.
     */
    default T get() {
        return null;
    }

    /**
     * Use this only on the key of a registry, for example {@code Registries.BLOCK}.
     * Throws an exception when no server runs.
     *
     * @return the registry that this key names
     */
    default T registryFromRegistryKey() {
        return null;
    }

    /**
     * Throws an exception when no server runs or the key is not registered.
     */
    default Holder<T> holder() {
        return null;
    }
}
