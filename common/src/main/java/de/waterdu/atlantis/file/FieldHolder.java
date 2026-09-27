package de.waterdu.atlantis.file;

import de.waterdu.atlantis.util.java.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public record FieldHolder(Field field, Type type) {

    private FieldHolder(Field field) {
        this(field, ReflectionUtils.getTypeForField(field));
    }

    public static FieldHolder of(Field field) {
        return new FieldHolder(field);
    }
}
