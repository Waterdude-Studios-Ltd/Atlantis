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

/**
 * Holds a set of page layouts, each found by its page key.
 */
public class LayoutGroup {

    private final Map<Key<? extends Page>, Layout<? extends Page>> layouts = new HashMap<>();

    /**
     * Adds a layout for the key. It replaces the layout that the key had before.
     *
     * @param key the key of the page
     * @param layout the layout of the page
     * @param <P> the type of the page
     */
    public <P extends Page> void add(Key<P> key, Layout<P> layout) {
        this.layouts.put(key, layout);
    }

    /**
     * Returns the shared layout for the key, not a copy. If no layout has the key, this method throws a
     * {@link NullPointerException}.
     *
     * @param actions only apply if the layout has no actions yet. See {@link Layout#withActions(Layout.Action[])}.
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <P extends Page> Layout<P> get(Key<P> key, Layout.Action<P>... actions) {
       return ((Layout<P>) this.layouts.get(key)).withActions(actions);
    }

    /**
     * Converts a {@link LayoutGroup} to and from a JSON object that maps each key to its layout.
     */
    public static class TypeAdapter implements JsonSerializer<LayoutGroup>, JsonDeserializer<LayoutGroup> {

        /**
         * Reads a layout group from a JSON object. Each member name becomes a key and each value a {@link Layout}.
         *
         * @param json the JSON object to read
         * @param typeOfT the type to read
         * @param context the context that reads each layout
         * @return the new layout group
         */
        @Override
        public LayoutGroup deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            var layouts = new LayoutGroup();
            json.getAsJsonObject().asMap().forEach((key, layout) -> 
                    layouts.add(Key.of(key), context.deserialize(layout, Layout.class))
            );
            return layouts;
        }

        /**
         * Writes the layout group as a JSON object, with the string form of each key as the member name.
         *
         * @param src the layout group to write
         * @param typeOfSrc the type to write
         * @param context the context that writes each layout
         * @return the JSON object
         */
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
