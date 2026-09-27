package de.waterdu.atlantis.util.java;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

public final class ClassNameCache {

    private static final LoadingCache<Class<?>, Names> NAMES = Caffeine.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(Names::new);

    private ClassNameCache() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static String name(Class<?> clazz) {
        return NAMES.get(clazz).name;
    }

    public static String simpleName(Class<?> clazz) {
        return NAMES.get(clazz).simpleName;
    }

    public static String canonicalName(Class<?> clazz) {
        return NAMES.get(clazz).canonicalName;
    }

    public static String packageName(Class<?> clazz) {
        return NAMES.get(clazz).packageName;
    }

    public static String typeName(Class<?> clazz) {
        return NAMES.get(clazz).typeName;
    }

    private record Names(String name, String simpleName, String canonicalName, String packageName, String typeName) {

        private Names(Class<?> clazz) {
            this(clazz.getName(), clazz.getSimpleName(), clazz.getCanonicalName(), clazz.getPackageName(), clazz.getTypeName());
        }
    }
}
