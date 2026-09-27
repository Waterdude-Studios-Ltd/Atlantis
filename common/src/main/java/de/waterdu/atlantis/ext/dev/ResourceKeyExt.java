package de.waterdu.atlantis.ext.dev;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;

public interface ResourceKeyExt<T> {

    default Registry<T> registryAccess() {
        return null;
    }

    default T get() {
        return null;
    }

    default T registryFromRegistryKey() {
        return null;
    }

    default Holder<T> holder() {
        return null;
    }
}
