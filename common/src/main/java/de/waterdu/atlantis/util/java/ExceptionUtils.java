package de.waterdu.atlantis.util.java;

public final class ExceptionUtils {

    private ExceptionUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void throwCannotInstantiateClassException() {
        throw new IllegalStateException("Cannot instantiate this class");
    }
}
