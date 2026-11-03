package de.waterdu.atlantis.util.java;

import com.google.gson.reflect.TypeToken;
import de.waterdu.atlantis.AtlantisLogger;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Reads types, finds classes and invokes methods with reflection.
 */
public final class ReflectionUtils {

    private ReflectionUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Gets the generic type of the field as a Gson type. It keeps the type arguments of parameterized and generic
     * array types.
     *
     * @param field the field
     * @return the type of the field
     */
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

    /**
     * Also initializes the class if it exists.
     */
    public static boolean doesClassExist(String classpath) {
        try {
            Class.forName(classpath);
            return true;
        } catch (ClassNotFoundException e) {
            AtlantisLogger.warn("Class not found: " + classpath, e);
            return false;
        }
    }

    /**
     * @return the result, or {@code null} if the method is not accessible or throws
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeSafely(Method method, Object object, Object... args) {
        try {
            return (T) method.invoke(object, args);
        } catch (ClassCastException | IllegalAccessException | InvocationTargetException e) {
            AtlantisLogger.warn("Failed to invoke method " + method.getName() + " on " + object.getClass().getSimpleName(), e);
            return null;
        }
    }
}
