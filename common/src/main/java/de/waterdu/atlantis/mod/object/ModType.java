package de.waterdu.atlantis.mod.object;

/**
 * Has a {@link ModObjectKey} that identifies a type from another mod.
 */
public interface ModType {

    /**
     * @return the key of the type
     */
    ModObjectKey type();

    /**
     * Checks if the namespaces of both keys are equal.
     *
     * @param object the other type
     * @return {@code true} if the namespaces match
     */
    default boolean namespaceMatches(ModType object) {
        return this.type().namespace().equals(object.type().namespace());
    }

    /**
     * Checks if the paths of both keys are equal.
     *
     * @param object the other type
     * @return {@code true} if the paths match
     */
    default boolean objectMatches(ModType object) {
        return this.type().object().equals(object.type().object());
    }

    /**
     * Checks if the namespaces and the paths of both keys are equal.
     *
     * @param object the other type
     * @return {@code true} if the keys match
     */
    default boolean typeMatches(ModType object) {
        return this.namespaceMatches(object) && this.objectMatches(object);
    }
}
