package de.waterdu.atlantis.util.java.interfaces;

@FunctionalInterface
public interface ThrowingSupplier<T> {

    T get() throws Throwable;
}
