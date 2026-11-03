package de.waterdu.atlantis.util.java.interfaces;

/**
 * Supplies a value. It can throw any {@link Throwable}.
 *
 * @param <T> the type of the value
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {

    /**
     * Gets the value.
     *
     * @return the value
     * @throws Throwable if the value cannot be supplied
     */
    T get() throws Throwable;
}
