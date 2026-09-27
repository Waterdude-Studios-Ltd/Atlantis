package de.waterdu.atlantis.mod.impl.neoforge.modloader.event;

import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.object.ModObjectKey;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.world.InteractionResult;

import java.util.function.Function;

public class NeoForgeEvent<Payload extends EventPayload> extends UEvent<Payload> {

    public static final ModObjectKey TYPE = new ModObjectKey(Mods.NEOFORGE_ID, "event");

    private final NeoForgeEventBus eventBus;

    public NeoForgeEvent(Key<UEvent<Payload>> eventType, NeoForgeEventBus eventBus) {
        super(TYPE, eventType);
        this.eventBus = eventBus;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Payload emit(Payload payload) {
        return ((NeoForgeEventImpl<Payload>) this.eventBus.post(new NeoForgeEventImpl<>(this.eventType(), payload))).payload();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void listen(Function<Payload, InteractionResult> handler) {
        this.eventBus.listen(NeoForgeEventImpl.class, event -> {
            if (this.eventType().equals(event.type())) {
                var payload = (Payload) event.payload();
                var cancellable = payload instanceof Cancellable it ? it : null;

                if (cancellable != null && cancellable.cancelled()) {
                    return;
                }

                var result = handler.apply(payload);

                if (result != InteractionResult.PASS) {
                    if (cancellable != null) {
                        cancellable.cancel();
                    }
                }
            }
        });
    }
}
