package de.waterdu.atlantis.util.java;

/**
 * Throws common exceptions.
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Throws an exception that says the class cannot be instantiated. Private constructors of static utility classes
     * call this.
     *
     * @throws IllegalStateException always
     */
    public static void throwCannotInstantiateClassException() {
        throw new IllegalStateException("Cannot instantiate this class");
    }
}
