package de.waterdu.atlantis.file.datatypes;

import de.waterdu.atlantis.util.java.ClassNameCache;

import java.util.UUID;

public interface Data extends Configuration {

    UUID uuid();

    void setUUID(UUID uuid);

    String identifier();

    default String dir() {
        return "";
    }

    default void save() {
        this.save(false);
    }

    default void save(boolean immediate) {
        this.proxy().save(this, immediate);
    }

    default void saveAll() {
        this.proxy().save(this.getClass());
    }

    default int defaultHashCode() {
        return this.uuid().hashCode();
    }

    default boolean defaultEquals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!this.getClass().isInstance(obj)) {
            return false;
        }
        return ((Data) obj).uuid().equals(this.uuid());
    }

    default String defaultToString() {
        return ClassNameCache.simpleName(this.getClass()) + "(" + this.uuid().toString() + ")";
    }
}
