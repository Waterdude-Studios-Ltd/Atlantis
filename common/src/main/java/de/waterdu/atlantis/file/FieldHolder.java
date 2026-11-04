package de.waterdu.atlantis.file;

import de.waterdu.atlantis.util.java.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Holds a field and its generic type.
 *
 * @param field the field
 * @param type the generic type of the field
 */
public record FieldHolder(Field field, Type type) {

    /**
     * Creates a holder with the type from {@link ReflectionUtils#getTypeForField(Field)}.
     *
     * @param field the field
     */
    private FieldHolder(Field field) {
        this(field, ReflectionUtils.getTypeForField(field));
    }

    /**
     * Creates a holder for the field and its generic type.
     *
     * @param field the field
     * @return the new holder
     */
    public static FieldHolder of(Field field) {
        return new FieldHolder(field);
    }
}
