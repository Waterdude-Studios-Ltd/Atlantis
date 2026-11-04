package de.waterdu.atlantis.mod.impl.common.event.meta;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UEVs;
import de.waterdu.atlantis.util.config.Key;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gathers Gson type adapters from listeners.
 */
public class GatherTypeAdaptersEventPayload implements EventPayload {

    private final Map<Type, Object> typeAdapters = new ConcurrentHashMap<>();

    /**
     * @return {@link UEvent#GATHER_TYPE_ADAPTERS}
     */
    @Override
    public Key<?> type() {
        return UEvent.GATHER_TYPE_ADAPTERS;
    }

    /**
     * @param adapterType the exact type that the adapter handles. Subtypes do not use the adapter.
     * @param adapter any object that {@link com.google.gson.GsonBuilder#registerTypeAdapter(Type, Object)} accepts
     */
    public void addTypeAdapter(Type adapterType, Object adapter) {
        this.typeAdapters.put(adapterType, adapter);
    }

    /**
     * {@link #addTypeAdapter(Type, Object)} for each pair of arguments.
     *
     * @param typeAdapterPairs a type, then its adapter, repeated. An odd count throws an
     *     {@link IllegalArgumentException}.
     */
    public void addTypeAdapters(Object... typeAdapterPairs) {
        if ((typeAdapterPairs.length & 1) == 1) {
            throw new IllegalArgumentException("Odd number of objects passed to paired varargs");
        }
        for (var i = 0; i < typeAdapterPairs.length; i += 2) {
            this.addTypeAdapter((Type) typeAdapterPairs[i], typeAdapterPairs[i + 1]);
        }
    }

    /**
     * Fires the event and puts the gathered adapters into {@code dest}.
     */
    public static void retrieve(Map<Type, Object> dest) {
        dest.putAll(Mods.modLoader().event(UEvent.GATHER_TYPE_ADAPTERS).emit(new GatherTypeAdaptersEventPayload()).typeAdapters);
    }
}
