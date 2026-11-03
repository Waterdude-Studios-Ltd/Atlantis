package de.waterdu.atlantis.occasion;

import com.google.common.collect.Lists;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.impl.common.event.entity.PlayerEventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import de.waterdu.atlantis.util.entity.PlayerReference;
import de.waterdu.atlantis.util.java.EventHandler;
import de.waterdu.atlantis.util.java.collection.Pair;
import de.waterdu.atlantis.util.java.interfaces.EventConsumer;
import de.waterdu.atlantis.util.level.ConfigurableLevel;
import de.waterdu.atlantis.util.level.LevelPosition;
import de.waterdu.atlantis.util.level.LocalPosition;
import de.waterdu.atlantis.util.server.ServerUtils;
import de.waterdu.atlantis.util.text.MessageUtils;
import net.minecraft.Util;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runs a sequence of {@link Moment}s for a group of member players. {@link OccasionManager} ticks each occasion and
 * forwards events to it.
 */
@NullMarked
public class Occasion extends EventHandler<Occasion> {

    public static final Occasion VOID = new Occasion(Util.NIL_UUID);

    protected final UUID uuid;
    protected final AtomicBoolean repeats;
    protected final AtomicLong tick;
    protected final List<PlayerReference> members;
    protected final Set<PlayerReference> memberSet;
    protected final List<Moment> moments;
    protected final Map<UUID, Pair<Integer, Moment>> momentMap;
    protected final AtomicInteger currentMoment;

    protected final Map<PlayerReference, LevelPosition> specificRecoveryPositions;
    protected final AtomicReference<LevelPosition> recoveryPosition;

    protected final AtomicReference<Consumer<Occasion>> onComplete;
    protected final AtomicReference<BiConsumer<Long, Occasion>> onTick;
    protected final AtomicReference<BiConsumer<PlayerReference, Occasion>> onPlayerAdded;
    protected final AtomicReference<BiConsumer<PlayerReference, Occasion>> onPlayerRemoved;
    protected final AtomicReference<BiConsumer<PlayerReference, Occasion>> onPlayerChanged;

    /**
     * Creates an occasion with a random UUID.
     */
    private Occasion() {
        this(UUID.randomUUID());
    }

    /**
     * Creates an occasion that does not repeat, has no members and no moments, and recovers players to the spawn point.
     *
     * @param uuid the UUID of the occasion
     */
    private Occasion(UUID uuid) {
        super();
        this.uuid = uuid;
        this.repeats = new AtomicBoolean(false);
        this.tick = new AtomicLong(0);
        this.members = Lists.newCopyOnWriteArrayList();
        this.memberSet = new HashSet<>();
        this.moments = new ArrayList<>();
        this.momentMap = new HashMap<>();
        this.currentMoment = new AtomicInteger(0);
        this.specificRecoveryPositions = new HashMap<>();
        this.recoveryPosition = new AtomicReference<>(ServerUtils.spawnPoint());
        this.onComplete = new AtomicReference<>(occasion -> {});
        this.onTick = new AtomicReference<>((tick, occasion) -> {});
        this.onPlayerAdded = new AtomicReference<>((player, occasion) -> {});
        this.onPlayerRemoved = new AtomicReference<>((player, occasion) -> {});
        this.onPlayerChanged = new AtomicReference<>((player, occasion) -> {});
    }

    /**
     * Runs the start callback of the current moment, if there is one.
     *
     * @return this occasion
     */
    private Occasion init() {
        var moment = this.currentMoment();
        if (moment != null) {
            moment.onStart.get().accept(moment);
        }
        return this;
    }

    /**
     * @return the UUID of this occasion
     */
    public UUID uuid() {
        return this.uuid;
    }

    /**
     * @return {@code true} if the occasion resets and runs again after it completes
     */
    public boolean repeats() {
        return this.repeats.get();
    }

    /**
     * Removes all members. If the occasion repeats, also resets the ticks and all moments.
     */
    public void reset() {
        this.removeAllMembers();
        if (this.repeats()) {
            this.resetTick();
            this.currentMoment.set(0);
            this.specificRecoveryPositions.clear();
            for (var moment : this.moments) {
                moment.reset();
            }
        }
    }

    /**
     * @return {@code true} if the occasion is not completed
     */
    public boolean active() {
        return !this.completed();
    }

    /**
     * @return {@code true} if the occasion has gone past its last moment
     */
    public boolean completed() {
        return this.currentMoment.get() >= this.moments.size();
    }

    /**
     * @param members any objects that {@link PlayerReference#getSafely(Object)} accepts. Other objects are ignored.
     * @return the number of players that were not members before
     */
    public int addMembers(Object... members) {
        var added = new AtomicInteger(0);
        for (var member : members) {
            PlayerReference.getSafely(member).ifPresent(reference -> {
                if (!this.memberSet.contains(reference)) {
                    this.members.add(reference);
                    this.memberSet.add(reference);
                    OccasionManager.addPlayer(reference, this);
                    this.onPlayerAdded.get().accept(reference, this);
                    this.onPlayerChanged.get().accept(reference, this);
                    var moment = this.currentMoment();
                    if (moment != null) {
                        moment.onPlayerAdded.get().accept(reference, moment);
                        moment.onPlayerChanged.get().accept(reference, moment);
                    }
                    added.getAndIncrement();
                }
            });
        }
        return added.get();
    }

    /**
     * Checks if the player is a member of this occasion. Event handlers with a player getter run only for members.
     *
     * @param player the player from the event
     * @return {@code true} if the player is a member
     */
    @Override
    public boolean playerValid(Player player) {
        return this.isMember(player);
    }

    /**
     * Checks if the player is a member of this occasion.
     *
     * @param reference any object that {@link PlayerReference#getSafely(Object)} accepts
     * @return {@code true} if the player is a member. {@code false} if the object is not a valid player reference.
     */
    public boolean isMember(Object reference) {
        return PlayerReference.getSafely(reference).map(this.memberSet::contains).orElse(false);
    }

    /**
     * Inserts the moment at the index.
     *
     * @param index the position of the moment in the sequence
     * @param moment the moment to add
     * @return the moment
     */
    public Moment addMoment(int index, Moment moment) {
        this.moments.add(index, moment);
        this.momentMap.put(moment.uuid(), Pair.of(index, moment));
        return moment;
    }

    /**
     * @return the live list of members, not a copy
     */
    protected List<PlayerReference> membersDirect() {
        return this.members;
    }

    /**
     * Runs the action for each member, online or offline.
     *
     * @param forEach runs for each member
     */
    public void forEachMember(Consumer<PlayerReference> forEach) {
        for (var reference : this.members) {
            forEach.accept(reference);
        }
    }

    /**
     * @param forEach runs for each online member
     * @param orElse runs for each offline member. Can be null.
     */
    public void forEachMember(Consumer<ServerPlayer> forEach, @Nullable Consumer<PlayerReference> orElse) {
        for (var reference : this.members) {
            var player = reference.entity();
            if (player.isPresent()) {
                forEach.accept(player.get());
            } else if (orElse != null) {
                orElse.accept(reference);
            }
        }
    }

    /**
     * Sends the message to all members with {@link MessageUtils#send}.
     *
     * @param message the message to send
     * @param <T> the type of the message
     */
    public <T> void send(T message) {
        MessageUtils.send(message, this.members);
    }

    /**
     * @return the current moment, or {@code null} when the occasion is completed
     */
    public @Nullable Moment currentMoment() {
        return this.completed() ? null : this.moments.get(this.currentMomentOrdinal());
    }

    /**
     * @return the index of the current moment. It is equal to or more than {@link #momentCount()} when the occasion
     *     is completed.
     */
    public int currentMomentOrdinal() {
        return this.currentMoment.get();
    }

    /**
     * Gets the moment with the UUID.
     *
     * @param uuid the UUID of the moment
     * @return the moment, or {@code null} when the moment is not part of this occasion
     */
    public @Nullable Moment moment(UUID uuid) {
        if (!this.momentMap.containsKey(uuid)) {
            return null;
        }
        return this.momentMap.get(uuid).getB();
    }

    /**
     * Gets the moment at the index.
     *
     * @param ordinal the index of the moment
     * @return the moment, or {@code null} when the index is out of range
     */
    public @Nullable Moment moment(int ordinal) {
        if (ordinal < 0 || ordinal >= this.moments.size()) {
            return null;
        }
        return this.moments.get(ordinal);
    }

    /**
     * @return -1 when the moment is not part of this occasion
     */
    public int momentOrdinal(Moment moment) {
        if (!this.momentMap.containsKey(moment.uuid())) {
            return -1;
        }
        return this.momentMap.get(moment.uuid()).getA();
    }

    /**
     * Ends the current moment and starts the next one.
     *
     * @return {@code true} if there is no next moment. Then the occasion runs its completion callback and resets.
     */
    public boolean incrementMoment() {
        var prevMoment = this.currentMoment();
        if (prevMoment != null) {
            prevMoment.onEnd.get().accept(prevMoment);
            prevMoment.updateBossBar(ServerBossEvent::removeAllPlayers);
            this.forEachMember(member -> prevMoment.updateIndividualBossBar(member, ServerBossEvent::removeAllPlayers));
        }

        this.currentMoment.getAndIncrement();

        var nextMoment = this.currentMoment();
        if (nextMoment != null) {
            nextMoment.onStart.get().accept(nextMoment);
        }

        var complete = this.completed();
        if (complete) {
            this.onComplete.get().accept(this);
            this.reset();
        }

        return complete;
    }

    /**
     * Ends the current moment and starts the moment at the index.
     *
     * @return {@code false} when the index is out of range. Then nothing changes.
     */
    public boolean gotoMoment(int index) {
        if (index < 0 || index >= this.momentCount()) {
            return false;
        }

        var prevMoment = this.currentMoment();
        if (prevMoment != null) {
            prevMoment.onEnd.get().accept(prevMoment);
            prevMoment.updateBossBar(ServerBossEvent::removeAllPlayers);
            this.forEachMember(member -> prevMoment.updateIndividualBossBar(member, ServerBossEvent::removeAllPlayers));
        }

        this.currentMoment.set(index);

        var nextMoment = this.currentMoment();
        if (nextMoment != null) {
            nextMoment.onStart.get().accept(nextMoment);
        }

        return true;
    }

    /**
     * Ends the current moment, runs the completion callback and resets the occasion. The reset removes all members.
     */
    public void complete() {
        var moment = this.currentMoment();
        if (moment != null) {
            moment.onEnd.get().accept(moment);
            moment.updateBossBar(ServerBossEvent::removeAllPlayers);
            this.forEachMember(member -> moment.updateIndividualBossBar(member, ServerBossEvent::removeAllPlayers));
        }
        this.currentMoment.set(this.momentCount());
        this.onComplete.get().accept(this);
        this.reset();
    }

    /**
     * Teleports the player to their recovery position. If the player is offline, they are teleported when they log in.
     *
     * @return {@code false} when the player is not a member
     */
    public boolean removeMember(Object reference) {
        var moment = this.currentMoment();
        return PlayerReference.getSafely(reference).map(player -> {
            var changed = this.members.remove(player);
            if (changed) {
                this.removePlayer(moment, player);
            }
            return changed;
        }).orElse(false);
    }

    /**
     * Teleports each player to their recovery position. Offline players are teleported when they log in.
     */
    public void removeAllMembers() {
        var moment = this.currentMoment();
        for (var player : this.members) {
            this.removePlayer(moment, player);
        }
        this.members.clear();
    }

    /**
     * Removes the player from the member set and the manager, and teleports them to their recovery position. If the
     * player is offline, the position is stored in {@link OccasionRecovery}. Does not remove the player from the
     * member list.
     *
     * @param moment the current moment, or {@code null} when there is none
     * @param player the player to remove
     */
    private void removePlayer(@Nullable Moment moment, PlayerReference player) {
        this.memberSet.remove(player);
        OccasionManager.removePlayer(player);
        var recovery = this.recoveryPosition(player);
        if (player.entity().isPresent()) {
            recovery.teleport(player);
        } else {
            OccasionRecovery.add(player, -1, recovery);
        }
        this.onPlayerRemoved.get().accept(player, this);
        this.onPlayerChanged.get().accept(player, this);
        if (moment != null) {
            moment.removePlayer(player);
        }
    }

    /**
     * @return the number of members
     */
    public int memberCount() {
        return this.memberSet.size();
    }

    /**
     * @return the number of moments
     */
    public int momentCount() {
        return this.moments.size();
    }

    /**
     * Teleports the player to the start position of the current moment. Does nothing when there is no current moment
     * or it has no start position.
     *
     * @param reference any object that {@link PlayerReference#get(Object)} accepts
     */
    public void teleportToStart(Object reference) {
        var moment = this.currentMoment();
        if (moment != null) {
            var startPosition = moment.startPosition.get();
            if (startPosition != null) {
                startPosition.teleport(PlayerReference.get(reference));
            }
        }
    }

    /**
     * Teleports all members to the start position of the current moment. Does nothing when there is no current moment
     * or it has no start position.
     */
    public void teleportAllToStart() {
        var moment = this.currentMoment();
        if (moment != null) {
            var startPosition = moment.startPosition.get();
            if (startPosition != null) {
                for (var reference : this.membersDirect()) {
                    startPosition.teleport(reference);
                }
            }
        }
    }

    /**
     * Gets the position where the player goes when they are removed.
     *
     * @param reference any object that {@link PlayerReference#get(Object)} accepts
     * @return the specific recovery position of the player, or the recovery position of the occasion when it has none
     */
    public LevelPosition recoveryPosition(Object reference) {
        var recoveryPosition = this.specificRecoveryPositions.get(PlayerReference.get(reference));
        if (recoveryPosition == null) {
            return this.recoveryPosition.get();
        }
        return recoveryPosition;
    }

    /**
     * Sets a recovery position for one player. It replaces the recovery position of the occasion for that player.
     * {@link #reset()} clears it when the occasion repeats.
     *
     * @param reference any object that {@link PlayerReference#get(Object)} accepts
     * @param position the recovery position of the player
     */
    public void setSpecificRecoveryPosition(Object reference, LevelPosition position) {
        this.specificRecoveryPositions.put(PlayerReference.get(reference), position);
    }

    /**
     * Teleports the player to their recovery position. Does not remove the player.
     *
     * @param reference any object that {@link PlayerReference#get(Object)} accepts
     */
    public void teleportToRecovery(Object reference) {
        this.recoveryPosition(reference).teleport(PlayerReference.get(reference));
    }

    /**
     * Runs the handlers of this occasion for the event, then passes the event to the current moment.
     *
     * @param payload the event payload
     * @param <Payload> the type of the event payload
     */
    @Override
    public <Payload extends EventPayload> void handleEvent(Payload payload) {
        super.handleEvent(payload);
        var moment = this.currentMoment();
        if (moment != null) {
            moment.handleEvent(payload);
        }
    }

    /**
     * Sets the tick counter to 0.
     */
    public void resetTick() {
        this.tick.set(0);
    }

    /**
     * @return the number of ticks since the occasion started or was last reset
     */
    public long currentTick() {
        return this.tick.get();
    }

    /**
     * Ticks the occasion and its current moment, then runs the tick callback. A completed occasion that repeats
     * resets instead.
     *
     * @return {@code true} if the occasion is completed and does not repeat, so that the manager removes it
     */
    protected boolean tick() {
        if (!this.completed()) {
            this.tick.incrementAndGet();
            var moment = this.currentMoment();
            if (moment != null) {
                moment.tick();
            }
            this.onTick.get().accept(this.currentTick(), this);
            return false;
        }

        if (this.repeats()) {
            this.reset();
            return false;
        }

        return true;
    }

    /**
     * Gets the hash code of the UUID of this occasion.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

    /**
     * Checks if the object is an occasion with the same UUID.
     *
     * @param obj the object to compare with
     * @return {@code true} if the UUIDs are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Occasion other)) {
            return false;
        }
        return this.uuid.equals(other.uuid);
    }

    /**
     * @return {@code Occasion(<uuid>)}
     */
    @Override
    public String toString() {
        return "Occasion(" + this.uuid + ")";
    }

    /**
     * Creates a builder for a new occasion with a random UUID.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds an {@link Occasion}. Call {@link #build()} to register and start it.
     */
    public static class Builder {

        private final Occasion occasion;

        /**
         * Creates a builder with a new occasion and the default login and logout handlers.
         */
        private Builder() {
            this.occasion = new Occasion();
            this.defaultLoginLogoutHandlers();
        }

        /**
         * Creates a builder for a moment of this occasion.
         *
         * @return the moment builder
         */
        public Moment.Builder newMoment() {
            return Moment.builder(this);
        }

        /**
         * @return the occasion that this builder builds
         */
        protected Occasion occasion() {
            return this.occasion;
        }

        /**
         * Makes the occasion reset and run again after it completes. Otherwise, it is removed after it completes.
         */
        public Builder repeats() {
            this.occasion.repeats.set(true);
            return this;
        }

        /**
         * Makes the occasion run once. It is removed after it completes. This is the default.
         *
         * @return this builder
         */
        public Builder single() {
            this.occasion.repeats.set(false);
            return this;
        }

        /**
         * Sets where removed members go.
         *
         * @param level the level of the position
         * @param position the position in the level
         * @return this builder
         */
        public Builder recoveryPosition(ConfigurableLevel level, LocalPosition position) {
            return this.recoveryPosition(LevelPosition.of(level, position));
        }

        /**
         * Sets where removed members go. The default is the overworld spawn point.
         */
        public Builder recoveryPosition(LevelPosition position) {
            this.occasion.recoveryPosition.set(position);
            return this;
        }

        /**
         * Also registers the event with {@link OccasionManager}.
         *
         * @param playerGetter gets the player from the event. If it is not null, the handlers for this event run only
         *     when the player is a member. The last getter for an event applies to all of its handlers.
         */
        public <Payload extends EventPayload> Builder eventHandler(
                Key<UEvent<Payload>> event,
                EventConsumer<Payload, Occasion> handler,
                @Nullable Function<Payload, ? extends Player> playerGetter
        ) {
            this.occasion.addEventHandler(event, handler, playerGetter);
            OccasionManager.registerEventHandlers(event);
            return this;
        }

        /**
         * Adds a handler that runs for every occurrence of the event, with no player check. Also registers the event
         * with {@link OccasionManager}.
         *
         * @param event the key of the event
         * @param handler the handler to run
         * @param <Payload> the type of the event payload
         * @return this builder
         */
        public <Payload extends EventPayload> Builder eventHandler(
                Key<UEvent<Payload>> event,
                EventConsumer<Payload, Occasion> handler
        ) {
            return this.eventHandler(event, handler, null);
        }

        /**
         * Removes all handlers of the occasion for the event.
         *
         * @param event the key of the event
         * @param <Payload> the type of the event payload
         * @return this builder
         */
        public <Payload extends EventPayload> Builder clearEventHandlers(Key<UEvent<Payload>> event) {
            this.occasion.clearEventHandlers(event);
            return this;
        }

        /**
         * The builder adds these handlers by default. They record when members log in and out,
         * so that the timeout of a moment can remove members that stay offline.
         */
        public Builder defaultLoginLogoutHandlers() {
            return this
                    .eventHandler(UEvent.LOG_IN,
                            (event, handler) -> {
                                var moment = handler.currentMoment();
                                if (moment != null) {
                                    moment.logouts.put(PlayerReference.get(event.player()), -1L);
                                }
                            },
                            PlayerEventPayload::player
                    )
                    .eventHandler(UEvent.LOG_OUT,
                            (event, handler) -> {
                                var moment = handler.currentMoment();
                                if (moment != null) {
                                    moment.logouts.put(PlayerReference.get(event.player()), System.currentTimeMillis());
                                }
                            },
                            PlayerEventPayload::player
                    );
        }

        /**
         * Removes all handlers for {@code LOG_IN} and {@code LOG_OUT}, not only the default ones.
         */
        public Builder noLoginLogoutHandlers() {
            return this.clearEventHandlers(UEvent.LOG_IN).clearEventHandlers(UEvent.LOG_OUT);
        }

        /**
         * Sets the callback that runs when the occasion completes, before it resets.
         *
         * @param onComplete receives the occasion
         * @return this builder
         */
        public Builder onComplete(Consumer<Occasion> onComplete) {
            this.occasion.onComplete.set(onComplete);
            return this;
        }

        /**
         * Sets the callback that runs each tick while the occasion is not completed, after the current moment ticks.
         *
         * @param onTick receives the current tick and the occasion
         * @return this builder
         */
        public Builder onTick(BiConsumer<Long, Occasion> onTick) {
            this.occasion.onTick.set(onTick);
            return this;
        }

        /**
         * Sets the callback that runs when a player becomes a member.
         *
         * @param onPlayerAdded receives the player and the occasion
         * @return this builder
         */
        public Builder onPlayerAdded(BiConsumer<PlayerReference, Occasion> onPlayerAdded) {
            this.occasion.onPlayerAdded.set(onPlayerAdded);
            return this;
        }

        /**
         * Sets the callback that runs when a member is removed.
         *
         * @param onPlayerRemoved receives the player and the occasion
         * @return this builder
         */
        public Builder onPlayerRemoved(BiConsumer<PlayerReference, Occasion> onPlayerRemoved) {
            this.occasion.onPlayerRemoved.set(onPlayerRemoved);
            return this;
        }

        /**
         * Sets the callback that runs when a player is added or removed, after the added or removed callback.
         *
         * @param onPlayerChanged receives the player and the occasion
         * @return this builder
         */
        public Builder onPlayerChanged(BiConsumer<PlayerReference, Occasion> onPlayerChanged) {
            this.occasion.onPlayerChanged.set(onPlayerChanged);
            return this;
        }

        /**
         * Registers the occasion with {@link OccasionManager} and starts its first moment.
         */
        public Occasion build() {
            return OccasionManager.add(this.occasion).init();
        }
    }
}
