package de.waterdu.atlantis.mod.impl.common.event.entity;

import de.waterdu.atlantis.mod.impl.common.event.Cancellable;
import de.waterdu.atlantis.mod.impl.common.event.EventPayload;
import de.waterdu.atlantis.mod.wrapper.modloader.event.UEvent;
import de.waterdu.atlantis.util.config.Key;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Fires on the server thread when a player attacks or right-clicks an entity, before the entity or any mod
 * handles it. A single right-click usually arrives as an {@link Action#INTERACT_AT} followed by an
 * {@link Action#INTERACT}, once per hand, so a listener that takes over right-clicks should cancel both.
 */
public class InteractEntityEventPayload implements EventPayload, Cancellable {

    private final ServerPlayer player;
    private final Entity entity;
    private final Action action;
    private final @Nullable InteractionHand hand;
    private boolean cancelled = false;

    public InteractEntityEventPayload(ServerPlayer player, Entity entity, Action action, @Nullable InteractionHand hand) {
        this.player = player;
        this.entity = entity;
        this.action = action;
        this.hand = hand;
    }

    @Override
    public Key<?> type() {
        return UEvent.PLAYER_INTERACT_ENTITY;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public Entity entity() {
        return this.entity;
    }

    public Action action() {
        return this.action;
    }

    /**
     * The hand used, or null for {@link Action#ATTACK}.
     */
    public @Nullable InteractionHand hand() {
        return this.hand;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean cancelled() {
        return this.cancelled;
    }

    public enum Action {

        ATTACK,
        INTERACT,
        INTERACT_AT,
        ;
    }
}
