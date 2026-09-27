package de.waterdu.atlantis.util.java;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class ReflectionUtils {

    private ReflectionUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static Type getTypeForField(Field field) {
        var type = field.getGenericType();
        if (type instanceof ParameterizedType pType) {
            return TypeToken.getParameterized(pType.getRawType(), pType.getActualTypeArguments()).getType();
        } else if (type instanceof GenericArrayType gaType) {
            return TypeToken.getArray(gaType.getGenericComponentType()).getType();
        } else {
            return TypeToken.get(field.getType()).getType();
        }
    }

    public static boolean doesClassExist(String classpath) {
        try {
            Class.forName(classpath);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeSafely(Method method, Object object, Object... args) {
        try {
            return (T) method.invoke(object, args);
        } catch (ClassCastException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }
}
