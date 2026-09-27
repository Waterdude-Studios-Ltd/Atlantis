package de.waterdu.atlantis.util.placeholder;

import java.util.List;

public interface AtlantisPlaceholderExtension<T> {

    String name();

    int priority();

    List<String> description();

    List<String> examples();

    boolean matches(T object, String placeholder);

    default boolean matchesObject(Object object, String placeholder) {
        return false;
    }

    String parse(T object, String placeholder);

    default String parseObject(Object object, String placeholder) {
        return null;
    }
}
