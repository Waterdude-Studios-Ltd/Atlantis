package de.waterdu.atlantis.file.datatypes;

import de.waterdu.atlantis.util.java.ClassNameCache;

import java.util.UUID;

/**
 * Marks a configuration that has many instances, each with its own UUID and file. Implementations must override
 * {@code hashCode}, {@code equals} and {@code toString}.
 */
public interface Data extends Configuration {

    /**
     * @return the UUID of this instance
     */
    UUID uuid();

    /**
     * Sets the UUID of this instance.
     *
     * @param uuid the new UUID
     */
    void setUUID(UUID uuid);

    /**
     * @return the file name of this instance, without the extension
     */
    String identifier();

    /**
     * @return the subdirectory of this instance in the config directory. Separate levels with {@code /}. Empty for
     *     none.
     */
    default String dir() {
        return "";
    }

    /**
     * Writes only this instance. Player data waits for the async autosave or for the player to log out.
     */
    default void save() {
        this.save(false);
    }

    /**
     * Writes only this instance. Data that is not player data always writes on another thread.
     *
     * @param immediate writes player data now. If not set, player data waits for the async autosave or for the player
     *     to log out.
     */
    default void save(boolean immediate) {
        this.proxy().save(this, immediate);
    }

    /**
     * Writes every instance of this type, not only this one.
     */
    default void saveAll() {
        this.proxy().save(this.getClass());
    }

    /**
     * Gets a hash code from the UUID. Implementations can return this from {@code hashCode}.
     *
     * @return the hash code of the UUID
     */
    default int defaultHashCode() {
        return this.uuid().hashCode();
    }

    /**
     * Compares by UUID. Implementations can return this from {@code equals}.
     *
     * @param obj the object to compare
     * @return {@code true} if the object is this instance, or an instance of this class with the same UUID
     */
    default boolean defaultEquals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!this.getClass().isInstance(obj)) {
            return false;
        }
        return ((Data) obj).uuid().equals(this.uuid());
    }

    /**
     * Gets a string with the class name and the UUID. Implementations can return this from {@code toString}.
     *
     * @return the simple class name followed by the UUID in parentheses
     */
    default String defaultToString() {
        return ClassNameCache.simpleName(this.getClass()) + "(" + this.uuid().toString() + ")";
    }
}
