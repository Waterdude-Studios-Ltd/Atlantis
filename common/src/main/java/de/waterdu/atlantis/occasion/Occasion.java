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

    private Occasion() {
        this(UUID.randomUUID());
    }

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

    private Occasion init() {
        var moment = this.currentMoment();
        if (moment != null) {
            moment.onStart.get().accept(moment);
        }
        return this;
    }

    public UUID uuid() {
        return this.uuid;
    }

    public boolean repeats() {
        return this.repeats.get();
    }

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

    public boolean active() {
        return !this.completed();
    }

    public boolean completed() {
        return this.currentMoment.get() >= this.moments.size();
    }

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

    public boolean isMember(Object reference) {
        return PlayerReference.getSafely(reference).map(this.memberSet::contains).orElse(false);
    }

    public Moment addMoment(int index, Moment moment) {
        this.moments.add(index, moment);
        this.momentMap.put(moment.uuid(), Pair.of(index, moment));
        return moment;
    }

    protected List<PlayerReference> membersDirect() {
        return this.members;
    }

    public void forEachMember(Consumer<PlayerReference> forEach) {
        for (var reference : this.members) {
            forEach.accept(reference);
        }
    }

    public void forEachMember(Consumer<ServerPlayer> forEach, Consumer<PlayerReference> orElse) {
        for (var reference : this.members) {
            var player = reference.entity();
            if (player.isPresent()) {
                forEach.accept(player.get());
            } else if (orElse != null) {
                orElse.accept(reference);
            }
        }
    }

    public <T> void send(T message) {
        MessageUtils.send(message, this.members);
    }

    public @Nullable Moment currentMoment() {
        return this.completed() ? null : this.moments.get(this.currentMomentOrdinal());
    }

    public int currentMomentOrdinal() {
        return this.currentMoment.get();
    }

    public @Nullable Moment moment(UUID uuid) {
        if (!this.momentMap.containsKey(uuid)) {
            return null;
        }
        return this.momentMap.get(uuid).getB();
    }

    public @Nullable Moment moment(int ordinal) {
        if (ordinal < 0 || ordinal >= this.moments.size()) {
            return null;
        }
        return this.moments.get(ordinal);
    }

    public int momentOrdinal(Moment moment) {
        if (!this.momentMap.containsKey(moment.uuid())) {
            return -1;
        }
        return this.momentMap.get(moment.uuid()).getA();
    }

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

    public void removeAllMembers() {
        var moment = this.currentMoment();
        for (var player : this.members) {
            this.removePlayer(moment, player);
        }
    }

    private void removePlayer(@Nullable Moment moment, PlayerReference player) {
        this.memberSet.remove(player);
        OccasionManager.removePlayer(player);
        var recovery = this.recoveryPosition(player);
        OccasionRecovery.add(player, -1, recovery);
        recovery.teleport(player);
        this.onPlayerRemoved.get().accept(player, this);
        this.onPlayerChanged.get().accept(player, this);
        if (moment != null) {
            moment.removePlayer(player);
        }
    }

    public int memberCount() {
        return this.memberSet.size();
    }

    public int momentCount() {
        return this.moments.size();
    }

    public void teleportToStart(Object reference) {
        var moment = this.currentMoment();
        if (moment != null) {
            var startPosition = moment.startPosition.get();
            if (startPosition != null) {
                startPosition.teleport(PlayerReference.get(reference));
            }
        }
    }

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

    public LevelPosition recoveryPosition(Object reference) {
        var recoveryPosition = this.specificRecoveryPositions.get(PlayerReference.get(reference));
        if (recoveryPosition == null) {
            return this.recoveryPosition.get();
        }
        return recoveryPosition;
    }

    public void setSpecificRecoveryPosition(Object reference, LevelPosition position) {
        this.specificRecoveryPositions.put(PlayerReference.get(reference), position);
    }

    public void teleportToRecovery(Object reference) {
        this.recoveryPosition(reference).teleport(PlayerReference.get(reference));
    }

    @Override
    public <Payload extends EventPayload> void handleEvent(Payload payload) {
        super.handleEvent(payload);
        var moment = this.currentMoment();
        if (moment != null) {
            moment.handleEvent(payload);
        }
    }

    public void resetTick() {
        this.tick.set(0);
    }

    public long currentTick() {
        return this.tick.get();
    }

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

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

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

    @Override
    public String toString() {
        return "Occasion(" + this.uuid + ")";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Occasion occasion;

        private Builder() {
            this.occasion = new Occasion();
            this.defaultLoginLogoutHandlers();
        }

        public Moment.Builder newMoment() {
            return Moment.builder(this);
        }

        public Builder repeats() {
            this.occasion.repeats.set(true);
            return this;
        }

        public Builder single() {
            this.occasion.repeats.set(false);
            return this;
        }

        public Builder recoveryPosition(ConfigurableLevel level, LocalPosition position) {
            return this.recoveryPosition(LevelPosition.of(level, position));
        }

        public Builder recoveryPosition(LevelPosition position) {
            this.occasion.recoveryPosition.set(position);
            return this;
        }

        public <Payload extends EventPayload> Builder eventHandler(
                Key<UEvent<Payload>> event,
                EventConsumer<Payload, Occasion> handler,
                @Nullable Function<Payload, ? extends Player> playerGetter
        ) {
            this.occasion.addEventHandler(event, handler, playerGetter);
            OccasionManager.registerEventHandlers(event);
            return this;
        }

        public <Payload extends EventPayload> Builder eventHandler(
                Key<UEvent<Payload>> event,
                EventConsumer<Payload, Occasion> handler
        ) {
            return this.eventHandler(event, handler, null);
        }

        public <Payload extends EventPayload> Builder clearEventHandlers(Key<UEvent<Payload>> event) {
            this.occasion.clearEventHandlers(event);
            return this;
        }

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

        public Builder noLoginLogoutHandlers() {
            return this.clearEventHandlers(UEvent.LOG_IN).clearEventHandlers(UEvent.LOG_OUT);
        }

        public Builder onComplete(Consumer<Occasion> onComplete) {
            this.occasion.onComplete.set(onComplete);
            return this;
        }

        public Builder onTick(BiConsumer<Long, Occasion> onTick) {
            this.occasion.onTick.set(onTick);
            return this;
        }

        public Builder onPlayerAdded(BiConsumer<PlayerReference, Occasion> onPlayerAdded) {
            this.occasion.onPlayerAdded.set(onPlayerAdded);
            return this;
        }

        public Builder onPlayerRemoved(BiConsumer<PlayerReference, Occasion> onPlayerRemoved) {
            this.occasion.onPlayerRemoved.set(onPlayerRemoved);
            return this;
        }

        public Builder onPlayerChanged(BiConsumer<PlayerReference, Occasion> onPlayerChanged) {
            this.occasion.onPlayerChanged.set(onPlayerChanged);
            return this;
        }

        public Occasion build() {
            return OccasionManager.add(this.occasion).init();
        }
    }
}
