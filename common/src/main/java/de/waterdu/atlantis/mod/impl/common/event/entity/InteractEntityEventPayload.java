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
 * Fires on the server thread when a player attacks or right-clicks an entity. It fires before the entity or any mod
 * handles the action. A single right-click usually arrives as an {@link Action#INTERACT_AT} and then an
 * {@link Action#INTERACT}, once for each hand. Thus, a listener that takes over right-clicks should cancel both.
 */
public class InteractEntityEventPayload implements EventPayload, Cancellable {

    private final ServerPlayer player;
    private final Entity entity;
    private final Action action;
    private final @Nullable InteractionHand hand;
    private boolean cancelled = false;

    /**
     * Creates the payload for a player that attacks or right-clicks an entity.
     *
     * @param player the player that acts
     * @param entity the entity that the player acts on
     * @param action the kind of action
     * @param hand the hand used, or {@code null} for {@link Action#ATTACK}
     */
    public InteractEntityEventPayload(ServerPlayer player, Entity entity, Action action, @Nullable InteractionHand hand) {
        this.player = player;
        this.entity = entity;
        this.action = action;
        this.hand = hand;
    }

    /**
     * @return {@link UEvent#PLAYER_INTERACT_ENTITY}
     */
    @Override
    public Key<?> type() {
        return UEvent.PLAYER_INTERACT_ENTITY;
    }

    /**
     * @return the player that acts
     */
    public ServerPlayer player() {
        return this.player;
    }

    /**
     * @return the entity that the player acts on
     */
    public Entity entity() {
        return this.entity;
    }

    /**
     * @return the kind of action
     */
    public Action action() {
        return this.action;
    }

    /**
     * The hand used, or null for {@link Action#ATTACK}.
     */
    public @Nullable InteractionHand hand() {
        return this.hand;
    }

    /**
     * Cancels the event. The entity and other mods then do not handle the action.
     */
    @Override
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * @return {@code true} if the event is cancelled
     */
    @Override
    public boolean cancelled() {
        return this.cancelled;
    }

    /**
     * Lists the kinds of action that a player can do on an entity.
     */
    public enum Action {

        ATTACK,
        INTERACT,
        INTERACT_AT,
        ;
    }
}
