package de.waterdu.atlantis.mod.object;

/**
 * Holds an object from another mod, together with the key of its type.
 *
 * @param type the key of the type of the object
 * @param reference the object
 */
public record ModObject(ModObjectKey type, Object reference) implements ModType {

    /**
     * Casts the object to the type that the caller expects. This method does not check the type.
     *
     * @param <T> the type of the object
     * @return the object
     */
    @SuppressWarnings("unchecked")
    public <T> T dereference() {
        return (T) this.reference;
    }

    /**
     * @param clazz only sets {@code T}. This method does not check the type, so a wrong class fails later, at the first
     *     use of the result.
     */
    @SuppressWarnings("unchecked")
    public <T> T dereference(Class<T> clazz) {
        return (T) this.reference;
    }
}
