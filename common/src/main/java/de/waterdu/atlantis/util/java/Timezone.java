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

public class Timezone {

    public static final Timezone UTC = Timezone.of("UTC");

    private final String id;
    private final Lazy<ZoneId> zone;

    private Timezone(String id) {
        this.id = id;
        this.zone = Lazy.of(() -> ZoneId.of(id));
    }

    public static Timezone of(String zone) {
        return new Timezone(zone);
    }

    public ZoneId zone() {
        return this.zone.get();
    }

    public String id() {
        return this.id;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(this.zone());
    }

    public static class TypeAdapter implements JsonSerializer<Timezone>, JsonDeserializer<Timezone> {

        @Override
        public Timezone deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Timezone.of(json.getAsString());
        }

        @Override
        public JsonElement serialize(Timezone src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.id());
        }
    }
}
