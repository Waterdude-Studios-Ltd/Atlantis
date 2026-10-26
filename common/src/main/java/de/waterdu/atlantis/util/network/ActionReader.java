package de.waterdu.atlantis.util.network;

import de.waterdu.atlantis.mod.impl.common.event.entity.InteractEntityEventPayload;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class ActionReader implements ServerboundInteractPacket.Handler {

    private InteractEntityEventPayload.@Nullable Action action;
    private @Nullable InteractionHand hand;

    public InteractEntityEventPayload.@Nullable Action action() {
        return this.action;
    }

    public @Nullable InteractionHand hand() {
        return this.hand;
    }

    @Override
    public void onInteraction(InteractionHand hand) {
        this.action = InteractEntityEventPayload.Action.INTERACT;
        this.hand = hand;
    }

    @Override
    public void onInteraction(InteractionHand hand, Vec3 location) {
        this.action = InteractEntityEventPayload.Action.INTERACT_AT;
        this.hand = hand;
    }

    @Override
    public void onAttack() {
        this.action = InteractEntityEventPayload.Action.ATTACK;
    }
}
