package de.waterdu.atlantis.util.java;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.util.java.interfaces.Lazy;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Holds a time zone ID and the {@link ZoneId} for it. The zone is resolved on first use, so an invalid ID throws only
 * then.
 */
public class Timezone {

    public static final Timezone UTC = Timezone.of("UTC");

    private final String id;
    private final Lazy<ZoneId> zone;

    /**
     * Creates a time zone.
     *
     * @param id the ID for {@link ZoneId#of(String)}
     */
    private Timezone(String id) {
        this.id = id;
        this.zone = Lazy.of(() -> ZoneId.of(id));
    }

    /**
     * Creates a time zone.
     *
     * @param zone the ID for {@link ZoneId#of(String)}
     * @return the new time zone
     */
    public static Timezone of(String zone) {
        return new Timezone(zone);
    }

    /**
     * @return the zone for the ID
     */
    public ZoneId zone() {
        return this.zone.get();
    }

    /**
     * @return the ID of the time zone
     */
    public String id() {
        return this.id;
    }

    /**
     * Gets the current date and time in this time zone.
     *
     * @return the current date and time
     */
    public LocalDateTime now() {
        return LocalDateTime.now(this.zone());
    }

    /**
     * Serializes a {@link Timezone} as its ID.
     */
    public static class TypeAdapter implements JsonSerializer<Timezone>, JsonDeserializer<Timezone> {

        /**
         * Creates a time zone from the JSON string.
         *
         * @param json the JSON string that holds the ID
         * @param typeOfT the type to deserialize to
         * @param context the deserialization context
         * @return the time zone
         */
        @Override
        public Timezone deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Timezone.of(json.getAsString());
        }

        /**
         * Writes the ID as a JSON string.
         *
         * @param src the time zone to serialize
         * @param typeOfSrc the type of the time zone
         * @param context the serialization context
         * @return the JSON string
         */
        @Override
        public JsonElement serialize(Timezone src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.id());
        }
    }
}
