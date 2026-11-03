package de.waterdu.atlantis.util.java;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Reads and writes JSON values with Gson.
 */
public final class JsonUtils {

    private JsonUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * @return {@code def} if the key is missing or its value is JSON null
     */
    public static <T> T getOrDefault(JsonObject object, String key, Class<T> type, T def, JsonDeserializationContext context) {
        if (object.has(key) && !object.get(key).isJsonNull()) {
            return context.deserialize(object.get(key), type);
        } else {
            return def;
        }
    }

    /**
     * @return {@code def} if the key is missing or its value is JSON null
     */
    public static <T> T getOrDefault(JsonObject object, String key, TypeToken<T> type, T def, JsonDeserializationContext context) {
        if (object.has(key) && !object.get(key).isJsonNull()) {
            return context.deserialize(object.get(key), type.getType());
        } else {
            return def;
        }
    }

    /**
     * Serializes each element of the collection and adds the result to a new JSON array.
     *
     * @param <T> the type of the elements
     * @param collection the elements to serialize
     * @param serializer serializes one element
     * @return the JSON array
     */
    public static <T> JsonArray serializeList(Collection<T> collection, Function<T, JsonElement> serializer) {
        var result = new JsonArray();
        for (var element : collection) {
            result.add(serializer.apply(element));
        }
        return result;
    }

    /**
     * @return the elements, without those for which the deserializer returns {@code null}
     */
    public static <T> ImmutableList<T> deserializeListImmutable(JsonArray array, Function<JsonElement, T> deserializer) {
        var result = JsonUtils.deserializeList(array, deserializer);
        result.remove(null);
        return ImmutableList.copyOf(result);
    }

    /**
     * @return the elements, without those for which the deserializer returns {@code null}
     */
    public static <T> List<T> deserializeList(JsonArray array, Function<JsonElement, T> deserializer) {
        var result = new ArrayList<T>();
        for (var property : array) {
            var apply = deserializer.apply(property);
            if (apply != null) {
                result.add(apply);
            }
        }
        return result;
    }

    /**
     * Deserializes the JSON array into an array, without the elements for which the deserializer returns
     * {@code null}.
     *
     * @param <T> the type of the elements
     * @param array the JSON array
     * @param deserializer deserializes one element
     * @return the elements. The runtime type of the array is {@code Object[]}, so a caller that expects a more specific
     *     array type gets a {@link ClassCastException}.
     * @deprecated use {@link #deserializeArray(JsonArray, Class, Function)}, which creates an array of the correct type
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <T> T[] deserializeArray(JsonArray array, Function<JsonElement, T> deserializer) {
        return (T[]) JsonUtils.deserializeList(array, deserializer).toArray();
    }

    /**
     * Deserializes the JSON array into an array, without the elements for which the deserializer returns
     * {@code null}.
     *
     * @param <T> the type of the elements
     * @param array the JSON array
     * @param type the component type of the array
     * @param deserializer deserializes one element
     * @return the elements
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] deserializeArray(JsonArray array, Class<T> type, Function<JsonElement, T> deserializer) {
        var result = JsonUtils.deserializeList(array, deserializer);
        return result.toArray((T[]) Array.newInstance(type, result.size()));
    }
}
