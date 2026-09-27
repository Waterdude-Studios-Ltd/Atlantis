package de.waterdu.atlantis.ui.api;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.waterdu.atlantis.util.config.Key;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class LayoutGroup {
    
    private final Map<Key<? extends Page>, Layout<? extends Page>> layouts = new HashMap<>();

    public <P extends Page> void add(Key<P> key, Layout<P> layout) {
        this.layouts.put(key, layout);
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <P extends Page> Layout<P> get(Key<P> key, Layout.Action<P>... actions) {
       return ((Layout<P>) this.layouts.get(key)).withActions(actions);
    }

    public static class TypeAdapter implements JsonSerializer<LayoutGroup>, JsonDeserializer<LayoutGroup> {

        @Override
        public LayoutGroup deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var layouts = new LayoutGroup();
            json.getAsJsonObject().asMap().forEach((key, layout) -> 
                    layouts.add(Key.of(key), context.deserialize(layout, Layout.class))
            );
            return layouts;
        }

        @Override
        public JsonElement serialize(LayoutGroup src, Type typeOfSrc, JsonSerializationContext context) {
            var object = new JsonObject();
            src.layouts.forEach((key, layout) -> 
                    object.add(key.toString(), context.serialize(layout, Layout.class))
            );
            return object;
        }
    }
}
