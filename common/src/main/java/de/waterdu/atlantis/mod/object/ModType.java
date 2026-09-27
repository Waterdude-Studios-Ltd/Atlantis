package de.waterdu.atlantis.mod.object;

public interface ModType {

    ModObjectKey type();

    default boolean namespaceMatches(ModType object) {
        return this.type().namespace().equals(object.type().namespace());
    }

    default boolean objectMatches(ModType object) {
        return this.type().object().equals(object.type().object());
    }

    default boolean typeMatches(ModType object) {
        return this.namespaceMatches(object) && this.objectMatches(object);
    }
}
