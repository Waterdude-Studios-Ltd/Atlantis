package de.waterdu.atlantis.mod.impl.fabric.modloader.event;

import de.waterdu.atlantis.util.config.Key;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;

public interface FabricEventImpl {

    Event<FabricEventImpl> EVENT = EventFactory.createArrayBacked(FabricEventImpl.class, listeners -> (type, payload) -> {
        for (var listener : listeners) {
            var result = listener.onEvent(type, payload);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    });

    InteractionResult onEvent(Key<?> type, Object payload);
}
