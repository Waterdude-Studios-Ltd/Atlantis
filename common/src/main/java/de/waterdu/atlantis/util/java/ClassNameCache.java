package de.waterdu.atlantis.util.java;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

/**
 * Caches the names of classes. An entry expires 30 minutes after its last access.
 */
public final class ClassNameCache {

    private static final LoadingCache<Class<?>, Names> NAMES = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(Names::new);

    private ClassNameCache() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Gets the cached value of {@link Class#getName()}.
     *
     * @param clazz the class
     * @return the name of the class
     */
    public static String name(Class<?> clazz) {
        return NAMES.get(clazz).name;
    }

    /**
     * Gets the cached value of {@link Class#getSimpleName()}.
     *
     * @param clazz the class
     * @return the simple name of the class
     */
    public static String simpleName(Class<?> clazz) {
        return NAMES.get(clazz).simpleName;
    }

    /**
     * Gets the cached value of {@link Class#getCanonicalName()}.
     *
     * @param clazz the class
     * @return the canonical name of the class, or {@code null} if it has none
     */
    public static String canonicalName(Class<?> clazz) {
        return NAMES.get(clazz).canonicalName;
    }

    /**
     * Gets the cached value of {@link Class#getPackageName()}.
     *
     * @param clazz the class
     * @return the package name of the class
     */
    public static String packageName(Class<?> clazz) {
        return NAMES.get(clazz).packageName;
    }

    /**
     * Gets the cached value of {@link Class#getTypeName()}.
     *
     * @param clazz the class
     * @return the type name of the class
     */
    public static String typeName(Class<?> clazz) {
        return NAMES.get(clazz).typeName;
    }

    /**
     * Holds the names of one class.
     *
     * @param name the name of the class
     * @param simpleName the simple name of the class
     * @param canonicalName the canonical name of the class
     * @param packageName the package name of the class
     * @param typeName the type name of the class
     */
    private record Names(String name, String simpleName, String canonicalName, String packageName, String typeName) {

        /**
         * Reads all names from the class.
         *
         * @param clazz the class
         */
        private Names(Class<?> clazz) {
            this(clazz.getName(), clazz.getSimpleName(), clazz.getCanonicalName(), clazz.getPackageName(), clazz.getTypeName());
        }
    }
}
