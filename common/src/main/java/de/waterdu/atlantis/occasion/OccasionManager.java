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
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Keeps track of all active occasions and their members. Ticks the occasions and forwards events to them.
 */
public final class OccasionManager {

    private static final OccasionListeners LISTENERS = new OccasionListeners();
    private static final Map<UEvent.Priority, Set<Key<?>>> REGISTERED_HANDLERS = new HashMap<>();
    private static final Set<Occasion> OCCASIONS = Sets.newConcurrentHashSet();
    private static final Map<UUID, Occasion> UUID_MAP = new ConcurrentHashMap<>();
    private static final Map<PlayerReference, Occasion> PLAYER_MAP = new ConcurrentHashMap<>();

    private OccasionManager() {
        ExceptionUtils.throwCannotInstantiateClassException();
    }

    /**
     * Subscribes the login, logout and tick listeners of the occasions.
     */
    public static void register() {
        LISTENERS.register();
    }

    /**
     * {@link #registerEventHandlers(UEvent.Priority, Key)} with the priority from the settings.
     */
    public static <Payload extends EventPayload> void registerEventHandlers(Key<UEvent<Payload>> event) {
        OccasionManager.registerEventHandlers(Settings.get().getOccasionsDefaultEventHandlerPriority(), event);
    }

    /**
     * Forwards the event to all occasions. Each event is registered only once for each priority.
     * The event handler methods of the builders call this.
     */
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

    /**
     * Forwards each event to all occasions, with the priority from the settings.
     *
     * @param events the keys of the events
     */
    @SafeVarargs
    public static void registerEventHandlers(Key<UEvent<?>>... events) {
        OccasionManager.registerEventHandlers(Settings.get().getOccasionsDefaultEventHandlerPriority(), events);
    }

    /**
     * Forwards each event to all occasions, with the priority from the settings.
     *
     * @param events the events
     */
    public static void registerEventHandlers(UEvent<?>... events) {
        OccasionManager.registerEventHandlers(Settings.get().getOccasionsDefaultEventHandlerPriority(), events);
    }

    /**
     * Forwards each event to all occasions. Each event is registered only once for each priority.
     *
     * @param priority the priority of the listeners
     * @param events the keys of the events
     */
    @SafeVarargs
    public static void registerEventHandlers(UEvent.Priority priority, Key<UEvent<?>>... events) {
        for (var eventKey : events) {
            registerEventHandlers(priority, eventKey);
        }
    }

    /**
     * Forwards each event to all occasions. Each event type is registered only once for each priority.
     *
     * @param priority the priority of the listeners
     * @param events the events
     */
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

    /**
     * Passes the event to every occasion.
     *
     * @param payload the event payload
     */
    public static void handleEvent(EventPayload payload) {
        for (var occasion : OCCASIONS) {
            occasion.handleEvent(payload);
        }
    }

    /**
     * Registers the occasion so that it ticks and gets events.
     *
     * @param occasion the occasion to add
     * @return the occasion
     */
    static Occasion add(Occasion occasion) {
        OCCASIONS.add(occasion);
        UUID_MAP.put(occasion.uuid(), occasion);
        return occasion;
    }

    /**
     * Unregisters the occasion and removes the player mapping of each of its members.
     *
     * @param occasion the occasion to remove
     */
    private static void remove(Occasion occasion) {
        OCCASIONS.remove(occasion);
        UUID_MAP.remove(occasion.uuid());
        for (var reference : occasion.membersDirect()) {
            OccasionManager.removePlayer(reference);
        }
    }

    /**
     * @param uuid the UUID of the occasion, not of a player
     */
    public static @Nullable Occasion getFor(UUID uuid) {
        return UUID_MAP.get(uuid);
    }

    /**
     * Gets the occasion that the player is a member of.
     *
     * @param reference the player
     * @return the occasion, or {@code null} when the player is not a member of one
     */
    public static @Nullable Occasion getFor(PlayerReference reference) {
        return PLAYER_MAP.get(reference);
    }

    /**
     * @return {@code true} if the player is a member of an occasion that is not completed
     */
    public static boolean isIn(PlayerReference reference) {
        var occasion = OccasionManager.getFor(reference);
        return occasion != null && occasion.active();
    }

    /**
     * Records that the player is a member of the occasion. It replaces an earlier occasion of the player.
     *
     * @param reference the player
     * @param occasion the occasion of the player
     */
    static void addPlayer(PlayerReference reference, Occasion occasion) {
        PLAYER_MAP.put(reference, occasion);
    }

    /**
     * Removes the record of the occasion of the player.
     *
     * @param reference the player
     */
    static void removePlayer(PlayerReference reference) {
        PLAYER_MAP.remove(reference);
    }

    /**
     * Ticks every occasion. Removes each occasion that is completed and does not repeat, together with the player
     * mappings of its members.
     */
    static void tick() {
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
