package de.waterdu.atlantis.util.network;

import de.waterdu.atlantis.mod.impl.common.event.entity.InteractEntityEventPayload;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Reads the action and the hand of a {@link ServerboundInteractPacket}. Pass it to
 * {@link ServerboundInteractPacket#dispatch}, then read the results.
 */
public final class ActionReader implements ServerboundInteractPacket.Handler {

    private InteractEntityEventPayload.@Nullable Action action;
    private @Nullable InteractionHand hand;

    /**
     * @return the action of the packet, or {@code null} if no packet was read
     */
    public InteractEntityEventPayload.@Nullable Action action() {
        return this.action;
    }

    /**
     * @return null for attacks
     */
    public @Nullable InteractionHand hand() {
        return this.hand;
    }

    /**
     * Stores the {@code INTERACT} action and the hand.
     *
     * @param hand the hand that the player uses
     */
    @Override
    public void onInteraction(@NonNull InteractionHand hand) {
        this.action = InteractEntityEventPayload.Action.INTERACT;
        this.hand = hand;
    }

    /**
     * Stores the {@code INTERACT_AT} action and the hand. The location is not stored.
     *
     * @param hand the hand that the player uses
     * @param location the location on the entity that the player interacts with
     */
    @Override
    public void onInteraction(@NonNull InteractionHand hand, @NonNull Vec3 location) {
        this.action = InteractEntityEventPayload.Action.INTERACT_AT;
        this.hand = hand;
    }

    /**
     * Stores the {@code ATTACK} action.
     */
    @Override
    public void onAttack() {
        this.action = InteractEntityEventPayload.Action.ATTACK;
    }
}
