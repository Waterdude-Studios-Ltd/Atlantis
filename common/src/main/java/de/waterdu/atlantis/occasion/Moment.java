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

    public static Moment create(Occasion occasion) {
        return new Moment(occasion);
    }

    public Occasion occasion() {
        return this.occasion;
    }

    public boolean playerValid(Player player) {
        return this.occasion().isMember(player);
    }

    public void removePlayer(PlayerReference player) {
        this.onPlayerRemoved.get().accept(player, this);
        this.onPlayerChanged.get().accept(player, this);
        player.entity().ifPresent(entity -> {
            this.updateBossBar(bar -> bar.removePlayer(entity));
            this.updateIndividualBossBar(player, bar -> bar.removePlayer(entity));
            this.removeIndividualBossBar(player);
        });
    }

    public UUID uuid() {
        return this.uuid;
    }

    public int ordinal() {
        return this.occasion().momentOrdinal(this);
    }

    public long duration() {
        return this.duration.get();
    }

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

    public void updateBossBar(Consumer<ServerBossEvent> consumer) {
        if (this.header.get() == null) {
            this.header.set(new ServerBossEvent(Component.literal(""), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS));
            this.occasion().forEachMember(player -> player.entity().ifPresent(entity -> this.header.get().addPlayer(entity)));
        }
        consumer.accept(this.header.get());
    }

    public void updateIndividualBossBar(PlayerReference reference, Consumer<ServerBossEvent> consumer) {
        consumer.accept(this.individualHeaders.computeIfAbsent(reference, key -> {
            var event = new ServerBossEvent(Component.literal(""), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
            reference.entity().ifPresent(event::addPlayer);
            return event;
        }));
    }

    public void removeIndividualBossBar(PlayerReference reference) {
        this.individualHeaders.remove(reference);
    }

    public void resetTick() {
        this.tick.set(0);
    }

    public long currentTick() {
        return this.tick.get();
    }

    public boolean exceededDuration() {
        return this.currentTick() >= this.duration();
    }

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

    protected static Builder builder(Occasion.Builder occasion) {
        return new Builder(occasion);
    }

    public static class Builder {

        private final Occasion.Builder parent;
        private final Moment moment;

        private Builder(Occasion.Builder parent) {
            this.parent = parent;
            this.moment = Moment.create(this.parent.build());
        }

        public Builder duration(long duration) {
            this.moment.duration.set(duration);
            return this;
        }

        public Builder timeout(long timeout) {
            this.moment.timeout.set(timeout);
            return this;
        }

        public Builder startPosition(ConfigurableLevel level, LocalPosition position) {
            return this.startPosition(LevelPosition.of(level, position));
        }

        public Builder startPosition(LevelPosition position) {
            this.moment.startPosition.set(position);
            return this;
        }

        public <Payload extends EventPayload> Builder eventHandler(
                Key<UEvent<Payload>> event,
                EventConsumer<Payload, Moment> handler,
                @Nullable Function<Payload, ? extends Player> playerGetter
        ) {
            this.moment.addEventHandler(event, handler, playerGetter);
            OccasionManager.registerEventHandlers(event);
            return this;
        }

        public <Payload extends EventPayload> Builder eventHandler(
                Key<UEvent<Payload>> event,
                EventConsumer<Payload, Moment> handler
        ) {
            return this.eventHandler(event, handler, null);
        }

        public <Payload extends EventPayload> Builder clearEventHandlers(Key<UEvent<Payload>> event) {
            this.moment.clearEventHandlers(event);
            return this;
        }

        public Builder onStart(Consumer<Moment> onStart) {
            this.moment.onStart.set(onStart);
            return this;
        }

        public Builder onEnd(Consumer<Moment> onEnd) {
            this.moment.onEnd.set(onEnd);
            return this;
        }

        public Builder onTick(BiConsumer<Long, Moment> onTick) {
            this.moment.onTick.set(onTick);
            return this;
        }

        public Builder onPlayerAdded(BiConsumer<PlayerReference, Moment> onPlayerAdded) {
            this.moment.onPlayerAdded.set(onPlayerAdded);
            return this;
        }

        public Builder onPlayerRemoved(BiConsumer<PlayerReference, Moment> onPlayerRemoved) {
            this.moment.onPlayerRemoved.set(onPlayerRemoved);
            return this;
        }

        public Builder onPlayerChanged(BiConsumer<PlayerReference, Moment> onPlayerChanged) {
            this.moment.onPlayerChanged.set(onPlayerChanged);
            return this;
        }

        public Occasion.Builder build() {
            return this.build(this.moment.occasion().momentCount());
        }

        public Occasion.Builder build(int index) {
            this.moment.occasion().addMoment(index, this.moment);
            return this.parent;
        }
    }
}
