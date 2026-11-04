package de.waterdu.atlantis.file.typeadapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * Serializes a {@link ResourceLocation} as a string.
 */
public class ResourceLocationTypeAdapter implements JsonSerializer<ResourceLocation>, JsonDeserializer<ResourceLocation> {

    /**
     * Parses the string in lower case.
     *
     * @param json the JSON string
     * @param typeOfT the type to read
     * @param context the deserialization context
     * @return the resource location, or {@code null} if the string is not valid
     */
    @Override
    public ResourceLocation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return ResourceLocation.tryParse(json.getAsString().toLowerCase(Locale.ROOT));
    }

    /**
     * Writes the resource location in the form {@code namespace:path}.
     *
     * @param src the resource location
     * @param typeOfSrc the type to write
     * @param context the serialization context
     * @return the JSON string
     */
    @Override
    public JsonElement serialize(ResourceLocation src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }
}
