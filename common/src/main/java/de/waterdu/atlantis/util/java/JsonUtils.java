package de.waterdu.atlantis.util.java;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class JsonUtils {

    private JsonUtils() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static <T> T getOrDefault(JsonObject object, String key, Class<T> type, T def, JsonDeserializationContext context) {
        if (object.has(key) && !object.get(key).isJsonNull()) {
            return context.deserialize(object.get(key), type);
        } else {
            return def;
        }
    }

    public static <T> T getOrDefault(JsonObject object, String key, TypeToken<T> type, T def, JsonDeserializationContext context) {
        if (object.has(key) && !object.get(key).isJsonNull()) {
            return context.deserialize(object.get(key), type.getType());
        } else {
            return def;
        }
    }

    public static <T> JsonArray serializeList(Collection<T> collection, Function<T, JsonElement> serializer) {
        var result = new JsonArray();
        for (var element : collection) {
            result.add(serializer.apply(element));
        }
        return result;
    }

    public static <T> ImmutableList<T> deserializeListImmutable(JsonArray array, Function<JsonElement, T> deserializer) {
        var result = JsonUtils.deserializeList(array, deserializer);
        result.remove(null);
        return ImmutableList.copyOf(result);
    }

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

    @SuppressWarnings("unchecked")
    public static <T> T[] deserializeArray(JsonArray array, Function<JsonElement, T> deserializer) {
        var result = (T[]) new Object[array.size()];
        var i = 0;
        for (var property : array) {
            var apply = deserializer.apply(property);
            if (apply != null) {
                result[i++] = apply;
            }
        }
        return result;
    }
}
