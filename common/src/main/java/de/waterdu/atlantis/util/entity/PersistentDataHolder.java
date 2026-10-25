package de.waterdu.atlantis.util.entity;

import net.minecraft.nbt.CompoundTag;

/**
 * Implemented on every {@code Entity} by Atlantis. Use {@link EntityUtils#persistentData} rather than this directly.
 */
public interface PersistentDataHolder {

    CompoundTag atlantis$persistentData();
}
