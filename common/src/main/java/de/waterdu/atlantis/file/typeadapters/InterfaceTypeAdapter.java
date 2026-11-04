package de.waterdu.atlantis.file.typeadapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Serializes an interface value together with the class name of its implementation, so that it can be deserialized
 * again.
 *
 * @param <T> the interface type
 */
public class InterfaceTypeAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

    /**
     * Writes an object with a {@code type} member that holds the class name and a {@code data} member that holds the
     * value.
     *
     * @param object the value to serialize
     * @param interfaceType the interface type
     * @param context the serialization context
     * @return the JSON object
     */
    @Override
    public final JsonElement serialize(final T object, final Type interfaceType, final JsonSerializationContext context) {
        var member = new JsonObject();
        member.addProperty("type", object.getClass().getName());
        member.add("data", context.serialize(object));
        return member;
    }

    /**
     * Reads the {@code data} member as the class that the {@code type} member names.
     *
     * @param elem the JSON object
     * @param interfaceType the interface type
     * @param context the deserialization context
     * @return the value
     * @throws JsonParseException if a member is missing or the class does not exist
     */
    @Override
    public final T deserialize(final JsonElement elem, final Type interfaceType, final JsonDeserializationContext context) throws JsonParseException {
        var member = (JsonObject) elem;
        var typeString = this.get(member, "type");
        var data = this.get(member, "data");
        var actualType = this.typeForName(typeString);
        return context.deserialize(data, actualType);
    }

    /**
     * Gets the class with the name in the element.
     *
     * @param typeElem the element that holds the class name
     * @return the class
     * @throws JsonParseException if the class does not exist
     */
    private Type typeForName(final JsonElement typeElem) {
        try {
            return Class.forName(typeElem.getAsString());
        }
        catch (ClassNotFoundException e) {
            throw new JsonParseException(e);
        }
    }

    /**
     * Gets a member of the object.
     *
     * @param wrapper the JSON object
     * @param memberName the name of the member
     * @return the member
     * @throws JsonParseException if the member is missing
     */
    private JsonElement get(final JsonObject wrapper, final String memberName) {
        var elem = wrapper.get(memberName);
        if (elem == null) {
            throw new JsonParseException("no '" + memberName + "' member found in json file.");
        }
        return elem;
    }
}