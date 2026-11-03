package de.waterdu.atlantis.util.placeholder;

import java.util.List;

/**
 * Supplies placeholders to a placeholder mod through {@link AtlantisPlaceholderProxy}.
 *
 * @param <T> the type of the context object that the placeholders read from
 */
public interface AtlantisPlaceholderExtension<T> {

    /**
     * @return the name of the extension
     */
    String name();

    /**
     * @return the priority of the extension in the placeholder mod
     */
    int priority();

    /**
     * @return the lines that describe the extension
     */
    List<String> description();

    /**
     * @return the example placeholders of the extension
     */
    List<String> examples();

    /**
     * Checks if the extension can parse the placeholder for the context object.
     *
     * @param object the context object
     * @param placeholder the placeholder to check
     * @return {@code true} if the extension can parse the placeholder
     */
    boolean matches(T object, String placeholder);

    /**
     * Checks if the extension can parse the placeholder for a context object of any type. The default returns
     * {@code false}.
     *
     * @param object the context object
     * @param placeholder the placeholder to check
     * @return {@code true} if the extension can parse the placeholder
     */
    default boolean matchesObject(Object object, String placeholder) {
        return false;
    }

    /**
     * Gets the value of the placeholder for the context object.
     *
     * @param object the context object
     * @param placeholder the placeholder to parse
     * @return the value of the placeholder
     */
    String parse(T object, String placeholder);

    /**
     * Gets the value of the placeholder for a context object of any type. The default returns {@code null}.
     *
     * @param object the context object
     * @param placeholder the placeholder to parse
     * @return the value of the placeholder, or {@code null} if the extension cannot parse it
     */
    default String parseObject(Object object, String placeholder) {
        return null;
    }
}
