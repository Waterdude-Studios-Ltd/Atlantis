package de.waterdu.atlantis.mod.impl.fabric.modloader.event;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.InteractionResult;

import java.util.function.Function;

public class FabricEvent<Payload extends EventPayload> extends UEvent<Payload> {

    public static final ModObjectKey TYPE = new ModObjectKey(Mods.FABRIC_ID, "event");

    public FabricEvent(Key<UEvent<Payload>> eventType) {
        super(TYPE, eventType);
    }

    @Override
    public Payload emit(Payload payload) {
        FabricEventImpl.EVENT.invoker().onEvent(this.eventType(), payload);
        return payload;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void listen(Function<Payload, InteractionResult> handler) {
        FabricEventImpl.EVENT.register((type, payload) -> {
            if (this.eventType().equals(type)) {
                return handler.apply((Payload) payload);
            } else {
                return InteractionResult.PASS;
            }
        });
    }
}
