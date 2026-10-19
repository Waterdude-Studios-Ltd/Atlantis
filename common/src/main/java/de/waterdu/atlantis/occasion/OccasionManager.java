package de.waterdu.atlantis.occasion;

import com.google.common.collect.Sets;
import de.waterdu.atlantis.Settings;
import de.waterdu.atlantis.mod.Mods;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.ExceptionUtils;
import net.minecraft.world.InteractionResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class OccasionManager {

    private static final AtomicReference<OccasionListeners> LISTENER = new AtomicReference<>();
    private static final Map<UEvent.Priority, Set<Key<?>>> REGISTERED_HANDLERS = new HashMap<>();
    private static final Set<Occasion> OCCASIONS = Sets.newConcurrentHashSet();
    private static final Map<UUID, Optional<Occasion>> UUID_MAP = new ConcurrentHashMap<>();
    private static final Map<PlayerReference, Optional<Occasion>> PLAYER_MAP = new ConcurrentHashMap<>();

    private OccasionManager() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    public static void register() {
        LISTENER.set(new OccasionListeners());
    }

    public static <Payload extends EventPayload> void registerEventHandlers(Key<UEvent<Payload>> event) {
        OccasionManager.registerEventHandlers(Settings.get().getOccasionsDefaultEventHandlerPriority(), event);
    }

    public static <Payload extends EventPayload> void registerEventHandlers(UEvent.Priority priority, Key<UEvent<Payload>> event) {
        if (REGISTERED_HANDLERS.computeIfAbsent(priority, key -> new HashSet<>()).add(event)) {
            @SuppressWarnings("unchecked")
            Key<UEvent<EventPayload>> castKey = (Key<UEvent<EventPayload>>) (Key<?>) event;
            Mods.modLoader().event(castKey).listen(priority, payload -> {
                handleEvent(payload);
                return InteractionResult.PASS;
            });
        }
    }

    @SafeVarargs
    public static void registerEventHandlers(Key<UEvent<?>>... events) {
        OccasionManager.registerEventHandlers(Settings.get().getOccasionsDefaultEventHandlerPriority(), events);
    }

    public static void registerEventHandlers(UEvent<?>... events) {
        OccasionManager.registerEventHandlers(Settings.get().getOccasionsDefaultEventHandlerPriority(), events);
    }

    @SafeVarargs
    public static void registerEventHandlers(UEvent.Priority priority, Key<UEvent<?>>... events) {
        for (var eventKey : events) {
            registerEventHandlers(priority, eventKey);
        }
    }

    public static void registerEventHandlers(UEvent.Priority priority, UEvent<?>... events) {
        for (var event : events) {
            if (REGISTERED_HANDLERS.computeIfAbsent(priority, key -> new HashSet<>()).add(event.eventType())) {
                @SuppressWarnings("unchecked")
                UEvent<EventPayload> castEvent = (UEvent<EventPayload>) event;
                castEvent.listen(priority, payload -> {
                    handleEvent(payload);
                    return InteractionResult.PASS;
                });
            }
        }
    }

    public static void handleEvent(EventPayload payload) {
        for (var occasion : OCCASIONS) {
            occasion.handleEvent(payload);
        }
    }

    protected static Occasion add(Occasion occasion) {
        OCCASIONS.add(occasion);
        UUID_MAP.put(occasion.uuid(), Optional.of(occasion));
        return occasion;
    }

    protected static void remove(Occasion occasion) {
        OCCASIONS.remove(occasion);
        UUID_MAP.remove(occasion.uuid());
        for (var reference : occasion.membersDirect()) {
            OccasionManager.removePlayer(reference);
        }
    }

    public static Optional<Occasion> getFor(UUID uuid) {
        return UUID_MAP.getOrDefault(uuid, Optional.empty());
    }

    public static Optional<Occasion> getFor(PlayerReference reference) {
        return PLAYER_MAP.getOrDefault(reference, Optional.empty());
    }

    public static boolean isIn(PlayerReference reference) {
        return OccasionManager.getFor(reference).map(Occasion::active).orElse(false);
    }

    protected static void removePlayer(PlayerReference reference) {
        PLAYER_MAP.remove(reference);
    }

    protected static void tick() {
        OCCASIONS.removeIf(occasion -> {
            if (occasion.tick()) {
                UUID_MAP.remove(occasion.uuid());
                for (var reference : occasion.membersDirect()) {
                    OccasionManager.removePlayer(reference);
                }
                return true;
            }
            return false;
        });
    }
}
