package de.waterdu.atlantis.mod.impl.common.event.meta;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.mod.wrapper.pokemon.pokemon.properties.stats.UEVs;
import de.waterdu.atlantis.util.config.Key;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GatherTypeAdaptersEventPayload implements EventPayload {

    private final Map<Type, Object> typeAdapters = new ConcurrentHashMap<>();

    @Override
    public Key<?> type() {
        return UEvent.GATHER_TYPE_ADAPTERS;
    }

    public void addTypeAdapter(Type adapterType, Object adapter) {
        this.typeAdapters.put(adapterType, adapter);
    }

    public void addTypeAdapters(Object... typeAdapterPairs) {
        if ((typeAdapterPairs.length & 1) == 1) {
            throw new IllegalArgumentException("Odd number of objects passed to paired varargs");
        }
        for (var i = 0; i < typeAdapterPairs.length; i += 2) {
            this.addTypeAdapter((Type) typeAdapterPairs[i], typeAdapterPairs[i + 1]);
        }
    }

    public static void retrieve(Map<Type, Object> dest) {
        dest.putAll(Mods.modLoader().event(UEvent.GATHER_TYPE_ADAPTERS).emit(new GatherTypeAdaptersEventPayload()).typeAdapters);
    }
}
