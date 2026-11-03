package de.waterdu.atlantis.occasion;

import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.EventHandler;
import de.waterdu.atlantis.util.java.interfaces.EventConsumer;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.level.LocalPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Holds one stage of an {@link Occasion}. Each moment has its own tick counter, duration, callbacks, event handlers,
 * start position and boss bars.
 */
public class Moment extends EventHandler<Moment> {

    public static final Moment VOID = new Moment(Occasion.VOID);

    protected final Occasion occasion;

    protected final UUID uuid;
    protected final AtomicLong tick;
    protected final AtomicLong duration;
    protected final AtomicLong timeout;
    protected final Map<PlayerReference, Long> logouts;

    protected final AtomicReference<LevelPosition> startPosition;
    protected final AtomicReference<ServerBossEvent> header;
    protected final Map<PlayerReference, ServerBossEvent> individualHeaders;

    protected final AtomicReference<Consumer<Moment>> onStart;
    protected final AtomicReference<Consumer<Moment>> onEnd;
    protected final AtomicReference<BiConsumer<Long, Moment>> onTick;
    protected final AtomicReference<BiConsumer<PlayerReference, Moment>> onPlayerAdded;
    protected final AtomicReference<BiConsumer<PlayerReference, Moment>> onPlayerRemoved;
    protected final AtomicReference<BiConsumer<PlayerReference, Moment>> onPlayerChanged;

    /**
     * Creates a moment with a random UUID, a duration of 100 ticks and callbacks that do nothing.
     *
     * @param occasion the occasion that the moment belongs to
     */
    private Moment(Occasion occasion) {
        super();
        this.occasion = occasion;
        this.uuid = UUID.randomUUID();
        this.tick = new AtomicLong(0);
        this.duration = new AtomicLong(100);
        this.timeout = new AtomicLong(0);
        this.logouts = new HashMap<>();
        this.startPosition = new AtomicReference<>();
        this.header = new AtomicReference<>();
        this.individualHeaders = new HashMap<>();
        this.onStart = new AtomicReference<>(moment -> {});
        this.onEnd = new AtomicReference<>(moment -> {});
        this.onTick = new AtomicReference<>((tick, moment) -> {});
        this.onPlayerAdded = new AtomicReference<>((player, moment) -> {});
        this.onPlayerRemoved = new AtomicReference<>((player, moment) -> {});
        this.onPlayerChanged = new AtomicReference<>((player, moment) -> {});
    }

    /**
     * Creates a moment for the occasion. It does not add the moment to the occasion.
     *
     * @param occasion the occasion that the moment belongs to
     * @return the new moment
     */
    public static Moment create(Occasion occasion) {
        return new Moment(occasion);
    }

    /**
     * @return the occasion that this moment belongs to
     */
    public Occasion occasion() {
        return this.occasion;
    }

    /**
     * Checks if the player is a member of the occasion. The event handlers of this moment use it to filter players.
     *
     * @param player the player to check
     * @return {@code true} if the player is a member of the occasion
     */
    public boolean playerValid(Player player) {
        return this.occasion().isMember(player);
    }

    /**
     * Runs the removal callbacks and hides the boss bars of this moment from the player.
     * Does not remove the player from the occasion. Use {@link Occasion#removeMember(Object)} for that.
     */
    public void removePlayer(PlayerReference player) {
        this.onPlayerRemoved.get().accept(player, this);
        this.onPlayerChanged.get().accept(player, this);
        player.entity().ifPresent(entity -> {
            this.updateBossBar(bar -> bar.removePlayer(entity));
            this.updateIndividualBossBar(player, bar -> bar.removePlayer(entity));
            this.removeIndividualBossBar(player);
        });
    }

    /**
     * @return the random UUID of this moment
     */
    public UUID uuid() {
        return this.uuid;
    }

    /**
     * @return -1 when the moment is not part of its occasion
     */
    public int ordinal() {
        return this.occasion().momentOrdinal(this);
    }

    /**
     * The moment does not end by itself when the duration is over. Check {@link #exceededDuration()}.
     *
     * @return the duration in ticks
     */
    public long duration() {
        return this.duration.get();
    }

    /**
     * Sets the tick counter to 0, forgets the logout times, and hides and removes all boss bars of this moment.
     */
    public void reset() {
        this.resetTick();
        this.logouts.clear();
        var globalHeader = this.header.get();
        if (globalHeader != null) {
            globalHeader.removeAllPlayers();
        }
        this.header.set(null);
        for (var individualHeader : this.individualHeaders.values()) {
            individualHeader.removeAllPlayers();
        }
        this.individualHeaders.clear();
    }

    /**
     * Changes the boss bar that all members share. The first call creates it and shows it to the online members.
     * Members that join later do not see it until you add them.
     */
    public void updateBossBar(Consumer<ServerBossEvent> consumer) {
        if (this.header.get() == null) {
            this.header.set(new ServerBossEvent(Component.literal(""), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS));
            this.occasion().forEachMember(player -> player.entity().ifPresent(entity -> this.header.get().addPlayer(entity)));
        }
        consumer.accept(this.header.get());
    }

    /**
     * Changes the boss bar of one player. The first call creates it and shows it to the player if they are online.
     */
    public void updateIndividualBossBar(PlayerReference reference, Consumer<ServerBossEvent> consumer) {
        consumer.accept(this.individualHeaders.computeIfAbsent(reference, key -> {
            var event = new ServerBossEvent(Component.literal(""), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
            reference.entity().ifPresent(event::addPlayer);
            return event;
        }));
    }

    /**
     * Forgets the boss bar of the player, but does not hide it. Remove the player from the boss bar first.
     */
    public void removeIndividualBossBar(PlayerReference reference) {
        this.individualHeaders.remove(reference);
    }

    /**
     * Sets the tick counter of this moment to 0.
     */
    public void resetTick() {
        this.tick.set(0);
    }

    /**
     * @return the number of ticks since the moment started or was last reset
     */
    public long currentTick() {
        return this.tick.get();
    }

    /**
     * Checks if the moment has run for its full duration.
     *
     * @return {@code true} if {@link #currentTick()} is at least {@link #duration()}
     */
    public boolean exceededDuration() {
        return this.currentTick() >= this.duration();
    }

    /**
     * Adds one to the tick counter and removes the members whose logout timeout is over. Then runs the tick callback.
     * The occasion calls it each tick while this is the current moment.
     */
    protected void tick() {
        this.tick.incrementAndGet();
        var timeout = System.currentTimeMillis() - this.timeout.get();
        for (var reference : this.occasion().membersDirect()) {
            var logout = this.logouts.getOrDefault(reference, -1L);
            if (logout > 0 && logout < timeout) {
                this.occasion().removeMember(reference);
                this.logouts.put(reference, -1L);
            }
        }
        this.onTick.get().accept(this.currentTick(), this);
    }

    /**
     * Creates a builder for a new moment of the occasion that the parent builder builds.
     *
     * @param occasion the builder of the occasion
     * @return the new builder
     */
    protected static Builder builder(Occasion.Builder occasion) {
        return new Builder(occasion);
    }

    /**
     * Builds a {@link Moment} and adds it to the occasion of the parent {@link Occasion.Builder}.
     */
    public static class Builder {

        private final Occasion.Builder parent;
        private final Moment moment;

        /**
         * Creates a builder and a new moment for the occasion of the parent builder.
         *
         * @param parent the builder of the occasion
         */
        private Builder(Occasion.Builder parent) {
            this.parent = parent;
            this.moment = Moment.create(this.parent.occasion());
        }

        /**
         * @param duration the duration in ticks. The default is 100. See {@link Moment#duration()}.
         */
        public Builder duration(long duration) {
            this.moment.duration.set(duration);
            return this;
        }

        /**
         * Needs the default login and logout handlers of {@link Occasion.Builder}.
         *
         * @param timeout the time in milliseconds before a member that logged out is removed from the occasion. The
         *     default is 0, which removes the member at the next tick.
         */
        public Builder timeout(long timeout) {
            this.moment.timeout.set(timeout);
            return this;
        }

        /**
         * Sets the position that members teleport to when the occasion sends them to the start.
         *
         * @param level the level of the position
         * @param position the position in the level
         * @return this builder
         */
        public Builder startPosition(ConfigurableLevel level, LocalPosition position) {
            return this.startPosition(LevelPosition.of(level, position));
        }

        /**
         * Sets the position that members teleport to when the occasion sends them to the start.
         *
         * @param position the start position
         * @return this builder
         */
        public Builder startPosition(LevelPosition position) {
            this.moment.startPosition.set(position);
            return this;
        }

        /**
         * Adds a handler that runs only while this moment is the current moment.
         * Also registers the event with {@link OccasionManager}.
         *
         * @param playerGetter gets the player from the event. If it is not null, the handlers for this event run only
         *     when the player is a member. The last getter for an event applies to all of its handlers.
         */
        public <Payload extends EventPayload> Builder eventHandler(
                Key<UEvent<Payload>> event,
                EventConsumer<Payload, Moment> handler,
                @Nullable Function<Payload, ? extends Player> playerGetter
        ) {
            this.moment.addEventHandler(event, handler, playerGetter);
            OccasionManager.registerEventHandlers(event);
            return this;
        }

        /**
         * Adds a handler that runs only while this moment is the current moment. The handler runs for every player.
         *
         * @param <Payload> the type of the event payload
         * @param event the key of the event
         * @param handler the handler to add
         * @return this builder
         */
        public <Payload extends EventPayload> Builder eventHandler(
                Key<UEvent<Payload>> event,
                EventConsumer<Payload, Moment> handler
        ) {
            return this.eventHandler(event, handler, null);
        }

        /**
         * Removes all handlers of the event from this moment.
         *
         * @param <Payload> the type of the event payload
         * @param event the key of the event
         * @return this builder
         */
        public <Payload extends EventPayload> Builder clearEventHandlers(Key<UEvent<Payload>> event) {
            this.moment.clearEventHandlers(event);
            return this;
        }

        /**
         * Sets the callback that runs when this moment starts. It replaces the earlier callback.
         *
         * @param onStart the callback
         * @return this builder
         */
        public Builder onStart(Consumer<Moment> onStart) {
            this.moment.onStart.set(onStart);
            return this;
        }

        /**
         * Sets the callback that runs when this moment ends. It replaces the earlier callback.
         *
         * @param onEnd the callback
         * @return this builder
         */
        public Builder onEnd(Consumer<Moment> onEnd) {
            this.moment.onEnd.set(onEnd);
            return this;
        }

        /**
         * Sets the callback that runs each tick while this is the current moment. It replaces the earlier callback.
         *
         * @param onTick gets the current tick of the moment and the moment
         * @return this builder
         */
        public Builder onTick(BiConsumer<Long, Moment> onTick) {
            this.moment.onTick.set(onTick);
            return this;
        }

        /**
         * Sets the callback that runs when a player joins the occasion while this is the current moment. It replaces
         * the earlier callback.
         *
         * @param onPlayerAdded the callback
         * @return this builder
         */
        public Builder onPlayerAdded(BiConsumer<PlayerReference, Moment> onPlayerAdded) {
            this.moment.onPlayerAdded.set(onPlayerAdded);
            return this;
        }

        /**
         * Sets the callback that runs when {@link Moment#removePlayer(PlayerReference)} removes a player. It replaces
         * the earlier callback.
         *
         * @param onPlayerRemoved the callback
         * @return this builder
         */
        public Builder onPlayerRemoved(BiConsumer<PlayerReference, Moment> onPlayerRemoved) {
            this.moment.onPlayerRemoved.set(onPlayerRemoved);
            return this;
        }

        /**
         * Sets the callback that runs after a player is added or removed. It replaces the earlier callback.
         *
         * @param onPlayerChanged the callback
         * @return this builder
         */
        public Builder onPlayerChanged(BiConsumer<PlayerReference, Moment> onPlayerChanged) {
            this.moment.onPlayerChanged.set(onPlayerChanged);
            return this;
        }

        /**
         * Adds the moment after the last moment of the occasion.
         *
         * @return the builder of the occasion
         */
        public Occasion.Builder build() {
            return this.build(this.moment.occasion().momentCount());
        }

        /**
         * Adds the moment to the occasion at the index.
         *
         * @param index the position of the moment in the occasion
         * @return the builder of the occasion
         */
        public Occasion.Builder build(int index) {
            this.moment.occasion().addMoment(index, this.moment);
            return this.parent;
        }
    }
}
