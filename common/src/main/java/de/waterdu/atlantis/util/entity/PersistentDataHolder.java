package de.waterdu.atlantis.util.entity;

import net.minecraft.nbt.CompoundTag;

/**
 * Atlantis implements this on every {@code Entity}. Use {@link EntityUtils#persistentData} instead of this directly.
 */
public interface PersistentDataHolder {

    /**
     * @return the mutable persistent tag of the entity
     */
    CompoundTag atlantis$persistentData();
}
